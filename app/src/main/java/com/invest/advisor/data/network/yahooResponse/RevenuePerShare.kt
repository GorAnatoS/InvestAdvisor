package com.invest.advisor.data.network.yahooResponse


import com.google.gson.annotations.SerializedName

data class RevenuePerShare(
    val raw: Double,
    val fmt: String
)