package com.muhasibtech.app.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.SettingKeys
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityDocumentListBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    private val adapter = DocumentListAdapter(
        onClick = { doc ->
            DocumentEditActivity.startEdit(this, doc.id)
        },
        onLongClick = { doc ->
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

        val addDocType = when (docType) {
            SALES_ALL -> DocType.SALE_CASH
            PURCHASES_ALL -> DocType.PURCHASE_CASH
            CASH_ALL -> DocType.RECEIPT
            ALL -> DocType.JOURNAL_DAILY
            else -> docType
        }

        binding.fab.show()
        binding.fab.setOnClickListener {
            DocumentEditActivity.startAdd(this, addDocType)
        }

        observeDocuments(docType)
    }

    private fun observeDocuments(docType: String) {
        val flow: Flow<List<com.muhasibtech.app.data.local.entity.DocumentEntity>> = when (docType) {
            ALL -> repo.documents.observeAll()
            SALES_ALL -> repo.queries.observeSales()
            PURCHASES_ALL -> repo.queries.observePurchases()
            CASH_ALL -> repo.queries.observeReceiptPayment()
            else -> repo.documents.observeByType(docType)
        }

        lifecycleScope.launch {
            flow.collect { list ->
                adapter.submit(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
