package com.lucathomas.stockscanner

import kotlinx.coroutines.flow.Flow

class DataRepository(private val dataDao: DataDao) {
    
    val allData: Flow<List<Data>> = dataDao.getAllDataFlow()

    suspend fun insert(data: Data) {
        dataDao.insert(data)
    }

    suspend fun findByEan(ean: String): Data? {
        return dataDao.findByEan(ean)
    }
    
    suspend fun delete(data: Data) {
        dataDao.delete(data)
    }

    suspend fun deleteAll() {
        dataDao.deleteAll()
    }
}