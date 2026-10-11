package com.muhasibtech.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.databinding.ItemMainChildBinding
import com.muhasibtech.app.databinding.ItemMainSectionBinding

class MainAdapter(
    private val onChildClick: (String) -> Unit,
    private val onHeaderClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHILD = 1
    }

    private var items: List<MainListItem> = emptyList()

    fun submit(newItems: List<MainListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MainListItem.Header -> TYPE_HEADER
            is MainListItem.Child -> TYPE_CHILD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemMainSectionBinding.inflate(inflater, parent, false))
        } else {
            ChildViewHolder(ItemMainChildBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            is MainListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.bind(item, onHeaderClick)
            }

            is MainListItem.Child -> {
                val childHolder = holder as ChildViewHolder
                childHolder.bind(item, onChildClick)
            }
        }
    }

    class HeaderViewHolder(
        private val binding: ItemMainSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MainListItem.Header, onHeaderClick: (Int) -> Unit) {
            binding.tvSectionTitle.text = item.title
            binding.tvArrow.text = if (item.expanded) "▲" else "▼"

            binding.root.setOnClickListener {
                onHeaderClick(item.id)
            }
        }
    }

    class ChildViewHolder(
        private val binding: ItemMainChildBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MainListItem.Child, onChildClick: (String) -> Unit) {
            binding.tvChildTitle.text = item.title

            binding.root.setOnClickListener {
                onChildClick(item.id)
            }
        }
    }
}
