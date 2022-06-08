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

package com.tencent.devops.rds.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.rds.pojo.ProductDetailReq
import com.tencent.devops.rds.pojo.ProductDetailResp
import com.tencent.devops.rds.pojo.ProductListReq
import com.tencent.devops.rds.pojo.RdsProductStatusResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_RDS_PRODUCT"], description = "user-product资源")
@Path("/user/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRdsProductResource {

    @ApiOperation("初始化RDS产品chart")
    @POST
    @Path("/init")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun init(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("chart名称", required = false)
        @FormDataParam("chartName")
        chartName: String,
        @ApiParam("cli替换后的压缩包chart", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<RdsProductStatusResult>

    @ApiOperation("升级RDS产品chart")
    @POST
    @Path("/upgrade")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upgrade(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("chart名称", required = false)
        @FormDataParam("chartName")
        chartName: String,
        @ApiParam("cli替换后的压缩包chart", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<RdsProductStatusResult>

    @ApiOperation("获取product状态列表")
    @POST
    @Path("/list")
    fun list(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("列表请求信息", required = true)
        req: ProductListReq
    ): Result<Page<RdsProductStatusResult>>

    @ApiOperation("获取product详情")
    @POST
    @Path("/{productCode}")
    fun get(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("productCode")
        productCode: String,
        @ApiParam("详情请求信息", required = true)
        detail: ProductDetailReq
    ): Result<ProductDetailResp>

    @ApiOperation("获取product状态")
    @GET
    @Path("/{productCode}/status")
    fun status(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("productCode")
        productCode: String
    ): Result<RdsProductStatusResult?>
}