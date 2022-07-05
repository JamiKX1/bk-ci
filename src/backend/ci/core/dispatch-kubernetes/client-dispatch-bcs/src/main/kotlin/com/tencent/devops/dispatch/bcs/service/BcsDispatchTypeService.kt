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

package com.tencent.devops.dispatch.bcs.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.bcs.client.BcsBuilderClient
import com.tencent.devops.dispatch.bcs.client.BcsJobClient
import com.tencent.devops.dispatch.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.bcs.pojo.BcsBuilderStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsJob
import com.tencent.devops.dispatch.bcs.pojo.BcsJobStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.NfsConfig
import com.tencent.devops.dispatch.bcs.pojo.canReStart
import com.tencent.devops.dispatch.bcs.pojo.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.isFailed
import com.tencent.devops.dispatch.bcs.pojo.isRunning
import com.tencent.devops.dispatch.bcs.pojo.isStarting
import com.tencent.devops.dispatch.bcs.pojo.isSuccess
import com.tencent.devops.dispatch.kubernetes.pojo.DockerRegistry
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchBuilderDebugStatus
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugOperateBuilderType
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.debug.DispatchDebugTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.interfaces.DispatchTypeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service("bcsDispatchTypeService")
class BcsDispatchTypeService @Autowired constructor(
    private val bcsJobClient: BcsJobClient,
    private val bcsTaskClient: BcsTaskClient,
    private val bcsBuilderClient: BcsBuilderClient
) : DispatchTypeService {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsDispatchTypeService::class.java)
    }

    @Value("\${bcs.resources.job.cpu}")
    var cpu: Double = 32.0

    @Value("\${bcs.resources.job.memory}")
    var memory: Int = 65535

    @Value("\${bcs.resources.job.disk}")
    var disk: Int = 500

    @Value("\${bcs.sleepEntrypoint}")
    val entrypoint: String = "sleep.sh"

    override val slaveEnv = "Bcs"

    override fun createJob(userId: String, jobReq: DispatchJobReq): DispatchTaskResp {
        val job = with(jobReq) {
            BcsJob(
                name = alias,
                builderName = podNameSelector,
                shareDiskMountPath = mountPath,
                deadline = activeDeadlineSeconds,
                image = image,
                registry = DockerRegistry(
                    host = registry.host,
                    username = registry.username,
                    password = registry.password
                ),
                cpu = cpu,
                memory = memory,
                disk = disk,
                env = params?.env,
                command = params?.command,
                workDir = params?.workDir,
                nfs = params?.nfsVolume?.map { nfsVo ->
                    NfsConfig(
                        server = nfsVo.server,
                        path = nfsVo.path,
                        mountPath = nfsVo.mountPath
                    )
                }
            )
        }

        val result = bcsJobClient.createJob(userId, job)
        if (result.isNotOk() || result.data == null) {
            return DispatchTaskResp(
                result.data?.taskId,
                result.message
            )
        }
        return DispatchTaskResp(result.data.taskId)
    }

    override fun getJobStatus(userId: String, jobName: String): DispatchBuildStatusResp {
        val result = bcsJobClient.getJobStatus(userId, jobName)
        if (result.isNotOk()) {
            return DispatchBuildStatusResp(
                status = DispatchBuildStatusEnum.failed.name,
                errorMsg = result.message
            )
        }
        val status = BcsJobStatusEnum.realNameOf(result.data?.status)
        if (status == null || status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status?.message)
        }
        return when {
            status.isSuccess() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            status.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    override fun getJobLogs(userId: String, jobName: String, sinceTime: Int?): DispatchJobLogResp {
        val result = bcsJobClient.getJobLogs(userId, jobName, sinceTime)
        if (result.isNotOk()) {
            return DispatchJobLogResp(
                log = result.data,
                errorMsg = result.message
            )
        }
        return DispatchJobLogResp(log = result.data)
    }

    override fun getTaskStatus(userId: String, taskId: String): DispatchBuildStatusResp {
        val taskResponse = bcsTaskClient.getTasksStatus(userId, taskId)
        val status = BcsTaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.code}, msg: $msg")
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, taskResponse.data.message)
        }
        return when {
            status!!.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            status.isSuccess() -> {
                DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            }
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }

    override fun getDebugBuilderStatus(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        retryTime: Int
    ): Result<DispatchBuilderDebugStatus> {
        val result = bcsBuilderClient.getBuilderDetail(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            retryTime = retryTime
        )

        if (result.isNotOk()) {
            return Result(
                result.code,
                result.message,
            )
        }

        val status = result.data!!
        val resultStatus = when {
            status.canReStart() -> DispatchBuilderDebugStatus.CAN_RESTART
            status.isRunning() -> DispatchBuilderDebugStatus.RUNNING
            status.isStarting() -> DispatchBuilderDebugStatus.STARTING
            else -> DispatchBuilderDebugStatus.UNKNOWN
        }
        return Result(
            resultStatus
        )
    }

    override fun operateDebugBuilder(
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String,
        param: DispatchDebugOperateBuilderParams
    ): String {
        return bcsBuilderClient.operateBuilder(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            param = when (param.type) {
                DispatchDebugOperateBuilderType.START_SLEEP -> BcsStartBuilderParams(
                    env = param.env,
                    command = listOf("/bin/sh", entrypoint)
                )
                DispatchDebugOperateBuilderType.STOP -> BcsStopBuilderParams()
                DispatchDebugOperateBuilderType.DELETE -> BcsDeleteBuilderParams()
            }
        )
    }

    override fun waitDebugTaskFinish(userId: String, taskId: String): DispatchDebugTaskStatus {
        val startResult = bcsTaskClient.waitTaskFinish(userId, taskId)
        if (startResult.first == BcsTaskStatusEnum.SUCCEEDED) {
            return DispatchDebugTaskStatus(DispatchDebugTaskStatusEnum.SUCCEEDED, null)
        } else {
            return DispatchDebugTaskStatus(DispatchDebugTaskStatusEnum.FAILED, startResult.second)
        }
    }

    override fun waitDebugBuilderRunning(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        builderName: String
    ): DispatchBuilderDebugStatus {
        val status = bcsBuilderClient.waitContainerRunning(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            containerName = builderName
        )
        return when (status) {
            BcsBuilderStatusEnum.READY_TO_RUN, BcsBuilderStatusEnum.STOP_FAILED ->
                DispatchBuilderDebugStatus.CAN_RESTART
            BcsBuilderStatusEnum.RUNNING -> DispatchBuilderDebugStatus.RUNNING
            BcsBuilderStatusEnum.STARTING -> DispatchBuilderDebugStatus.STARTING
            else -> DispatchBuilderDebugStatus.UNKNOWN
        }
    }

    override fun getDebugWebsocketUrl(
        projectId: String,
        pipelineId: String,
        staffName: String,
        builderName: String
    ): String {
        return bcsBuilderClient.getWebsocketUrl(projectId, pipelineId, staffName, builderName).data!!
    }

    override fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        logger.info("projectId: $projectId, buildId: $buildId build and push image. " +
                        JsonUtil.toJson(dispatchBuildImageReq)
        )

        return DispatchTaskResp(bcsBuilderClient.buildAndPushImage(
            userId, dispatchBuildImageReq))
    }
}
