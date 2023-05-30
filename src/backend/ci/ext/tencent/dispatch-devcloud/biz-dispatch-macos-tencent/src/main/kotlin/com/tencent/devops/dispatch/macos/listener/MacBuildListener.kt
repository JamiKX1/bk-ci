package com.tencent.devops.dispatch.macos.listener

import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.dispatch.macos.service.BuildHistoryService
import com.tencent.devops.dispatch.macos.service.BuildTaskService
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import com.tencent.devops.dispatch.macos.service.MacosVMRedisService
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class MacBuildListener @Autowired constructor(
    private val buildHistoryService: BuildHistoryService,
    private val buildTaskService: BuildTaskService,
    private val redisOperation: RedisOperation,
    private val devCloudMacosService: DevCloudMacosService,
    private val macosVMRedisService: MacosVMRedisService,
    private val buildLogPrinter: BuildLogPrinter
) : BuildListener {

    override fun getShutdownQueue(): String {
        return ".macos"
    }

    override fun getStartupDemoteQueue(): String {
        return ".macos.demote"
    }

    override fun getStartupQueue(): String {
        return ".macos"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.MACOS_DEVCLOUD
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("MacOS Dispatch on start up - ($dispatchMessage)")
        val devCloudMacosVmInfo = devCloudMacosService.creatVM(dispatchMessage)

        devCloudMacosVmInfo?.let {
            devCloudMacosService.saveVM(it)
            buildHistoryService.saveBuildHistory(dispatchMessage, it.ip, it.id, "DEVCLOUD")
            macosVMRedisService.saveRedisBuild(dispatchMessage, it.ip)

            logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] " +
                            "Success to start vm(${it.ip}|${it.id})")

            log(
                buildLogPrinter = buildLogPrinter,
                buildId = dispatchMessage.buildId,
                containerHashId = dispatchMessage.containerHashId,
                vmSeqId = dispatchMessage.vmSeqId,
                message = "DevCloud MacOS IP：${it.ip}",
                executeCount = dispatchMessage.executeCount
            )
        } ?: run {
            // 如果没有找到合适的vm机器，则等待10秒后再执行, 总共执行30次（5min）
            logRed(
                buildLogPrinter,
                dispatchMessage.buildId,
                dispatchMessage.containerHashId,
                dispatchMessage.vmSeqId,
                "No idle macOS resources found, wait 10 seconds and try again",
                dispatchMessage.executeCount
            )

            logger.error("Can not found any idle vm for this build($dispatchMessage),wait for 10s")
            retry(sleepTimeInMS = 10000, retryTimes = 30)
        }
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("[${event.pipelineId}|${event.pipelineId}|${event.buildId}] Build shutdown with event($event)")
        // 如果是某个job关闭，则锁到job，如果是整条流水线shutdown，则锁到buildid级别
        val lockKey =
            if (event.vmSeqId == null)
                "$LOCK_SHUTDOWN:${event.buildId}" else "$LOCK_SHUTDOWN:${event.buildId}:${event.vmSeqId}"
        val redisLock = RedisLock(
            redisOperation,
            lockKey,
            20
        )
        try {
            if (!redisLock.tryLock()) {
                return
            }

            val buildTaskRecords = buildTaskService.getByBuildIdAndVmSeqId(
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                executeCount = event.executeCount
            )
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] " +
                            "buildTaskRecords: ${buildTaskRecords.size}")

            if (buildTaskRecords.isEmpty()) {
                logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Recycling the macos failed.")
                return
            }

            doShutdown(buildTaskRecords, event, event.userId, event.projectId)
        } catch (e: Exception) {
            logger.error("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun doShutdown(
        buildTaskRecords: Result<TBuildTaskRecord>,
        event: PipelineAgentShutdownEvent,
        creator: String,
        projectId: String
    ) {
        buildTaskRecords.forEach { buildTask ->
            // 关闭的时候对container进行锁操作，防止重复操作
            try {
                val vmIp = buildTask.vmIp
                val vmId = buildTask.vmId
                logger.info(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] " +
                        "Get the vm ip($vmIp),vm id($vmId)"
                )
                macosVMRedisService.deleteRedisBuild(vmIp)
                devCloudMacosService.deleteVM(
                    creator = creator,
                    projectId = projectId,
                    pipelineId = buildTask.pipelineId,
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    vmId = vmId
                )
                logger.info("[${event.buildId}]|[${event.vmSeqId}] end build. buildId: ${buildTask.id}")
                buildHistoryService.endBuild(MacJobStatus.Done, buildTask.buildHistoryId, buildTask.id)
            } catch (e: Exception) {
                val vmIp = buildTask.vmIp
                logger.error(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}] shutdown error,vm is $vmIp",
                    e
                )

                if (e is SocketTimeoutException) {
                    logger.error(
                        "[${event.projectId}|${event.pipelineId}|${event.buildId}] " +
                            "vm is $vmIp, end build."
                    )
                    buildHistoryService.endBuild(
                        MacJobStatus.ShutDownError,
                        buildTask.buildHistoryId,
                        buildTask.id
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MacBuildListener::class.java)
        private const val LOCK_SHUTDOWN = "dispatcher:locker:macos:shutdown"
    }
}
