package com.invest.advisor.data.repository

import androidx.lifecycle.LiveData
import com.invest.advisor.data.db.entity.Securities
import com.invest.advisor.data.db.secList.SecListEntry


/**
 * Created by qsufff on 7/29/2020.
 */

interface SecuritiesRepository {
    suspend fun getSecuritiesList(): LiveData<List<Securities>>
}