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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.pojo.GitCIModelDetail
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.user.UserReportResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.api.ServiceGitResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitCIDetailService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val pipelineResourceDao: GitPipelineResourceDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIDetailService::class.java)
    }

    @Value("\${gateway.reportPrefix}")
    private lateinit var reportPrefix: String

    private val channelCode = ChannelCode.GIT

    fun getProjectLatestBuildDetail(userId: String, gitProjectId: Long, pipelineId: String?): GitCIModelDetail? {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )
        val eventBuildRecord = gitRequestEventBuildDao.getLatestBuild(dslContext, gitProjectId, pipelineId) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = eventBuildRecord.buildId,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        return GitCIModelDetail(pipeline, eventRecord!!, modelDetail)
    }

    fun getBuildDetail(userId: String, gitProjectId: Long, buildId: String): GitCIModelDetail? {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )
        val eventBuildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = buildId,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        return GitCIModelDetail(pipeline, eventRecord!!, modelDetail)
    }

    fun batchGetBuildDetail(userId: String, gitProjectId: Long, buildIds: List<String>): Map<String, GitCIBuildHistory> {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )
        val history = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = conf.projectCode!!,
            buildId = buildIds.toSet(),
            channelCode = channelCode
        ).data!!
        val infoMap = mutableMapOf<String, GitCIBuildHistory>()
        history.forEach {
            val buildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, it.id) ?: return@forEach
            val eventRecord = gitRequestEventDao.get(dslContext, buildRecord.eventId) ?: return@forEach
            var realEvent = eventRecord
            // 如果是来自fork库的分支，单独标识
            if (eventRecord.sourceGitProjectId != null) {
                try {
                    val gitToken = client.getScm(ServiceGitResource::class).getToken(eventRecord.sourceGitProjectId!!).data!!
                    logger.info("get token for gitProjectId[${eventRecord.sourceGitProjectId!!}] form scm, token: $gitToken")
                    val sourceRepositoryConf = client.getScm(ServiceGitResource::class).getProjectInfo(gitToken.accessToken, eventRecord.sourceGitProjectId!!).data
                    // 两个项目ID不同说明不是同一个库，为fork库
                    if (sourceRepositoryConf != null && eventRecord.sourceGitProjectId != sourceRepositoryConf.gitProjectId.toLong()) {
                        realEvent = eventRecord.copy(
                            branch = "${sourceRepositoryConf.nameWithNamespace}:${eventRecord.branch}"
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Cannot get source GitProjectInfo: ", e)
                }
            }

            val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, buildRecord.pipelineId) ?: return@forEach
            infoMap[it.id] = GitCIBuildHistory(
                displayName = pipeline.displayName,
                pipelineId = pipeline.pipelineId,
                gitRequestEvent = realEvent,
                buildHistory = it
            )
        }
        return infoMap
    }

    fun search(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): FileInfoPage<FileInfo> {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )

        val propMap = HashMap<String, String>()
        propMap["pipelineId"] = pipelineId
        propMap["buildId"] = buildId
        // val searchProps = SearchProps(emptyList(), propMap)

        val prop = listOf(Property("pipelineId", pipelineId), Property("buildId", buildId))

        return client.get(ServiceArtifactoryResource::class).search(
            projectId = conf.projectCode!!,
            page = page,
            pageSize = pageSize,
            searchProps = prop
        ).data!!
    }

    fun downloadUrl(
        userId: String,
        gitUserId: String,
        gitProjectId: Long,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )

        // 校验工蜂项目权限
        val checkAuth = client.getScm(ServiceGitCiResource::class).checkUserGitAuth(gitUserId, gitProjectId.toString())
        if (!checkAuth.data!!) {
            throw CustomException(Response.Status.FORBIDDEN, "用户没有工蜂项目权限，无法获取下载链接")
        }

        try {
            val url = client.get(ServiceArtifactoryDownLoadResource::class).downloadIndexUrl(
                projectId = conf.projectCode!!,
                artifactoryType = artifactoryType,
                userId = userId,
                path = path,
                ttl = 10,
                directed = true
            ).data!!
            return Url(getUrl(url.url)!!, getUrl(url.url2))
        } catch (e: Exception) {
            logger.error("Artifactory download url failed. ${e.message}")
            throw CustomException(Response.Status.BAD_REQUEST, "Artifactory download url failed. ${e.message}")
        }
    }

    private fun getUrl(url: String?): String? {
        if (url == null) {
            return url
        }
        // 没有被替换掉域名的url
        if (!url.startsWith("/")) {
            return url
        } else {
            return reportPrefix + url
        }
    }

    fun getReports(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): List<Report> {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )

        return client.get(UserReportResource::class).get(userId, conf.projectCode!!, pipelineId, buildId).data!!
    }

    fun getPipelineWithId(
        userId: String,
        gitProjectId: Long,
        pipelineId: String
    ): GitProjectPipeline? {
        logger.info("get pipeline with pipelineId: $pipelineId, gitProjectId: $gitProjectId")
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId)
        if (conf == null) {
            repositoryConfService.initGitCISetting(userId, gitProjectId)
            return null
        }
        val pipeline = pipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null

        return GitProjectPipeline(
            gitProjectId = gitProjectId,
            pipelineId = pipeline.pipelineId,
            filePath = pipeline.filePath,
            displayName = pipeline.displayName,
            enabled = pipeline.enabled,
            creator = pipeline.creator,
            latestBuildInfo = null
        )
    }
}
