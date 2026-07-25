package com.searcher.zonenews.net


import com.searcher.zonenews.entry.ArticleDetailEntry
import com.searcher.zonenews.entry.CommonResponseEntry
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.entry.LoginEntry
import com.searcher.zonenews.entry.MyFormationEntry
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.entry.TopicListEntry
import com.searcher.zonenews.entry.ViewHisEntry
import com.searcher.zonenews.entry.PublisherRegionEntry
import com.searcher.zonenews.entry.PublisherInfoEntry
import com.searcher.zonenews.utils.network.exception.GenericResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query


interface AppHttpService {
    @POST("auth/login")
    suspend fun loginApp(
        @Body requestBody: RequestBody
    ): GenericResponse<LoginEntry> //登录App

    @GET("feed")
    suspend fun getHomeData(
        @Query("tag") tag: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("lang") language: String? = null
    ): HomeDataListEntry //首页数据

    @GET("feed/topic/{topic}")
    suspend fun getFeedByTopic(
        @Path("topic") topic: String,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("lang") language: String? = null
    ): HomeDataListEntry // Topic-specific feed

    @GET("search/trending")
    suspend fun querySearchList(
        @Query("lang") language: String? = null
    ): GenericResponse<SearchListEntry> //查询搜索

    @GET("search")
    suspend fun searchNewsByTitle(
        @Query("q") title: String,
        @Query("lang") language: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sortby") sortBy: String? = null
    ): GenericResponse<SearchListEntry>//搜索列表

    @GET("profile/sectors")
    suspend fun getSectors(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry> //获取所有行业板块

    @GET("profile/listtopics")
    suspend fun getRegions(
        @Query("lang") language: String? = null,
        @Query("type") type: String? = "regions"
    ): GenericResponse<TopicListEntry> //获取所有地区 (topic regions for topic selection menu, uses listtopics with type=regions)

    @GET("article/{id}")
    suspend fun queryArticleDetail(
        @Path("id") articleId: String,
        @Query("lang") language: String? = null
    ): GenericResponse<ArticleDetailEntry> //新闻详情

    @POST("article/{id}/feedback")
    suspend fun addFeedBack(
        @Path("id") articleId: String,
        @Body requestBody: RequestBody
    ): GenericResponse<CommonResponseEntry> //文章反馈

    @POST("profile/saveadd")
    suspend fun collectNews(@Query("articleID") id: String): GenericResponse<CommonResponseEntry>//收藏

    @POST("profile/reading-history")
    suspend fun saveNewsHis(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry>//添加记录

    @POST("profile/saved/delete")
    suspend fun deleteCollect(@Query("articleID") id: String): GenericResponse<CommonResponseEntry> //取消收藏

    @POST("profile/history/delete")
    suspend fun deleteHistory(@Query("articleID") id: String): GenericResponse<CommonResponseEntry> //删除浏览历史

    @GET("feed/personal")
    suspend fun queryPersonRecommend(
        @Query("offset") pageNo: Int,
        @Query("limit") pageSize: Int,
        @Query("lang") language: String? = null,
        @Query("sortby") sortBy: String? = null
    ): GenericResponse<SearchListEntry>//查询个人推荐

    @GET("feed/trending-topics")
    suspend fun getTrendingTopics(
        @Query("lang") language: String? = null,
        @Query("all") all: Boolean? = null
    ): GenericResponse<TopicListEntry> //获取热门话题 (all=true for all topics, all=false or null for 3-6 topics)

    @GET("profile/topics")
    suspend fun queryMyTopics(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry> //获取我的主题

    @GET("profile/listtopics")
    suspend fun queryAllTopics(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry>//获取所有主题

    @POST("profile/edittopic")
    suspend fun editTopic(
        @Query("action") actionType: String,
        @Query("topic") topic: String,
        @Query("lang") language: String? = null
    ): GenericResponse<CommonResponseEntry> //编辑话题 action:ADD/DELETE

    @GET("profile/publisher-region")
    suspend fun getPublisherRegions(
        @Query("lang") language: String? = null
    ): GenericResponse<PublisherRegionEntry> //获取发布者地区

    @POST("profile/publisher-region")
    suspend fun editPublisherRegion(
        @Query("action") action: String,
        @Query("tag") tag: String,
        @Query("lang") language: String? = null
    ): GenericResponse<CommonResponseEntry> //编辑发布者地区 action:ADD/REMOVE

    @GET("profile")
    suspend fun queryMyFormation(): GenericResponse<MyFormationEntry>//查询我的信息

    @GET("profile/history")
    suspend fun queryViewHis(
        @Query("lang") language: String? = null
    ): GenericResponse<ViewHisEntry> //浏览历史

    @GET("profile/saved")
    suspend fun queryMyCollect(
        @Query("lang") language: String? = null
    ): GenericResponse<ViewHisEntry>//我的收藏

    @POST("auth/logout")
    suspend fun logoutApp(): GenericResponse<CommonResponseEntry> //退出登录

    @POST("auth/register")
    suspend fun registerApp(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry> //注册

    @POST("auth/firebase")
    suspend fun loginWithFirebase(@Body requestBody: RequestBody): GenericResponse<LoginEntry> //Firebase登录 (Google, Facebook, Apple)

    @GET("auth/refresh-token")
    suspend fun refreshToken(): GenericResponse<CommonResponseEntry> //刷新JWT Token

    @PUT("profile/language")
    suspend fun changeLanguage(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry> //更改语言

    @POST("profile/redeem")
    suspend fun redeemCode(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry> //兑换Zone News Pro代码

    @POST("profile/cancelsubscription")
    suspend fun cancelSubscription(): GenericResponse<CommonResponseEntry> //取消Pro订阅

    @GET("info/publisher/{id}")
    suspend fun getPublisherInfo(
        @Path("id") publisherId: Int,
        @Query("lang") language: String? = null
    ): GenericResponse<PublisherInfoEntry> //获取发布者信息

    @POST("profile/delete-account")
    suspend fun deleteAccount(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry> //删除账号

    @POST("profile/verify-purchase")
    suspend fun verifyPurchase(@Body requestBody: RequestBody): GenericResponse<CommonResponseEntry> //验证Google Play购买

    @GET("feed/levity")
    suspend fun getLevityFeed(
        @Query("lang") language: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): HomeDataListEntry // Levity Mode Feed
}
