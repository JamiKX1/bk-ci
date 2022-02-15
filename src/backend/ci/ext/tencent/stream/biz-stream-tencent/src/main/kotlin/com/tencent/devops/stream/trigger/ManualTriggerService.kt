/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind.Companion.OBJECT_KIND_OPENAPI
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.parsers.triggerParameter.TriggerParameter
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.trigger.v1.YamlBuild
import com.tencent.devops.stream.trigger.v2.StreamYamlBuild
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList")
class ManualTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val gitCIEventService: GitCIEventService,
    private val yamlBuild: YamlBuild,
    private val yamlBuildV2: StreamYamlBuild,
    private val streamYamlService: StreamYamlService,
    private val triggerExceptionService: TriggerExceptionService,
    private val triggerMatcher: TriggerMatcher,
    private val gitCISettingDao: StreamBasicSettingDao,
    private val triggerParameter: TriggerParameter,
    private val objectMapper: ObjectMapper
) {

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(ManualTriggerService::class.java)
    }

    fun triggerBuild(userId: String, pipelineId: String, triggerBuildReq: TriggerBuildReq): TriggerBuildResult {
        logger.info("Trigger build, userId: $userId, pipeline: $pipelineId, triggerBuildReq: $triggerBuildReq")

        // open api模拟工蜂事件触发
        val gitRequestEvent = if (!triggerBuildReq.payload.isNullOrEmpty()) {
            mockWebhookTrigger(triggerBuildReq)
        } else {
            GitRequestEventHandle.createManualTriggerEvent(userId, triggerBuildReq)
        }
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        val existsPipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, triggerBuildReq.gitProjectId, pipelineId)
                ?: throw OperationException("stream pipeline: $pipelineId is not exist")
        // 如果该流水线已保存过，则继续使用
        val buildPipeline = GitProjectPipeline(
            gitProjectId = existsPipeline.gitProjectId,
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            latestBuildInfo = null,
            latestBuildBranch = null
        )

        // 流水线未启用在手动触发处直接报错
        if (!buildPipeline.enabled) {
            throw CustomException(
                status = Response.Status.METHOD_NOT_ALLOWED,
                message = "${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})"
            )
        }

        val gitCIBasicSetting =
            gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                message = TriggerReason.CI_DISABLED.detail
            )

        val originYaml = triggerBuildReq.yaml
        // 如果当前文件没有内容直接不触发
        if (originYaml.isNullOrBlank()) {
            logger.warn("Matcher is false, event: ${gitRequestEvent.id} yaml is null")
            gitCIEventService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = buildPipeline.pipelineId.ifBlank { null },
                pipelineName = buildPipeline.displayName,
                filePath = buildPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_CONTENT_NULL.name,
                reasonDetail = TriggerReason.CI_YAML_CONTENT_NULL.detail,
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = gitRequestEvent.branch
            )
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.CI_YAML_CONTENT_NULL.name +
                        "(${TriggerReason.CI_YAML_CONTENT_NULL.detail.format("")})"
            )
        }

        if (!ScriptYmlUtils.isV2Version(originYaml)) {
            val (yamlObject, normalizedYaml) =
                prepareCIBuildYaml(
                    gitRequestEvent = gitRequestEvent,
                    originYaml = originYaml,
                    filePath = existsPipeline.filePath,
                    pipelineId = existsPipeline.pipelineId,
                    pipelineName = existsPipeline.displayName
                ) ?: throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = TriggerReason.CI_YAML_INVALID.name +
                        "(${TriggerReason.CI_YAML_INVALID.detail.format("prepare error")})"
                )

            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml,
                parsedYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                commitMsg = triggerBuildReq.customCommitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING,
                version = null
            )
            val result = yamlBuild.gitStartBuild(
                pipeline = buildPipeline,
                event = gitRequestEvent,
                yaml = yamlObject,
                gitBuildId = gitBuildId
            ) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.PIPELINE_RUN_ERROR.name +
                    "(${TriggerReason.PIPELINE_RUN_ERROR.detail})"
            )
            return TriggerBuildResult(
                projectId = triggerBuildReq.gitProjectId,
                branch = triggerBuildReq.branch,
                customCommitMsg = triggerBuildReq.customCommitMsg,
                description = triggerBuildReq.description,
                commitId = triggerBuildReq.commitId,
                buildId = result.id,
                buildUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                    homePage = v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                    gitProjectId = buildPipeline.gitProjectId,
                    pipelineId = pipelineId,
                    buildId = result.id
                )
            )
        } else {
            val result = handleTrigger(
                gitRequestEvent = gitRequestEvent,
                originYaml = originYaml,
                buildPipeline = buildPipeline,
                triggerBuildReq = triggerBuildReq,
                gitCIBasicSetting = gitCIBasicSetting
            ) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.PIPELINE_RUN_ERROR.name +
                    "(${TriggerReason.PIPELINE_RUN_ERROR.detail})"
            )
            return TriggerBuildResult(
                projectId = triggerBuildReq.gitProjectId,
                branch = triggerBuildReq.branch,
                customCommitMsg = triggerBuildReq.customCommitMsg,
                description = triggerBuildReq.description,
                commitId = triggerBuildReq.commitId,
                buildId = result.id,
                buildUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                    homePage = v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                    gitProjectId = buildPipeline.gitProjectId,
                    pipelineId = pipelineId,
                    buildId = result.id
                )
            )
        }
    }

    private fun mockWebhookTrigger(triggerBuildReq: TriggerBuildReq): GitRequestEvent {
        if (triggerBuildReq.eventType.isNullOrBlank()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "eventType can't be empty"
            )
        }
        val gitEvent = try {
            objectMapper.readValue<GitEvent>(triggerBuildReq.payload!!)
        } catch (ignore: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${ignore.message}")
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "Fail to parse the git web hook commit event, errMsg: ${ignore.message}"
            )
        }
        val gitRequestEvent =
            triggerParameter.getGitRequestEvent(gitEvent, triggerBuildReq.payload!!) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "event invalid"
            )
        // 仅支持当前仓库下的 event
        if (gitRequestEvent.gitProjectId != triggerBuildReq.gitProjectId) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = "Only events in the current repository [${triggerBuildReq.gitProjectId}] are supported"
            )
        }
        return gitRequestEvent.copy(objectKind = OBJECT_KIND_OPENAPI)
    }

    fun handleTrigger(
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline,
        triggerBuildReq: TriggerBuildReq,
        gitCIBasicSetting: GitCIBasicSetting
    ): BuildId? {
        var buildId: BuildId? = null
        triggerExceptionService.handleManualTrigger {
            buildId = trigger(gitRequestEvent, originYaml, buildPipeline, triggerBuildReq, gitCIBasicSetting)
        }
        return buildId
    }

    private fun trigger(
        gitRequestEvent: GitRequestEvent,
        originYaml: String,
        buildPipeline: GitProjectPipeline,
        triggerBuildReq: TriggerBuildReq,
        gitCIBasicSetting: GitCIBasicSetting
    ): BuildId? {
        val yamlReplaceResult = yamlTriggerFactory.requestTriggerV2.prepareCIBuildYaml(
            gitRequestEvent = gitRequestEvent,
            isMr = false,
            originYaml = originYaml,
            filePath = buildPipeline.filePath,
            pipelineId = buildPipeline.pipelineId,
            pipelineName = buildPipeline.displayName,
            event = null,
            changeSet = null,
            forkGitProjectId = null
        )!!
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = gitRequestEvent.id!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml),
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            objectKind = gitRequestEvent.objectKind,
            commitMsg = triggerBuildReq.customCommitMsg,
            triggerUser = gitRequestEvent.userId,
            sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )
        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        streamBasicSettingService.refreshSetting(gitRequestEvent.userId, gitRequestEvent.gitProjectId)

        var params = emptyMap<String, String>()
        if (gitRequestEvent.gitEvent != null) {
            params = triggerMatcher.getStartParams(
                context = StreamTriggerContext(
                    gitEvent = gitRequestEvent.gitEvent!!,
                    requestEvent = gitRequestEvent,
                    streamSetting = gitCIBasicSetting,
                    pipeline = buildPipeline,
                    originYaml = originYaml,
                    mrChangeSet = null
                ),
                triggerOn = null,
                isTrigger = true
            ).map { entry -> entry.key to entry.value.toString() }.toMap()
        }
        return yamlBuildV2.gitStartBuild(
            pipeline = buildPipeline,
            event = gitRequestEvent,
            yaml = yamlReplaceResult.normalYaml,
            parsedYaml = parsedYaml,
            originYaml = originYaml,
            normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml),
            gitBuildId = gitBuildId,
            isTimeTrigger = false,
            onlySavePipeline = false,
            params = params,
            yamlTransferData = yamlReplaceResult.yamlTransferData
        )
    }

    private fun prepareCIBuildYaml(
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?
    ): Pair<CIBuildYaml, String>? {

        if (originYaml.isNullOrBlank()) {
            return null
        }

        val yamlObject = try {
            streamYamlService.createCIBuildYaml(originYaml, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.warn("v1 git ci yaml is invalid", e)
            // 手动触发不发送commitCheck
            gitCIEventService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message),
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = gitRequestEvent.branch
            )
            return null
        }

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")
        return Pair(yamlObject, normalizedYaml)
    }
}
