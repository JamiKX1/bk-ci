package com.tencent.devops.auth.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthResourceType
import com.tencent.devops.model.auth.tables.records.TAuthResourceTypeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AuthResourceTypeDao {
    fun list(dslContext: DSLContext): Result<TAuthResourceTypeRecord> {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .where(DELETE.eq(false))
                .orderBy(ID.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TAuthResourceTypeRecord> {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc(), RESOURCE_TYPE)
                .limit(pageSize).offset((page - 1) * pageSize)
                .skipCheck()
                .fetch()
        }
    }

    fun batchUpdateAuthResourceType(
        dslContext: DSLContext,
        authActionResourceTypes: List<TAuthResourceTypeRecord>
    ) {
        if (authActionResourceTypes.isEmpty()) {
            return
        }
        with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            authActionResourceTypes.forEach {
                dslContext.update(this)
                    .set(NAME, it.name)
                    .set(DESC, it.desc)
                    .where(ID.eq(it.id))
                    .execute()
            }
        }
    }
}
