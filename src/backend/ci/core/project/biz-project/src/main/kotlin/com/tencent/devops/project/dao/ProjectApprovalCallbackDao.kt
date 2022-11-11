package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProjectApprovalCallback
import com.tencent.devops.model.project.tables.records.TProjectApprovalCallbackRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class ProjectApprovalCallbackDao {
    fun create(
        dslContext: DSLContext,
        applicant: String,
        englishName: String,
        callbackId: String,
        sn: String
    ): Int {
        with(TProjectApprovalCallback.T_PROJECT_APPROVAL_CALLBACK) {
            return dslContext.insertInto(
                this,
                APPLICANT,
                ENGLISH_NAME,
                CALLBACK_ID, SN
            ).values(
                applicant,
                englishName,
                callbackId,
                sn
            ).execute()
        }
    }

    fun getCallbackBySn(
        dslContext: DSLContext,
        sn: String
    ): TProjectApprovalCallbackRecord? {
        with(TProjectApprovalCallback.T_PROJECT_APPROVAL_CALLBACK) {
            return dslContext.selectFrom(this).where(SN.eq(sn)).fetchAny()
        }
    }
}
