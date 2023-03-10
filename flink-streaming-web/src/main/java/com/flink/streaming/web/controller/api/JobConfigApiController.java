package com.flink.streaming.web.controller.api;

import cn.hutool.core.collection.CollectionUtil;
import com.flink.streaming.common.constant.SystemConstant;
import com.flink.streaming.common.enums.JobTypeEnum;
import com.flink.streaming.common.model.CheckPointParam;
import com.flink.streaming.web.ao.JobConfigAO;
import com.flink.streaming.web.ao.JobServerAO;
import com.flink.streaming.web.common.FlinkConstants;
import com.flink.streaming.web.common.FlinkYarnRestUriConstants;
import com.flink.streaming.web.common.RestResult;
import com.flink.streaming.web.common.SystemConstants;
import com.flink.streaming.web.common.util.CliConfigUtil;
import com.flink.streaming.web.common.util.HttpServiceCheckerUtil;
import com.flink.streaming.web.common.util.HttpUtil;
import com.flink.streaming.web.controller.web.BaseController;
import com.flink.streaming.web.enums.AlarmTypeEnum;
import com.flink.streaming.web.enums.DeployModeEnum;
import com.flink.streaming.web.enums.JobConfigStatus;
import com.flink.streaming.web.enums.SysConfigEnum;
import com.flink.streaming.web.enums.SysErrorEnum;
import com.flink.streaming.web.enums.YN;
import com.flink.streaming.web.exceptions.BizException;
import com.flink.streaming.web.factory.JobServerAOFactory;
import com.flink.streaming.web.model.dto.JobConfigDTO;
import com.flink.streaming.web.model.dto.JobConfigHistoryDTO;
import com.flink.streaming.web.model.dto.PageModel;
import com.flink.streaming.web.model.dto.SavepointBackupDTO;
import com.flink.streaming.web.model.param.JobConfigHisotryParam;
import com.flink.streaming.web.model.param.JobConfigParam;
import com.flink.streaming.web.model.param.UpsertJobConfigParam;
import com.flink.streaming.web.model.vo.DeployFlinkVO;
import com.flink.streaming.web.model.vo.DeployFlinkVO.FlinkTask;
import com.flink.streaming.web.model.vo.PageVO;
import com.flink.streaming.web.service.JobAlarmConfigService;
import com.flink.streaming.web.service.JobConfigHistoryService;
import com.flink.streaming.web.service.JobConfigService;
import com.flink.streaming.web.service.SavepointBackupService;
import com.flink.streaming.web.service.SystemConfigService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.org.yaml.snakeyaml.Yaml;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuhuipei
 * @Description:
 * @date 2020-07-07
 * @time 22:00
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class JobConfigApiController extends BaseController {

  @Autowired
  private JobConfigService jobConfigService;

  @Autowired
  private JobConfigAO jobConfigAO;

  @Autowired
  private JobConfigHistoryService jobConfigHistoryService;

  @Autowired
  private SystemConfigService systemConfigService;

  @Autowired
  private JobAlarmConfigService jobAlarmConfigService;

  @Autowired
  private SavepointBackupService savepointBackupService;

  @RequestMapping("/start")
  public RestResult<String> start(Long id, Long savepointId) {
    try {
      this.getJobServerAO(id).start(id, savepointId, this.getUserName());
    } catch (BizException e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(SysErrorEnum.START_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping("/stop")
  public RestResult<String> stop(Long id) {
    try {
      this.getJobServerAO(id).stop(id, this.getUserName());
    } catch (BizException e) {
      log.warn("???????????? id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(SysErrorEnum.STOP_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping("/close")
  public RestResult<String> close(Long id) {
    try {
      this.getJobServerAO(id).close(id, this.getUserName());
    } catch (BizException e) {
      log.warn("???????????? id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(SysErrorEnum.START_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping("/open")
  public RestResult<String> open(Long id) {
    try {
      this.getJobServerAO(id).open(id, this.getUserName());
    } catch (BizException e) {
      log.warn("???????????? id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(SysErrorEnum.START_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping("/delete")
  public RestResult<String> delete(Long id) {
    try {
      jobConfigService.deleteJobConfigById(id, this.getUserName());
    } catch (BizException e) {
      log.warn("???????????? id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("???????????? id={}", id, e);
      return RestResult.error(SysErrorEnum.START_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping("/savepoint")
  public RestResult<String> savepoint(Long id) {
    try {
      this.getJobServerAO(id).savepoint(id);
    } catch (BizException e) {
      log.warn("savepoint is error id={}", id, e);
      return RestResult.error(e.getCode(), e.getErrorMsg());
    } catch (Exception e) {
      log.error("savepoint is error id={}", id, e);
      return RestResult.error(SysErrorEnum.SAVEPOINT_JOB_FAIL);
    }
    return RestResult.success();
  }

  @RequestMapping(value = "/addConfig", method = {RequestMethod.POST})
  public RestResult addConfig(UpsertJobConfigParam upsertJobConfigParam) {

    try {
      RestResult restResult = checkUpsertJobConfigParam(upsertJobConfigParam);
      if (restResult != null) {
        return restResult;
      }
      jobConfigAO.addJobConfig(UpsertJobConfigParam.toDTO(upsertJobConfigParam));
    } catch (BizException biz) {
      log.warn("addJobConfig is error ", biz);
      return RestResult.error(biz.getErrorMsg());
    } catch (Exception e) {
      log.error("addJobConfig is error", e);
      return RestResult.error(e.getMessage());
    }
    return RestResult.success();
  }

  @RequestMapping(value = "/editConfig", method = {RequestMethod.POST})
  public RestResult editConfig(UpsertJobConfigParam upsertJobConfigParam) {
    try {
      RestResult restResult = checkUpsertJobConfigParam(upsertJobConfigParam);
      if (restResult != null) {
        return restResult;
      }
      JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(upsertJobConfigParam.getId());
      if (jobConfigDTO == null) {
        return RestResult.error("???????????????");
      }

      completeJObConfigDTO(jobConfigDTO);

      if (YN.getYNByValue(jobConfigDTO.getIsOpen()).getCode()) {
        return RestResult.error(SysErrorEnum.JOB_CONFIG_JOB_IS_OPEN.getErrorMsg());
      }
      JobConfigDTO jobConfigNew = UpsertJobConfigParam.toDTO(upsertJobConfigParam);

      jobConfigAO.updateJobConfigById(jobConfigNew);
    } catch (BizException biz) {
      log.warn("updateJobConfigById is error ", biz);
      return RestResult.error(biz.getErrorMsg());
    } catch (Exception e) {
      log.error("updateJobConfigById is error", e);
      return RestResult.error(e.getMessage());
    }
    return RestResult.success();
  }

  @RequestMapping(value = "/copyConfig", method = {RequestMethod.POST})
  public RestResult copyConfig(UpsertJobConfigParam upsertJobConfigParam) {
    try {
      JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(upsertJobConfigParam.getId());
      if (jobConfigDTO == null) {
        return RestResult.error("???????????????????????????");
      }
      /*
       * copy job conf ?????????id??????
       * ???????????????????????????copy_????????????_${jobConfigDTO.getJobName()}?????? ??????????????????????????? ????????????
       * isOpen 0
       */
      jobConfigDTO.setId(null);
      jobConfigDTO.setJobName(
          String
              .format("copy_%s_%s", StringUtils.lowerCase(RandomStringUtils.randomAlphanumeric(4)),
                  jobConfigDTO.getJobName()));
      jobConfigDTO.setStatus(JobConfigStatus.STOP);
      jobConfigDTO.setIsOpen(YN.N.getValue());
      jobConfigDTO.setJobId(null);
      jobConfigDTO.setLastRunLogId(null);
      jobConfigDTO.setVersion(0);
      jobConfigDTO.setLastStartTime(null);
      jobConfigDTO.setLastRunLogId(null);

      jobConfigAO.addJobConfig(jobConfigDTO);

    } catch (BizException biz) {
      log.warn("copyJobConfigById is error ", biz);
      return RestResult.error(biz.getErrorMsg());
    } catch (Exception e) {
      log.error("copyJobConfigById is error", e);
      return RestResult.error(e.getMessage());
    }
    return RestResult.success();
  }

  /**
   * ??????????????????
   *
   * @param jobConfigParam
   * @return
   * @author wxj
   * @date 2021???12???1??? ??????5:09:06
   * @version V1.0
   */
  @RequestMapping(value = "/listTask", method = {RequestMethod.POST})
  public RestResult listTask(ModelMap modelMap, JobConfigParam jobConfigParam) {
    if (jobConfigParam == null) {
      jobConfigParam = new JobConfigParam();
    }
    PageModel<JobConfigDTO> pageModel = jobConfigService.queryJobConfig(jobConfigParam);
    completeJObConfigDTO(pageModel);
    PageVO pageVO = new PageVO();
    pageVO.setPageNum(pageModel.getPageNum());
    pageVO.setPages(pageModel.getPages());
    pageVO.setPageSize(pageModel.getPageSize());
    pageVO.setTotal(pageModel.getTotal());
    pageVO.setData(pageModel);
    return RestResult.success(pageVO);
  }

  /**
   * ??????????????????
   *
   * @param modelMap
   * @param jobConfigParam
   * @return
   * @author wxj
   * @date 2021???12???20??? ??????11:07:22
   * @version V1.0
   */
  @RequestMapping(value = "/jobConfigHistoryPage")
  public RestResult<?> listPage(ModelMap modelMap, JobConfigHisotryParam jobConfigParam) {
    PageModel<JobConfigHistoryDTO> pageModel = jobConfigHistoryService
        .queryJobConfigHistory(jobConfigParam);
    PageVO pageVO = new PageVO();
    pageVO.setPageNum(pageModel.getPageNum());
    pageVO.setPages(pageModel.getPages());
    pageVO.setPageSize(pageModel.getPageSize());
    pageVO.setTotal(pageModel.getTotal());
    pageVO.setData(pageModel);
    return RestResult.success(pageVO);
  }

  /**
   * ????????????????????????
   *
   * @param modelMap
   * @param id
   * @return
   * @author wxj
   * @date 2021???12???20??? ??????2:13:50
   * @version V1.0
   */
  @RequestMapping("/jobConfigHistoryDetail")
  public RestResult<?> detailPage(ModelMap modelMap, Long id) {
    JobConfigHistoryDTO jobConfigHistoryDTO = jobConfigHistoryService.getJobConfigHistoryById(id);
    return RestResult.success(jobConfigHistoryDTO);
  }

  /**
   * ??????????????????????????????Flink????????????
   *
   * @param deployConfigFile ????????????
   * @param deployPath       ?????????????????????
   * @param deployUser       ????????????
   * @param versionDesc      ??????????????????
   * @return
   * @throws Exception
   * @author wxj
   * @date 2021???12???27??? ??????4:11:34
   * @version V1.0
   */
  @RequestMapping("/deployFlinkTask")
  public RestResult<?> deployFlinkTask(String deployConfigFile, String deployPath,
      String deployUser, String versionDesc) throws Exception {
    Yaml yaml = new Yaml();
    try (InputStream in = new FileInputStream(deployConfigFile)) {
      DeployFlinkVO deploy = yaml.loadAs(in, DeployFlinkVO.class);
      int count = 0;
      for (FlinkTask task : deploy.getTaskList()) {
        if (task.getId() == null) {
          continue;
        }
        count++;
        String jobDesc =
            (StringUtils.isBlank(task.getJobDesc()) ? "" : task.getJobDesc() + "-") + versionDesc;
        if (jobDesc.length() > 100) {
          jobDesc = jobDesc.substring(0, 100);
        }
        JobTypeEnum jobType =
            task.getJobType() == null ? JobTypeEnum.SQL_STREAMING : task.getJobType();
        DeployModeEnum deployMode =
            task.getDeployMode() == null ? DeployModeEnum.STANDALONE : task.getDeployMode();
        String flinkSql = StringUtils.isBlank(task.getSqlFile()) ? null
            : this.readTextFile(deployPath + SystemConstant.VIRGULE + task.getSqlFile());
        String alarmTypes = getAlarmTypes(task.getAlarmTypes());
        String flinkRunConfig = task.getFlinkRunConfig();
        UpsertJobConfigParam jobConfigParam = new UpsertJobConfigParam();
        jobConfigParam.setId(task.getId());
        jobConfigParam.setJobType(jobType.getCode());
        jobConfigParam.setJobName(task.getJobName());
        jobConfigParam.setJobDesc(jobDesc);
        jobConfigParam.setDeployMode(deployMode.name());
        jobConfigParam.setFlinkRunConfig(flinkRunConfig);
        jobConfigParam.setFlinkCheckpointConfig(task.getFlinkCheckpointConfig());
        jobConfigParam.setFlinkSql(flinkSql);
        jobConfigParam.setAlarmTypes(alarmTypes);
        jobConfigParam.setExtJarPath(task.getExtJarPath());
        jobConfigParam.setCustomArgs(task.getCustomArgs());
        jobConfigParam.setCustomMainClass(task.getCustomMainClass());
        jobConfigParam.setCustomJarUrl(task.getCustomJarUrl());

        JobConfigDTO job = jobConfigService.getJobConfigByIdContainDelete(task.getId());
        if (job != null) { // ??????
          if (jobType != job.getJobTypeEnum()) {
            throw new BizException("?????????????????????[" + task.getId() + "]??????????????????");
          }
          jobConfigParam.setIsOpen(job.getIsOpen());
          jobConfigParam.setStatus(job.getStatus().getCode());
          JobConfigDTO updateJobConfig = UpsertJobConfigParam.toDTO(jobConfigParam);
          if (job.getIsDeleted() == 1) { // ??????????????????????????????????????????
            jobConfigService.recoveryDeleteJobConfigById(job.getId(), deployUser);
          }
          jobConfigAO.updateJobConfigById(updateJobConfig);
        } else { // ??????
          jobConfigParam.setIsOpen(1);
          jobConfigParam.setStatus(JobConfigStatus.SUCCESS.getCode());
          jobConfigParam.setFlinkRunConfig(flinkRunConfig == null ? "" : flinkRunConfig);
          JobConfigDTO addJobConfig = UpsertJobConfigParam.toDTO(jobConfigParam);
          jobConfigAO.addJobConfig(addJobConfig);
        }
        log.info("[{}]???????????????[{}]{},{}", deployUser, jobConfigParam.getId(),
            jobConfigParam.getJobName(), versionDesc);
      }
      // ??????????????????
      for (FlinkTask task : deploy.getTaskList()) {
        if (task.getId() == null) {
          continue;
        }
        if (task.getDeployStartFlag() != null && !task.getDeployStartFlag()) {
          continue;
        }
        JobConfigDTO job = jobConfigService.getJobConfigById(task.getId());
        if (job.getIsOpen() != 1) {
          continue;
        }
        // ????????????
        if (StringUtils.isNotBlank(job.getJobId())) {
          this.getJobServerAO(job.getId()).stop(job.getId(), deployUser);
        }
        // ?????????????????????SavePoint???????????????
        List<SavepointBackupDTO> savepointBackupDTOList = savepointBackupService
            .lasterHistory10(job.getId());
        Long savepointId = (savepointBackupDTOList != null && savepointBackupDTOList.size() > 0)
            ? savepointBackupDTOList.get(0).getId() : null;
        this.getJobServerAO(job.getId()).start(job.getId(), savepointId, deployUser);
      }
      return RestResult.success("????????????" + count + "????????????");
    } catch (Exception e) {
      log.error("????????????!", e);
      return RestResult.error(e.getMessage());
    }
  }

  private String getAlarmTypes(List<AlarmTypeEnum> list) {
    if (list == null || list.size() == 0) {
      return null;
    }
    String result = "";
    for (AlarmTypeEnum alarmTypeEnum : list) {
      result += (result.length() > 0 ? "," : "") + alarmTypeEnum.getCode();
    }
    return result;
  }

  private String readTextFile(String fileName) {
    try (InputStream fin = new FileInputStream(fileName);) {
      byte[] buffer = new byte[fin.available()];
      fin.read(buffer);
      fin.close();
      String result = new String(buffer, SystemConstants.CODE_UTF_8);
      return result;
    } catch (Exception e) {
      log.error("????????????[" + fileName + "]?????????", e);
    }
    return null;
  }

  //CHECKSTYLE:OFF
  private RestResult checkUpsertJobConfigParam(UpsertJobConfigParam upsertJobConfigParam) {
    if (upsertJobConfigParam == null) {
      return RestResult.error("???????????????");
    }
    if (StringUtils.isEmpty(upsertJobConfigParam.getJobName())) {
      return RestResult.error("?????????????????????");
    }
    if (upsertJobConfigParam.getJobName().length() > 50) {
      return RestResult.error("????????????????????????50?????????");
    }
    if (!upsertJobConfigParam.getJobName().matches("[0-9A-Za-z_]*")) {
      return RestResult.error("???????????????????????????,??????????????????");
    }

    // jar??????????????????
    if (JobTypeEnum.JAR.equals(upsertJobConfigParam.getJobType())) {

      if (StringUtils.isEmpty(upsertJobConfigParam.getCustomMainClass())) {
        return RestResult.error("??????????????????");
      }

      if (StringUtils.isEmpty(upsertJobConfigParam.getCustomJarUrl())) {
        return RestResult.error("??????jar???????????????");
      }
    }
    // sql???????????????????????????JobType=null?????????????????????
    if (JobTypeEnum.SQL_STREAMING.equals(upsertJobConfigParam.getJobType())
        || upsertJobConfigParam.getJobType() == null
        || JobTypeEnum.SQL_STREAMING.getCode() == upsertJobConfigParam.getJobType().intValue()) {
      if (StringUtils.isEmpty(upsertJobConfigParam.getFlinkSql())) {
        return RestResult.error("sql??????????????????");
      }
      if (StringUtils.isNotEmpty(upsertJobConfigParam.getExtJarPath())) {
        String[] urls = upsertJobConfigParam.getExtJarPath().split(SystemConstant.LINE_FEED);
        for (String url : urls) {
          if (StringUtils.isEmpty(url)) {
            continue;
          }
        }
      }
    }
    if (JobTypeEnum.SQL_BATCH.getCode() == upsertJobConfigParam.getJobType() && StringUtils
        .isNotEmpty(upsertJobConfigParam.getCron())) {
      if (!CronExpression.isValidExpression(upsertJobConfigParam.getCron())) {
        return RestResult.error("cron??????????????????");
      }
    }

    if (StringUtils.isNotEmpty(upsertJobConfigParam.getFlinkCheckpointConfig())) {
      CheckPointParam checkPointParam = CliConfigUtil
          .checkFlinkCheckPoint(upsertJobConfigParam.getFlinkCheckpointConfig());
      RestResult restResult = this.checkPointParam(checkPointParam);
      if (restResult != null && !restResult.isSuccess()) {
        return restResult;
      }
    }

    if (DeployModeEnum.YARN_PER.name().equals(upsertJobConfigParam.getDeployMode()) ||
        DeployModeEnum.YARN_APPLICATION.name().equals(upsertJobConfigParam.getDeployMode())) {
      if (StringUtils.isEmpty(upsertJobConfigParam.getFlinkRunConfig())) {
        return RestResult.error("flink????????????????????????");
      }
      if (upsertJobConfigParam.getFlinkRunConfig().contains("-Dyarn.application.name=")) {
        return RestResult.error("???????????????-Dyarn.application.name= ?????? ");
      }
    }

    return null;
  }
//CHECKSTYLE:ON

  /**
   * ??????JobServerAO
   *
   * @author zhuhuipei
   * @date 2020/11/4
   * @time 11:19
   */
  private JobServerAO getJobServerAO(Long id) {
    JobConfigDTO jobConfigDTO = jobConfigService.getJobConfigById(id);
    if (jobConfigDTO == null) {
      throw new BizException(SysErrorEnum.JOB_CONFIG_JOB_IS_NOT_EXIST);
    }
    return JobServerAOFactory.getJobServerAO(jobConfigDTO.getDeployModeEnum());
  }

  private RestResult checkPointParam(CheckPointParam checkPointParam) {
    if (checkPointParam == null) {
      return RestResult.success();
    }
    if (StringUtils.isNotEmpty(checkPointParam.getCheckpointingMode())) {
      if (!(FlinkConstants.EXACTLY_ONCE.equalsIgnoreCase(checkPointParam.getCheckpointingMode())
          || FlinkConstants.AT_LEAST_ONCE
          .equalsIgnoreCase(checkPointParam.getCheckpointingMode()))) {
        return RestResult.error("checkpointingMode ???????????????  AT_LEAST_ONCE ?????? EXACTLY_ONCE");
      }
    }
    if (StringUtils.isNotEmpty(checkPointParam.getExternalizedCheckpointCleanup())) {
      if (!(FlinkConstants.DELETE_ON_CANCELLATION
          .equalsIgnoreCase(checkPointParam.getExternalizedCheckpointCleanup())
          || FlinkConstants.RETAIN_ON_CANCELLATION
          .equalsIgnoreCase(checkPointParam.getExternalizedCheckpointCleanup()))) {
        return RestResult.error(
            "externalizedCheckpointCleanup ???????????????DELETE_ON_CANCELLATION ?????? RETAIN_ON_CANCELLATION");
      }
    }
    return RestResult.success();
  }


  /**
   * ??????????????????
   *
   * @author wxj
   * @date 2021???12???21??? ??????5:01:47
   * @version V1.0
   */
  private void completeJObConfigDTO(JobConfigDTO jobConfigDTO) {
    Map<DeployModeEnum, String> domainKey = new HashMap<>();
    domainKey.put(DeployModeEnum.YARN_PER,
        systemConfigService.getSystemConfigByKey(SysConfigEnum.YARN_RM_HTTP_ADDRESS.getKey()));
    domainKey.put(DeployModeEnum.YARN_APPLICATION,
        systemConfigService.getSystemConfigByKey(SysConfigEnum.YARN_RM_HTTP_ADDRESS.getKey()));
    domainKey.put(DeployModeEnum.LOCAL,
        systemConfigService.getSystemConfigByKey(SysConfigEnum.FLINK_REST_HTTP_ADDRESS.getKey()));
    domainKey.put(DeployModeEnum.STANDALONE, systemConfigService
        .getSystemConfigByKey(SysConfigEnum.FLINK_REST_HA_HTTP_ADDRESS.getKey()));
    // ??????FlinkRunUrl??????
    String domain = domainKey.get(jobConfigDTO.getDeployModeEnum());
    if (StringUtils.isNotEmpty(domain)) {
      if ((DeployModeEnum.YARN_PER.equals(jobConfigDTO.getDeployModeEnum())
          || DeployModeEnum.YARN_APPLICATION.equals(jobConfigDTO.getDeployModeEnum()))
          && !StringUtils
          .isEmpty(jobConfigDTO.getJobId())) {
        jobConfigDTO.setFlinkRunUrl(HttpUtil.buildUrl(domain,
            FlinkYarnRestUriConstants.getUriOverviewForYarn(jobConfigDTO.getJobId())));
      }
      if (DeployModeEnum.LOCAL.equals(jobConfigDTO.getDeployModeEnum()) && !StringUtils
          .isEmpty(jobConfigDTO.getJobId())) {
        jobConfigDTO.setFlinkRunUrl(domain + String
            .format(FlinkYarnRestUriConstants.URI_YARN_JOB_OVERVIEW, jobConfigDTO.getJobId()));
      }
      if (DeployModeEnum.STANDALONE.equals(jobConfigDTO.getDeployModeEnum()) && !StringUtils
          .isEmpty(jobConfigDTO.getJobId())) {
        String[] urls = domain.split(SystemConstant.SEMICOLON);
        for (String url : urls) {
          if (HttpServiceCheckerUtil.checkUrlConnect(url)) {
            jobConfigDTO.setFlinkRunUrl(url.trim() + String
                .format(FlinkYarnRestUriConstants.URI_YARN_JOB_OVERVIEW, jobConfigDTO.getJobId()));
            break;
          }
        }
      }
    }
    // ??????AlarmStrs??????
    List<AlarmTypeEnum> list = jobAlarmConfigService.findByJobId(jobConfigDTO.getId());
    if (CollectionUtil.isNotEmpty(list)) {
      List<Integer> alarmTypes = new ArrayList<Integer>();
      StringBuilder str = new StringBuilder("[");
      for (AlarmTypeEnum alarmTypeEnum : list) {
        alarmTypes.add(alarmTypeEnum.getCode());
        if (str.length() > 1) {
          str.append(" ");
        }
        switch (alarmTypeEnum) {
          case DINGDING:
            str.append("??????");
            break;
          case CALLBACK_URL:
            str.append("??????");
            break;
          case AUTO_START_JOB:
            str.append("????????????");
            break;
          default:
        }
      }
      str.append("]");
      jobConfigDTO.setAlarmStrs(str.toString());
      jobConfigDTO.setAlarmTypes(alarmTypes);
      jobConfigDTO.setAlarmTypeEnumList(list);
    }
  }

  /**
   * ??????????????????
   *
   * @param pageModel
   * @author wxj
   * @date 2021???12???21??? ??????5:01:47
   * @version V1.0
   */
  private void completeJObConfigDTO(PageModel<JobConfigDTO> pageModel) {
    if (pageModel == null || pageModel.size() == 0) {
      return;
    }
    List<Long> jobIdList = pageModel.stream().map(jobConfigVO -> jobConfigVO.getId())
        .collect(Collectors.toList());
    Map<Long, List<AlarmTypeEnum>> map = jobAlarmConfigService.findByJobIdList(jobIdList);

    for (JobConfigDTO jobConfigDTO : pageModel) {
      // ??????FlinkRunUrl??????
      String domain = systemConfigService.getFlinkUrl(jobConfigDTO.getDeployModeEnum());

      if (StringUtils.isNotEmpty(domain)) {
        if ((DeployModeEnum.YARN_PER.equals(jobConfigDTO.getDeployModeEnum())
            || DeployModeEnum.YARN_APPLICATION.equals(jobConfigDTO.getDeployModeEnum())
            && !StringUtils.isEmpty(jobConfigDTO.getJobId()))) {
          jobConfigDTO.setFlinkRunUrl(HttpUtil.buildUrl(domain,
              FlinkYarnRestUriConstants.getUriOverviewForYarn(jobConfigDTO.getJobId())));
        }
        if ((DeployModeEnum.LOCAL.equals(jobConfigDTO.getDeployModeEnum())
            || DeployModeEnum.STANDALONE.equals(jobConfigDTO.getDeployModeEnum()))
            && !StringUtils.isEmpty(jobConfigDTO.getJobId())) {
          jobConfigDTO.setFlinkRunUrl(domain + String
              .format(FlinkYarnRestUriConstants.URI_YARN_JOB_OVERVIEW, jobConfigDTO.getJobId()));
        }
      }
      // ??????AlarmStrs??????
      List<AlarmTypeEnum> list = map.get(jobConfigDTO.getId());
      if (CollectionUtil.isNotEmpty(list)) {
        List<Integer> alarmTypes = new ArrayList<Integer>();
        StringBuilder str = new StringBuilder("[");
        for (AlarmTypeEnum alarmTypeEnum : list) {
          alarmTypes.add(alarmTypeEnum.getCode());
          if (str.length() > 1) {
            str.append(" ");
          }
          switch (alarmTypeEnum) {
            case DINGDING:
              str.append("??????");
              break;
            case CALLBACK_URL:
              str.append("??????");
              break;
            case AUTO_START_JOB:
              str.append("????????????");
              break;
            default:
          }
        }
        str.append("]");
        jobConfigDTO.setAlarmStrs(str.toString());
        jobConfigDTO.setAlarmTypes(alarmTypes);
        jobConfigDTO.setAlarmTypeEnumList(list);
      }
    }
  }

}
