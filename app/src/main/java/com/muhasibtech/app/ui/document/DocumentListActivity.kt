package com.muhasibtech.app.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
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

    private var docType: String = ALL
    private var currentDocs: List<DocumentEntity> = emptyList()
    private var allRows: List<DocRowData> = emptyList()
    private var partyNames: Map<Long, String> = emptyMap()
    private var currencyCodes: Map<Long, String> = emptyMap()
    private var dateFrom: String = ""
    private var dateTo: String = ""

    private val adapter = DocumentListAdapter(
        onClick = { doc -> DocumentEditActivity.startEdit(this, doc.id) },
        onLongClick = { doc -> confirmDelete(doc) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDocumentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: ALL
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)

        binding.toolbar.title = title
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupHeaders()

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    binding.etSearch.visibility =
                        if (binding.etSearch.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }
                R.id.action_date -> {
                    showDateRangeDialog()
                    true
                }
                else -> false
            }
        }

        binding.etSearch.doAfterTextChanged { applyFilter() }

        binding.fab.setOnClickListener {
            if (docType == CASH_ALL) {
                showVoucherTypeDialog()
            } else {
                val addDocType = when (docType) {
                    SALES_ALL -> DocType.SALE_CASH
                    PURCHASES_ALL -> DocType.PURCHASE_CASH
                    ALL -> DocType.JOURNAL_DAILY
                    else -> docType
                }
                DocumentEditActivity.startAdd(this, addDocType)
            }
        }

        observeDocuments()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadMaps()
            rebuildRows()
        }
    }

    private fun setupHeaders() {
        if (docType == CASH_ALL) {
            binding.tvHNumber.setText(R.string.voucher_number)
            binding.tvHDate.setText(R.string.date)
            binding.tvHName.setText(R.string.total_col)
            binding.tvHCurrency.setText(R.string.statement_col)
            binding.tvHAmount.setText(R.string.type_col)
        }
    }

    private fun showVoucherTypeDialog() {
        val names = arrayOf(getString(R.string.receipt_short), getString(R.string.payment_short))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.type_col)
            .setItems(names) { _, which ->
                val type = if (which == 0) DocType.RECEIPT else DocType.PAYMENT
                DocumentEditActivity.startAdd(this, type)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDateRangeDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etFrom = EditText(this).apply { hint = getString(R.string.from_date) }
        val etTo = EditText(this).apply { hint = getString(R.string.to_date) }

        container.addView(etFrom)
        container.addView(etTo)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.search_date)
            .setView(container)
            .setPositiveButton(R.string.search) { _, _ ->
                dateFrom = etFrom.text.toString().trim()
                dateTo = etTo.text.toString().trim()
                applyFilter()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private suspend fun loadMaps() {
        partyNames = repo.accounts.observeAll().first().associate { it.id to it.name }
        currencyCodes = repo.currencies.observeAll().first().associate { it.id to it.code }
    }

    private fun observeDocuments() {
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
        allRows = currentDocs.map { doc ->
            val isVoucherList = docType == CASH_ALL

            DocRowData(
                doc = doc,
                number = doc.docNumber,
                date = doc.date,
                partyName = if (isVoucherList) doc.notes
                else if (doc.partyAccountId == 0L) "" else (partyNames[doc.partyAccountId] ?: ""),
                currencyCode = if (isVoucherList) {
                    if (doc.docType == DocType.RECEIPT) getString(R.string.receipt_short)
                    else getString(R.string.payment_short)
                } else {
                    currencyCodes[doc.currencyId] ?: ""
                },
                amount = doc.netAmount
            )
        }
        applyFilter()
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()

        var filtered = allRows

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.partyName.contains(query) || it.number.toString().contains(query)
            }
        }

        if (dateFrom.isNotEmpty()) {
            filtered = filtered.filter { it.date >= dateFrom }
        }

        if (dateTo.isNotEmpty()) {
            filtered = filtered.filter { it.date <= dateTo }
        }

        adapter.submit(filtered)

        if (docType == CASH_ALL) {
            val receipts = filtered.filter { it.doc.docType == DocType.RECEIPT }.sumOf { it.amount }
            val payments = filtered.filter { it.doc.docType == DocType.PAYMENT }.sumOf { it.amount }
            binding.tvTotal.text = String.format(
                Locale.US,
                "%s: %.2f | %s: %.2f",
                getString(R.string.receipt_short), receipts,
                getString(R.string.payment_short), payments
            )
        } else {
            binding.tvTotal.text = String.format(Locale.US, "%.2f", filtered.sumOf { it.amount })
        }

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
                    lifecycleScope.launch { repo.documentService.deleteDocument(doc.id) }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
