package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber")
class RbacCacheService constructor(
    private val dslContext: DSLContext,
    private val authResourceTypeDao: AuthResourceTypeDao,
    private val authActionDao: AuthActionDao,
    private val authHelper: AuthHelper,
    private val iamConfiguration: IamConfiguration
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacCacheService::class.java)
    }

    /*获取资源类型下的动作*/
    private val resourceType2ActionCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, List<ActionInfoVo>>()
    private val resourceTypeCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, ResourceTypeInfoVo>()
    private val actionCache = CacheBuilder.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*action*/, ActionInfoVo>()

    // 用户-管理员项目 缓存， 5分钟有效时间
    private val projectManager = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<String>>()

    fun listResourceTypes(): List<ResourceTypeInfoVo> {
        if (resourceTypeCache.asMap().values.isEmpty()) {
            authResourceTypeDao.list(dslContext).forEach {
                val resourceTypeInfo = ResourceTypeInfoVo(
                    resourceType = it.resourceType,
                    name = it.name,
                    parent = it.parent,
                    system = it.system
                )
                resourceTypeCache.put(it.resourceType, resourceTypeInfo)
            }
        }
        return resourceTypeCache.asMap().values.toList()
    }

    fun listResourceType2Action(resourceType: String): List<ActionInfoVo> {
        if (resourceType2ActionCache.getIfPresent(resourceType) == null) {
            val actionList = authActionDao.list(dslContext, resourceType)
            if (actionList.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_ACTION_EMPTY,
                    params = arrayOf(resourceType),
                    defaultMessage = "the action relate with the resource type($resourceType) does not exist"
                )
            }
            val actionInfoVoList = actionList.map {
                ActionInfoVo(
                    action = it.action,
                    actionName = it.actionName,
                    resourceType = it.resourceType,
                    relatedResourceType = it.relatedResourceType
                )
            }
            resourceType2ActionCache.put(resourceType, actionInfoVoList)
        }
        return resourceType2ActionCache.getIfPresent(resourceType)!!
    }

    fun getActionInfo(action: String): ActionInfoVo {
        if (actionCache.getIfPresent(action) == null) {
            val actionRecord = authActionDao.get(dslContext, action)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ACTION_NOT_EXIST,
                    params = arrayOf(action),
                    defaultMessage = "the action($action) does not exist"
                )
            val actionInfo = ActionInfoVo(
                action = actionRecord.action,
                actionName = actionRecord.actionName,
                resourceType = actionRecord.resourceType,
                relatedResourceType = actionRecord.relatedResourceType
            )
            actionCache.put(action, actionInfo)
        }
        return actionCache.getIfPresent(action)!!
    }

    fun getResourceTypeInfo(resourceType: String): ResourceTypeInfoVo {
        if (resourceTypeCache.getIfPresent(resourceType) == null) {
            listResourceTypes()
        }
        return resourceTypeCache.getIfPresent(resourceType) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_FOUND,
            params = arrayOf(resourceType),
            defaultMessage = "the resource type($resourceType) does not exist"
        )
    }

    fun checkProjectManager(userId: String, projectCode: String): Boolean {
        val projectCodes = projectManager.getIfPresent(userId)
        if (projectCodes != null && projectCodes.contains(projectCode)) {
            return true
        }
        val hasProjectManage = validateUserProjectPermission(
            userId = userId,
            projectCode = projectCode,
            permission = AuthPermission.MANAGE
        )
        if (hasProjectManage) {
            val newProjectCodes = projectManager.getIfPresent(userId)?.toMutableList() ?: mutableListOf()
            newProjectCodes.add(projectCode)
            projectManager.put(userId, newProjectCodes)
        }
        return hasProjectManage
    }

    private fun validateUserProjectPermission(
        userId: String,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        logger.info("[rbac] validate user project permission|userId = $userId|permission=$permission")
        val startEpoch = System.currentTimeMillis()
        try {
            val action = RbacAuthUtils.buildAction(permission, authResourceType = AuthResourceType.PROJECT)
            val instanceDTO = InstanceDTO()
            instanceDTO.system = iamConfiguration.systemId
            instanceDTO.id = projectCode
            instanceDTO.type = AuthResourceType.PROJECT.value
            return authHelper.isAllowed(
                userId,
                action,
                instanceDTO
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to validate user project permission"
            )
        }
    }
}
