package com.muhasibtech.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityMainBinding
import com.muhasibtech.app.ui.document.DocumentListActivity
import com.muhasibtech.app.ui.list.ListMode
import com.muhasibtech.app.ui.list.SimpleListActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: AppRepository

    private val expandedSections = mutableSetOf(1, 2, 3, 4, 5)

    private val mainAdapter = MainAdapter(
        onChildClick = { childId -> onChildClicked(childId) },
        onHeaderClick = { headerId -> toggleHeader(headerId) }
    )

    private data class Section(
        val id: Int,
        val titleRes: Int,
        val children: List<Pair<String, Int>>
    )

    private val sections by lazy {
        listOf(
            Section(
                id = 1,
                titleRes = R.string.section_stock_ops,
                children = listOf(
                    MainNav.STOCK_OUT to R.string.child_stock_out,
                    MainNav.STOCK_IN to R.string.child_stock_in,
                    MainNav.STOCK_TRANSFER to R.string.child_stock_transfer,
                    MainNav.STOCK_ADJUSTMENT to R.string.child_stock_adjustment,
                    MainNav.ADD_WAREHOUSE to R.string.child_add_warehouse,
                    MainNav.STOCKTAKE to R.string.child_stocktake
                )
            ),
            Section(
                id = 2,
                titleRes = R.string.section_accounts,
                children = listOf(
                    MainNav.JOURNAL_DAILY to R.string.child_journal_daily,
                    MainNav.JOURNAL_OPENING to R.string.child_journal_opening,
                    MainNav.ADD_ACCOUNT to R.string.child_add_account,
                    MainNav.CASHBOX_STATEMENT to R.string.child_cashbox_statement,
                    MainNav.CHART_ACCOUNTS to R.string.child_chart_accounts,
                    MainNav.YEARLY_CLOSE to R.string.child_yearly_close
                )
            ),
            Section(
                id = 3,
                titleRes = R.string.section_items,
                children = listOf(
                    MainNav.ITEMS to R.string.child_items,
                    MainNav.SALE_PRICES to R.string.child_sale_prices,
                    MainNav.ITEM_UNITS to R.string.child_item_units,
                    MainNav.QUOTATION to R.string.child_quotation,
                    MainNav.PURCHASE_REQUEST to R.string.child_purchase_request
                )
            ),
            Section(
                id = 4,
                titleRes = R.string.section_currencies,
                children = listOf(
                    MainNav.CURRENCIES to R.string.child_currencies,
                    MainNav.EXCHANGE_RATES to R.string.child_exchange_rates,
                    MainNav.ACCOUNT_LIMITS to R.string.child_account_limits
                )
            ),
            Section(
                id = 5,
                titleRes = R.string.section_reports,
                children = listOf(
                    MainNav.REPORT_ITEM_MOVEMENT to R.string.child_item_movement,
                    MainNav.REPORT_REMAINING_STOCK to R.string.child_remaining_stock,
                    MainNav.REPORT_ACCOUNT_BALANCES to R.string.child_account_balances,
                    MainNav.REPORT_TRIAL_BALANCE to R.string.child_trial_balance,
                    MainNav.REPORT_INCOME to R.string.child_income,
                    MainNav.REPORT_FINANCIAL_POSITION to R.string.child_financial_position,
                    MainNav.REPORT_OTHER to R.string.child_other_reports
                )
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        setupToolbar()
        setupShortcuts()
        setupRecyclerView()

        binding.btnSwitchWarehouse.setOnClickListener {
            showWarehouseSwitchDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshWarehouse()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notifications -> {
                    SimpleListActivity.start(this, ListMode.ALERTS)
                    true
                }

                R.id.action_share -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name))
                    }
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.share)))
                    true
                }

                else -> false
            }
        }
    }

    private fun setupShortcuts() {
        binding.btnSales.setOnClickListener {
            DocumentListActivity.start(this, DocumentListActivity.SALES_ALL, getString(R.string.sales))
        }

        binding.btnPurchases.setOnClickListener {
            DocumentListActivity.start(this, DocumentListActivity.PURCHASES_ALL, getString(R.string.purchases))
        }

        binding.btnReceiptPayment.setOnClickListener {
            DocumentListActivity.start(this, DocumentListActivity.CASH_ALL, getString(R.string.receipt_payment))
        }

        binding.btnAccounts.setOnClickListener {
            SimpleListActivity.start(this, ListMode.ACCOUNTS)
        }
    }

    private fun setupRecyclerView() {
        binding.rvMain.layoutManager = LinearLayoutManager(this)
        binding.rvMain.adapter = mainAdapter
        loadMainItems()
    }

    private fun toggleHeader(headerId: Int) {
        if (expandedSections.contains(headerId)) {
            expandedSections.remove(headerId)
        } else {
            expandedSections.add(headerId)
        }
        loadMainItems()
    }

    private fun loadMainItems() {
        val flat = mutableListOf<MainListItem>()

        sections.forEach { section ->
            val expanded = expandedSections.contains(section.id)

            flat.add(
                MainListItem.Header(
                    id = section.id,
                    title = getString(section.titleRes),
                    expanded = expanded
                )
            )

            if (expanded) {
                section.children.forEach { child ->
                    flat.add(
                        MainListItem.Child(
                            id = child.first,
                            title = getString(child.second)
                        )
                    )
                }
            }
        }

        mainAdapter.submit(flat)
    }

    private fun refreshWarehouse() {
        lifecycleScope.launch {
            var currentId = repo.settings.getCurrentWarehouseId()

            if (currentId == 0L) {
                val defaultWarehouse = repo.warehouses.getDefault()
                    ?: repo.warehouses.observeActive().firstOrNull()

                if (defaultWarehouse != null) {
                    currentId = defaultWarehouse.id
                    repo.settings.setCurrentWarehouseId(currentId)
                }
            }

            val warehouseName = if (currentId == 0L) {
                getString(R.string.warehouse)
            } else {
                repo.warehouses.getById(currentId)?.name ?: getString(R.string.warehouse)
            }

            binding.tvWarehouse.text = warehouseName
        }
    }

    private fun showWarehouseSwitchDialog() {
        lifecycleScope.launch {
            val warehouses = repo.warehouses.observeActive().first()

            if (warehouses.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.no_results, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val names = warehouses.map { it.name }.toTypedArray()
            val currentId = repo.settings.getCurrentWarehouseId()
            
            var selectedIndex = 0
            for (i in warehouses.indices) {
                if (warehouses[i].id == currentId) {
                    selectedIndex = i
                    break
                }
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.warehouse)
                .setSingleChoiceItems(names, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton(R.string.save) { _, _ ->
                    val chosen = warehouses[selectedIndex]
                    lifecycleScope.launch {
                        repo.settings.setCurrentWarehouseId(chosen.id)
                        refreshWarehouse()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun onChildClicked(id: String) {
        when (id) {

            MainNav.STOCK_OUT -> DocumentListActivity.start(this, DocType.STOCK_OUT, getString(R.string.child_stock_out))
            MainNav.STOCK_IN -> DocumentListActivity.start(this, DocType.STOCK_IN, getString(R.string.child_stock_in))
            MainNav.STOCK_TRANSFER -> DocumentListActivity.start(this, DocType.STOCK_TRANSFER, getString(R.string.child_stock_transfer))
            MainNav.STOCK_ADJUSTMENT -> DocumentListActivity.start(this, DocType.STOCK_ADJUSTMENT, getString(R.string.child_stock_adjustment))
            MainNav.STOCKTAKE -> DocumentListActivity.start(this, DocType.STOCKTAKE, getString(R.string.child_stocktake))

            MainNav.JOURNAL_DAILY -> DocumentListActivity.start(this, DocType.JOURNAL_DAILY, getString(R.string.child_journal_daily))
            MainNav.JOURNAL_OPENING -> DocumentListActivity.start(this, DocType.JOURNAL_OPENING, getString(R.string.child_journal_opening))
            MainNav.QUOTATION -> DocumentListActivity.start(this, DocType.QUOTATION, getString(R.string.child_quotation))
            MainNav.PURCHASE_REQUEST -> DocumentListActivity.start(this, DocType.PURCHASE_REQUEST, getString(R.string.child_purchase_request))

            MainNav.ADD_WAREHOUSE -> SimpleListActivity.start(this, ListMode.WAREHOUSES)
            MainNav.ADD_ACCOUNT -> SimpleListActivity.start(this, ListMode.ACCOUNTS)
            MainNav.CHART_ACCOUNTS -> SimpleListActivity.start(this, ListMode.ACCOUNTS)
            MainNav.CASHBOX_STATEMENT -> SimpleListActivity.start(this, ListMode.CASHBOXES)
            MainNav.YEARLY_CLOSE -> SimpleListActivity.start(this, ListMode.YEARLY_CLOSINGS)

            MainNav.ITEMS -> SimpleListActivity.start(this, ListMode.ITEMS)
            MainNav.SALE_PRICES -> SimpleListActivity.start(this, ListMode.ITEM_PRICES)
            MainNav.ITEM_UNITS -> SimpleListActivity.start(this, ListMode.ITEM_UNITS)

            MainNav.CURRENCIES -> SimpleListActivity.start(this, ListMode.CURRENCIES)
            MainNav.EXCHANGE_RATES -> SimpleListActivity.start(this, ListMode.EXCHANGE_RATES)
            MainNav.ACCOUNT_LIMITS -> SimpleListActivity.start(this, ListMode.ACCOUNT_LIMITS)

            MainNav.REPORT_ITEM_MOVEMENT -> SimpleListActivity.start(this, ListMode.STOCK_BALANCES)
            MainNav.REPORT_REMAINING_STOCK -> SimpleListActivity.start(this, ListMode.STOCK_BALANCES)
            MainNav.REPORT_ACCOUNT_BALANCES -> SimpleListActivity.start(this, ListMode.ACCOUNTS)
            MainNav.REPORT_TRIAL_BALANCE -> SimpleListActivity.start(this, ListMode.JOURNAL_LINES)
            MainNav.REPORT_INCOME -> SimpleListActivity.start(this, ListMode.JOURNAL_LINES)
            MainNav.REPORT_FINANCIAL_POSITION -> SimpleListActivity.start(this, ListMode.ACCOUNTS)
            MainNav.REPORT_OTHER -> SimpleListActivity.start(this, ListMode.JOURNAL_LINES)
        }
    }

    object MainNav {
        const val STOCK_OUT = "stock_out"
        const val STOCK_IN = "stock_in"
        const val STOCK_TRANSFER = "stock_transfer"
        const val STOCK_ADJUSTMENT = "stock_adjustment"
        const val ADD_WAREHOUSE = "add_warehouse"
        const val STOCKTAKE = "stocktake"

        const val JOURNAL_DAILY = "journal_daily"
        const val JOURNAL_OPENING = "journal_opening"
        const val ADD_ACCOUNT = "add_account"
        const val CASHBOX_STATEMENT = "cashbox_statement"
        const val CHART_ACCOUNTS = "chart_accounts"
        const val YEARLY_CLOSE = "yearly_close"

        const val ITEMS = "items"
        const val SALE_PRICES = "sale_prices"
        const val ITEM_UNITS = "item_units"
        const val QUOTATION = "quotation"
        const val PURCHASE_REQUEST = "purchase_request"

        const val CURRENCIES = "currencies"
        const val EXCHANGE_RATES = "exchange_rates"
        const val ACCOUNT_LIMITS = "account_limits"

        const val REPORT_ITEM_MOVEMENT = "report_item_movement"
        const val REPORT_REMAINING_STOCK = "report_remaining_stock"
        const val REPORT_ACCOUNT_BALANCES = "report_account_balances"
        const val REPORT_TRIAL_BALANCE = "report_trial_balance"
        const val REPORT_INCOME = "report_income"
        const val REPORT_FINANCIAL_POSITION = "report_financial_position"
        const val REPORT_OTHER = "report_other"
    }
}
