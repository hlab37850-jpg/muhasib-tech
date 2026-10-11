package com.muhasibtech.app.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.local.entity.CashboxEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.local.entity.ItemEntity
import com.muhasibtech.app.data.local.entity.WarehouseEntity
import com.muhasibtech.app.data.model.AccountNature
import com.muhasibtech.app.data.model.AdjustmentType
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocumentRequest
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityDocumentEditBinding
import com.muhasibtech.app.ui.list.ListMode
import com.muhasibtech.app.ui.list.SimpleListActivity
import com.muhasibtech.app.util.AppDate
import com.muhasibtech.app.util.CurrencyPicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DocumentEditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DOC_TYPE = "extra_doc_type"
        private const val EXTRA_DOCUMENT_ID = "extra_document_id"

        fun startAdd(context: Context, docType: String) {
            val intent = Intent(context, DocumentEditActivity::class.java)
            intent.putExtra(EXTRA_DOC_TYPE, docType)
            context.startActivity(intent)
        }

        fun startEdit(context: Context, documentId: Long) {
            val intent = Intent(context, DocumentEditActivity::class.java)
            intent.putExtra(EXTRA_DOCUMENT_ID, documentId)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityDocumentEditBinding
    private lateinit var repo: AppRepository
    private lateinit var lineAdapter: DocumentLineAdapter

    private var docType: String = DocType.SALE_CASH
    private var documentId: Long = 0

    private var partyAccountId: Long = 0
    private var warehouseId: Long = 0
    private var currencyId: Long = 0
    private var cashboxId: Long = 0

    private var settlementType: String = AdjustmentType.OPENING

    private var discountType: String = DiscountType.AMOUNT
    private var discountValue: Double = 0.0
    private var taxRate: Double = 0.0
    private var feesAmount: Double = 0.0
    private var feesNote: String = ""
    private var paidAmount: Double = 0.0
    private var suppressSwitch: Boolean = false

    private val lines = mutableListOf<LineUi>()

    private var warehouses: List<WarehouseEntity> = emptyList()
    private var currencies: List<CurrencyEntity> = emptyList()
    private var cashboxes: List<CashboxEntity> = emptyList()
    private var accounts: List<AccountEntity> = emptyList()
    private var items: MutableList<ItemEntity> = mutableListOf()

    private val partyNames = mutableListOf<String>()
    private val itemNames = mutableListOf<String>()

    private lateinit var partyAdapter: ArrayAdapter<String>
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDocumentEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: DocType.SALE_CASH
        documentId = intent.getLongExtra(EXTRA_DOCUMENT_ID, 0)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> { saveDocument(); true }
                R.id.action_new -> { resetForm(); true }
                R.id.action_delete -> { deleteCurrent(); true }
                R.id.action_share -> { shareDocument(); true }
                else -> false
            }
        }

        partyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, partyNames)
        itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemNames)

        binding.etParty.setAdapter(partyAdapter)
        binding.etItemSearch.setAdapter(itemAdapter)

        binding.etParty.setOnItemClickListener { _, _, position, _ ->
            partyAccountId = accounts[position].id
        }

        binding.etItemSearch.setOnItemClickListener { _, _, position, _ ->
            openItemDialog(items[position], null)
            binding.etItemSearch.setText("")
        }

        binding.btnAddItem.setOnClickListener {
            if (isVoucher()) {
                openVoucherLineDialog(null)
            } else if (isAccountDocument()) {
                openAccountLineDialog(null)
            } else {
                openItemDialog(null, null)
            }
        }

        binding.btnClearItem.setOnClickListener {
            binding.etItemSearch.setText("")
        }

        binding.etDate.setOnClickListener { openDatePicker() }
        binding.btnCalendar.setOnClickListener { openDatePicker() }

        binding.layoutSummary.setOnClickListener {
            if (isDiscountSupported()) showDiscountDialog()
        }

        binding.switchCredit.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitch) return@setOnCheckedChangeListener

            val oldType = docType

            docType = when {
                isSaleFamily() -> if (checked) DocType.SALE_CREDIT else DocType.SALE_CASH
                isPurchaseFamily() -> if (checked) DocType.PURCHASE_CREDIT else DocType.PURCHASE_CASH
                isVoucher() -> if (checked) DocType.PAYMENT else DocType.RECEIPT
                else -> docType
            }

            if (isVoucher() && documentId > 0L) {
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.save_confirm) + " " + getString(R.string.number))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        documentId = 0L
                        updateHeaderVisibility()
                        updateTitle()
                        updateNumberPreview()
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        docType = oldType
                        suppressSwitch = true
                        suppressSwitch = false
                        updateHeaderVisibility()
                        updateTitle()
                    }
                    .show()
                return@setOnCheckedChangeListener
            }

            updateHeaderVisibility()
            updateTitle()
            if (documentId == 0L) updateNumberPreview()
        }

        binding.cbReference.setOnCheckedChangeListener { _, _ -> updateTitle() }

        loadMasterData()
    }

    private fun isSaleFamily(): Boolean =
        docType == DocType.SALE_CASH || docType == DocType.SALE_CREDIT

    private fun isPurchaseFamily(): Boolean =
        docType == DocType.PURCHASE_CASH || docType == DocType.PURCHASE_CREDIT

    private fun isAccountDocument(): Boolean =
        docType == DocType.RECEIPT ||
            docType == DocType.PAYMENT ||
            docType == DocType.JOURNAL_DAILY ||
            docType == DocType.JOURNAL_OPENING

    private fun isVoucher(): Boolean = docType == DocType.RECEIPT || docType == DocType.PAYMENT

    private fun isDiscountSupported(): Boolean = isSaleFamily() || isPurchaseFamily()

    private fun loadMasterData() {
        lifecycleScope.launch {
            warehouses = repo.warehouses.observeActive().first()
            currencies = repo.currencies.observeActive().first()
            cashboxes = repo.cashboxes.observeActive().first()
            accounts = repo.accounts.observeActive().first()
            items = repo.items.observeActive().first().toMutableList()

            partyNames.clear()
            partyNames.addAll(accounts.map { it.name })
            partyAdapter.notifyDataSetChanged()

            itemNames.clear()
            itemNames.addAll(items.map { it.name })
            itemAdapter.notifyDataSetChanged()

            if (documentId > 0) {
                val loaded = loadDocument()
                if (!loaded) initDefaults()
            } else {
                initDefaults()
            }

            setupSpinners()
            setupLinesAdapter()
            updateHeaderVisibility()
            updateTitle()
            updateNumberPreview()
            updateSummary()
        }
    }

    private suspend fun initDefaults() {
        warehouseId = repo.settings.getCurrentWarehouseId()
        currencyId = repo.settings.getCurrentCurrencyId()
        cashboxId = repo.cashboxes.getDefault()?.id ?: 0
        binding.etDate.setText(AppDate.today())
    }

    private suspend fun loadDocument(): Boolean {
        val doc = repo.documents.getById(documentId) ?: return false

        docType = doc.docType
        partyAccountId = doc.partyAccountId
        warehouseId = doc.warehouseId
        currencyId = doc.currencyId
        cashboxId = doc.cashboxId
        settlementType = if (doc.settlementType.isBlank()) AdjustmentType.OPENING else doc.settlementType

        discountType = doc.discountType
        discountValue = doc.discountValue
        taxRate = doc.taxRate
        feesAmount = doc.feesAmount
        feesNote = doc.feesNote
        paidAmount = doc.paidAmount

        binding.etDate.setText(doc.date)
        binding.etNotes.setText(doc.notes)
        binding.cbReference.isChecked = doc.reference.isNotBlank()
        binding.etParty.setText(
            if (doc.partyAccountId == 0L) "" else (accounts.firstOrNull { it.id == doc.partyAccountId }?.name ?: "")
        )

        suppressSwitch = true
        binding.switchCredit.isChecked =
            docType == DocType.SALE_CREDIT ||
                docType == DocType.PURCHASE_CREDIT ||
                docType == DocType.PAYMENT
        suppressSwitch = false

        val docLines = repo.documentLines.getByDocument(documentId)
        lines.clear()

        docLines.forEach { line ->
            if (isAccountDocument()) {
                lines.add(
                    LineUi(
                        accountId = line.accountId,
                        accountName = accounts.firstOrNull { it.id == line.accountId }?.name ?: "",
                        debit = line.debit,
                        credit = line.credit,
                        notes = line.notes
                    )
                )
            } else {
                lines.add(
                    LineUi(
                        itemId = line.itemId,
                        itemName = items.firstOrNull { it.id == line.itemId }?.name ?: "",
                        qty = line.qty,
                        price = line.price,
                        total = line.total,
                        notes = line.notes
                    )
                )
            }
        }

        return true
    }

    private fun setupSpinners() {
        setupSpinner(
            binding.spWarehouse,
            warehouses.map { it.name },
            warehouses.map { it.id },
            warehouseId
        ) { id -> warehouseId = id }

        setupSpinner(
            binding.spCurrency,
            currencies.map { it.name },
            currencies.map { it.id },
            currencyId
        ) { id -> currencyId = id }

        setupSpinner(
            binding.spCashbox,
            cashboxes.map { it.name },
            cashboxes.map { it.id },
            cashboxId
        ) { id -> cashboxId = id }
    }

    private fun setupSpinner(
        spinner: Spinner,
        names: List<String>,
        ids: List<Long>,
        selectedId: Long,
        onPicked: (Long) -> Unit
    ) {
        if (names.isEmpty()) {
            spinner.adapter = null
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onPicked(ids[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val index = ids.indexOf(selectedId)
        if (index >= 0) spinner.setSelection(index)
    }

    private fun lineMode(): String {
        return when {
            isVoucher() -> DocumentLineAdapter.MODE_VOUCHER
            isAccountDocument() -> DocumentLineAdapter.MODE_JOURNAL
            else -> DocumentLineAdapter.MODE_ITEM
        }
    }

    private fun setupLinesAdapter() {
        lineAdapter = DocumentLineAdapter(
            mode = lineMode(),
            onClick = { index -> editLine(index) },
            onDelete = { index -> deleteLine(index) }
        )

        binding.rvLines.layoutManager = LinearLayoutManager(this)
        binding.rvLines.isNestedScrollingEnabled = false
        binding.rvLines.adapter = lineAdapter
        lineAdapter.submit(lines.toList())
    }

    private fun updateHeaderVisibility() {
        val saleOrPurchase = isSaleFamily() || isPurchaseFamily()

        if (isVoucher()) {
            binding.tvHItem.setText(R.string.col_account)
            binding.tvHQty.setText(R.string.amount)
            binding.tvHPrice.setText(R.string.notes)
            binding.tvHTotal.text = ""
        } else if (isAccountDocument()) {
            binding.tvHItem.setText(R.string.col_account)
            binding.tvHQty.setText(R.string.debit)
            binding.tvHPrice.setText(R.string.credit)
            binding.tvHTotal.text = ""
        } else {
            binding.tvHItem.setText(R.string.col_item)
            binding.tvHQty.setText(R.string.qty)
            binding.tvHPrice.setText(R.string.price)
            binding.tvHTotal.setText(R.string.total)
        }

        binding.switchCredit.visibility = if (saleOrPurchase || isVoucher()) View.VISIBLE else View.GONE
        binding.cbReference.visibility = if (saleOrPurchase) View.VISIBLE else View.GONE

        binding.spCashbox.visibility = if (
            docType == DocType.SALE_CASH ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.RECEIPT ||
            docType == DocType.PAYMENT
        ) View.VISIBLE else View.GONE

        binding.spWarehouse.visibility = if (isAccountDocument()) View.GONE else View.VISIBLE
        binding.etParty.visibility = if (saleOrPurchase) View.VISIBLE else View.GONE

        if (isAccountDocument()) {
            binding.etItemSearch.setHint(R.string.select_account)
            binding.etItemSearch.setAdapter(partyAdapter)
        } else {
            binding.etItemSearch.setHint(R.string.write_item_name)
            binding.etItemSearch.setAdapter(itemAdapter)
        }
    }

    private fun updateTitle() {
        val base = when (docType) {
            DocType.SALE_CASH -> R.string.title_sale_cash
            DocType.SALE_CREDIT -> R.string.title_sale_credit
            DocType.PURCHASE_CASH -> R.string.title_purchase_cash
            DocType.PURCHASE_CREDIT -> R.string.title_purchase_credit
            DocType.RECEIPT -> R.string.title_receipt
            DocType.PAYMENT -> R.string.title_payment
            DocType.JOURNAL_DAILY -> R.string.child_journal_daily
            DocType.JOURNAL_OPENING -> R.string.child_journal_opening
            DocType.STOCK_OUT -> R.string.child_stock_out
            DocType.STOCK_IN -> R.string.child_stock_in
            DocType.STOCK_TRANSFER -> R.string.child_stock_transfer
            DocType.STOCK_ADJUSTMENT -> R.string.child_stock_adjustment
            DocType.STOCKTAKE -> R.string.child_stocktake
            DocType.QUOTATION -> R.string.child_quotation
            DocType.PURCHASE_REQUEST -> R.string.child_purchase_request
            else -> R.string.app_name
        }

        val prefix = if (binding.cbReference.isChecked) getString(R.string.reference) + " " else ""
        binding.toolbar.title = prefix + getString(base)
    }

    private fun updateNumberPreview() {
        lifecycleScope.launch {
            if (documentId == 0L) {
                val next = repo.documents.getLastDocNumber(docType) + 1
                binding.etDocNumber.setText(getString(R.string.number) + "#" + next)
            } else {
                val doc = repo.documents.getById(documentId)
                binding.etDocNumber.setText(getString(R.string.number) + "#" + (doc?.docNumber ?: 0))
            }
        }
    }

    private fun openDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.etDate.setText(format.format(calendar.time))
        }

        picker.show(supportFragmentManager, "date_picker")
    }

    private fun editLine(index: Int) {
        if (index !in lines.indices) return

        if (isVoucher()) {
            openVoucherLineDialog(index)
        } else if (isAccountDocument()) {
            openAccountLineDialog(index)
        } else {
            val item = items.firstOrNull { it.id == lines[index].itemId }
            openItemDialog(item, index)
        }
    }

    private fun deleteLine(index: Int) {
        if (index !in lines.indices) return
        lines.removeAt(index)
        lineAdapter.submit(lines.toList())
        updateSummary()
    }

    private fun openItemDialog(item: ItemEntity?, index: Int?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_line, null)

        val dlgName = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.dlgItemName)
        val dlgQty = dialogView.findViewById<android.widget.EditText>(R.id.dlgQty)
        val dlgPrice = dialogView.findViewById<android.widget.EditText>(R.id.dlgPrice)
        val dlgTotal = dialogView.findViewById<android.widget.EditText>(R.id.dlgTotal)
        val dlgNotes = dialogView.findViewById<android.widget.EditText>(R.id.dlgNotes)

        dlgName.setAdapter(itemAdapter)

        var selectedItemId = item?.id ?: 0

        if (item != null) dlgName.setText(item.name)
        if (index != null) {
            dlgQty.setText(fmt(lines[index].qty))
            dlgPrice.setText(fmt(lines[index].price))
            dlgTotal.setText(fmt(lines[index].total))
            dlgNotes.setText(lines[index].notes)
        }

        dlgName.setOnItemClickListener { _, _, position, _ ->
            selectedItemId = items[position].id
        }

        var updating = false

        fun recalcTotal() {
            if (updating) return
            updating = true
            val q = dlgQty.text.toString().toDoubleOrNull() ?: 0.0
            val p = dlgPrice.text.toString().toDoubleOrNull() ?: 0.0
            dlgTotal.setText(fmt(q * p))
            updating = false
        }

        fun recalcPrice() {
            if (updating) return
            updating = true
            val q = dlgQty.text.toString().toDoubleOrNull() ?: 0.0
            val t = dlgTotal.text.toString().toDoubleOrNull() ?: 0.0
            if (q != 0.0) dlgPrice.setText(fmt(t / q))
            updating = false
        }

        dlgQty.doAfterTextChanged { recalcTotal() }
        dlgPrice.doAfterTextChanged { recalcTotal() }
        dlgTotal.doAfterTextChanged { recalcPrice() }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.ImageButton>(R.id.btnCloseTop).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnCancelDlg).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnCloseDlg).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<android.widget.Button>(R.id.btnMoreDetails).setOnClickListener {
            dlgNotes.visibility = if (dlgNotes.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        dialogView.findViewById<android.widget.ImageButton>(R.id.btnInfo).setOnClickListener {
            val name = dlgName.text.toString().trim()
            val infoItem = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull { it.name == name }
            if (infoItem != null) showItemInfoDialog(infoItem)
        }

        dialogView.findViewById<android.widget.Button>(R.id.btnAddLineDlg).setOnClickListener {
            val name = dlgName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qty = dlgQty.text.toString().toDoubleOrNull() ?: 0.0
            if (qty == 0.0) {
                Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = dlgPrice.text.toString().toDoubleOrNull() ?: 0.0
            val total = dlgTotal.text.toString().toDoubleOrNull() ?: 0.0
            val finalTotal = if (total != 0.0) total else qty * price

            lifecycleScope.launch {
                var finalItem = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull { it.name == name }

                if (finalItem == null) {
                    val newId = repo.items.insert(ItemEntity(name = name))
                    finalItem = ItemEntity(id = newId, name = name)
                    items.add(finalItem)
                    itemNames.add(name)
                    itemAdapter.notifyDataSetChanged()
                }

                val notes = dlgNotes.text.toString().trim()

                if (index == null) {
                    lines.add(
                        LineUi(
                            itemId = finalItem.id,
                            itemName = finalItem.name,
                            qty = qty,
                            price = price,
                            total = finalTotal,
                            notes = notes
                        )
                    )
                } else {
                    lines[index] = lines[index].copy(
                        itemId = finalItem.id,
                        itemName = finalItem.name,
                        qty = qty,
                        price = price,
                        total = finalTotal,
                        notes = notes
                    )
                }

                lineAdapter.submit(lines.toList())
                updateSummary()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showItemInfoDialog(item: ItemEntity) {
        lifecycleScope.launch {
            val balance = repo.stockBalances.getOnce(item.id, warehouseId)
            val available = balance?.qty ?: 0.0

            val salePrice = repo.itemPrices.getLatest(item.id, currencyId)?.price ?: 0.0
            val purchaseLine = repo.queries.getLastPurchaseLine(item.id)
            val purchasePrice = purchaseLine?.price ?: 0.0
            val saleLine = repo.queries.getLastSaleLine(item.id)
            val lastSale = saleLine?.price ?: 0.0
            val lastSaleDate = if (saleLine != null) {
                repo.documents.getById(saleLine.documentId)?.date ?: ""
            } else {
                ""
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_item_info, null)

            dialogView.findViewById<android.widget.TextView>(R.id.infoTitle).text = item.name
            dialogView.findViewById<android.widget.TextView>(R.id.infoAvailable).text =
                getString(R.string.available_qty) + ": " + fmt(available)
            dialogView.findViewById<android.widget.TextView>(R.id.infoSalePrice).text =
                getString(R.string.sale_price) + ": " + fmt(salePrice)
            dialogView.findViewById<android.widget.TextView>(R.id.infoPurchasePrice).text =
                getString(R.string.purchase_price) + ": " + fmt(purchasePrice)
            dialogView.findViewById<android.widget.TextView>(R.id.infoLastSale).text =
                getString(R.string.last_sale_price) + ": " + fmt(lastSale) + " / " + lastSaleDate

            val dialog = MaterialAlertDialogBuilder(this@DocumentEditActivity)
                .setView(dialogView)
                .create()

            dialogView.findViewById<android.widget.Button>(R.id.btnInfoClose).setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<android.widget.Button>(R.id.btnItemData).setOnClickListener {
                dialog.dismiss()
                SimpleListActivity.start(this@DocumentEditActivity, ListMode.ITEMS)
            }

            dialog.show()
        }
    }

    private fun openVoucherLineDialog(index: Int?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_voucher_line, null)

        val dlgName = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.dlgAccountName)
        val dlgAmount = dialogView.findViewById<android.widget.EditText>(R.id.dlgAmount)
        val dlgNotes = dialogView.findViewById<android.widget.EditText>(R.id.dlgNotes)
        val rgCurrency = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgCurrency)

        dlgName.setAdapter(partyAdapter)

        var selectedAccountId = 0L
        if (index != null) selectedAccountId = lines[index].accountId

        if (index != null) {
            val line = lines[index]
            dlgName.setText(line.accountName)
            dlgAmount.setText(fmt(if (line.credit != 0.0) line.credit else line.debit))
            dlgNotes.setText(line.notes)
        }

        dlgName.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accounts[position].id
        }

        CurrencyPicker.build(this, rgCurrency, currencies, currencyId) { id ->
            currencyId = id
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.Button>(R.id.btnCloseDlg).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<android.widget.Button>(R.id.btnAddLineDlg).setOnClickListener {
            val name = dlgName.text.toString().trim()
            val amount = dlgAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || amount == 0.0 || selectedAccountId == 0L) {
                Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val notes = dlgNotes.text.toString().trim()
            val debit = if (docType == DocType.PAYMENT) amount else 0.0
            val credit = if (docType == DocType.RECEIPT) amount else 0.0

            if (index == null) {
                lines.add(
                    LineUi(
                        accountId = selectedAccountId,
                        accountName = name,
                        debit = debit,
                        credit = credit,
                        notes = notes
                    )
                )
            } else {
                lines[index] = lines[index].copy(
                    accountId = selectedAccountId,
                    accountName = name,
                    debit = debit,
                    credit = credit,
                    notes = notes
                )
            }

            lineAdapter.submit(lines.toList())
            updateSummary()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openAccountLineDialog(index: Int?) {
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = accounts.map { it.name }.toTypedArray()

        var selected = 0
        if (index != null) {
            val found = accounts.indexOfFirst { it.id == lines[index].accountId }
            if (found >= 0) selected = found
        }

        var current = selected

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_account)
            .setSingleChoiceItems(names, current) { _, which -> current = which }
            .setPositiveButton(R.string.save) { _, _ ->
                showAmountDialog(accounts[current], index)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAmountDialog(account: AccountEntity, index: Int?) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        if (docType == DocType.RECEIPT || docType == DocType.PAYMENT) {
            val etAmount = android.widget.EditText(this).apply {
                hint = getString(R.string.amount)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

            if (index != null) {
                etAmount.setText(fmt(if (docType == DocType.RECEIPT) lines[index].credit else lines[index].debit))
            }

            container.addView(etAmount)

            MaterialAlertDialogBuilder(this)
                .setTitle(account.name)
                .setView(container)
                .setPositiveButton(R.string.save) { _, _ ->
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

                    if (index == null) {
                        lines.add(
                            LineUi(
                                accountId = account.id,
                                accountName = account.name,
                                debit = if (docType == DocType.PAYMENT) amount else 0.0,
                                credit = if (docType == DocType.RECEIPT) amount else 0.0
                            )
                        )
                    } else {
                        lines[index] = lines[index].copy(
                            accountId = account.id,
                            accountName = account.name,
                            debit = if (docType == DocType.PAYMENT) amount else 0.0,
                            credit = if (docType == DocType.RECEIPT) amount else 0.0
                        )
                    }

                    lineAdapter.submit(lines.toList())
                    updateSummary()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

        } else {
            val etDebit = android.widget.EditText(this).apply { hint = getString(R.string.debit) }
            val etCredit = android.widget.EditText(this).apply { hint = getString(R.string.credit) }

            if (index != null) {
                etDebit.setText(fmt(lines[index].debit))
                etCredit.setText(fmt(lines[index].credit))
            }

            container.addView(etDebit)
            container.addView(etCredit)

            MaterialAlertDialogBuilder(this)
                .setTitle(account.name)
                .setView(container)
                .setPositiveButton(R.string.save) { _, _ ->
                    val debit = etDebit.text.toString().toDoubleOrNull() ?: 0.0
                    val credit = etCredit.text.toString().toDoubleOrNull() ?: 0.0

                    if (index == null) {
                        lines.add(
                            LineUi(
                                accountId = account.id,
                                accountName = account.name,
                                debit = debit,
                                credit = credit
                            )
                        )
                    } else {
                        lines[index] = lines[index].copy(
                            accountId = account.id,
                            accountName = account.name,
                            debit = debit,
                            credit = credit
                        )
                    }

                    lineAdapter.submit(lines.toList())
                    updateSummary()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun updateSummary() {
        if (isAccountDocument()) {
            val debit = lines.sumOf { it.debit }
            val credit = lines.sumOf { it.credit }

            binding.tvTotal.text = fmt(debit)
            binding.tvDiscount.text = fmt(credit)
            binding.tvTax.text = ""
            binding.tvFees.text = ""
            binding.tvPaid.text = ""
            binding.tvNet.text = fmt(debit - credit)
            return
        }

        val total = lines.sumOf { it.total }

        val discountAmount = if (discountType == DiscountType.PERCENT) {
            total * discountValue / 100.0
        } else {
            discountValue
        }

        val taxAmount = (total - discountAmount) * taxRate / 100.0
        val net = total - discountAmount + taxAmount + feesAmount

        val remaining = if (docType == DocType.SALE_CREDIT || docType == DocType.PURCHASE_CREDIT) {
            net - paidAmount
        } else {
            0.0
        }

        binding.tvTotal.text = fmt(total)
        binding.tvDiscount.text = fmt(discountAmount)
        binding.tvTax.text = fmt(taxAmount)
        binding.tvFees.text = fmt(feesAmount)
        binding.tvPaid.text = fmt(paidAmount)
        binding.tvNet.text = fmt(net) + " / " + fmt(remaining)
    }

    private fun showDiscountDialog() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val radioGroup = android.widget.RadioGroup(this)
        val amountId = 1001
        val percentId = 1002

        val rbAmount = android.widget.RadioButton(this).apply {
            id = amountId
            text = getString(R.string.discount_amount)
        }

        val rbPercent = android.widget.RadioButton(this).apply {
            id = percentId
            text = getString(R.string.discount_percent)
        }

        radioGroup.addView(rbAmount)
        radioGroup.addView(rbPercent)

        if (discountType == DiscountType.PERCENT) radioGroup.check(percentId) else radioGroup.check(amountId)

        val etDiscount = android.widget.EditText(this).apply { hint = getString(R.string.discount) }
        val etTax = android.widget.EditText(this).apply { hint = getString(R.string.tax_rate) }
        val etFees = android.widget.EditText(this).apply { hint = getString(R.string.fees) }
        val etFeesNote = android.widget.EditText(this).apply { hint = getString(R.string.fees_note) }
        val etPaid = android.widget.EditText(this).apply { hint = getString(R.string.paid_amount) }

        etDiscount.setText(fmt(discountValue))
        etTax.setText(fmt(taxRate))
        etFees.setText(fmt(feesAmount))
        etFeesNote.setText(feesNote)
        etPaid.setText(fmt(paidAmount))

        container.addView(radioGroup)
        container.addView(etDiscount)
        container.addView(etTax)
        container.addView(etFees)
        container.addView(etFeesNote)
        container.addView(etPaid)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discount)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                discountType = if (radioGroup.checkedRadioButtonId == percentId) {
                    DiscountType.PERCENT
                } else {
                    DiscountType.AMOUNT
                }

                discountValue = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
                taxRate = etTax.text.toString().toDoubleOrNull() ?: 0.0
                feesAmount = etFees.text.toString().toDoubleOrNull() ?: 0.0
                feesNote = etFeesNote.text.toString().trim()
                paidAmount = etPaid.text.toString().toDoubleOrNull() ?: 0.0

                updateSummary()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resetForm() {
        documentId = 0
        lines.clear()
        lineAdapter.submit(lines.toList())

        partyAccountId = 0
        discountType = DiscountType.AMOUNT
        discountValue = 0.0
        taxRate = 0.0
        feesAmount = 0.0
        feesNote = ""
        paidAmount = 0.0

        binding.etParty.setText("")
        binding.etNotes.setText("")
        binding.cbReference.isChecked = false
        binding.etDate.setText(AppDate.today())

        lifecycleScope.launch {
            initDefaults()
            updateNumberPreview()
            updateSummary()
        }
    }

    private fun deleteCurrent() {
        if (documentId == 0L) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.documentService.deleteDocument(documentId)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@DocumentEditActivity, e.message ?: getString(R.string.save_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun shareDocument() {
        val builder = StringBuilder()
        builder.appendLine(getString(R.string.app_name))
        builder.appendLine(binding.toolbar.title.toString())
        builder.appendLine(binding.etDocNumber.text.toString())
        builder.appendLine(getString(R.string.date) + ": " + binding.etDate.text.toString())

        val party = binding.etParty.text.toString().trim()
        if (party.isNotEmpty()) builder.appendLine(getString(R.string.party) + ": " + party)

        lines.forEach { line ->
            if (isAccountDocument()) {
                builder.appendLine(line.accountName + " | " + fmt(line.debit) + " | " + fmt(line.credit))
            } else {
                builder.appendLine(line.itemName + " | " + fmt(line.qty) + " | " + fmt(line.price) + " | " + fmt(line.total))
            }
        }

        builder.appendLine(getString(R.string.total) + ": " + binding.tvTotal.text)
        builder.appendLine(getString(R.string.summary) + ": " + binding.tvNet.text)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, builder.toString())
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share)))
    }

    private fun saveDocument() {
        if (lines.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.save_confirm)
            .setPositiveButton(R.string.yes) { _, _ -> doSave() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun doSave() {
        lifecycleScope.launch {
            try {
                val partyText = binding.etParty.text.toString().trim()
                var partyId = partyAccountId

                if (partyText.isNotEmpty() && (isSaleFamily() || isPurchaseFamily())) {
                    val existing = accounts.firstOrNull { it.name == partyText }

                    if (existing != null) {
                        partyId = existing.id
                    } else {
                        val nature = if (isPurchaseFamily()) AccountNature.LIABILITY else AccountNature.ASSET
                        val newId = repo.accounts.insert(AccountEntity(name = partyText, nature = nature))
                        val newAccount = AccountEntity(id = newId, name = partyText, nature = nature)
                        accounts = accounts + newAccount
                        partyNames.add(partyText)
                        partyAdapter.notifyDataSetChanged()
                        partyId = newId
                    }
                }

                val reference = if (binding.cbReference.isChecked) binding.etDocNumber.text.toString() else ""

                val requestLines = lines.mapIndexed { index, line ->
                    DocumentRequest.Line(
                        itemId = line.itemId,
                        accountId = line.accountId,
                        qty = line.qty,
                        price = line.price,
                        total = if (isAccountDocument()) 0.0 else line.total,
                        debit = line.debit,
                        credit = line.credit,
                        notes = line.notes,
                        sortOrder = index
                    )
                }

                val request = DocumentRequest(
                    documentId = documentId,
                    docType = docType,
                    date = binding.etDate.text.toString().trim(),
                    reference = reference,
                    partyAccountId = partyId,
                    warehouseId = warehouseId,
                    currencyId = currencyId,
                    cashboxId = cashboxId,
                    settlementType = settlementType,
                    notes = binding.etNotes.text.toString().trim(),
                    discountType = discountType,
                    discountValue = discountValue,
                    taxRate = taxRate,
                    feesAmount = feesAmount,
                    feesNote = feesNote,
                    paidAmount = paidAmount,
                    lines = requestLines
                )

                repo.documentService.saveDocument(request)

                Toast.makeText(this@DocumentEditActivity, R.string.saved_success, Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@DocumentEditActivity,
                    e.message ?: getString(R.string.save_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
