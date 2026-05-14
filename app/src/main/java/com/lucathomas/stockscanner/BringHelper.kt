package com.lucathomas.stockscanner

import android.content.Context
import android.util.Log
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject

object BringHelper {
    private const val BASE_URL = "https://api.getbring.com/rest/v2"
    private const val API_KEY = "cof4Nc6D8saplXjE3h3HXqHH8m7VU2i1Gs0g85Sp"

    private fun baseHeaders(token: String? = null): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers["X-BRING-API-KEY"] = API_KEY
        headers["X-BRING-CLIENT"] = "android"
        headers["X-BRING-APPLICATION"] = "bring"
        headers["X-BRING-COUNTRY"] = "DE"
        headers["Authorization"] = if (token != null) "Bearer $token" else "Bearer"
        return headers
    }

    fun login(context: Context, email: String, password: String, callback: (String?, String?) -> Unit) {
        val url = "$BASE_URL/bringauth"

        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val uuid = jsonResponse.optString("uuid")
                    val token = jsonResponse.optString("access_token")
                    if (uuid.isNotEmpty() && token.isNotEmpty()) {
                        callback(uuid, token)
                    } else {
                        Log.e("BringHelper", "Login response missing fields: $response")
                        callback(null, null)
                    }
                } catch (e: Exception) {
                    Log.e("BringHelper", "Login parse error", e)
                    callback(null, null)
                }
            },
            { error ->
                Log.e("BringHelper", "Login error: ${error.networkResponse?.statusCode} - ${error.message}")
                callback(null, null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = baseHeaders()
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("email" to email, "password" to password)
            }
        }
        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }

    fun getLists(context: Context, userUuid: String, token: String, callback: (List<BringList>?) -> Unit) {
        val url = "$BASE_URL/bringusers/$userUuid/lists"

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                try {
                    val listsArray = response.getJSONArray("lists")
                    val lists = mutableListOf<BringList>()
                    for (i in 0 until listsArray.length()) {
                        val obj = listsArray.getJSONObject(i)
                        lists.add(BringList(obj.getString("listUuid"), obj.getString("name")))
                    }
                    callback(lists)
                } catch (e: Exception) {
                    Log.e("BringHelper", "GetLists parse error", e)
                    callback(null)
                }
            },
            { error ->
                Log.e("BringHelper", "GetLists error: ${error.networkResponse?.statusCode}")
                callback(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = baseHeaders(token)
        }
        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }

    fun getFullList(context: Context, listUuid: String, token: String, callback: (BringListData?) -> Unit) {
        val url = "$BASE_URL/bringlists/$listUuid"

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                try {
                    val purchaseArray = response.optJSONArray("purchase")
                        ?: response.optJSONObject("items")?.optJSONArray("purchase")
                        ?: response.optJSONArray("items")
                    val recentlyArray = response.optJSONArray("recently")
                        ?: response.optJSONObject("items")?.optJSONArray("recently")

                    if (purchaseArray == null) {
                        Log.e("BringHelper", "getFullList: unexpected response keys: ${response.keys().asSequence().toList()}")
                    }
                    callback(BringListData(
                        purchase = parseItemArray(purchaseArray),
                        recently = parseItemArray(recentlyArray).take(30)
                    ))
                } catch (e: Exception) {
                    Log.e("BringHelper", "getFullList parse error", e)
                    callback(null)
                }
            },
            { error ->
                Log.e("BringHelper", "getFullList error: ${error.networkResponse?.statusCode}")
                callback(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = baseHeaders(token)
        }
        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }

    // addToPurchase=false → move to recently (check off)
    // addToPurchase=true  → move to purchase (re-add)
    fun updateItem(
        context: Context,
        listUuid: String,
        token: String,
        itemName: String,
        addToPurchase: Boolean = false,
        callback: (Boolean) -> Unit
    ) {
        val url = "$BASE_URL/bringlists/$listUuid"

        val request = object : StringRequest(Method.PUT, url,
            { callback(true) },
            { error ->
                Log.e("BringHelper", "updateItem error: ${error.networkResponse?.statusCode}")
                callback(false)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = baseHeaders(token)
            override fun getParams(): MutableMap<String, String> {
                return if (addToPurchase) hashMapOf("purchase" to itemName)
                else hashMapOf("recently" to itemName)
            }
        }
        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }

    fun productMatchesItem(product: OFFProduct, itemName: String): Boolean {
        val term = itemName.lowercase().trim()
        if (term.isEmpty()) return false

        val sources = listOfNotNull(
            product.name.takeIf { it.isNotBlank() },
            product.nameDe.takeIf { it.isNotBlank() },
            product.genericName.takeIf { it.isNotBlank() },
            product.brand.takeIf { it.isNotBlank() },
            product.category.takeIf { it.isNotBlank() }
        ).map { it.lowercase() }

        // Split sources into individual word tokens (handles German compounds via endsWith/startsWith)
        val sourceTokens = sources.flatMap { it.split(WORD_SPLIT) }.filter { it.length >= 3 }.toSet()
        val termTokens = term.split(WORD_SPLIT).filter { it.length >= 3 }

        val searchTerms = buildSet<String> {
            add(term)
            synonymMap[term]?.let { addAll(it) }
            termTokens.forEach { t -> synonymMap[t]?.let { addAll(it) } }
        }

        // Returns true if a single token matches any source token (exact or German compound suffix)
        // Only endsWith: in German, the base word is always last (Schlagsahne→sahne, Vollmilch→milch)
        // startsWith would match "Brotaufstrich" for "Brot" or "Speiseöle" for "speiseöl" — wrong
        fun tokenMatches(t: String): Boolean =
            t in sourceTokens ||
            (t.length >= 4 && sourceTokens.any { src -> src.endsWith(t) })

        for (st in searchTerms) {
            val stTokens = st.split(WORD_SPLIT).filter { it.length >= 3 }
            when {
                stTokens.isEmpty() -> Unit
                // Single token: direct compound match
                stTokens.size == 1 -> if (tokenMatches(stTokens[0])) return true
                // Multi-word synonym (e.g. "creme fraiche"): ALL tokens must be present
                // to avoid "creme" in "nuss-nougat-creme" being a false hit
                else -> if (stTokens.all { tokenMatches(it) }) return true
            }
        }

        // Reverse: source tokens matched back against the term via exact, compound, or synonyms
        for (srcToken in sourceTokens) {
            if (srcToken.length < 4) continue
            if (srcToken in termTokens) return true
            if (termTokens.any { t -> t.endsWith(srcToken) }) return true
            synonymMap[srcToken]?.let { syns ->
                if (term in syns || termTokens.any { it in syns }) return true
            }
        }

        return false
    }

    private val WORD_SPLIT = Regex("[\\s,;/()+\\-]+")

    private val synonymGroups: List<Set<String>> = listOf(
        setOf("sahne", "schlagsahne", "crème fraîche", "creme fraiche", "obers", "rahm",
              "kochsahne", "sauerrahm", "schmand", "cream", "whipping cream", "sour cream"),
        setOf("toast", "toastbrot", "sandwichbrot", "sandwich toast", "toastbrote"),
        setOf("brot", "vollkornbrot", "graubrot", "roggenbrot", "mischbrot", "bauernbrot", "weißbrot"),
        setOf("milch", "vollmilch", "halbfettmilch", "h-milch", "frischmilch", "magermilch",
              "fettarme milch", "uht-milch"),
        setOf("butter", "markenbutter", "süßrahmbutter", "sauerrahmbutter", "streichbutter"),
        setOf("ei", "eier", "hühnerei", "hühnereier", "frische eier"),
        setOf("mehl", "weizenmehl", "dinkelmehl", "roggenmehl", "vollkornmehl", "haushaltsmehl"),
        setOf("zucker", "rohrzucker", "kristallzucker", "haushaltszucker", "rübenzucker"),
        setOf("öl", "speiseöl", "pflanzenöl", "sonnenblumenöl", "rapsöl", "olivenöl"),
        setOf("margarine", "pflanzenmargarine", "streichfett", "halbfettmargarine"),
        setOf("joghurt", "naturjoghurt", "fruchtjoghurt", "yogurt", "jogurt", "joghurtzubereitung"),
        setOf("käse", "schnittkäse", "gouda", "emmentaler", "edamer", "camembert", "brie",
              "bergkäse", "tilsiter"),
        setOf("quark", "magerquark", "speisequark", "topfen", "frischkäse"),
        setOf("wurst", "aufschnitt", "salami", "fleischwurst", "brühwurst", "mortadella",
              "lyoner", "bierwurst"),
        setOf("schinken", "kochschinken", "rohschinken", "parmaschinken", "schwarzwälder schinken"),
        setOf("hähnchen", "huhn", "hühnchen", "geflügel", "chicken", "hähnchenbrustfilet",
              "hühnerbrust", "hähnchenfilet"),
        setOf("nudeln", "pasta", "spaghetti", "penne", "fusilli", "farfalle", "linguine",
              "tagliatelle", "rigatoni", "makkaroni"),
        setOf("kartoffeln", "kartoffel", "erdäpfel", "frühkartoffeln"),
        setOf("tomaten", "tomate", "paradeiser", "cocktailtomaten", "rispentomaten"),
        setOf("zwiebeln", "zwiebel", "schalotten", "lauchzwiebeln"),
        setOf("äpfel", "apfel", "apfel boskoop", "äpfeln"),
        setOf("müsli", "granola", "cornflakes", "cerealien", "frühstücksflocken", "haferflocken"),
        setOf("kaffee", "kaffeebohnen", "filterkaffee", "espresso", "kaffeepulver", "instantkaffee"),
        setOf("schokolade", "vollmilchschokolade", "zartbitterschokolade", "schoki", "tafelschokolade"),
        setOf("saft", "orangensaft", "apfelsaft", "fruchtsaft", "direktsaft", "multivitaminsaft"),
        setOf("wasser", "mineralwasser", "tafelwasser", "stilles wasser", "sprudelwasser"),
        setOf("lachs", "räucherlachs", "lachsfilet", "atlantiklachs", "wildlachs"),
        setOf("fisch", "fischfilet", "kabeljau", "seelachs", "pangasius", "tilapia"),
        setOf("reis", "langkornreis", "basmati", "jasminreis", "vollkornreis", "risottoreis"),
        setOf("banane", "bananen"),
        setOf("gurke", "gurken", "salatgurke"),
        setOf("paprika", "paprikaschote", "paprikaschoten"),
        setOf("salat", "kopfsalat", "feldsalat", "eisbergsalat", "rucola"),
        setOf("hackfleisch", "gehacktes", "hackfleischmischung", "rinderhackfleisch"),
        setOf("chips", "kartoffelchips", "tortillachips", "snack"),
        setOf("jodsalz", "salz", "meersalz", "tafelsalz", "speisesalz"),
        setOf("essig", "weinessig", "apfelessig", "balsamico", "balsamicoessig"),
    )

    private val synonymMap: Map<String, Set<String>> by lazy {
        buildMap<String, MutableSet<String>> {
            for (group in synonymGroups) {
                for (term in group) {
                    getOrPut(term) { mutableSetOf() }.addAll(group - term)
                }
            }
        }
    }

    private fun parseItemArray(array: JSONArray?): List<BringItem> {
        if (array == null) return emptyList()
        val items = mutableListOf<BringItem>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name").ifEmpty { obj.optString("itemId", "") }
                if (name.isNotEmpty()) {
                    items.add(BringItem(name, obj.optString("specification", "")))
                }
            } catch (_: Exception) {}
        }
        return items
    }
}

data class BringList(val uuid: String, val name: String)
data class BringItem(val name: String, val specification: String)
data class BringListData(val purchase: List<BringItem>, val recently: List<BringItem>)
