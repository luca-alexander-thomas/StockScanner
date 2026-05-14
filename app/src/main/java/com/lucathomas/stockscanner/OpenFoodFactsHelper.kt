package com.lucathomas.stockscanner

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import org.json.JSONObject

object FoodFactsHelper {

    private const val BASE_URL = "https://world.openfoodfacts.org/api/v0/product/"

    fun fetchProductData(context: Context, ean: String, callback: (OFFProduct?) -> Unit) {
        val url = "$BASE_URL$ean.json"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.optInt("status")
                    if (status == 1) {
                        val productJson = response.getJSONObject("product")
                        val product = OFFProduct(
                            ean = ean,
                            name = productJson.optString("product_name", context.getString(R.string.unknown_product)),
                            nameDe = productJson.optString("product_name_de", ""),
                            genericName = productJson.optString("generic_name", ""),
                            brand = productJson.optString("brands", context.getString(R.string.unknown_brand)),
                            category = productJson.optString("categories", context.getString(R.string.no_category)),
                            imageUrl = productJson.optString("image_url", ""),
                            nutriScore = productJson.optString("nutriscore_grade", context.getString(R.string.unknown)),
                            ingredients = productJson.optString("ingredients_text", context.getString(R.string.no_ingredients_info)),
                            nutriments = productJson.optJSONObject("nutriments") ?: JSONObject(),
                            allergens = productJson.optString("allergens_from_ingredients", ""),
                            additives = productJson.optString("additives_tags", "").replace("en:", "").replace("de:", ""),
                            novaGroup = productJson.optString("nova_group", context.getString(R.string.unknown)),
                            ecoScore = productJson.optString("ecoscore_grade", context.getString(R.string.unknown))
                        )
                        callback(product)
                    } else {
                        callback(null)
                    }
                } catch (e: JSONException) {
                    Log.e("FoodFactsHelper", "JSON parsing error", e)
                    callback(null)
                }
            },
            { error ->
                Log.e("FoodFactsHelper", "Request error: ${error.networkResponse?.statusCode}", error)
                callback(null)
            }
        )

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }
}

data class OFFProduct(
    val ean: String,
    val name: String,
    val nameDe: String = "",
    val genericName: String = "",
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
