package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceIndexResource
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import java.util.Date

@RestResource
class AppExperienceIndexResourceImpl : AppExperienceIndexResource {
    override fun banners(userId: String, page: Int?, pageSize: Int?): Result<List<IndexBannerVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexBannerVO>()
        for (i in 1..(pageSize ?: 3)) {
            banners.add(
                IndexBannerVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    bannerUrl = "https://www.tencent.com/img/brief/pic.jpg"
                )
            )
        }

        return Result(banners)
    }

    override fun hots(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..(pageSize ?: 20)) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031467 + 1013L,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }

    override fun necessary(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..(pageSize ?: 10)) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031461 + 1013L,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }

    override fun newest(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..(pageSize ?: 19)) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031463 + 1013L,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }

        return Result(banners)
    }

    override fun hotCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..(pageSize ?: 30)) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031463 + 1013L,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }
        return Result(banners)
    }

    override fun newCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        // TODO 真正的实现
        val banners = mutableListOf<IndexAppInfoVO>()
        for (i in 1..(pageSize ?: 31)) {
            banners.add(
                IndexAppInfoVO(
                    experienceHashId = HashUtil.encodeIntId(i),
                    experienceName = "test_$i",
                    createTime = Date().time,
                    size = i * 1031463 + 1013L,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                )
            )
        }
        return Result(banners)
    }
}