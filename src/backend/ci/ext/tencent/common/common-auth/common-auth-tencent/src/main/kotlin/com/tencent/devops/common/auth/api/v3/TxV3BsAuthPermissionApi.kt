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

package com.tencent.devops.common.auth.api.v3

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class TxV3BsAuthPermissionApi @Autowired constructor(
    val client: Client
) : AuthPermissionApi {

    private val allActionMap = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String>()

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        val resourceTypeStr = buildResourceType(resourceType)
        val action = buildAction(resourceTypeStr, permission)
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = user,
            action = action
        ).data!!
    }

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: AuthPermission,
        relationResourceType: AuthResourceType?
    ): Boolean {
        // 校验用户在对应项目下是否有allAction权限
        if (allActionPermission(user, projectCode)) {
            return true
        }

        // 没有allAction权限则按对应的action校验
        val resourceTypeStr = buildResourceType(resourceType)
        val action = buildAction(resourceTypeStr, permission)
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = user,
            resourceType = resourceTypeStr,
            projectCode = projectCode,
            action = action,
            resourceCode = resourceCode,
            relationResourceType = relationResourceType?.value ?: null
        ).data!!
    }

    override fun getUserResourceByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        // 若有allAction权限,则有所有实例的权限,返回“*”,由上层查询所有实例信息
        if (allActionPermission(user, projectCode)) {
            return arrayListOf("*")
        }

        val resourceTypeStr = buildResourceType(resourceType)
        val action = buildAction(resourceTypeStr, permission)
        return client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            userId = user,
            action = action,
            resourceType = resourceTypeStr,
            projectCode = projectCode
        ).data ?: emptyList()
    }

    override fun getUserResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permissions: Set<AuthPermission>,
        supplier: (() -> List<String>)?
    ): Map<AuthPermission, List<String>> {
        val actions = mutableListOf<String>()
        val resourceTypeStr = buildResourceType(resourceType)
        permissions.forEach {
            actions.add(buildAction(resourceTypeStr, it))
        }
        return client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            userId = user,
            resourceType = resourceType.value,
            projectCode = projectCode,
            action = actions
        ).data ?: emptyMap()
    }

    override fun getUserResourcesByPermissions(
        userId: String,
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        permissions: Set<AuthPermission>,
        systemId: AuthServiceCode,
        supplier: (() -> List<String>)?
    ): Map<AuthPermission, List<String>> {
        TODO("Not yet implemented")
    }

    override fun addResourcePermissionForUsers(userId: String, projectCode: String, serviceCode: AuthServiceCode, permission: AuthPermission, resourceType: AuthResourceType, resourceCode: String, userIdList: List<String>, supplier: (() -> List<String>)?): Boolean {
        TODO("Not yet implemented")
    }

    private fun allActionPermission(
        userId: String,
        projectCode: String
    ): Boolean {
        val cacheKey = buildAllActionKey(userId, projectCode)
        if (!allActionMap.getIfPresent(cacheKey).isNullOrEmpty()) {
            return true
        }
        val action = "all_action"
        val hasAllAction = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            resourceType = AuthResourceType.PROJECT.value,
            projectCode = projectCode,
            action = action,
            resourceCode = projectCode,
            relationResourceType = null
        ).data!!
        if (hasAllAction) {
            allActionMap.put(cacheKey, "1")
        }
        return hasAllAction
    }

    /**
     * 为解决历史问题。 体验组与质量红线组对应value相同。但是在iamV3内的权限模型action不相同。
     */
    private fun buildResourceType(resourceType: AuthResourceType): String {
        return when (resourceType) {
            AuthResourceType.EXPERIENCE_GROUP -> "experience_group"
            AuthResourceType.EXPERIENCE_TASK -> "experience_task"
            AuthResourceType.QUALITY_GROUP -> "rule"
            AuthResourceType.QUALITY_RULE -> "group"
            else -> resourceType.value
        }
    }

    private fun buildAction(resourceType: String, permission: AuthPermission): String {
        return "${resourceType}_${permission.value}"
    }

    private fun buildAllActionKey(user: String, projectCode: String): String {
        return "$user-$projectCode"
    }
}
