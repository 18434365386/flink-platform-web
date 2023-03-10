package com.flink.streaming.web.ao.impl;

import com.flink.streaming.common.enums.JobTypeEnum;
import com.flink.streaming.web.ao.AlarmServiceAO;
import com.flink.streaming.web.ao.DingDingService;
import com.flink.streaming.web.ao.JobServerAO;
import com.flink.streaming.web.ao.TaskServiceAO;
import com.flink.streaming.web.common.SystemConstants;
import com.flink.streaming.web.common.util.YarnUtil;
import com.flink.streaming.web.config.AlarmPoolConfig;
import com.flink.streaming.web.config.SavePointThreadPool;
import com.flink.streaming.web.enums.*;
import com.flink.streaming.web.exceptions.BizException;
import com.flink.streaming.web.model.dto.JobConfigDTO;
import com.flink.streaming.web.model.vo.CallbackDTO;
import com.flink.streaming.web.rpc.FlinkRestRpcAdapter;
import com.flink.streaming.web.rpc.YarnRestRpcAdapter;
import com.flink.streaming.web.rpc.model.JobInfo;
import com.flink.streaming.web.rpc.model.JobStandaloneInfo;
import com.flink.streaming.web.service.JobAlarmConfigService;
import com.flink.streaming.web.service.JobConfigService;
import com.flink.streaming.web.service.SystemConfigService;
import com.flink.streaming.web.thread.AlarmDingdingThread;
import com.flink.streaming.web.thread.AlarmHttpThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zhuhuipei
 * @Description:
 * @date 2020-09-22
 * @time 19:59
 */
@Component
@Slf4j
public class TaskServiceAOImpl implements TaskServiceAO {

  @Autowired
  private JobConfigService jobConfigService;

  @Autowired
  private FlinkRestRpcAdapter flinkRestRpcAdapter;

  @Autowired
  private YarnRestRpcAdapter yarnRestRpcAdapter;

  @Autowired
  private AlarmServiceAO alarmServiceAO;

  @Autowired
  private JobServerAO jobYarnServerAO;

  @Autowired
  private JobServerAO jobStandaloneServerAO;

  @Autowired
  private SystemConfigService systemConfigService;

  @Autowired
  private JobAlarmConfigService jobAlarmConfigService;

  @Autowired
  private DingDingService dingDingService;

  private ThreadPoolExecutor threadPoolExecutor = AlarmPoolConfig.getInstance()
      .getThreadPoolExecutor();

  @Override
  public void checkJobStatus() {
    List<JobConfigDTO> jobConfigDTOList = jobConfigService
        .findJobConfigByStatus(JobConfigStatus.RUN.getCode());
    if (CollectionUtils.isEmpty(jobConfigDTOList)) {
      log.warn("????????????????????????????????????");
      return;
    }
    for (JobConfigDTO jobConfigDTO : jobConfigDTOList) {
      if (JobTypeEnum.SQL_BATCH.equals(jobConfigDTO.getJobTypeEnum())) {
        log.warn("??????????????????????????????");
        return;
      }
      List<AlarmTypeEnum> alarmTypeEnumList = jobAlarmConfigService
          .findByJobId(jobConfigDTO.getId());
      switch (jobConfigDTO.getDeployModeEnum()) {
        case YARN_PER:
        case YARN_APPLICATION:
          this.checkYarn(jobConfigDTO, alarmTypeEnumList);
          this.sleep();
          break;
        case LOCAL:
        case STANDALONE:
          this.checkStandalone(jobConfigDTO, alarmTypeEnumList);
          this.sleep();
          break;
        default:
          break;
      }
    }
  }

