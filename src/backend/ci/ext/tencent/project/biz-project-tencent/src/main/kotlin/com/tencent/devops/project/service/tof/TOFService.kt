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

package com.tencent.devops.project.service.tof

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.constant.CommonMessageCode.SUCCESS
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.monitoring.api.service.StatusReportResource
import com.tencent.devops.monitoring.pojo.UsersStatus
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.constant.ProjectMessageCode.FAILED_USER_INFORMATION
import com.tencent.devops.project.constant.ProjectMessageCode.QUERY_ORG_FAIL
import com.tencent.devops.project.constant.ProjectMessageCode.QUERY_PAR_DEPARTMENT_FAIL
import com.tencent.devops.project.constant.ProjectMessageCode.QUERY_SUB_DEPARTMENT_FAIL
import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.StaffInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.pojo.tof.APIModule
import com.tencent.devops.project.pojo.tof.ChildDeptRequest
import com.tencent.devops.project.pojo.tof.ChildDeptResponse
import com.tencent.devops.project.pojo.tof.DeptInfoRequest
import com.tencent.devops.project.pojo.tof.DeptInfoResponse
import com.tencent.devops.project.pojo.tof.DeptStaffsRequest
import com.tencent.devops.project.pojo.tof.ParentDeptInfoRequest
import com.tencent.devops.project.pojo.tof.Response
import com.tencent.devops.project.pojo.tof.StaffInfoRequest
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectUserService
import com.tencent.devops.project.utils.CostUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * API
 * /esb/docs/ieod/system/tof/
 */
