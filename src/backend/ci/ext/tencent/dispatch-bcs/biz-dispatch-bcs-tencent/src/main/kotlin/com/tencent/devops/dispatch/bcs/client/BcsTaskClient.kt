package com.tencent.devops.dispatch.bcs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsResult
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.getCodeMessage
import com.tencent.devops.dispatch.bcs.pojo.bcs.isFailed
import com.tencent.devops.dispatch.bcs.pojo.bcs.isRunning
import com.tencent.devops.dispatch.bcs.pojo.bcs.isSuccess
import com.tencent.devops.dispatch.bcs.pojo.bcs.resp.BcsTaskStatusResp
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class BcsTaskClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: BcsClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsBuilderClient::class.java)
    }

    @Value("\${bcs.apiUrl}")
    val bcsApiUrl: String = ""

    fun getTasksStatus(
        userId: String,
        taskId: String,
        retryFlag: Int = 3
    ): BcsResult<BcsTaskStatusResp> {
        val url = "$bcsApiUrl/api/v1/devops/taskstatus/$taskId"
        val request = clientCommon.baseRequest(userId, url).get().build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Get task status failed, responseCode: ${response.code()}")

                    // 接口请求失败时，sleep 5s，再查一次
                    Thread.sleep(5 * 1000)
                    OkhttpUtils.doHttp(request).use {
                        val retryResponseContent = it.body()!!.string()
                        if (!it.isSuccessful) {
                            // 没机会了，只能失败
                            logger.error("$taskId retry get task status failed, retry responseCode: ${it.code()}")
                            throw BuildFailureException(
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                                ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                                "获取BCS TASK状态接口异常：http response code: ${response.code()}"
                            )
                        }

                        logger.info("retry response: $retryResponseContent")
                        return objectMapper.readValue(retryResponseContent)
                    }
                }

                return objectMapper.readValue(responseContent)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryFlag > 0) {
                logger.info("$taskId get task SocketTimeoutException. retry: $retryFlag")
                return getTasksStatus(userId, taskId, retryFlag - 1)
            } else {
                logger.error("$taskId get task status failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.TASK_STATUS_INTERFACE_ERROR.formatErrorMessage,
                    errorMessage = "获取BCS TASK状态接口超时, url: $url"
                )
            }
        }
    }

    fun waitTaskFinish(userId: String, taskId: String): Pair<BcsTaskStatusEnum, String?> {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("$taskId bcs task timeout")
                return Pair(BcsTaskStatusEnum.TIME_OUT, "获取BCS任务执行超时（10min）")
            }
            Thread.sleep(1 * 1000)
            val (status, errorMsg) = getTaskResult(userId, taskId).apply {
                if (first == null) {
                    return Pair(BcsTaskStatusEnum.FAILED, second)
                }
            }
            return when {
                status!!.isRunning() -> continue@loop
                status.isSuccess() -> {
                    Pair(BcsTaskStatusEnum.SUCCEEDED, null)
                }
                else -> Pair(status, errorMsg)
            }
        }
    }

    private fun getTaskResult(userId: String, taskId: String): Pair<BcsTaskStatusEnum?, String?> {
        val taskResponse = getTasksStatus(userId, taskId)
        val status = BcsTaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.code}, msg: $msg")
            return Pair(status, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return Pair(status, taskResponse!!.data?.message)
        }

        return Pair(status, null)
    }
}
