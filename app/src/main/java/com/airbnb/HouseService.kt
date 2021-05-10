package com.airbnb

import retrofit2.Call
import retrofit2.http.GET

interface HouseService {
    @GET("/v3/6cdcd7b9-3ed4-4902-9ce2-99bb0d198248")
    fun getHouseList(): Call<HouseDto>
}