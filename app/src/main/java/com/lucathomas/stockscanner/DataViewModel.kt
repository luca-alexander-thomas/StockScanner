package com.lucathomas.stockscanner

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class DataViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: DataRepository
    val allData: LiveData<List<Data>>

    private val _currentProduct = MutableLiveData<Product?>()
    val currentProduct: LiveData<Product?> = _currentProduct

    init {
        val dataDao = MainActivity.DataDatabase.getDatabase(application).dataDao()
        repository = DataRepository(dataDao)
        allData = repository.allData.asLiveData()
    }

    fun setProduct(product: Product?) {
        _currentProduct.value = product
        
        // Automatisches Speichern in der Historie, wenn ein Produkt gefunden wurde
        if (product != null && product.name.isNotEmpty()) {
            saveToHistory(product)
        }
    }

    /**
     * Lädt ein Produkt aus der Historie in die Detailansicht
     */
    fun selectFromHistory(data: Data) {
        val product = Product(
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

    private fun saveToHistory(product: Product) = viewModelScope.launch {
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