package com.example.basededatos.network

import com.example.basededatos.model.University
import retrofit2.http.GET
import retrofit2.http.Query

interface UniversityApi {
    @GET("search")
    suspend fun getUniversities(
        @Query("country") country: String? = null,
        @Query("name") name: String? = null
    ): List<University>
}