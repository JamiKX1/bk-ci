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

package com.tencent.devops.sign.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.pojo.IosProfile
import com.tencent.devops.sign.pojo.IpaCustomizedSignRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Context

@Api(tags = ["USER_SIGN"], description = "用户接口-应用签名")
@Path("/user/sign")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSignResource {

    @ApiOperation("ipa包签名-定制证书")
    @POST
    @Path("/ipa/customized")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun ipaCustomizedSign(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("IPA包签名信息", required = true)
        ipaSignRequest: IpaCustomizedSignRequest,
        @ApiParam("ipa包文件", required = true)
        @FormDataParam("ipaFile")
        ipaInputStream: InputStream,
        @FormDataParam("ipaFile")
        ipaDisposition: FormDataContentDisposition
    ): Result<String?>

    @ApiOperation("获取Keystore某应用下所有证书/描述文件信息")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/parameters")
    @Path("/keystore/{appId}/certs")
    fun getKeystoreCerts(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("appId")
        appId: String
    ): Result<List<IosProfile>>
}