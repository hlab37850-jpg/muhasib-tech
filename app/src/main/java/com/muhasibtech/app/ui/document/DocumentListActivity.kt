package com.muhasibtech.app.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.DocumentEntity
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.SettingKeys
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityDocumentListBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DocumentListActivity : AppCompatActivity() {

    companion object {
        const val ALL = "ALL"
        const val SALES_ALL = "SALES_ALL"
        const val PURCHASES_ALL = "PURCHASES_ALL"
        const val CASH_ALL = "CASH_ALL"

        private const val EXTRA_DOC_TYPE = "extra_doc_type"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, docType: String, title: String) {
            val intent = Intent(context, DocumentListActivity::class.java)
            intent.putExtra(EXTRA_DOC_TYPE, docType)
            intent.putExtra(EXTRA_TITLE, title)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityDocumentListBinding
    private lateinit var repo: AppRepository

    private var currentDocs: List<DocumentEntity> = emptyList()
    private var partyNames: Map<Long, String> = emptyMap()
    private var currencyCodes: Map<Long, String> = emptyMap()

    private val adapter = DocumentListAdapter(
        onClick = { doc -> DocumentEditActivity.startEdit(this, doc.id) },
        onLongClick = { doc -> confirmDelete(doc) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDocumentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: ALL
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)

        binding.toolbar.title = title
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_search) {
                binding.etSearch.visibility =
                    if (binding.etSearch.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                true
            } else {
                false
            }
        }

        binding.etSearch.doAfterTextChanged { applyFilter() }

        val addDocType = when (docType) {
            SALES_ALL -> DocType.SALE_CASH
            PURCHASES_ALL -> DocType.PURCHASE_CASH
            CASH_ALL -> DocType.RECEIPT
            ALL -> DocType.JOURNAL_DAILY
            else -> docType
        }

        binding.fab.setOnClickListener {
            DocumentEditActivity.startAdd(this, addDocType)
        }

        observeDocuments(docType)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadMaps()
            rebuildRows()
        }
    }

    private suspend fun loadMaps() {
        partyNames = repo.accounts.observeAll().first().associate { it.id to it.name }
        currencyCodes = repo.currencies.observeAll().first().associate { it.id to it.code }
    }

    private fun observeDocuments(docType: String) {
        val flow: Flow<List<DocumentEntity>> = when (docType) {
            ALL -> repo.documents.observeAll()
            SALES_ALL -> repo.queries.observeSales()
            PURCHASES_ALL -> repo.queries.observePurchases()
            CASH_ALL -> repo.queries.observeReceiptPayment()
            else -> repo.documents.observeByType(docType)
        }

        lifecycleScope.launch {
            flow.collect { list ->
                currentDocs = list
                rebuildRows()
            }
        }
    }

    private fun rebuildRows() {
        val rows = currentDocs.map { doc ->
            DocRowData(
                doc = doc,
                number = doc.docNumber,
                date = doc.date,
                partyName = if (doc.partyAccountId == 0L) "" else (partyNames[doc.partyAccountId] ?: ""),
                currencyCode = currencyCodes[doc.currencyId] ?: "",
                amount = doc.netAmount
            )
        }

        allRows = rows
        applyFilter()
    }

    private var allRows: List<DocRowData> = emptyList()

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()

        val filtered = if (query.isEmpty()) {
            allRows
        } else {
            allRows.filter {
                it.partyName.contains(query) || it.number.toString().contains(query)
            }
        }

        adapter.submit(filtered)

        val total = filtered.sumOf { it.amount }
        binding.tvTotal.text = String.format(Locale.US, "%.2f", total)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(doc: DocumentEntity) {
        lifecycleScope.launch {
            val canDelete = repo.settings.getBoolean(SettingKeys.ALLOW_DELETE_OPERATION, true)
            if (!canDelete) return@launch

            MaterialAlertDialogBuilder(this@DocumentListActivity)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        repo.documentService.deleteDocument(doc.id)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
