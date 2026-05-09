package com.lucathomas.stockscanner

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

object OpenFoodFactsHelper {

    private const val BASE_URL = "https://world.openfoodfacts.org/api/v0/product/"

    fun fetchProductData(context: Context, ean: String, callback: (Product?) -> Unit) {
        val url = "$BASE_URL$ean.json"
        val requestQueue: RequestQueue = Volley.newRequestQueue(context)

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.optInt("status")
                    if (status == 1) {
                        val productJson = response.getJSONObject("product")
                        val product = Product(
                            ean = ean,
                            name = productJson.optString("product_name", "Unbekanntes Produkt"),
                            brand = productJson.optString("brands", "Unbekannte Marke"),
                            category = productJson.optString("categories", "Keine Kategorie"),
                            imageUrl = productJson.optString("image_url", ""),
                            nutriScore = productJson.optString("nutriscore_grade", "unknown"),
                            ingredients = productJson.optString("ingredients_text", "Keine Angaben zu Inhaltsstoffen."),
                            nutriments = productJson.optJSONObject("nutriments") ?: JSONObject(),
                            allergens = productJson.optString("allergens_from_ingredients", ""),
                            additives = productJson.optString("additives_tags", "").replace("en:", "").replace("de:", ""),
                            novaGroup = productJson.optString("nova_group", "unknown"),
                            ecoScore = productJson.optString("ecoscore_grade", "unknown")
                        )
                        callback(product)
                    } else {
                        callback(null)
                    }
                } catch (e: JSONException) {
                    Log.e("OpenFoodFactsHelper", "JSON parsing error", e)
                    callback(null)
                }
            },
            { error ->
                Log.e("OpenFoodFactsHelper", "Request error", error)
                callback(null)
            }
        )

        requestQueue.add(jsonObjectRequest)
    }
}

data class Product(
    val ean: String,
    val name: String,
    val brand: String,
    val category: String,
    val imageUrl: String,
    val nutriScore: String,
    val nutriments: JSONObject,
    val ingredients: String,
    val allergens: String = "",
    val additives: String = "",
    val novaGroup: String = "unknown",
    val ecoScore: String = "unknown"
)