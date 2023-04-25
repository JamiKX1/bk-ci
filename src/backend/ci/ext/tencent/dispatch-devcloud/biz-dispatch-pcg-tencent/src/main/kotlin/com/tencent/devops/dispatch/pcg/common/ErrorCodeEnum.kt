package com.tencent.devops.dispatch.pcg.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class ErrorCodeEnum(
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2133023, "2133023"),//Dispatcher-pcg系统错误
    IMAGE_ILLEGAL_ERROR(2133024, "2133024"),//The pcg dispatch image is illegal
    START_UP_ERROR(2133025, "2133025"),//Start up pcg docker error, response is null
    START_UP_RESPONSE_JSON_ERROR(2133026, "2133026"),//Fail to start up pcg docker, parse responseJson error
    START_UP_FAIL(2133027, "2133027");//Fail to start up pcg docker

}
