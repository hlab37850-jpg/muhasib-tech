package com.muhasibtech.app.ui.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.local.entity.CategoryEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.local.entity.ItemEntity
import com.muhasibtech.app.data.local.entity.ItemGroupEntity
import com.muhasibtech.app.data.local.entity.UnitEntity
import com.muhasibtech.app.data.local.entity.UserEntity
import com.muhasibtech.app.data.local.entity.WarehouseEntity
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivitySimpleListBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SimpleListActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MODE = "extra_mode"

        fun start(context: Context, mode: ListMode) {
            val intent = Intent(context, SimpleListActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode.name)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivitySimpleListBinding
    private lateinit var repo: AppRepository
    private lateinit var mode: ListMode

    private val adapter = SimpleListAdapter(
        onClick = { item -> onItemClicked(item) },
        onLongClick = { item -> onItemLongClicked(item) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySimpleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        val modeName = intent.getStringExtra(EXTRA_MODE)
        mode = try {
            ListMode.valueOf(modeName ?: "")
        } catch (e: Exception) {
            finish()
            return
        }

        binding.toolbar.title = getTitleForMode()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        if (isEditable()) {
            binding.fab.visibility = View.VISIBLE
            binding.fab.setOnClickListener { openAddDialog() }
        } else {
            binding.fab.visibility = View.GONE
        }

        observeData()
    }

    private fun getTitleForMode(): String {
        return getString(
            when (mode) {
                ListMode.ACCOUNTS -> R.string.title_accounts
                ListMode.CATEGORIES -> R.string.title_categories
                ListMode.CURRENCIES -> R.string.title_currencies
                ListMode.EXCHANGE_RATES -> R.string.title_exchange_rates
                ListMode.WAREHOUSES -> R.string.title_warehouses
                ListMode.CASHBOXES -> R.string.title_cashboxes
                ListMode.ACCOUNT_LIMITS -> R.string.title_account_limits
                ListMode.USERS -> R.string.title_users
                ListMode.ALERTS -> R.string.title_alerts
                ListMode.YEARLY_CLOSINGS -> R.string.title_yearly_closings
                ListMode.ITEM_GROUPS -> R.string.title_item_groups
                ListMode.UNITS -> R.string.title_units
                ListMode.ITEMS -> R.string.title_items
                ListMode.ITEM_UNITS -> R.string.title_item_units
                ListMode.ITEM_PRICES -> R.string.title_item_prices
                ListMode.STOCK_BALANCES -> R.string.title_stock_balances
                ListMode.JOURNAL_LINES -> R.string.title_journal_lines
                ListMode.TRIAL_BALANCE -> R.string.title_trial_balance
                ListMode.INCOME_STATEMENT -> R.string.title_income_statement
                ListMode.FINANCIAL_POSITION -> R.string.title_financial_position
            }
        )
    }

    private fun isEditable(): Boolean {
        return when (mode) {
            ListMode.ACCOUNTS,
            ListMode.CATEGORIES,
            ListMode.CURRENCIES,
            ListMode.WAREHOUSES,
            ListMode.UNITS,
            ListMode.ITEM_GROUPS,
            ListMode.ITEMS,
            ListMode.USERS -> true

            else -> false
        }
    }

    private fun observeData() {
        val flow: Flow<List<SimpleItem>> = when (mode) {

            ListMode.ACCOUNTS -> repo.accounts.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, it.code) }
            }

            ListMode.CATEGORIES -> repo.categories.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, it.type) }
            }

            ListMode.CURRENCIES -> repo.currencies.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, it.code) }
            }

            ListMode.EXCHANGE_RATES -> repo.queries.observeExchangeRates().map { list ->
                list.map { SimpleItem(it.id, "عملة: ${it.currencyId} - سعر: ${it.rate}", it.rateDate) }
            }

            ListMode.WAREHOUSES -> repo.warehouses.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, if (it.isDefault) "افتراضي" else "") }
            }

            ListMode.CASHBOXES -> repo.cashboxes.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, "حساب: ${it.accountId}") }
            }

            ListMode.ACCOUNT_LIMITS -> repo.queries.observeAccountLimits().map { list ->
                list.map {
                    SimpleItem(
                        it.id,
                        "حساب: ${it.accountId} - عملة: ${it.currencyId}",
                        "مدين: ${it.debitLimit} - دائن: ${it.creditLimit}"
                    )
                }
            }

            ListMode.USERS -> repo.users.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.fullName, it.username) }
            }

            ListMode.ALERTS -> repo.alerts.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.title, it.body) }
            }

            ListMode.YEARLY_CLOSINGS -> repo.yearlyClosings.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.closingDate, "عملة: ${it.currencyId}") }
            }

            ListMode.ITEM_GROUPS -> repo.itemGroups.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, "") }
            }

            ListMode.UNITS -> repo.units.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, "") }
            }

            ListMode.ITEMS -> repo.items.observeAll().map { list ->
                list.map { SimpleItem(it.id, it.name, it.barcode) }
            }

            ListMode.ITEM_UNITS -> repo.queries.observeItemUnits().map { list ->
                list.map {
                    SimpleItem(
                        it.id,
                        "صنف: ${it.itemId} - وحدة: ${it.unitId}",
                        "تحويل: ${it.conversionFactor}"
                    )
                }
            }

            ListMode.ITEM_PRICES -> repo.queries.observeItemPrices().map { list ->
                list.map {
                    SimpleItem(
                        it.id,
                        "صنف: ${it.itemId} - سعر: ${it.price}",
                        it.effectiveDate
                    )
                }
            }

            ListMode.STOCK_BALANCES -> repo.queries.observeStockBalances().map { list ->
                list.map {
                    SimpleItem(
                        it.id,
                        "صنف: ${it.itemId} - مخزن: ${it.warehouseId}",
                        "كمية: ${it.qty} - تكلفة: ${it.averageCost}"
                    )
                }
            }

            ListMode.JOURNAL_LINES -> repo.queries.observeJournalLines().map { list ->
                list.map {
                    SimpleItem(
                        it.id,
                        "حساب: ${it.accountId}",
                        "مدين: ${it.debit} - دائن: ${it.credit} - ${it.date}"
                    )
                }
            }

            ListMode.TRIAL_BALANCE -> repo.queries.observeTrialBalance().map { list ->
                list.map {
                    SimpleItem(
                        it.accountId,
                        listOf(it.code, it.accountName).filter { part -> part.isNotBlank() }.joinToString(" - "),
                        getString(R.string.report_debit_credit, it.debit.toString(), it.credit.toString())
                    )
                }
            }

            ListMode.INCOME_STATEMENT -> repo.queries.observeIncomeStatement().map { list ->
                list.map {
                    val natureLabel = if (it.nature == "REVENUE") {
                        getString(R.string.nature_revenue)
                    } else {
                        getString(R.string.nature_expense)
                    }
                    SimpleItem(
                        it.accountId,
                        it.accountName,
                        "$natureLabel: ${it.amount}"
                    )
                }
            }

            ListMode.FINANCIAL_POSITION -> repo.queries.observeFinancialPosition().map { list ->
                list.map {
                    val natureLabel = when (it.nature) {
                        "ASSET" -> getString(R.string.nature_asset)
                        "LIABILITY" -> getString(R.string.nature_liability)
                        else -> getString(R.string.nature_equity)
                    }
                    SimpleItem(
                        it.accountId,
                        it.accountName,
                        "$natureLabel: ${it.amount}"
                    )
                }
            }
        }

        lifecycleScope.launch {
            flow.collect { list ->
                adapter.submit(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onItemClicked(item: SimpleItem) {
        if (!isEditable()) return
        openEditDialog(item)
    }

    private fun onItemLongClicked(item: SimpleItem) {
        if (!isEditable()) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAddDialog() {
        when (mode) {

            ListMode.ACCOUNTS -> showEntityDialog(R.string.name, R.string.code, "", "") { primary, secondary ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.accounts.insert(
                            AccountEntity(name = primary, code = secondary, nature = "ASSET")
                        )
                    }
                }
            }

            ListMode.CATEGORIES -> showEntityDialog(R.string.name, null, "", "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.categories.insert(CategoryEntity(name = primary, type = "GENERAL"))
                    }
                }
            }

            ListMode.CURRENCIES -> showEntityDialog(R.string.name, R.string.code, "", "") { primary, secondary ->
                if (primary.isEmpty() || secondary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.currencies.insert(CurrencyEntity(name = primary, code = secondary))
                    }
                }
            }

            ListMode.WAREHOUSES -> showEntityDialog(R.string.name, null, "", "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.warehouses.insert(WarehouseEntity(name = primary))
                    }
                }
            }

            ListMode.UNITS -> showEntityDialog(R.string.name, null, "", "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.units.insert(UnitEntity(name = primary))
                    }
                }
            }

            ListMode.ITEM_GROUPS -> showEntityDialog(R.string.name, null, "", "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.itemGroups.insert(ItemGroupEntity(name = primary))
                    }
                }
            }

            ListMode.ITEMS -> showEntityDialog(R.string.name, R.string.code, "", "") { primary, secondary ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.items.insert(ItemEntity(name = primary, barcode = secondary))
                    }
                }
            }

            ListMode.USERS -> showEntityDialog(R.string.name, R.string.code, "", "") { primary, secondary ->
                if (primary.isEmpty() || secondary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        repo.users.insert(
                            UserEntity(
                                username = secondary,
                                fullName = primary,
                                passwordHash = ""
                            )
                        )
                    }
                }
            }

            else -> {}
        }
    }

    private fun openEditDialog(item: SimpleItem) {
        when (mode) {

            ListMode.ACCOUNTS -> showEntityDialog(R.string.name, R.string.code, item.primary, item.secondary) { primary, secondary ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.accounts.getById(item.id)
                        if (entity != null) {
                            repo.accounts.update(entity.copy(name = primary, code = secondary))
                        }
                    }
                }
            }

            ListMode.CATEGORIES -> showEntityDialog(R.string.name, null, item.primary, "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.categories.getById(item.id)
                        if (entity != null) {
                            repo.categories.update(entity.copy(name = primary))
                        }
                    }
                }
            }

            ListMode.CURRENCIES -> showEntityDialog(R.string.name, R.string.code, item.primary, item.secondary) { primary, secondary ->
                if (primary.isEmpty() || secondary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.currencies.getById(item.id)
                        if (entity != null) {
                            repo.currencies.update(entity.copy(name = primary, code = secondary))
                        }
                    }
                }
            }

            ListMode.WAREHOUSES -> showEntityDialog(R.string.name, null, item.primary, "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.warehouses.getById(item.id)
                        if (entity != null) {
                            repo.warehouses.update(entity.copy(name = primary))
                        }
                    }
                }
            }

            ListMode.UNITS -> showEntityDialog(R.string.name, null, item.primary, "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.units.getById(item.id)
                        if (entity != null) {
                            repo.units.update(entity.copy(name = primary))
                        }
                    }
                }
            }

            ListMode.ITEM_GROUPS -> showEntityDialog(R.string.name, null, item.primary, "") { primary, _ ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.itemGroups.getById(item.id)
                        if (entity != null) {
                            repo.itemGroups.update(entity.copy(name = primary))
                        }
                    }
                }
            }

            ListMode.ITEMS -> showEntityDialog(R.string.name, R.string.code, item.primary, item.secondary) { primary, secondary ->
                if (primary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.items.getById(item.id)
                        if (entity != null) {
                            repo.items.update(entity.copy(name = primary, barcode = secondary))
                        }
                    }
                }
            }

            ListMode.USERS -> showEntityDialog(R.string.name, R.string.code, item.primary, item.secondary) { primary, secondary ->
                if (primary.isEmpty() || secondary.isEmpty()) {
                    toastRequired()
                } else {
                    lifecycleScope.launch {
                        val entity = repo.users.getById(item.id)
                        if (entity != null) {
                            repo.users.update(entity.copy(fullName = primary, username = secondary))
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun deleteItem(item: SimpleItem) {
        lifecycleScope.launch {
            when (mode) {

                ListMode.ACCOUNTS -> {
                    repo.accounts.getById(item.id)?.let { repo.accounts.delete(it) }
                }

                ListMode.CATEGORIES -> {
                    repo.categories.getById(item.id)?.let { repo.categories.delete(it) }
                }

                ListMode.CURRENCIES -> {
                    repo.currencies.getById(item.id)?.let { repo.currencies.delete(it) }
                }

                ListMode.WAREHOUSES -> {
                    repo.warehouses.getById(item.id)?.let { repo.warehouses.delete(it) }
                }

                ListMode.UNITS -> {
                    repo.units.getById(item.id)?.let { repo.units.delete(it) }
                }

                ListMode.ITEM_GROUPS -> {
                    repo.itemGroups.getById(item.id)?.let { repo.itemGroups.delete(it) }
                }

                ListMode.ITEMS -> {
                    repo.items.getById(item.id)?.let { repo.items.delete(it) }
                }

                ListMode.USERS -> {
                    repo.users.getById(item.id)?.let { repo.users.delete(it) }
                }

                else -> {}
            }
        }
    }

    private fun showEntityDialog(
        primaryHintRes: Int,
        secondaryHintRes: Int?,
        initialPrimary: String,
        initialSecondary: String,
        onSave: (String, String) -> Unit
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etPrimary = EditText(this).apply {
            hint = getString(primaryHintRes)
            setText(initialPrimary)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        container.addView(etPrimary)

        var etSecondary: EditText? = null

        if (secondaryHintRes != null) {
            etSecondary = EditText(this).apply {
                hint = getString(secondaryHintRes)
                setText(initialSecondary)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(etSecondary)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val primary = etPrimary.text.toString().trim()
                val secondary = etSecondary?.text.toString().trim()
                onSave(primary, secondary)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toastRequired() {
        Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
    }
}