  @Override
  @Deprecated
  public void checkYarnJobByStop() {
    List<JobConfigDTO> jobConfigDTOList = jobConfigService
        .findJobConfigByStatus(JobConfigStatus.STOP.getCode());
    if (CollectionUtils.isEmpty(jobConfigDTOList)) {
      return;
    }
    for (JobConfigDTO jobConfigDTO : jobConfigDTOList) {
      if (jobConfigDTO.getIsOpen().intValue() == YN.N.getValue()) {
        continue;
      }
      if (JobTypeEnum.SQL_BATCH.equals(jobConfigDTO.getJobTypeEnum())) {
        log.warn("??????????????????????????????");
        return;
      }
      switch (jobConfigDTO.getDeployModeEnum()) {
        case YARN_PER:
        case YARN_APPLICATION:
          String appId = null;
          try {
            String queueName = YarnUtil.getQueueName(jobConfigDTO.getFlinkRunConfig());
            if (StringUtils.isEmpty(queueName)) {
              continue;
            }
            log.info("check job getJobName={} queueName={}", jobConfigDTO.getJobName(), queueName);
            appId = yarnRestRpcAdapter.getAppIdByYarn(jobConfigDTO.getJobName(), queueName);
          } catch (BizException be) {
            if (SysErrorEnum.YARN_CODE.getCode().equals(be.getCode())) {
              continue;
            }
            log.error("[BizException]getAppIdByYarn  is error ", be);
          } catch (Exception e) {
            log.error("[Exception]getAppIdByYarn is error ", e);
            continue;
          }
          if (!StringUtils.isEmpty(appId)) {
            JobInfo jobInfo = yarnRestRpcAdapter.getJobInfoForPerYarnByAppId(appId);
            if (jobInfo != null && SystemConstants.STATUS_RUNNING.equals(jobInfo.getStatus())) {
              log.warn("?????????????????? jobYarnInfo={} id={}", jobInfo, appId);
              yarnRestRpcAdapter.cancelJobForYarnByAppId(appId, jobInfo.getId());
            }
          }
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void autoSavePoint() {
    List<JobConfigDTO> jobConfigDTOList = jobConfigService
        .findJobConfigByStatus(JobConfigStatus.RUN.getCode());
    if (CollectionUtils.isEmpty(jobConfigDTOList)) {
      log.warn("autoSavePoint is null  ?????????????????????????????? ");
      return;
    }
    for (JobConfigDTO jobConfigDTO : jobConfigDTOList) {

      //sql???jar ??????????????????SavePoint
      if (JobTypeEnum.SQL_STREAMING.equals(jobConfigDTO.getJobTypeEnum())
          || JobTypeEnum.JAR.equals(jobConfigDTO.getJobTypeEnum())) {
        SavePointThreadPool.getInstance().getThreadPoolExecutor()
            .execute(new SavePoint(jobConfigDTO));
        sleep();
      }


    }
  }

  /**
   * ??????SavePoint
   */
  class SavePoint implements Runnable {

    private JobConfigDTO jobConfigDTO;

    SavePoint(JobConfigDTO jobConfigDTO) {
      this.jobConfigDTO = jobConfigDTO;
    }

    @Override
    public void run() {
      try {
        switch (jobConfigDTO.getDeployModeEnum()) {
          case YARN_PER:
          case YARN_APPLICATION:
            jobYarnServerAO.savepoint(jobConfigDTO.getId());
            break;
          case LOCAL:
          case STANDALONE:
            jobStandaloneServerAO.savepoint(jobConfigDTO.getId());
            break;
          default:
        }

      } catch (Exception e) {
        log.error("??????savepoint ??????", e);
      }
    }
  }


  private void sleep() {
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
    }
  }

  private void checkYarn(JobConfigDTO jobConfigDTO, List<AlarmTypeEnum> alarmTypeEnumList) {
    if (StringUtils.isEmpty(jobConfigDTO.getJobId())) {
      log.error("??????[{}]???????????????", jobConfigDTO.getId());
      return;
    }
    //??????????????????
    JobInfo jobInfo = yarnRestRpcAdapter.getJobInfoForPerYarnByAppId(jobConfigDTO.getJobId());
    if (jobInfo != null && (SystemConstants.STATUS_RUNNING.equals(jobInfo.getStatus())
        || SystemConstants.STATUS_INITIALIZING.equals(jobInfo.getStatus())
        || SystemConstants.STATUS_SCHEDULED.equals(jobInfo.getStatus()))) {
      return;
    }
    log.info("jobInfo={}", jobInfo);
    //??????????????????
    log.error("??????????????????[{}]?????????yarn???????????????????????????????????????web??????????????? {}",
        jobConfigDTO.getId(), jobConfigDTO);
    JobConfigDTO jobConfig = JobConfigDTO.bulidStop(jobConfigDTO.getId());
    jobConfigService.updateJobConfigById(jobConfig);
    //????????????????????????????????????
    this.alermAndAutoJob(alarmTypeEnumList,
        SystemConstants.buildDingdingMessage("???????????????[" + jobConfigDTO.getId()
            + "]??????????????????????????????" + jobConfigDTO.getJobName()), jobConfigDTO,
        jobConfigDTO.getDeployModeEnum());
  }

  private void checkStandalone(JobConfigDTO jobConfigDTO, List<AlarmTypeEnum> alarmTypeEnumList) {
    if (StringUtils.isEmpty(jobConfigDTO.getJobId())) {
      String message = SystemConstants.buildDingdingMessage(
          "???????????????jobId????????????[" + jobConfigDTO.getId() + "]?????????" + jobConfigDTO.getJobName());
      log.error(message);
      return;
    }
    //??????????????????
    JobStandaloneInfo jobStandaloneInfo = flinkRestRpcAdapter
        .getJobInfoForStandaloneByAppId(jobConfigDTO.getJobId(), jobConfigDTO.getDeployModeEnum());
    if (jobStandaloneInfo != null && SystemConstants.STATUS_RUNNING
        .equals(jobStandaloneInfo.getState())) {
      return;
    }
    //??????????????????
    log.error("??????????????????[{}]?????????yarn????????????????????????????????????????????????jobStandaloneInfo={}", jobConfigDTO.getId(),
        jobStandaloneInfo);
    JobConfigDTO jobConfig = JobConfigDTO.bulidStop(jobConfigDTO.getId());
    jobConfigService.updateJobConfigById(jobConfig);
    //????????????????????????????????????
    this.alermAndAutoJob(alarmTypeEnumList,
        SystemConstants.buildDingdingMessage("???????????????[" + jobConfigDTO.getId()
            + "]??????????????????????????????" + jobConfigDTO.getJobName()), jobConfigDTO,
        DeployModeEnum.STANDALONE);
  }

  /**
   * ??????????????????????????? //TODO ???????????????????????????????????????
   *
   * @author zhuhuipei
   * @date 2021/2/28
   * @time 19:50
   */
  private void alermAndAutoJob(List<AlarmTypeEnum> alarmTypeEnumList, String cusContent,
      JobConfigDTO jobConfigDTO, DeployModeEnum deployModeEnum) {
    threadPoolExecutor.execute(new Runnable() {
      @Override
      public void run() {
        dingDingService.doAlarmNotify(cusContent, jobConfigDTO, deployModeEnum);
      }
    });
    if (CollectionUtils.isEmpty(alarmTypeEnumList)) {
      log.warn("??????[{}-{}]??????????????????????????????????????????????????????????????????????????????", jobConfigDTO.getId(),
          jobConfigDTO.getJobName());
      return;
    }
    CallbackDTO callbackDTO = CallbackDTO.to(jobConfigDTO);
    //??????
    for (AlarmTypeEnum alarmTypeEnum : alarmTypeEnumList) {
      switch (alarmTypeEnum) {
        case DINGDING:
          this.dingdingAlarm(cusContent, callbackDTO.getJobConfigId());
          break;
        case CALLBACK_URL:
          this.httpAlarm(callbackDTO);
          break;
        default:
      }
    }
    //????????????
    if (alarmTypeEnumList.contains(AlarmTypeEnum.AUTO_START_JOB)) {
      log.info("?????????????????????,?????????????????? JobConfigId={}", callbackDTO.getJobConfigId());
      try {
        switch (deployModeEnum) {
          case YARN_PER:
          case YARN_APPLICATION:
            jobYarnServerAO
                .start(callbackDTO.getJobConfigId(), null, SystemConstants.USER_NAME_TASK_AUTO);
            break;
          case STANDALONE:
            jobStandaloneServerAO
                .start(callbackDTO.getJobConfigId(), null, SystemConstants.USER_NAME_TASK_AUTO);
            break;
          default:
        }
      } catch (Exception e) {
        log.error("???????????????????????? JobConfigId={}", callbackDTO.getJobConfigId(), e);
      }
    }
  }

  /**
   * ????????????
   *
   * @author zhuhuipei
   * @date 2021/2/28
   * @time 19:56
   */
  private void dingdingAlarm(String content, Long jobConfigId) {
    String alartUrl = systemConfigService
        .getSystemConfigByKey(SysConfigEnum.DINGDING_ALARM_URL.getKey());
    if (StringUtils.isEmpty(alartUrl)) {
      log.warn("##### ????????????url?????????????????????[{}]???????????? #####", jobConfigId);
      return;
    }
    threadPoolExecutor
        .execute(new AlarmDingdingThread(alarmServiceAO, content, jobConfigId, alartUrl));
  }

  /**
   * ???????????????????????????
   *
   * @author zhuhuipei
   * @date 2021/2/28
   * @time 19:56
   */
  private void httpAlarm(CallbackDTO callbackDTO) {
    String alartUrl = systemConfigService
        .getSystemConfigByKey(SysConfigEnum.CALLBACK_ALARM_URL.getKey());
    if (StringUtils.isEmpty(alartUrl)) {
      log.warn("##### ????????????url?????????????????????[{}]???????????? #####", callbackDTO.getJobConfigId());
      return;
    }
    threadPoolExecutor.execute(new AlarmHttpThread(alarmServiceAO, callbackDTO, alartUrl));
  }
}
