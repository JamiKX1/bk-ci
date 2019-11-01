package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.TcmElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceTcmResource
import com.tencent.devops.plugin.pojo.tcm.TcmReqParam
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.service.PipelineUserService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TcmTaskAtom @Autowired constructor(
    private val pipelineUserService: PipelineUserService,
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
)
    : IAtomTask<TcmElement> {
    private val logger = LoggerFactory.getLogger(TcmTaskAtom::class.java)

    override fun getParamElement(task: PipelineBuildTask): TcmElement {
        return JsonUtil.mapTo(task.taskParams, TcmElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: TcmElement, runVariables: Map<String, String>): AtomResponse {
        logger.info("Enter TcmTaskAtom Run...")
        val buildId = task.buildId
        val elementId = task.taskId

        val userId = if (param.startWithSaver == true) {
            val lastModifyUserMap = pipelineUserService.listUpdateUsers(setOf(task.pipelineId))
            lastModifyUserMap[task.pipelineId] ?: task.starter
        } else {
            task.starter
        }
        val appId = param.appId
        val tcmAppId = param.tcmAppId
        val templateId = param.templateId
        val taskName = parseVariable(param.name, runVariables) + "-" + System.currentTimeMillis()
        val workJson = if (param.workJson != null && param.workJson!!.isNotEmpty()) {
            val mapStr = parseVariable(JsonUtil.toJson(param.workJson!!), runVariables)
            JsonUtil.to<List<Map<String, String>>>(mapStr)
        } else {
            listOf()
        }

        val tcmReqParam = TcmReqParam(userId, appId, tcmAppId, templateId, taskName, workJson)
        LogUtils.addLine(rabbitTemplate, buildId, "tcm原子请求参数:\n ${tcmReqParam.beanToMap()}", elementId, task.containerHashId,task.executeCount ?: 1)
        return try {
            val pipelineId = task.pipelineId
            val lastUpdateUser = pipelineUserService.list(setOf(pipelineId)).firstOrNull()?.updateUser ?: ""
            client.get(ServiceTcmResource::class).startTask(tcmReqParam, buildId, lastUpdateUser)
            LogUtils.addLine(rabbitTemplate, buildId, "tcm原子执行成功", elementId, task.containerHashId,task.executeCount ?: 1)
            defaultSuccessAtomResponse
        } catch (e: Exception) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "tcm原子执行失败:${e.message}", elementId, task.containerHashId,task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "tcm原子执行失败:${e.message}"
            )
        }
    }
}
