package com.example.basededatos.model

import com.google.gson.annotations.SerializedName

data class University(
    @SerializedName("name") val name: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("alpha_two_code") val alphaTwoCode: String?,
    @SerializedName("state-province") val stateProvince: String?,
    @SerializedName("domains") val domains: List<String>?,
    @SerializedName("web_pages") val webPages: List<String>?
)