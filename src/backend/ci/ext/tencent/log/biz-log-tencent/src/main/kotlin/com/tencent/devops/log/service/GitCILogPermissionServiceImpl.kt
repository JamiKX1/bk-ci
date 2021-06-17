package com.tencent.devops.log.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCILogPermissionServiceImpl @Autowired constructor(
    val client: Client
) : LogPermissionService {
    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        val action = permission?.value ?: AuthPermission.VIEW.value
        logger.info("GitCILogPermissionServiceImpl user:$userId projectId: $projectCode ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId, action, projectCode, AuthResourceType.PIPELINE_DEFAULT.value).data ?: false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCILogPermissionServiceImpl::class.java)
    }
}
