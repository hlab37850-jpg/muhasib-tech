package com.muhasibtech.app.ui.document

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.databinding.ItemDocumentLineBinding
import java.util.Locale

class DocumentLineAdapter(
    private val isAccounting: Boolean,
    private val onClick: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<DocumentLineAdapter.ViewHolder>() {

    private var items: List<LineUi> = emptyList()

    fun submit(newItems: List<LineUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = items[position]

        if (isAccounting) {
            holder.binding.tvItem.text = line.accountName
            holder.binding.tvQty.text = String.format(Locale.US, "%.2f", line.debit)
            holder.binding.tvPrice.text = String.format(Locale.US, "%.2f", line.credit)
            holder.binding.tvTotal.text = ""
        } else {
            holder.binding.tvItem.text = line.itemName
            holder.binding.tvQty.text = String.format(Locale.US, "%.2f", line.qty)
            holder.binding.tvPrice.text = String.format(Locale.US, "%.2f", line.price)
            holder.binding.tvTotal.text = String.format(Locale.US, "%.2f", line.total)
        }

        holder.binding.root.setOnClickListener { onClick(position) }
        holder.binding.btnDelete.setOnClickListener { onDelete(position) }
    }

    class ViewHolder(val binding: ItemDocumentLineBinding) : RecyclerView.ViewHolder(binding.root)
}
