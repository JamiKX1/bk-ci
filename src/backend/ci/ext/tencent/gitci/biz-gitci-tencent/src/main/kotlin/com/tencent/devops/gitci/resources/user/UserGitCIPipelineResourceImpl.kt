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

package com.tencent.devops.gitci.resources.user

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.user.UserGitCIPipelineResource
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.service.GitCIBasicSettingService
import com.tencent.devops.gitci.v2.service.GitCIV2PipelineService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class UserGitCIPipelineResourceImpl @Autowired constructor(
    private val pipelineV2Service: GitCIV2PipelineService,
    private val gitCIBasicSettingService: GitCIBasicSettingService
) : UserGitCIPipelineResource {

    override fun getPipelineList(
        userId: String,
        projectId: String,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<GitProjectPipeline>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        if (!gitCIBasicSettingService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目无法开启工蜂CI，请联系蓝盾助手")
        }
        return Result(
            pipelineV2Service.getPipelineList(
                userId = userId,
                gitProjectId = gitProjectId,
                keyword = keyword,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getPipeline(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<GitProjectPipeline?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        if (!gitCIBasicSettingService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目无法开启工蜂CI，请联系蓝盾助手")
        }
        return Result(
            pipelineV2Service.getPipelineListById(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId
            )
        )
    }

    override fun enablePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        enabled: Boolean
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        if (!gitCIBasicSettingService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目无法开启工蜂CI，请联系蓝盾助手")
        }
        return Result(
            pipelineV2Service.enablePipeline(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                enabled = enabled
            )
        )
    }

    override fun listPipelineNames(userId: String, projectId: String): Result<List<GitProjectPipeline>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        if (!gitCIBasicSettingService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目无法开启工蜂CI，请联系蓝盾助手")
        }
        return Result(
            pipelineV2Service.getPipelineListWithoutHistory(
                userId = userId,
                gitProjectId = gitProjectId
            )
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
