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
 *
 */

<<<<<<<< HEAD:src/backend/ci/core/auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/service/iam/impl/PermissionResourceCallbackServiceImpl.kt
package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceCallbackService
========
package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserWsTemplateResource
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.service.WorkspaceTemplateService
import org.slf4j.LoggerFactory
>>>>>>>> origin/integration:src/backend/ci/ext/tencent/remotedev/biz-remotedev-tencent/src/main/kotlin/com/tencent/devops/remotedev/resources/user/UserWsTemplateResourceImpl.kt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

<<<<<<<< HEAD:src/backend/ci/core/auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/service/iam/impl/PermissionResourceCallbackServiceImpl.kt
@Service
class PermissionResourceCallbackServiceImpl @Autowired constructor(
    private val resourceService: ResourceService,
) : PermissionResourceCallbackService {
    override fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO {
        return resourceService.getProject(callBackInfo, token)
    }

    override fun getInstanceByResource(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        return resourceService.getInstanceByResource(
            callBackInfo = callBackInfo,
            token = token
        )
========
@RestResource
@Suppress("ALL")
class UserWsTemplateResourceImpl @Autowired constructor(
    private val workspaceTemplateService: WorkspaceTemplateService

) : UserWsTemplateResource {
    companion object {
        val logger = LoggerFactory.getLogger(UserWsTemplateResourceImpl::class.java)!!
    }

    override fun getWorkspaceTemplateList(userId: String): Result<List<WorkspaceTemplate>> {
        logger.info("WorkspaceTemplateService|getWorkspaceTemplateList|userId|$userId")
        return Result(workspaceTemplateService.getWorkspaceTemplateList())
>>>>>>>> origin/integration:src/backend/ci/ext/tencent/remotedev/biz-remotedev-tencent/src/main/kotlin/com/tencent/devops/remotedev/resources/user/UserWsTemplateResourceImpl.kt
    }
}
