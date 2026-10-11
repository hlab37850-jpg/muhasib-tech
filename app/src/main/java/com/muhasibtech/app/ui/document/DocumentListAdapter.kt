package com.muhasibtech.app.ui.document

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.data.local.entity.DocumentEntity
import com.muhasibtech.app.databinding.ItemDocumentRowBinding
import java.util.Locale

data class DocRowData(
    val doc: DocumentEntity,
    val number: Long,
    val date: String,
    val partyName: String,
    val currencyCode: String,
    val amount: Double
)

class DocumentListAdapter(
    private val onClick: (DocumentEntity) -> Unit,
    private val onLongClick: (DocumentEntity) -> Unit
) : RecyclerView.Adapter<DocumentListAdapter.ViewHolder>() {

    private var rows: List<DocRowData> = emptyList()

    fun submit(newRows: List<DocRowData>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]

        holder.binding.tvNumber.text = row.number.toString()
        holder.binding.tvDate.text = row.date
        holder.binding.tvName.text = row.partyName
        holder.binding.tvCurrency.text = row.currencyCode
        holder.binding.tvAmount.text = String.format(Locale.US, "%.2f", row.amount)

        holder.binding.root.setOnClickListener { onClick(row.doc) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(row.doc)
            true
        }
    }

    class ViewHolder(val binding: ItemDocumentRowBinding) : RecyclerView.ViewHolder(binding.root)
}
