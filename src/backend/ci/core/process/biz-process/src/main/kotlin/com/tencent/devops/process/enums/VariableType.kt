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

package com.tencent.devops.process.enums

enum class VariableType(val hasPrefix: Boolean = false, val alisName: String = "") {
    // 预定义常量列表
    BK_CI_PIPELINE_ID,
    BK_CI_START_TYPE,
    BK_CI_PROJECT_NAME,
    BK_CI_PIPELINE_NAME,
    BK_CI_BUILD_ID,
    BK_CI_BUILD_NUM,
    BK_CI_BUILD_URL,
    BK_CI_BUILD_JOB_ID(alisName = "job.id"),
    BK_CI_BUILD_MSG,
    BK_CI_BUILD_TASK_ID(alisName = "step.id"),
    BK_CI_BUILD_REMARK,
    BK_CI_BUILD_FAIL_TASKS,
    BK_CI_BUILD_FAIL_TASKNAMES,
    BK_CI_TURBO_ID,
    BK_CI_MAJOR_VERSION,
    BK_CI_MINOR_VERSION,
    BK_CI_FIX_VERSION,
    BK_CI_BUILD_NO,
    BK_CI_PIPELINE_UPDATE_USER,
    BK_CI_PIPELINE_VERSION,
    BK_CI_PROJECT_NAME_CN,
    BK_CI_START_CHANNEL,
    BK_CI_START_USER_ID,
    BK_CI_START_USER_NAME,
    BK_CI_PARENT_PIPELINE_ID,
    BK_CI_PARENT_BUILD_ID,
    BK_CI_START_PIPELINE_USER_ID,
    BK_CI_START_WEBHOOK_USER_ID,
    BK_CI_RETRY_COUNT,
    BK_CI_ATOM_VERSION(alisName = "step.atom_version"),
    BK_CI_ATOM_CODE(alisName = "step.atom_code"),
    BK_CI_TASK_NAME(alisName = "step.name"),
    BK_CI_ATOM_NAME(alisName = "step.atom_name"),

    // GIT事件触发公共变量
    BK_CI_REPO_WEBHOOK_REPO_TYPE,
    BK_CI_REPO_WEBHOOK_REPO_URL,
    BK_CI_REPO_WEBHOOK_NAME,
    BK_CI_REPO_WEBHOOK_ALIAS_NAME,
    BK_CI_REPO_WEBHOOK_HASH_ID,
    BK_CI_REPO_GIT_WEBHOOK_COMMITID,
    BK_CI_REPO_GIT_WEBHOOK_EVENT_TYPE,
    BK_CI_REPO_GIT_WEBHOOK_INCLUDE_BRANCH,
    BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_BRANCH,
    BK_CI_REPO_GIT_WEBHOOK_INCLUDE_PATHS,
    BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_PATHS,
    BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_USERS,
    BK_CI_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH,
    BK_CI_GIT_WEBHOOK_FINAL_INCLUDE_PATH,
    BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_(hasPrefix = true), // 尾部可以通配n
    BK_CI_HOOK_MESSAGE,

    // GIT Commit Push Hook事件触发
    BK_CI_REPO_GIT_WEBHOOK_PUSH_USERNAME,
    BK_CI_REPO_GIT_WEBHOOK_BRANCH,
    BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT,
    BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT,
    BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_(hasPrefix = true), // 尾部可以通配n1和n2
    BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_(hasPrefix = true), // 尾部可以通配n1和n2
    BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_(hasPrefix = true), // 尾部可以通配n1和n2
    BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_COUNT,
    BK_REPO_GIT_WEBHOOK_PUSH_MODIFY_FILE_COUNT,
    BK_REPO_GIT_WEBHOOK_PUSH_DELETE_FILE_COUNT,
    BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND,
    BK_REPO_GIT_WEBHOOK_PUSH_ACTION_KIND,

