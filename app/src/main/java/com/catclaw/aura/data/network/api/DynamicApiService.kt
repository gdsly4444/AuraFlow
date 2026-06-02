package com.catclaw.aura.data.network.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Generic Retrofit service: pass a fully resolved [url] (absolute). Retrofit base URL is only a placeholder.
 */
internal interface DynamicApiService {

    @GET
    suspend fun get(
        @Url url: String,
        @QueryMap(encoded = true) queryParams: Map<String, String>,
        @HeaderMap headers: Map<String, String>,
    ): Response<ResponseBody>

    @POST
    suspend fun postJson(
        @Url url: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun postForm(
        @Url url: String,
        @FieldMap(encoded = true) fields: Map<String, String>,
        @HeaderMap headers: Map<String, String>,
    ): Response<ResponseBody>
}
