package com.invest.advisor.data.network.moexResponse

//import com.invest.advisor.data.db.entity.MOEXdata
import com.invest.advisor.data.network.ConnectivityInterceptor
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.http.GET
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by qsufff on 7/26/2020.
 */

//http://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities/AFKS.xml?iss.meta=off&iss.only=securities&securities.columns=SECID,SECNAME,LATNAME

//http://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities/AFKS.json?iss.meta=off&iss.only=securities,marketdata

//http://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.json?iss.meta=off&iss.only=securities&securities.columns=SECID

interface MoexApiService{
    @GET("securities.json?iss.meta=off&iss.only=securities")
    fun getSecuritiesAsync():
            Deferred<SecuritiesResponse>

    @GET("securities.json?iss.meta=off&iss.only=marketdata")
    fun getMarketDataAsync():
            Deferred<MarketDataResponse>


    companion object {
        operator fun invoke(
            connectivityInterceptor: ConnectivityInterceptor
        ): MoexApiService {
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
                .baseUrl("http://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MoexApiService::class.java)
        }
    }
}