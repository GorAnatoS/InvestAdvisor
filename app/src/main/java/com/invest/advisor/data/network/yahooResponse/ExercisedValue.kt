package com.invest.advisor.data.network.yahooResponse


import com.google.gson.annotations.SerializedName

data class ExercisedValue(
    val raw: Int,
    val fmt: Any?,
    val longFmt: String
)