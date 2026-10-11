package com.muhasibtech.app.ui.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.dao.AccountBalance
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.model.AccountNature
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityAccountsBinding
import com.muhasibtech.app.databinding.ItemAccountRowBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AccountsActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AccountsActivity::class.java))
        }
    }

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var repo: AppRepository

    private var accounts: List<AccountEntity> = emptyList()
    private var balances: Map<Long, AccountBalance> = emptyMap()
    private var filterRoot: Long = 0
    private val expanded = mutableSetOf<Long>()

    private val adapter = AccountTreeAdapter(
        onRowClick = { account -> onRowClicked(account) },
        onPlusClick = { account -> StatementActivity.start(this, account.id) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        binding.toolbar.title = getString(R.string.title_accounts)
        binding.toolbar.inflateMenu(R.menu.menu_accounts)

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_group) {
                showGroupDialog()
                true
            } else false
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnTransfer.setOnClickListener {
            TransferActivity.start(this)
        }

        binding.fab.setOnClickListener {
            showAddAccountDialog()
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            accounts = repo.accounts.observeAll().first()
            balances = repo.queries.getAccountBalances().associateBy { it.accountId }
            rebuild()
        }
    }

    private fun balanceOf(id: Long): Double {
        val b = balances[id] ?: return 0.0
        return b.totalDebit - b.totalCredit
    }

    private fun childrenOf(id: Long): List<AccountEntity> = accounts.filter { it.parentId == id }

    private fun rebuild() {
        val rows = mutableListOf<AccountEntity>()

        val roots = if (filterRoot == 0L) {
            accounts.filter { it.parentId == 0L }
        } else {
            accounts.filter { it.id == filterRoot }
        }

        roots.forEach { root ->
            rows.add(root)
            if (expanded.contains(root.id)) {
                addChildren(root.id, rows)
            }
        }

        adapter.submit(rows, expanded.toSet(), ::hasChildren, ::balanceOf)

        var onHand = 0.0
        var toHand = 0.0

        accounts.forEach { account ->
            val value = balanceOf(account.id)
            if (value > 0) onHand += value else toHand -= value
        }

        lifecycleScope.launch {
            val currencyName = repo.currencies.getById(repo.settings.getCurrentCurrencyId())?.name ?: ""
            binding.tvTotals.text = String.format(
                Locale.US,
                "%s: %.2f | %s: %.2f %s",
                getString(R.string.on_hand), onHand,
                getString(R.string.to_hand), toHand,
                currencyName
            )
        }
    }

    private fun addChildren(parentId: Long, rows: MutableList<AccountEntity>) {
        childrenOf(parentId).forEach { child ->
            rows.add(child)
            if (expanded.contains(child.id)) {
                addChildren(child.id, rows)
            }
        }
    }

    private fun hasChildren(id: Long): Boolean = childrenOf(id).isNotEmpty()

    private fun onRowClicked(account: AccountEntity) {
        if (hasChildren(account.id)) {
            if (expanded.contains(account.id)) expanded.remove(account.id)
            else expanded.add(account.id)
            rebuild()
        } else {
            StatementActivity.start(this, account.id)
        }
    }

    private fun showGroupDialog() {
        val roots = accounts.filter { it.parentId == 0L }
        val names = mutableListOf(getString(R.string.all_accounts))
        names.addAll(roots.map { it.name })

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_group)
            .setItems(names.toTypedArray()) { _, which ->
                filterRoot = if (which == 0) 0L else roots[which - 1].id
                binding.toolbar.title = if (filterRoot == 0L) {
                    getString(R.string.title_accounts)
                } else {
                    roots.first { it.id == filterRoot }.name
                }
                expanded.clear()
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddAccountDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val etName = EditText(this).apply { hint = getString(R.string.name) }
        val etCode = EditText(this).apply { hint = getString(R.string.code) }

        val parentNames = mutableListOf(getString(R.string.none))
        parentNames.addAll(accounts.map { it.name })

        val parentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            parentNames
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val spParent = Spinner(this).apply { adapter = parentAdapter }

        val natureNames = listOf(
            getString(R.string.nature_asset),
            getString(R.string.nature_liability),
            getString(R.string.nature_equity),
            getString(R.string.nature_revenue),
            getString(R.string.nature_expense)
        )

        val natureValues = arrayOf(
            AccountNature.ASSET,
            AccountNature.LIABILITY,
            AccountNature.EQUITY,
            AccountNature.REVENUE,
            AccountNature.EXPENSE
        )

        val natureAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            natureNames
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val spNature = Spinner(this).apply { adapter = natureAdapter }

        container.addView(etName)
        container.addView(etCode)
        container.addView(spParent)
        container.addView(spNature)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_account)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val parentId = if (spParent.selectedItemPosition == 0) {
                    0L
                } else {
                    accounts[spParent.selectedItemPosition - 1].id
                }

                lifecycleScope.launch {
                    repo.accounts.insert(
                        AccountEntity(
                            name = name,
                            code = etCode.text.toString().trim(),
                            parentId = parentId,
                            nature = natureValues[spNature.selectedItemPosition]
                        )
                    )
                    loadData()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

class AccountTreeAdapter(
    private val onRowClick: (AccountEntity) -> Unit,
    private val onPlusClick: (AccountEntity) -> Unit
) : RecyclerView.Adapter<AccountTreeAdapter.ViewHolder>() {

    private var rows: List<AccountEntity> = emptyList()
    private var expanded: Set<Long> = emptySet()
    private var hasChildren: (Long) -> Boolean = { false }
    private var balanceOf: (Long) -> Double = { 0.0 }

    fun submit(
        newRows: List<AccountEntity>,
        expandedSet: Set<Long>,
        hasChildrenFn: (Long) -> Boolean,
        balanceFn: (Long) -> Double
    ) {
        rows = newRows
        expanded = expandedSet
        hasChildren = hasChildrenFn
        balanceOf = balanceFn
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAccountRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = rows[position]
        val balance = balanceOf(account.id)

        holder.binding.tvName.text = account.name
        holder.binding.tvCode.text = account.code
        holder.binding.tvBalance.text = String.format(Locale.US, "%.2f", balance)

        if (balance > 0) {
            holder.binding.tvBalance.setTextColor(0xFFB91C1C.toInt())
        } else if (balance < 0) {
            holder.binding.tvBalance.setTextColor(0xFF15803D.toInt())
        }

        if (!hasChildren(account.id)) {
            holder.binding.tvArrow.text = ""
        } else if (expanded.contains(account.id)) {
            holder.binding.tvArrow.text = "▼"
            holder.binding.tvArrow.setTextColor(0xFFB91C1C.toInt())
        } else {
            holder.binding.tvArrow.text = "◀"
            holder.binding.tvArrow.setTextColor(0xFFB91C1C.toInt())
        }

        holder.binding.root.setOnClickListener { onRowClick(account) }
        holder.binding.btnPlus.setOnClickListener { onPlusClick(account) }
    }

    class ViewHolder(val binding: ItemAccountRowBinding) : RecyclerView.ViewHolder(binding.root)
}
