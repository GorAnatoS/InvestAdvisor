package com.invest.advisor.data.network.yahooResponse


import com.google.gson.annotations.SerializedName

data class NumberOfAnalystOpinions(
    val raw: Int,
    val fmt: String,
    val longFmt: String
)