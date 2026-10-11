package com.muhasibtech.app.ui.document

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.data.local.entity.DocumentEntity
import com.muhasibtech.app.databinding.ItemDocumentListBinding
import java.util.Locale

class DocumentListAdapter(
    private val onClick: (DocumentEntity) -> Unit,
    private val onLongClick: (DocumentEntity) -> Unit
) : RecyclerView.Adapter<DocumentListAdapter.ViewHolder>() {

    private var items: List<DocumentEntity> = emptyList()

    fun submit(newItems: List<DocumentEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = items[position]

        holder.binding.tvPrimary.text = "رقم #${doc.docNumber} - ${doc.date}"
        holder.binding.tvSecondary.text = String.format(
            Locale.US,
            "الصافي: %.2f - المتبقي: %.2f",
            doc.netAmount,
            doc.remainingAmount
        )

        holder.binding.root.setOnClickListener {
            onClick(doc)
        }

        holder.binding.root.setOnLongClickListener {
            onLongClick(doc)
            true
        }
    }

    class ViewHolder(val binding: ItemDocumentListBinding) : RecyclerView.ViewHolder(binding.root)
}
