package com.lucathomas.stockscanner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.lucathomas.stockscanner.databinding.FragmentBringBinding

class BringFragment : Fragment() {

    private var _binding: FragmentBringBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DataViewModel by activityViewModels()

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadBringList(silent = true)
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvBringItems.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeRefresh.setOnRefreshListener { loadBringList() }

        // Refresh immediately when scanner checks off an item
        viewModel.bringRefreshTrigger.observe(viewLifecycleOwner) {
            loadBringList(silent = true)
        }
    }

    override fun onResume() {
        super.onResume()
        loadBringList()
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun loadBringList(silent: Boolean = false) {
        if (!isAdded) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val token = prefs.getString("bring_token", "") ?: ""
        val listUuid = prefs.getString("bring_list_id", "") ?: ""

        if (token.isEmpty() || listUuid.isEmpty()) {
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.isVisible = false
            binding.layoutEmpty.isVisible = true
            binding.tvEmpty.text = "Bring! nicht konfiguriert.\nBitte in den Einstellungen einrichten."
            binding.rvBringItems.isVisible = false
            return
        }

        if (!silent) {
            binding.progressBar.isVisible = true
            binding.layoutEmpty.isVisible = false
        }

        BringHelper.getFullList(requireContext(), listUuid, token) { listData ->
            if (!isAdded) return@getFullList
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.isVisible = false

            if (listData == null) {
                if (!silent) {
                    binding.layoutEmpty.isVisible = true
                    binding.tvEmpty.text = "Fehler beim Laden der Liste"
                    binding.rvBringItems.isVisible = false
                }
                return@getFullList
            }

            val entries = buildList {
                if (listData.purchase.isNotEmpty()) {
                    add(BringEntry.Header("Einkaufsliste"))
                    listData.purchase.forEach { add(BringEntry.Item(it, isRecently = false)) }
                }
                if (listData.recently.isNotEmpty()) {
                    add(BringEntry.Header("Zuletzt abgehakt"))
                    listData.recently.forEach { add(BringEntry.Item(it, isRecently = true)) }
                }
            }

            if (entries.isEmpty()) {
                binding.layoutEmpty.isVisible = true
                binding.tvEmpty.text = "Keine Artikel auf der Liste"
                binding.rvBringItems.isVisible = false
            } else {
                binding.layoutEmpty.isVisible = false
                binding.rvBringItems.isVisible = true
                binding.rvBringItems.adapter = BringAdapter(entries) { item, isReAdd ->
                    BringHelper.updateItem(
                        requireContext(), listUuid, token, item.name,
                        addToPurchase = isReAdd
                    ) { success ->
                        if (success && isAdded) loadBringList(silent = true)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
