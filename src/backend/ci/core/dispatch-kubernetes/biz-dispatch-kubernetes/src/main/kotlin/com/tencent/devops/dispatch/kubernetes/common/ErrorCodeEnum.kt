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

package com.tencent.devops.dispatch.kubernetes.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2126032, "2126032"),
    // Dispatcher-kubernetes系统错误
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2126033, "2126033"),
    // Dispatcher-kubernetes 构建机启动失败，没有空闲的构建机
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2126034, "2126034"),
    // Dispatcher-kubernetes 异常，异常信息 - 构建机创建失败
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2126035, "2126035"),
    // Dispatcher-kubernetes 异常，异常信息 - 构建机启动失败
    CREATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126036, "2126036"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建容器接口异常
    CREATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126037, "2126037"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建容器接口返回失败
    OPERATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126038, "2126038"),
    // Dispatcher-kubernetes 异常，异常信息 - 操作容器接口异常
    OPERATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126039, "2126039"),
    // Dispatcher-kubernetes 异常，异常信息 - 操作容器接口返回失败
    VM_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126040, "2126040"),
    // Dispatcher-kubernetes 异常，异常信息 - 获取容器状态接口异常
    CREATE_IMAGE_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126041, "2126041"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建镜像接口异常
    CREATE_IMAGE_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126042, "2126042"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建镜像接口返回失败
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126043, "2126043"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建镜像新版本接口异常
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126044, "2126044"),
    // Dispatcher-kubernetes 异常，异常信息 - 创建镜像新版本接口返回失败
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126045, "2126045"),
    // Dispatcher-kubernetes 异常，异常信息 - 获取TASK状态接口异常
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126046, "2126046"),
    // Dispatcher-kubernetes 异常，异常信息 - 获取websocket接口异常
    WEBSOCKET_NO_GATEWAY_PROXY(ErrorType.SYSTEM, 2126047, "2126047");
    // 请检查webConsole网关代理配置
}
