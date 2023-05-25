package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.BkTag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import kotlin.math.log

@Service
@Primary
class TxMigrateCreatorFixServiceImpl @Autowired constructor(
    private val deptService: DeptService,
    private val client: Client,
    private val tokenService: ClientTokenService,
    private val bkTag: BkTag
) : MigrateCreatorFixService {
    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    override fun getProjectCreator(
        projectCode: String,
        authSystemType: AuthSystemType,
        projectCreator: String,
        projectUpdator: String?
    ): String? {
        return when {
            authSystemType == AuthSystemType.V3_AUTH_TYPE -> projectCreator.takeIf { isUserExist(it) }
            isUserExist(projectCreator) -> projectCreator
            projectUpdator != null && isUserExist(projectUpdator) -> projectUpdator
            else -> {
                val managers = bkTag.invokeByTag(prodTag) {
                    client.getGateway(ServiceProjectAuthResource::class).getProjectUsers(
                        token = tokenService.getSystemToken(null)!!,
                        projectCode = projectCode,
                        group = BkAuthGroup.MANAGER
                    ).data
                }
                logger.info("get project managers $managers")
                managers?.find { isUserExist(it) }
            }
        }
    }

    override fun getResourceCreator(
        projectCreator: String,
        resourceCreator: String
    ): String {
        return if (isUserExist(resourceCreator)) resourceCreator else projectCreator
    }

    private fun isUserExist(name: String): Boolean =
        deptService.getUserInfo(userId = "admin", name = name) != null

    companion object {
        private val logger = LoggerFactory.getLogger(TxMigrateCreatorFixServiceImpl::class.java)
    }
}
