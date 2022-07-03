package com.example.cryptoapp.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.cryptoapp.data.database.AppDatabase
import com.example.cryptoapp.data.mapper.CoinMapper
import com.example.cryptoapp.data.network.ApiFactory
import com.example.cryptoapp.domain.entity.CoinInfo
import com.example.cryptoapp.domain.repository.CoinRepository
import kotlinx.coroutines.delay

class CoinRepositoryImpl(
   private val application: Application
) : CoinRepository {

    private val coinInfoDao = AppDatabase.getInstance(application).coinPriceInfoDao()
    private val mapper = CoinMapper()
    private val apiService = ApiFactory.apiService

    override fun getCoinInfoList(): LiveData<List<CoinInfo>> {
        return Transformations.map(coinInfoDao.getPriceList()){
            it.map { dbModel->
                mapper.mapDbModelToEntity(dbModel)
            }
        }
    }

    override fun getCoinInfo(fromSymbol: String): LiveData<CoinInfo> {
        return Transformations.map(coinInfoDao.getPriceInfoAboutCoin(fromSymbol)){
            mapper.mapDbModelToEntity(it)
        }
    }

    override suspend fun loadData() {
        while (true) {
            val topCoins = apiService.getTopCoinsInfo(limit = 50)
            val fSymbols = mapper.mapNamesListToString(topCoins)
            val jsonContainer = apiService.getFullPriceList(fSyms = fSymbols)
            val coinInfoListDto = mapper.mapJsonContainerToListCoinInfo(jsonContainer)
            val dbModelList = coinInfoListDto.map { mapper.mapDtoToDbModel(it) }
            coinInfoDao.insertPriceList(dbModelList)
            delay(10000)
        }
    }

}