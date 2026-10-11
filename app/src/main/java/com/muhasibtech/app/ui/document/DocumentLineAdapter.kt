package com.muhasibtech.app.ui.document

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.databinding.ItemDocumentLineBinding
import java.util.Locale

class DocumentLineAdapter(
    private val mode: String,
    private val onClick: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<DocumentLineAdapter.ViewHolder>() {

    companion object {
        const val MODE_ITEM = "item"
        const val MODE_VOUCHER = "voucher"
        const val MODE_JOURNAL = "journal"
    }

    private var items: List<LineUi> = emptyList()

    fun submit(newItems: List<LineUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = items[position]

        when (mode) {
            MODE_VOUCHER -> {
                val amount = if (line.credit != 0.0) line.credit else line.debit
                holder.binding.tvItem.text = line.accountName
                holder.binding.tvQty.text = String.format(Locale.US, "%.2f", amount)
                holder.binding.tvPrice.text = line.notes
                holder.binding.tvTotal.text = ""
            }
            MODE_JOURNAL -> {
                holder.binding.tvItem.text = line.accountName
                holder.binding.tvQty.text = String.format(Locale.US, "%.2f", line.debit)
                holder.binding.tvPrice.text = String.format(Locale.US, "%.2f", line.credit)
                holder.binding.tvTotal.text = ""
            }
            else -> {
                holder.binding.tvItem.text = line.itemName
                holder.binding.tvQty.text = String.format(Locale.US, "%.2f", line.qty)
                holder.binding.tvPrice.text = String.format(Locale.US, "%.2f", line.price)
                holder.binding.tvTotal.text = String.format(Locale.US, "%.2f", line.total)
            }
        }

        holder.binding.root.setOnClickListener { onClick(position) }
        holder.binding.btnDelete.setOnClickListener { onDelete(position) }
    }

    class ViewHolder(val binding: ItemDocumentLineBinding) : RecyclerView.ViewHolder(binding.root)
}
