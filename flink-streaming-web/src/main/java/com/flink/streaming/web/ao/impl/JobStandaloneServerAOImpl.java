package com.flink.streaming.web.ao.impl;

import com.flink.streaming.common.enums.JobTypeEnum;
import com.flink.streaming.web.ao.JobBaseServiceAO;
import com.flink.streaming.web.ao.JobServerAO;
import com.flink.streaming.web.common.MessageConstants;
import com.flink.streaming.web.common.SystemConstants;
import com.flink.streaming.web.enums.DeployModeEnum;
import com.flink.streaming.web.enums.JobConfigStatus;
import com.flink.streaming.web.enums.SysConfigEnum;
import com.flink.streaming.web.enums.SysErrorEnum;
import com.flink.streaming.web.enums.YN;
import com.flink.streaming.web.exceptions.BizException;
import com.flink.streaming.web.model.dto.JobConfigDTO;
import com.flink.streaming.web.model.dto.JobRunParamDTO;
import com.flink.streaming.web.model.entity.BatchJob;
import com.flink.streaming.web.quartz.BatchJobManagerScheduler;
import com.flink.streaming.web.rpc.CommandRpcClinetAdapter;
import com.flink.streaming.web.rpc.FlinkRestRpcAdapter;
import com.flink.streaming.web.rpc.model.JobStandaloneInfo;
import com.flink.streaming.web.service.JobConfigService;
import com.flink.streaming.web.service.SavepointBackupService;
import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuhuipei
 * @Description:
 * @date 2020-07-20
 * @time 23:11
 */
@Component(SystemConstants.BEANNAME_JOBSTANDALONESERVERAO)
@Slf4j
public class JobStandaloneServerAOImpl implements JobServerAO {


  @Autowired
  private JobConfigService jobConfigService;

  @Autowired
  private SavepointBackupService savepointBackupService;

  @Autowired
  private CommandRpcClinetAdapter commandRpcClinetAdapter;

  @Autowired
  private FlinkRestRpcAdapter flinkRestRpcAdapter;

  @Autowired
  private JobBaseServiceAO jobBaseServiceAO;

  @Autowired
  private BatchJobManagerScheduler batchJobRegister;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void start(Long id, Long savepointId, String userName) {

    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);

    if (StringUtils.isNotBlank(jobConfigDTO.getJobId())) {
      if (!jobConfigDTO.getJobTypeEnum().equals(JobTypeEnum.SQL_BATCH)) {
        JobStandaloneInfo jobStatus = flinkRestRpcAdapter
            .getJobInfoForStandaloneByAppId(jobConfigDTO.getJobId(),
                jobConfigDTO.getDeployModeEnum());
        if (StringUtils.isNotBlank(jobStatus.getState()) && SystemConstants.STATUS_RUNNING
            .equalsIgnoreCase(jobStatus.getState())) {
          throw new BizException(
              "?????????Flink?????????????????????ID=[" + jobConfigDTO.getJobId() + "]??????[ " + jobStatus.getState()
                  + "]????????????????????????????????????");
        }
      }

    }

    //1?????????jobConfigDTO ???????????????
    jobBaseServiceAO.checkStart(jobConfigDTO);

    // TODO ??????????????????????????????????????????

    //2???????????????sql ????????????????????????????????????????????????
    JobRunParamDTO jobRunParamDTO = jobBaseServiceAO.writeSqlToFile(jobConfigDTO);

    //3??????????????????????????????
    Long jobRunLogId = jobBaseServiceAO.insertJobRunLog(jobConfigDTO, userName);

    //4???????????????????????????????????????????????? ???????????? ??????????????????
    jobConfigService.updateStatusByStart(jobConfigDTO.getId(), userName, jobRunLogId,
        jobConfigDTO.getVersion());

    String savepointPath = savepointBackupService.getSavepointPathById(id, savepointId);

