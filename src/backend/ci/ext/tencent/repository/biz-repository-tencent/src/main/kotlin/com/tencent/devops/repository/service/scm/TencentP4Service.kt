package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.scm.api.ServiceP4Resource
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
@Primary
class TencentP4Service(
    private val repositoryService: RepositoryService,
    private val credentialService: CredentialService,
    private val client: Client
) : Ip4Service {
    override fun getChangelistFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<P4FileSpec> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        val repository = repositoryService.serviceGet(
            projectId = projectId,
            repositoryConfig =
            RepositoryConfigUtils.buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
        )
        val credentials = credentialService.getCredential(
            projectId = projectId,
            repository = repository
        )
        val username = credentials[0]
        if (username.isEmpty()) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
            )
        }
        if (credentials.size < 2) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
            )
        }
        val password = credentials[1]
        if (password.isEmpty()) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
            )
        }
        return client.getScm(ServiceP4Resource::class).getChangelistFiles(
            p4Port = repository.url,
            username = username,
            password = password,
            change = change
        ).data!!
    }
}