    // GIT Merge Request Hook 或 Merge Request Hook Accept事件触发
    BK_CI_REPO_GIT_WEBHOOK_MR_AUTHOR,
    BK_CI_REPO_GIT_WEBHOOK_TARGET_URL,
    BK_CI_REPO_GIT_WEBHOOK_SOURCE_URL,
    BK_CI_REPO_GIT_WEBHOOK_TARGET_BRANCH,
    BK_CI_REPO_GIT_WEBHOOK_SOURCE_BRANCH,
    BK_CI_REPO_GIT_WEBHOOK_MR_CREATE_TIME,
    BK_CI_REPO_GIT_WEBHOOK_MR_UPDATE_TIME,
    BK_CI_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP,
    BK_CI_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP,
    BK_CI_REPO_GIT_WEBHOOK_MR_ID,
    BK_CI_REPO_GIT_WEBHOOK_MR_NUMBER,
    BK_CI_REPO_GIT_WEBHOOK_MR_DESC,
    BK_CI_REPO_GIT_WEBHOOK_MR_TITLE,
    BK_CI_REPO_GIT_WEBHOOK_MR_ASSIGNEE,
    BK_CI_REPO_GIT_WEBHOOK_MR_URL,
    BK_CI_REPO_GIT_WEBHOOK_MR_REVIEWERS,
    BK_CI_REPO_GIT_WEBHOOK_MR_MILESTONE,
    BK_CI_REPO_GIT_WEBHOOK_MR_MILESTONE_DUE_DATE,
    BK_CI_REPO_GIT_WEBHOOK_MR_LABELS,
    BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT,
    BK_REPO_GIT_WEBHOOK_MR_LAST_COMMIT_MSG,

    // GIT Tag Push Hook事件触发
    BK_CI_REPO_GIT_WEBHOOK_TAG_NAME,
    BK_CI_REPO_GIT_WEBHOOK_TAG_OPERATION,
    BK_CI_REPO_GIT_WEBHOOK_TAG_USERNAME,
    BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM,

    // GIT Code Review Hook事件触发
    BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_TYPE,
    BK_REPO_GIT_WEBHOOK_REVIEW_REVIEWABLE_ID,
    BK_REPO_GIT_WEBHOOK_REVIEW_RESTRICT_TYPE,
    BK_REPO_GIT_WEBHOOK_REVIEW_APPROVING_REVIEWERS,
    BK_REPO_GIT_WEBHOOK_REVIEW_APPROVED_REVIEWERS,

    // SVN事件触发
    BK_CI_REPO_SVN_WEBHOOK_REVERSION,
    BK_CI_REPO_SVN_WEBHOOK_USERNAME,
    BK_CI_REPO_SVN_WEBHOOK_COMMIT_TIME,
    BK_CI_REPO_SVN_WEBHOOK_INCLUDE_PATHS,
    BK_CI_REPO_SVN_WEBHOOK_EXCLUDE_PATHS,
    BK_CI_REPO_SVN_WEBHOOK_INCLUDE_USERS,
    BK_CI_REPO_SVN_WEBHOOK_EXCLUDE_USERS,

    // GITHUB触发事件公共变量
    BK_CI_REPO_GIT_WEBHOOK_INCLUDE_BRANCHS,
    BK_CI_REPO_GIT_WEBHOOK_EXCLUDE_BRANCHS,

    // GITHUB CREATE Branch Or Tag事件触发
    BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME,
    BK_CI_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE,
    BK_CI_REPO_GITHUB_WEBHOOK_CREATE_USERNAME

    // GITHUB Commit Push Hook事件触发
    // 已覆盖

    // GITHUB Pull Request Hook事件触发
    // 已覆盖
    ;

    companion object {
        fun validate(variableName: String): Boolean {
            values().forEach { enumObj ->
                if (enumObj.name == variableName) {
                    return true
                } else if (enumObj.hasPrefix && variableName.startsWith(enumObj.name)) {
                    return true
                }
            }
            return false
        }
    }
}
