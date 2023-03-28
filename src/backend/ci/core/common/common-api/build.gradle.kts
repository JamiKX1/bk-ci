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

dependencies {
    api("javax.ws.rs:javax.ws.rs-api")
    api("io.swagger:swagger-annotations")
    api("org.hashids:hashids")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-base")
    api("org.bouncycastle:bcprov-jdk15on")
    api("com.github.fge:json-schema-validator")
    api("com.google.guava:guava")
    api("com.squareup.okhttp3:okhttp")
    api("commons-codec:commons-codec")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.apache.commons:commons-compress")
    api("org.apache.commons:commons-exec")
    api("org.apache.commons:commons-collections4")
    api("javax.servlet:javax.servlet-api")
    api("javax.validation:validation-api")
    api("com.vdurmont:emoji-java")
    api("org.apache.lucene:lucene-core")
    api("org.apache.commons:commons-csv")
    api("com.github.ben-manes.caffeine:caffeine")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
