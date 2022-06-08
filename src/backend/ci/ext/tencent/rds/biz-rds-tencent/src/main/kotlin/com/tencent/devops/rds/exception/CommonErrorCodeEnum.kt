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

package com.tencent.devops.rds.exception

import com.tencent.devops.common.api.pojo.ErrorType

/**
 * 通用的error code
 * 2130021 - 2130030
 */
enum class CommonErrorCodeEnum(
    override val errorType: ErrorType,
    override val errorCode: String,
    override val formatErrorMessage: String
) : ErrorCodeEnum {
    PARAMS_FORMAT_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2130021",
        formatErrorMessage = "参数格式错误 %s"
    ),
    PRODUCT_PERMISSION_INVALID(
        errorType = ErrorType.USER,
        errorCode = "2130022",
        formatErrorMessage = "非管理员无法操作: %s"
    ),
    PRODUCT_NOT_EXISTS(
        errorType = ErrorType.USER,
        errorCode = "2130022",
        formatErrorMessage = "实践ID不存在: %s"
    ),
    INIT_PROJECT_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2130023",
        formatErrorMessage = "初始化蓝盾项目失败: %s"
    )
}