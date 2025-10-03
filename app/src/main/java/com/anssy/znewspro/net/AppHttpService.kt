package com.anssy.znewspro.net


import com.anssy.znewspro.entry.ArticleDetailEntry
import com.anssy.znewspro.entry.CommonResponseEntry
import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.entry.LoginEntry
import com.anssy.znewspro.entry.MyFormationEntry
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.entry.PublisherRegionEntry
import com.anssy.znewspro.entry.AboutUsEntry
import com.anssy.znewspro.utils.network.exception.GenericResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface AppHttpService {
    @POST("auth/login")
    suspend fun loginApp(
        @Body requestBody: RequestBody
    ): GenericResponse<LoginEntry> //登录App

    @POST("feed")
    suspend fun getHomeData(
        @Body requestBody: RequestBody,
        @Query("lang") language: String? = null
    ): HomeDataListEntry //首页数据

    @GET("search/trending")
    suspend fun querySearchList(
        @Query("lang") language: String? = null
    ): GenericResponse<SearchListEntry> //查询搜索

    @GET("search")
    suspend fun searchNewsByTitle(
        @Query("q") title: String,
        @Query("lang") language: String? = null
    ): GenericResponse<SearchListEntry>//搜索列表

    @GET("article/{id}")
    suspend fun queryArticleDetail(
        @Path("id") articleId: String,
        @Query("lang") language: String? = null
    ): ArticleDetailEntry //新闻详情

    @POST("article/{id}/feedback")
    suspend fun addFeedBack(
        @Path("id") articleId: String,
        @Body requestBody: RequestBody
    ): GenericResponse<CommonResponseEntry> //文章反馈

    @POST("profile/saveadd")
    suspend fun collectNews(@Query("articleID") id: String): GenericResponse<CommonResponseEntry>//收藏

    @POST("track/action")
    suspend fun saveNewsHis(@Query("articleID") id: String): GenericResponse<CommonResponseEntry>//添加记录

    @POST("profile/saved/delete")
    suspend fun deleteCollect(@Query("articleID") id: String): GenericResponse<CommonResponseEntry> //取消收藏

    @POST("profile/history/delete")
    suspend fun deleteHistory(@Query("articleID") id: String): GenericResponse<CommonResponseEntry> //删除浏览历史

    @POST("feed/personal")
    suspend fun queryPersonRecommend(
        @Query("offset") pageNo: Int,
        @Query("limit") pageSize: Int,
        @Query("lang") language: String? = null,
        @Query("sortby") sortBy: String? = null
    ): GenericResponse<SearchListEntry>//查询个人推荐

    @GET("feed/trending-topics")
    suspend fun getTrendingTopics(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry> //获取热门话题

    @GET("profile/topics")
    suspend fun queryMyTopics(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry> //获取我的主题

    @GET("profile/listtopics")
    suspend fun queryAllTopics(
        @Query("lang") language: String? = null
    ): GenericResponse<TopicListEntry>//获取所有主题

    @GET("profile/edittopic")
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

    @GET("info/aboutus")
    suspend fun getAboutUs(
        @Query("lang") language: String? = null
    ): GenericResponse<AboutUsEntry> //关于我们
}
