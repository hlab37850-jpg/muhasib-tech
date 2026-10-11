package com.muhasibtech.app.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.databinding.ItemSimpleListBinding

data class SimpleItem(
    val id: Long,
    val primary: String,
    val secondary: String = ""
)

class SimpleListAdapter(
    private val onClick: (SimpleItem) -> Unit,
    private val onLongClick: (SimpleItem) -> Unit
) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

    private var items: List<SimpleItem> = emptyList()

    fun submit(newItems: List<SimpleItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimpleListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvPrimary.text = item.primary
        holder.binding.tvSecondary.text = item.secondary

        holder.binding.root.setOnClickListener {
            onClick(item)
        }

        holder.binding.root.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    class ViewHolder(val binding: ItemSimpleListBinding) : RecyclerView.ViewHolder(binding.root)
}