@Service
class TOFService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val userService: ProjectUserService
) {

    @Value("\${tof.host:#{null}}")
    private val tofHost: String? = null

    @Value("\${tof.appCode:#{null}}")
    private val tofAppCode: String? = null

    @Value("\${tof.appSecret:#{null}}")
    private val tofAppSecret: String? = null

    init {
        logger.info("Get the tof host($tofHost), code($tofAppCode) and secret($tofAppSecret)")
    }

    private val userInfoCache = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, StaffInfo>()

    private val userDeptCache = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, UserDeptDetail>()
    private val deptStaffsCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*dept-level*/, List<StaffInfo>>()

    fun getUserDeptDetail(operator: String?, userId: String, bkTicket: String): UserDeptDetail {
        validate()
        var detail = userDeptCache.getIfPresent(userId)
        if (detail == null) {
            detail = getDeftFromCache(userId) ?: getDeptFromTof(operator, userId, bkTicket)
            if (detail == null) {
                logger.info("user $userId is level office")
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = FAILED_USER_INFORMATION,
                        defaultMessage = "用户$userId 已离职",
                        params = arrayOf(userId),
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            userDeptCache.put(userId, detail)
        }

        return detail!!
    }

    fun getUserDeptDetail(userId: String): UserDeptDetail {
        return getUserDeptDetail(userId, "")
    }

    fun getUserDeptDetail(userId: String, bkTicket: String): UserDeptDetail {
        return getUserDeptDetail(null, userId, bkTicket)
    }

    fun getOrganizationInfo(
        userId: String,
        type: OrganizationType,
        id: Int
    ): List<OrganizationInfo> {
        validate()
        return getChildDeptInfos(userId, type, id).map {
            OrganizationInfo(it.ID, it.Name)
        }
    }

    fun getDeptInfo(userId: String, id: Int): DeptInfo {
        try {
            val path = "get_dept_info"
            val startTime = System.currentTimeMillis()
            val responseContent = request(
                path,
                DeptInfoRequest(
                    tofAppCode!!,
                    tofAppSecret!!,
                    id.toString()
                ),
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
            val response: Response<DeptInfoResponse> =
                objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail to get the dept info of id $id with response $responseContent")
                uploadTofStatus(
                    requestTime = startTime,
                    statusCode = response.code,
                    statusMessage = response.message,
                    errorCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL,
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            uploadTofStatus(
                requestTime = startTime,
                statusCode = response.code,
                statusMessage = "success",
                errorCode = SUCCESS,
                errorMessage = "call tof success"
            )
            val deptInfoResp = response.data
            return DeptInfo(
                deptInfoResp.TypeId,
                deptInfoResp.LeaderId,
                deptInfoResp.Name,
                deptInfoResp.Level,
                deptInfoResp.Enabled,
                deptInfoResp.ParentId,
                deptInfoResp.ID
            )
        } catch (e: Exception) {
            logger.warn("Fail to get the organization info of id $id", e)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    private fun getChildDeptInfos(userId: String, type: OrganizationType, id: Int): List<ChildDeptResponse> {
        try {
            val startTime = System.currentTimeMillis()
            val path = "get_child_dept_infos"
            val responseContent = request(
                path, ChildDeptRequest(
                    tofAppCode!!,
                    tofAppSecret!!,
                    getParentDeptIdByOrganizationType(type, id),
                    1
                ), I18nUtil.getCodeLanMessage(
                    messageCode = QUERY_SUB_DEPARTMENT_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
            val response: Response<List<ChildDeptResponse>> =
                objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail o get the child dept info of type $type and id $id with response $responseContent")
                uploadTofStatus(
                    requestTime = startTime,
                    statusCode = response.code,
                    statusMessage = response.message,
                    errorCode = QUERY_SUB_DEPARTMENT_FAIL,
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = QUERY_SUB_DEPARTMENT_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = QUERY_SUB_DEPARTMENT_FAIL,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            uploadTofStatus(
                requestTime = startTime,
                statusCode = response.code,
                statusMessage = "success",
                errorCode = SUCCESS,
                errorMessage = "call tof success"
            )
            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the organization info of type $type and id $id", t)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = QUERY_SUB_DEPARTMENT_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    private fun getParentDeptIdByOrganizationType(type: OrganizationType, id: Int): Int {
        return when (type) {
            OrganizationType.bg -> 0
            else -> id
        }
    }

    fun getStaffInfo(
        operator: String?,
        userId: String,
        bkTicket: String,
        userCache: Boolean? = true
    ): StaffInfo {
        try {
            var info: StaffInfo? = null
            if (userCache!!) {
                info = userInfoCache.getIfPresent(userId)
            }
            if (info == null) {
                val startTime = System.currentTimeMillis()
                logger.info("[$operator|$userId|$bkTicket] Start to get the staff info")
                val path = "get_staff_info_by_login_name"
                val responseContent = request(
                    path, StaffInfoRequest(
                        tofAppCode!!,
                        tofAppSecret!!, operator, userId, bkTicket
                    ), I18nUtil.getCodeLanMessage(
                        messageCode = FAILED_USER_INFORMATION,
                        language = I18nUtil.getLanguage(userId),
                        params = arrayOf(userId)
                    )
                )
                val response: Response<StaffInfo> = objectMapper.readValue(responseContent)
                if (response.data == null) {
                    uploadTofStatus(
                        requestTime = startTime,
                        statusCode = response.code,
                        statusMessage = response.message,
                        errorCode = FAILED_USER_INFORMATION,
                        errorMessage = I18nUtil.getCodeLanMessage(
                            messageCode = FAILED_USER_INFORMATION,
                            language = I18nUtil.getLanguage(userId),
                            params = arrayOf(userId)
                        )
                    )
                    logger.warn("Fail to get the staff info|$userId|$bkTicket|$responseContent")
                    throw OperationException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = FAILED_USER_INFORMATION,
                            language = I18nUtil.getLanguage(userId),
                            params = arrayOf(userId)
                        )
                    )
                }
                uploadTofStatus(
                    requestTime = startTime,
                    statusCode = response.code,
                    statusMessage = "success",
                    errorCode = SUCCESS,
                    errorMessage = "call tof success"
                )
                info = response.data
                userInfoCache.put(userId, info)
            }
            return info
        } catch (t: Throwable) {
            logger.warn("Fail to get the staff info of userId $userId with ticket $bkTicket", t)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = FAILED_USER_INFORMATION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(userId)
                )
            )
        }
    }

    fun getStaffInfo(userId: String, bkTicket: String): StaffInfo {
        return getStaffInfo(null, userId, bkTicket)
    }

    fun getStaffInfo(userId: String): StaffInfo {
        return getStaffInfo(null, userId, "")
    }

    fun getParentDeptInfo(groupId: String, level: Int): List<DeptInfo> {
        try {
            val path = "get_parent_dept_infos"
            val startTime = System.currentTimeMillis()
            val responseContent = request(
                path,
                ParentDeptInfoRequest(tofAppCode!!, tofAppSecret!!, groupId, level),
                I18nUtil.getCodeLanMessage(
                    messageCode = QUERY_ORG_FAIL
                )
            )
            val response: Response<List<DeptInfo>> = objectMapper.readValue(responseContent)
            if (response.data == null) {
                logger.warn("Fail to get the parent dept info of |$groupId|$level|$responseContent")
                uploadTofStatus(
                    requestTime = startTime,
                    statusCode = response.code,
                    statusMessage = response.message,
                    errorCode = QUERY_ORG_FAIL,
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = QUERY_ORG_FAIL
                    )
                )
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = QUERY_ORG_FAIL
                    )
                )
            }
            uploadTofStatus(
                requestTime = startTime,
                statusCode = response.code,
                statusMessage = "success",
                errorCode = SUCCESS,
                errorMessage = "call tof success"
            )

            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the parent dept info of group $groupId and level $level", t)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = QUERY_PAR_DEPARTMENT_FAIL
                )
            )
        }
    }

    private fun request(path: String, body: Any, errorMessage: String, apiModule: APIModule = APIModule.tof): String {
        val url = "http://$tofHost/component/compapi/${apiModule.name}/$path"
        val requestContent = objectMapper.writeValueAsString(body)
        val startTime = System.currentTimeMillis()
        logger.info("Start to request $url with body $requestContent")
        val requestBody = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent))
            .build()
        val response = request(requestBody, errorMessage)
        logger.info("Get the response $response of request $url")
        CostUtils.costTime(
            startTime = startTime,
            url = url,
            logger = logger
        )
        return response
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = HttpUtil.getHttpClient()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request $request with code ${response.code}, " +
                            "message ${response.message} and body $responseContent"
                )
                throw RuntimeException(errorMessage)
            }
            return responseContent
        }
    }

    private fun validate() {
        if (tofHost.isNullOrBlank()) {
            throw RuntimeException("TOF HOST is empty")
        }
        if (tofAppCode.isNullOrBlank()) {
            throw RuntimeException("TOF app code is empty")
        }
        if (tofAppSecret.isNullOrBlank()) {
            throw RuntimeException("TOF app secret is empty")
        }
    }

    private fun uploadTofStatus(
        requestTime: Long,
        statusCode: String,
        statusMessage: String,
        errorCode: String,
        errorMessage: String
    ) {
        val responseTime = System.currentTimeMillis()
        val usersStatus = UsersStatus(
            projectId = null,
            buildId = null,
            vmSeqId = null,
            pipelineId = null,
            channelCode = null,
            requestTime = requestTime,
            responseTime = responseTime,
            elapseTime = responseTime - requestTime,
            statusCode = statusCode,
            statusMessage = statusMessage,
            errorCode = errorCode,
            errorMsg = errorMessage
        )
        try {
            client.get(StatusReportResource::class).userUsers(usersStatus)
        } catch (e: Exception) {
            logger.warn("uploadTofStatus fail, error msg:$e")
        }
    }

    private fun getDeftFromCache(userId: String): UserDeptDetail? {
        val bkCacheDeft = userService.getUserDept(userId)
        if (bkCacheDeft != null) {
            return bkCacheDeft
        }
        return null
    }

    fun getDeptFromTof(
        operator: String?,
        userId: String,
        bkTicket: String,
        userCache: Boolean? = true
    ): UserDeptDetail? {
        logger.info("[$operator}|$userId|$bkTicket] Start to get the dept info")
        val staffInfo = getStaffInfo(operator, userId, bkTicket, userCache)
        if (checkUserLeave(staffInfo)) return null
        // 通过用户组查询父部门信息　(由于tof系统接口查询结构是从当前机构往上推查询，如果创建者机构层级大于4就查不完整1到3级的机构，所以查询级数设置为10)
        val deptInfos = getParentDeptInfo(staffInfo.groupId, 10) // 一共三级，从事业群->部门->中心
        var groupId = "0"
        var groupName = ""
        var bgId = "0"
        var bgName = ""
        var deptId = "0"
        var deptName = ""
        var centerId = "0"
        var centerName = ""
        groupId = staffInfo.groupId
        groupName = staffInfo.groupName
        for (deptInfo in deptInfos) {
            val level = deptInfo.level
            val name = deptInfo.name
            when (level) {
                "1" -> {
                    bgName = name
                    bgId = deptInfo.id
                }

                "2" -> {
                    deptName = name
                    deptId = deptInfo.id
                }

                "3" -> {
                    centerName = name
                    centerId = deptInfo.id
                }
            }
        }
        return UserDeptDetail(
            bgName = bgName,
            bgId = bgId,
            deptName = deptName,
            deptId = deptId,
            centerName = centerName,
            centerId = centerId,
            groupId = groupId,
            groupName = groupName
        )
    }

    fun checkUserLeave(userInfo: StaffInfo): Boolean {
        // 没有bgId的用户，一律视为离职用户
        if (userInfo.statusId.toInt() == 2) {
            return true
        }
        return false
    }

    fun getDeptStaffsWithLevel(deptId: String, level: Int): List<StaffInfo> {
        val deptStaffs = deptStaffsCache.getIfPresent("$deptId-$level")
        if (null != deptStaffs) {
            return deptStaffs
        }
        try {
            val startTime = System.currentTimeMillis()
            logger.info("[$deptId|$level] Start to get the dept staffs")
            val path = "get_dept_staffs_with_level"
            val responseContent = request(
                path, DeptStaffsRequest(
                    dept_id = deptId,
                    level = level,
                    app_code = tofAppCode!!,
                    app_secret = tofAppSecret!!
                ), I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL
                )
            )
            val response: Response<List<StaffInfo>> = objectMapper.readValue(responseContent)
            if (response.data == null) {
                uploadTofStatus(
                    requestTime = startTime,
                    statusCode = response.code,
                    statusMessage = response.message,
                    errorCode = FAILED_USER_INFORMATION,
                    errorMessage = I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL
                    )
                )
                logger.warn("Fail to get the dept staffs|$deptId|$level|$responseContent")
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL
                    )
                )
            }
            uploadTofStatus(
                requestTime = startTime,
                statusCode = response.code,
                statusMessage = "success",
                errorCode = SUCCESS,
                errorMessage = "call tof success"
            )
            return response.data
        } catch (t: Throwable) {
            logger.warn("Fail to get the dept staffs of deptId $deptId with level $level", t)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TOFService::class.java)
    }
}
