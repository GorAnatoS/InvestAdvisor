package com.invest.advisor.data.network.yahooResponse

import com.invest.advisor.data.network.ConnectivityInterceptor
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.http.GET
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Path

/**
 * Created by qsufff on 7/26/2020.
 */


//https://query1.finance.yahoo.com/v10/finance/quoteSummary/YNDX.ME?modules=assetProfile%2CfinancialData%2Cprice


interface YahooApiService{
    @GET("{assetName}?modules=assetProfile%2CfinancialData%2Cprice")
    fun getAssetProfileAsync(
        @Path("assetName") assetName: String
    ): Deferred<YahooResponse>

    companion object {
        operator fun invoke(
            connectivityInterceptor: ConnectivityInterceptor
        ): YahooApiService {
            val requestInterceptor = Interceptor {chain ->
                val url = chain.request()
                    .url()
                    .newBuilder()
                    //.addQueryParameter("key", API_KEY)
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .addInterceptor(connectivityInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://query1.finance.yahoo.com/v10/finance/quoteSummary/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YahooApiService::class.java)
        }
    }
}