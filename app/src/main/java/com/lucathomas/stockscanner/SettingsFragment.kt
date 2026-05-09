package com.lucathomas.stockscanner

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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