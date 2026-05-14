package com.lucathomas.stockscanner

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import android.widget.Toast

class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: DataViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("recreate_profile")?.setOnPreferenceClickListener {
            DataWedgeHelper.createProfile(requireContext())
            true
        }

        findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
            showClearHistoryDialog()
            true
        }

        setupBringSettings()
        
        // Load lists if we already have credentials
        loadBringLists()
    }

    private fun loadBringLists() {
        val emailPref = findPreference<EditTextPreference>("bring_email")
        val passwordPref = findPreference<EditTextPreference>("bring_password")
        val listPref = findPreference<ListPreference>("bring_list_id")

        val email = emailPref?.text ?: ""
        val password = passwordPref?.text ?: ""

        if (email.isNotEmpty() && password.isNotEmpty()) {
            BringHelper.login(requireContext(), email, password) { uuid, token ->
                if (uuid != null && token != null) {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    prefs.edit().putString("bring_user_uuid", uuid).putString("bring_token", token).apply()

                    BringHelper.getLists(requireContext(), uuid, token) { lists ->
                        if (lists != null && isAdded) {
                            listPref?.entries = lists.map { it.name }.toTypedArray()
                            listPref?.entryValues = lists.map { it.uuid }.toTypedArray()
                        }
                    }
                }
            }
        }
    }

    private fun setupBringSettings() {
        val emailPref = findPreference<EditTextPreference>("bring_email")
        val passwordPref = findPreference<EditTextPreference>("bring_password")
        val listPref = findPreference<ListPreference>("bring_list_id")

        // Initialize with empty arrays to prevent crash if clicked while empty
        listPref?.entries = arrayOf()
        listPref?.entryValues = arrayOf()

        val loginListener = Preference.OnPreferenceChangeListener { _, _ ->
            // Clear current list if credentials change
            listPref?.entries = arrayOf()
            listPref?.entryValues = arrayOf()
            true
        }

        emailPref?.onPreferenceChangeListener = loginListener
        passwordPref?.onPreferenceChangeListener = loginListener

        listPref?.setOnPreferenceClickListener {
            if (listPref.entries.isEmpty()) {
                val email = emailPref?.text ?: ""
                val password = passwordPref?.text ?: ""
                
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    BringHelper.login(requireContext(), email, password) { uuid, token ->
                        if (uuid != null && token != null) {
                            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            prefs.edit().putString("bring_user_uuid", uuid).putString("bring_token", token).apply()
                            
                            BringHelper.getLists(requireContext(), uuid, token) { lists ->
                                if (lists != null) {
                                    listPref.entries = lists.map { it.name }.toTypedArray()
                                    listPref.entryValues = lists.map { it.uuid }.toTypedArray()
                                    Toast.makeText(context, "Listen geladen. Klicken Sie erneut zum Auswählen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Bring! Login fehlgeschlagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Bitte erst E-Mail und Passwort eingeben", Toast.LENGTH_SHORT).show()
                }
                true // Consume click to prevent opening empty dialog
            } else {
                false // Let the dialog open normally
            }
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Historie löschen")
            .setMessage("Möchten Sie wirklich alle gescannten Produkte aus der Historie entfernen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.clearHistory()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}