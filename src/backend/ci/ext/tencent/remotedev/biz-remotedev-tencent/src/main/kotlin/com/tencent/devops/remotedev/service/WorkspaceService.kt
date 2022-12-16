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

package com.tencent.devops.remotedev.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceReq
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val gitTransferService: GitTransferService,
    private val client: Client
) {

    private val redisCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, String>() {
                override fun load(key: String): String {
                    return redisOperation.get(key) ?: ""
                }
            }
        )

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private const val REDIS_CALL_LIMIT_KEY = "remotedev:callLimit"
        private const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime"
        private const val REDIS_OFFICIAL_DEVFILE_KEY = "remotedev:devfile"
        private const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(1)
    }

    fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): List<RemoteDevRepository> {
        logger.info("$userId get user git repository|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        return checkOauthIllegal(userId) {
            gitTransferService.getProjectList(
                userId = userId,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                search = search,
                owned = false,
                minAccessLevel = GitAccessLevelEnum.DEVELOPER
            )
        }
    }

    fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): List<String> {
        logger.info("$userId get git repository branch list|$pathWithNamespace|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        return checkOauthIllegal(userId) {
            gitTransferService.getProjectBranches(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                search = search
            ) ?: emptyList()
        }
    }

    fun createWorkspace(userId: String, workspaceCreate: WorkspaceCreate): String {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")

        val pathWithNamespace = GitUtils.getDomainAndRepoName(workspaceCreate.repositoryUrl).second
        val projectName = pathWithNamespace.substring(pathWithNamespace.lastIndexOf("/") + 1)
        val yaml = if (workspaceCreate.useOfficialDevfile == false) {
            kotlin.runCatching {
                gitTransferService.getFileContent(
                    userId = userId,
                    pathWithNamespace = pathWithNamespace,
                    filePath = workspaceCreate.devFilePath!!,
                    ref = workspaceCreate.branch
                )
            }.getOrElse {
                logger.warn("get yaml failed ${it.message}")
                throw CustomException(Response.Status.BAD_REQUEST, "获取 devfile 异常 ${it.message}")
            }
        } else redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY)

        if (yaml.isBlank()) {
            logger.warn(
                "create workspace get devfile blank,return." +
                    "|useOfficialDevfile=${workspaceCreate.useOfficialDevfile}"
            )
            throw CustomException(Response.Status.BAD_REQUEST, "devfile 为空，请确认。")
        }

        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get $userId info error|${it.message}") }.getOrElse { null }?.data

        val workspace = with(workspaceCreate) {
            Workspace(
                workspaceId = null,
                name = UUID.randomUUID().toString(), // 先临时生成一个，后面拿k8s返回的name替换掉
                repositoryUrl = repositoryUrl,
                branch = branch,
                devFilePath = devFilePath,
                yaml = yaml,
                wsTemplateId = wsTemplateId,
                status = null,
                lastStatusUpdateTime = null,
                sleepingTime = null,
                createUserId = userId,
                workPath = Constansts.prefixWorkPath.plus(projectName),
                hostName = ""
            )
        }

        val devfile = DevfileUtil.parseDevfile(yaml)

        val workspaceId = workspaceDao.createWorkspace(
            userId = userId,
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            dslContext = dslContext,
            userInfo = userInfo
        )

        kotlin.runCatching {
            client.get(ServiceRemoteDevResource::class).createWorkspace(
                userId,
                WorkspaceReq(
                    workspaceId = workspaceId,
                    name = workspace.name,
                    repositoryUrl = workspace.repositoryUrl,
                    branch = workspace.branch,
                    devFilePath = workspace.devFilePath,
                    devFile = devfile
                )
            ).data!!
        }.fold(
            { workspaceName ->
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    // 创建成功后，更新name
                    workspaceDao.updateWorkspaceName(workspaceId, workspaceName, transactionContext)
                    workspaceDao.updateWorkspaceStatus(workspaceId, WorkspaceStatus.RUNNING, transactionContext)
                    workspaceHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        startUserId = userId,
                        lastSleepTimeCost = 0
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        operator = userId,
                        action = WorkspaceAction.CREATE,
                        actionMessage = String.format(
                            getOpHistory(OpHistoryCopyWriting.CREATE),
                            pathWithNamespace,
                            workspace.branch,
                            workspaceName
                        )
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        operator = userId,
                        action = WorkspaceAction.START,
                        actionMessage = getOpHistory(OpHistoryCopyWriting.FIRST_START)
                    )
                }

                // 获取远程登录url
                val workspaceUrl = client.get(ServiceRemoteDevResource::class).getWorkspaceUrl(
                    userId,
                    workspaceName
                ).data

                return workspaceUrl!!
            },
            {
                // 创建失败
                logger.warn("create workspace $workspaceId failed|${it.message}", it)
                workspaceDao.deleteWorkspace(workspaceId, dslContext)
                throw CustomException(Response.Status.BAD_REQUEST, "工作空间创建失败，参考原因:${it.message}")
            }
        )
    }

    fun startWorkspace(userId: String, workspaceId: Long): Boolean {
        logger.info("$userId start workspace $workspaceId")
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:startWorkspace:$workspaceId",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceId not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.isRunning()) {
                logger.info("$workspace has been started, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "$workspace has been started")
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceId = workspaceId,
                operator = userId,
                action = WorkspaceAction.START,
                actionMessage = getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
            )
            val res = kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class).startWorkspace(
                    userId,
                    workspace.name
                ).data ?: false
            }.getOrElse {
                logger.error("start workspace error |${it.message}", it)
                false
            }

            if (res) {
                val history = workspaceHistoryDao.fetchHistory(dslContext, workspaceId).firstOrNull()
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    workspaceDao.updateWorkspaceStatus(workspaceId, WorkspaceStatus.RUNNING, transactionContext)

                    val lastHistory = workspaceHistoryDao.fetchAnyHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId
                    )
                    if (lastHistory != null) {
                        workspaceDao.updateWorkspaceSleepingTime(
                            workspaceId = workspaceId,
                            sleepTime = Duration.between(lastHistory.endTime, LocalDateTime.now()).seconds.toInt(),
                            dslContext = transactionContext
                        )
                    }
                    workspaceHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        startUserId = userId,
                        lastSleepTimeCost = if (history != null) {
                            Duration.between(history.endTime, LocalDateTime.now()).seconds.toInt()
                        } else 0
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        operator = userId,
                        action = WorkspaceAction.START,
                        actionMessage = String.format(
                            getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            WorkspaceStatus.RUNNING.name
                        )
                    )
                }
            }
            return res
        }
    }

    fun stopWorkspace(userId: String, workspaceId: Long): Boolean {
        logger.info("$userId stop workspace $workspaceId")

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:stopWorkspace:$workspaceId",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceId not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.isSleeping()) {
                logger.info("$workspace has been stopped, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "$workspace has been stopped")
            }

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceId = workspaceId,
                operator = userId,
                action = WorkspaceAction.SLEEP,
                actionMessage = getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
            )
            val res = kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class).stopWorkspace(
                    userId,
                    workspace.name
                ).data ?: false
            }.getOrElse {
                logger.error("stop workspace error |${it.message}", it)
                false
            }

            if (res) {
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    workspaceDao.updateWorkspaceStatus(workspaceId, WorkspaceStatus.SLEEP, transactionContext)
                    val lastHistory = workspaceHistoryDao.fetchAnyHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId
                    )
                    if (lastHistory != null) {
                        workspaceDao.updateWorkspaceUsageTime(
                            workspaceId = workspaceId,
                            usageTime = Duration.between(lastHistory.startTime, LocalDateTime.now()).seconds.toInt(),
                            dslContext = transactionContext,
                        )
                        workspaceHistoryDao.updateWorkspaceHistory(
                            dslContext = transactionContext,
                            id = lastHistory.id,
                            stopUserId = userId
                        )
                    } else {
                        logger.error("$workspaceId get last history info null")
                    }
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        operator = userId,
                        action = WorkspaceAction.SLEEP,
                        actionMessage = String.format(
                            getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            WorkspaceStatus.SLEEP.name
                        )
                    )
                }
            }
            return res
        }
    }

    fun deleteWorkspace(userId: String, workspaceId: Long): Boolean {
        logger.info("$userId delete workspace $workspaceId")
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:deleteWorkspace:$workspaceId",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceId not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.isDeleted()) {
                logger.info("$workspace has been deleted, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "$workspace has been deleted")
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceId = workspaceId,
                operator = userId,
                action = WorkspaceAction.DELETE,
                actionMessage = getOpHistory(OpHistoryCopyWriting.DELETE)
            )
            val res = kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class).deleteWorkspace(
                    userId,
                    workspace.name
                ).data ?: false
            }.getOrElse {
                logger.error("stop workspace error |${it.message}", it)
                false
            }

            if (res) {
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    workspaceDao.updateWorkspaceStatus(workspaceId, WorkspaceStatus.DELETED, transactionContext)
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceId = workspaceId,
                        operator = userId,
                        action = WorkspaceAction.DELETE,
                        actionMessage = String.format(
                            getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            WorkspaceStatus.DELETED.name
                        )
                    )
                }
            }
            return res
        }
    }

    fun shareWorkspace(userId: String, workspaceId: Long, sharedUser: String): Boolean {
        logger.info("$userId share workspace $workspaceId|$sharedUser")
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:shareWorkspace:${workspaceId}_$sharedUser",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceId not find")
            if (userId != workspace.creator) throw CustomException(Response.Status.FORBIDDEN, "你没有权限操作")
            val shareInfo = WorkspaceShared(workspaceId, userId, sharedUser)
            if (workspaceSharedDao.exsitWorkspaceSharedInfo(shareInfo, dslContext)) {
                logger.info("$workspaceId has already shared to $sharedUser")
                throw CustomException(Response.Status.BAD_REQUEST, "$workspaceId has already shared to $sharedUser")
            }

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceSharedDao.createWorkspaceSharedInfo(userId, shareInfo, transactionContext)
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceId = workspaceId,
                    operator = userId,
                    action = WorkspaceAction.SHARE,
                    actionMessage = getOpHistory(OpHistoryCopyWriting.SHARE)
                )
            }
            return true
        }
    }

    fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Page<Workspace> {
        logger.info("$userId get user workspace list")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val count = workspaceDao.countUserWorkspace(dslContext, userId)
        val result = workspaceDao.limitFetchUserWorkspace(
            dslContext = dslContext,
            userId = userId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        ) ?: emptyList()

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                val status = WorkspaceStatus.values()[it.status]
                Workspace(
                    workspaceId = it.id,
                    name = it.name,
                    repositoryUrl = it.url,
                    branch = it.branch,
                    devFilePath = it.yamlPath,
                    yaml = it.yaml,
                    wsTemplateId = it.templateId,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime.timestamp(),
                    sleepingTime = if (status.isSleeping()) it.lastStatusUpdateTime.timestamp() else null,
                    createUserId = it.creator,
                    workPath = it.workPath,
                    hostName = it.hostName
                )
            }
        )
    }

    fun getWorkspaceUserDetail(userId: String): WorkspaceUserDetail {
        logger.info("$userId get his all workspace ")
        val workspaces = workspaceDao.fetchWorkspace(dslContext, userId) ?: emptyList()
        val status = workspaces.map { WorkspaceStatus.values()[it.status] }
        val usageTime = workspaces.sumOf { it.usageTime }

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY).toInt()
        return WorkspaceUserDetail(
            runningCount = status.count { it.isRunning() },
            sleepingCount = status.count { it.isSleeping() },
            deleteCount = status.count { it.isDeleted() },
            chargeableTime = usageTime - discountTime,
            usageTime = usageTime,
            sleepingTime = workspaces.sumOf { it.sleepingTime },
            cpu = workspaces.sumOf { it.cpu },
            memory = workspaces.sumOf { it.memory },
            disk = workspaces.sumOf { it.disk },
        )
    }

    fun getWorkspaceDetail(userId: String, workspaceId: Long): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceId")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val lastHistory = workspaceHistoryDao.fetchAnyHistory(dslContext, workspaceId) ?: return null

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY).toInt()

        val usageTime = workspace.usageTime + if (workspaceStatus.isRunning()) {
            // 如果正在运行，需要加上目前距离该次启动的时间
            Duration.between(lastHistory.startTime, LocalDateTime.now()).seconds
        } else 0

        val sleepingTime = workspace.sleepingTime + if (workspaceStatus.isSleeping()) {
            // 如果正在休眠，需要加上目前距离上次结束的时间
            Duration.between(lastHistory.endTime, LocalDateTime.now()).seconds
        } else 0

        val chargeableTime = usageTime - discountTime

        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                name = name,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = chargeableTime,
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml
            )
        }
    }

    fun getWorkspaceTimeline(
        userId: String,
        workspaceId: Long,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceOpHistory> {
        logger.info("$userId get workspace time line from id $workspaceId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceId)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceId = workspaceId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                WorkspaceOpHistory(
                    createdTime = it.createdTime.timestamp(),
                    operator = it.operator,
                    action = WorkspaceAction.values()[it.action],
                    actionMessage = it.actionMsg
                )
            }
        )
    }

    fun checkDevfile(userId: String, pathWithNamespace: String, branch: String): List<String> {
        logger.info("$userId get devfile list from git. $pathWithNamespace|$branch")
        return checkOauthIllegal(userId) {
            gitTransferService.getFileNameTree(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                path = Constansts.devFileDirectoryName, // 根目录
                ref = branch,
                recursive = false // 不递归
            )
        }
    }

    /**
     * 检查工蜂接口是否返回401，针对这种情况，抛出OAUTH_ILLEGAL 让前端跳转去重新授权
     */
    private fun <T> checkOauthIllegal(userId: String, action: () -> T): T {
        return kotlin.runCatching {
            action()
        }.onFailure {
            if (it is RemoteServiceException && it.httpStatus == HTTP_401 || it is OauthForbiddenException) {
                throw ErrorCodeException(
                    statusCode = 400,
                    errorCode = ErrorCodeEnum.OAUTH_ILLEGAL.errorCode.toString(),
                    defaultMessage = ErrorCodeEnum.OAUTH_ILLEGAL.formatErrorMessage.format(userId),
                    params = arrayOf(userId)
                )
            }
        }.getOrThrow()
    }

    private fun getOpHistory(key: OpHistoryCopyWriting) =
        redisCache.get(REDIS_OP_HISTORY_KEY_PREFIX + key.name).ifBlank {
            key.default
        }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun getTimeOutInactivityWorkspace (
        userId: String
    ): List<String> {
        logger.info("$userId getTimeOutInactivityWorkspace")
        return workspaceDao.getTimeOutInactivityWorkspace(
            Constansts.timeoutDays, dslContext)?.map { it.name } ?: emptyList()
    }

}
