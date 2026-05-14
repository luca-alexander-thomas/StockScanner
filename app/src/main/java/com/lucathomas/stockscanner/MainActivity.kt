package com.lucathomas.stockscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lucathomas.stockscanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: DataViewModel by viewModels()

    @Database(entities = [Data::class], version = 3, exportSchema = false)
    abstract class DataDatabase : RoomDatabase() {
        abstract fun dataDao(): DataDao
        companion object {
            @Volatile private var INSTANCE: DataDatabase? = null

            fun getDatabase(context: Context): DataDatabase {
                return INSTANCE ?: synchronized(this) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        DataDatabase::class.java,
                        "data_database"
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
                }
            }
        }
    }

    // Scan receiver is only registered while the Activity is in the foreground (onResume/onPause),
    // so Bring! check-off only happens when the user is actively using the app.
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != "com.lucathomas.stockscanner.SCAN_RESULT") return
            val barcode = intent.getStringExtra("com.symbol.datawedge.data_string") ?: return

            FoodFactsHelper.fetchProductData(context, barcode) { product ->
                viewModel.setProduct(product)

                if (navController.currentDestination?.id != R.id.bringFragment) return@fetchProductData

                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                if (!prefs.getBoolean("bring_enabled", false) || product == null) return@fetchProductData

                val token = prefs.getString("bring_token", "") ?: ""
                val listUuid = prefs.getString("bring_list_id", "") ?: ""
                if (token.isEmpty() || listUuid.isEmpty()) return@fetchProductData

                BringHelper.getFullList(context, listUuid, token) { listData ->
                    if (listData == null) return@getFullList
                    val match = listData.purchase.find { item ->
                        BringHelper.productMatchesItem(product, item.name)
                    } ?: return@getFullList

                    BringHelper.updateItem(context, listUuid, token, match.name) { success ->
                        if (success) {
                            Toast.makeText(context, "${match.name} erledigt!", Toast.LENGTH_SHORT).show()
                            viewModel.triggerBringRefresh()
                        }
                    }
                }
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "bring_enabled") updateBringTabVisibility(prefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.FirstFragment, R.id.bringFragment, R.id.SecondFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        DataWedgeHelper.createProfile(this)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.lucathomas.stockscanner.SCAN_RESULT")
        ContextCompat.registerReceiver(this, scanReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateBringTabVisibility(prefs)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(scanReceiver)
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun updateBringTabVisibility(prefs: SharedPreferences) {
        binding.bottomNav.menu.findItem(R.id.bringFragment)?.isVisible =
            prefs.getBoolean("bring_enabled", false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
