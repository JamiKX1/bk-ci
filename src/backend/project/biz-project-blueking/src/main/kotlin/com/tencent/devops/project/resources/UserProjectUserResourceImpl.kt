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

package com.tencent.devops.project.resources

import com.tencent.devops.common.auth.code.BkPipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.user.ProjectUser
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.UserCacheService
import com.tencent.devops.project.service.UserService
import com.tencent.devops.project.user.api.UserProjectUserResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectUserResourceImpl @Autowired constructor(
    private val userService: UserService,
    private val userCacheService: UserCacheService,
    private val projectService: ProjectService,
    private val pipelineAuthServiceCode: BkPipelineAuthServiceCode
) : UserProjectUserResource {

    override fun get(userId: String, bkToken: String?): Result<ProjectUser> {

        val staff = userService.getStaffInfo(userId, bkToken)
        return Result(
            ProjectUser(
                chineseName = staff.chineseName,
                avatarUrl = "",
                username = staff.username
            )
        )
    }

    override fun getDetail(userId: String, bkToken: String): Result<UserDeptDetail> {
        return Result(userCacheService.getDetailFromCache(userId))
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        return projectService.getProjectUsers(accessToken, userId, projectCode)
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String): Result<List<UserRole>> {
        return Result(projectService.getProjectUserRoles(accessToken, userId, projectCode, pipelineAuthServiceCode))
    }
}