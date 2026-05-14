package com.lucathomas.stockscanner

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class DataViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DataRepository
    val allData: LiveData<List<Data>>

    private val _currentProduct = MutableLiveData<OFFProduct?>()
    val currentProduct: LiveData<OFFProduct?> = _currentProduct

    // Fired when a Bring! item was checked off via scanner → BringFragment should refresh
    private val _bringRefreshTrigger = MutableLiveData<Unit>()
    val bringRefreshTrigger: LiveData<Unit> = _bringRefreshTrigger

    init {
        val dataDao = MainActivity.DataDatabase.getDatabase(application).dataDao()
        repository = DataRepository(dataDao)
        allData = repository.allData.asLiveData()
    }

    fun setProduct(product: OFFProduct?) {
        _currentProduct.value = product
        if (product != null && product.name.isNotEmpty()) {
            saveToHistory(product)
        }
    }

    fun triggerBringRefresh() {
        _bringRefreshTrigger.postValue(Unit)
    }

    fun selectFromHistory(data: Data) {
        val product = OFFProduct(
            ean = data.ean,
            name = data.name,
            brand = data.brandName,
            category = data.category,
            imageUrl = data.image,
            nutriScore = data.nutriScore,
            nutriments = try { JSONObject(data.nutrimentsJson) } catch (e: Exception) { JSONObject() },
            ingredients = data.ingredients,
            allergens = data.allergens,
            additives = data.additives,
            novaGroup = data.novaGroup,
            ecoScore = data.ecoScore
        )
        _currentProduct.value = product
    }

    private fun saveToHistory(product: OFFProduct) = viewModelScope.launch {
        val entity = Data(
            ean = product.ean,
            name = product.name,
            brandName = product.brand,
            category = product.category,
            image = product.imageUrl,
            nutriScore = product.nutriScore,
            ingredients = product.ingredients,
            nutrimentsJson = product.nutriments.toString(),
            allergens = product.allergens,
            additives = product.additives,
            novaGroup = product.novaGroup,
            ecoScore = product.ecoScore
        )
        repository.insert(entity)
    }

    fun clearHistory() = viewModelScope.launch {
        repository.deleteAll()
    }
}
