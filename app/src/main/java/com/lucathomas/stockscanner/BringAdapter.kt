package com.lucathomas.stockscanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.lucathomas.stockscanner.databinding.ItemBringBinding

sealed class BringEntry {
    data class Header(val title: String) : BringEntry()
    data class Item(val item: BringItem, val isRecently: Boolean) : BringEntry()
}

class BringAdapter(
    private val entries: List<BringEntry>,
    private val onAction: (BringItem, Boolean) -> Unit  // (item, isReAdd)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSectionTitle)
    }

    class ItemViewHolder(val binding: ItemBringBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (entries[position]) {
        is BringEntry.Header -> TYPE_HEADER
        is BringEntry.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemBringBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = entries[position]) {
            is BringEntry.Header -> (holder as HeaderViewHolder).tvTitle.text = entry.title
            is BringEntry.Item -> {
                val h = holder as ItemViewHolder
                h.binding.tvItemName.text = entry.item.name
                if (entry.item.specification.isNotBlank()) {
                    h.binding.tvItemSpec.text = entry.item.specification
                    h.binding.tvItemSpec.isVisible = true
                } else {
                    h.binding.tvItemSpec.isVisible = false
                }
                if (entry.isRecently) {
                    h.binding.btnAction.text = "Wieder hinzufügen"
                    h.binding.root.alpha = 0.65f
                } else {
                    h.binding.btnAction.text = "Erledigt"
                    h.binding.root.alpha = 1.0f
                }
                h.binding.btnAction.setOnClickListener { onAction(entry.item, entry.isRecently) }
                h.binding.root.setOnClickListener { onAction(entry.item, entry.isRecently) }
            }
        }
    }

    override fun getItemCount() = entries.size
}