    //??????????????????
    jobBaseServiceAO.aSyncExecJob(jobRunParamDTO, jobConfigDTO, jobRunLogId, savepointPath);

  }


  @Override
  public void stop(Long id, String userName) {
    log.info("[{}]??????????????????[{}]", userName, id);
    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);
    if (jobConfigDTO == null) {
      throw new BizException(SysErrorEnum.JOB_CONFIG_JOB_IS_NOT_EXIST);
    }
    JobStandaloneInfo jobStandaloneInfo = flinkRestRpcAdapter
        .getJobInfoForStandaloneByAppId(jobConfigDTO.getJobId(), jobConfigDTO.getDeployModeEnum());
    log.info("??????[{}]??????????????????{}", id, jobStandaloneInfo);
    if (jobStandaloneInfo == null || StringUtils.isNotEmpty(jobStandaloneInfo.getErrors())) {
      log.warn("??????????????????[{}]???getJobInfoForStandaloneByAppId is error jobStandaloneInfo={}", id,
          jobStandaloneInfo);
    } else {
      // ????????????savepoint
      if (StringUtils.isNotBlank(jobConfigDTO.getFlinkCheckpointConfig())
          && jobConfigDTO.getJobTypeEnum() != JobTypeEnum.SQL_BATCH
          && SystemConstants.STATUS_RUNNING.equals(jobStandaloneInfo.getState())) {
        log.info("??????????????????[{}]?????????-savepoint", id);
        this.savepoint(id);
      }
      //????????????
      if (SystemConstants.STATUS_RUNNING.equals(jobStandaloneInfo.getState())
          || SystemConstants.STATUS_RESTARTING.equals(jobStandaloneInfo.getState())) {
        flinkRestRpcAdapter
            .cancelJobForFlinkByAppId(jobConfigDTO.getJobId(), jobConfigDTO.getDeployModeEnum());
      }
    }
    JobConfigDTO jobConfig = new JobConfigDTO();
    jobConfig.setStatus(JobConfigStatus.STOP);
    jobConfig.setEditor(userName);
    jobConfig.setId(id);
    jobConfig.setJobId("");
    //????????????
    jobConfigService.updateJobConfigById(jobConfig);
  }

  @Override
  public void savepoint(Long id) {
    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);

    jobBaseServiceAO.checkSavepoint(jobConfigDTO);

    JobStandaloneInfo jobStandaloneInfo = flinkRestRpcAdapter
        .getJobInfoForStandaloneByAppId(jobConfigDTO.getJobId(), jobConfigDTO.getDeployModeEnum());
    if (jobStandaloneInfo == null || StringUtils.isNotEmpty(jobStandaloneInfo.getErrors())
        || !SystemConstants.STATUS_RUNNING.equals(jobStandaloneInfo.getState())) {
      log.warn(MessageConstants.MESSAGE_007, jobConfigDTO.getJobName());
      throw new BizException(MessageConstants.MESSAGE_007);
    }

    //1??? ??????savepoint
    try {
      //yarn??????????????????????????????????????????hdfs:///flink/savepoint/flink-streaming-platform-web/
      //LOCAL??????????????????????????????flink????????????
      String targetDirectory = SystemConstants.DEFAULT_SAVEPOINT_ROOT_PATH + id;
      if (DeployModeEnum.LOCAL.equals(jobConfigDTO.getDeployModeEnum())) {
        targetDirectory = "savepoint/" + id;
      }

      commandRpcClinetAdapter.savepointForPerCluster(jobConfigDTO.getJobId(), targetDirectory);
    } catch (Exception e) {
      log.error(MessageConstants.MESSAGE_008, e);
      throw new BizException(MessageConstants.MESSAGE_008);
    }

    String savepointPath = flinkRestRpcAdapter.savepointPath(jobConfigDTO.getJobId(),
        jobConfigDTO.getDeployModeEnum());
    if (StringUtils.isEmpty(savepointPath)) {
      log.warn(MessageConstants.MESSAGE_009, jobConfigDTO);
      throw new BizException(MessageConstants.MESSAGE_009);
    }
    //2??? ????????????Savepoint??????????????????
    savepointBackupService.insertSavepoint(id, savepointPath, new Date());
  }


  @Override
  @Transactional(rollbackFor = Exception.class)
  public void open(Long id, String userName) {
    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);
    if (jobConfigDTO == null) {
      return;
    }
    if (jobConfigDTO.getJobTypeEnum() == JobTypeEnum.SQL_BATCH && StringUtils
        .isNotEmpty(jobConfigDTO.getCron())) {
      batchJobRegister
          .registerJob(new BatchJob(id, jobConfigDTO.getJobName(), jobConfigDTO.getCron()));
    }

    jobConfigService.openOrClose(id, YN.Y, userName);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void close(Long id, String userName) {
    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);
    jobBaseServiceAO.checkClose(jobConfigDTO);
    jobConfigService.openOrClose(id, YN.N, userName);
    if (jobConfigDTO.getJobTypeEnum() == JobTypeEnum.SQL_BATCH) {
      batchJobRegister.deleteJob(id);

    }

  }


  private void checkSysConfig(Map<String, String> systemConfigMap, DeployModeEnum deployModeEnum) {
    if (systemConfigMap == null) {
      throw new BizException(SysErrorEnum.SYSTEM_CONFIG_IS_NULL);
    }
    if (!systemConfigMap.containsKey(SysConfigEnum.FLINK_HOME.getKey())) {
      throw new BizException(SysErrorEnum.SYSTEM_CONFIG_IS_NULL_FLINK_HOME);
    }

    if (DeployModeEnum.LOCAL == deployModeEnum
        && !systemConfigMap.containsKey(SysConfigEnum.FLINK_REST_HTTP_ADDRESS.getKey())) {
      throw new BizException(SysErrorEnum.SYSTEM_CONFIG_IS_NULL_FLINK_REST_HTTP_ADDRESS);
    }

    if (DeployModeEnum.STANDALONE == deployModeEnum
        && !systemConfigMap.containsKey(SysConfigEnum.FLINK_REST_HA_HTTP_ADDRESS.getKey())) {
      throw new BizException(SysErrorEnum.SYSTEM_CONFIG_IS_NULL_FLINK_REST_HA_HTTP_ADDRESS);
    }
  }
}
