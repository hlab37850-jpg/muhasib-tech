package com.muhasibtech.app.ui.document

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.R
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
            holder.binding.tvPrimary.text = line.accountName
            holder.binding.tvSecondary.text = String.format(
                Locale.US,
                "مدين: %.2f - دائن: %.2f",
                line.debit,
                line.credit
            )
        } else {
            holder.binding.tvPrimary.text = line.itemName
            holder.binding.tvSecondary.text = String.format(
                Locale.US,
                "كمية: %.2f - سعر: %.2f - إجمالي: %.2f",
                line.qty,
                line.price,
                line.total
            )
        }

        holder.binding.btnDelete.setText(R.string.delete)

        holder.binding.root.setOnClickListener {
            onClick(position)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    class ViewHolder(val binding: ItemDocumentLineBinding) : RecyclerView.ViewHolder(binding.root)
}
