package com.muhasibtech.app.ui.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.JournalLineEntity
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityStatementBinding
import com.muhasibtech.app.databinding.ItemStatementRowBinding
import kotlinx.coroutines.launch
import java.util.Locale

class StatementActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ACCOUNT_ID = "extra_account_id"

        fun start(context: Context, accountId: Long) {
            val intent = Intent(context, StatementActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_ID, accountId)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityStatementBinding
    private lateinit var repo: AppRepository
    private var accountId: Long = 0

    private val adapter = StatementAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository
        accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, 0)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnSettle.setOnClickListener {
            SettleActivity.start(this, accountId)
        }

        lifecycleScope.launch {
            val account = repo.accounts.getById(accountId)
            binding.toolbar.title = account?.name ?: getString(R.string.statement)

            repo.journalLines.observeByAccount(accountId).collect { lines ->
                showLines(lines)
            }
        }
    }

    private fun showLines(lines: List<JournalLineEntity>) {
        val ascending = lines.reversed()

        var running = 0.0
        val withBalance = ascending.map { line ->
            running += line.debit - line.credit
            Pair(line, running)
        }.reversed()

        adapter.submit(withBalance)

        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        binding.tvTotals.text = String.format(
            Locale.US,
            "%s: %.2f | %s: %.2f | %s: %.2f",
            getString(R.string.debit), totalDebit,
            getString(R.string.credit), totalCredit,
            getString(R.string.running_balance), totalDebit - totalCredit
        )
    }
}

class StatementAdapter : RecyclerView.Adapter<StatementAdapter.ViewHolder>() {

    private var rows: List<Pair<JournalLineEntity, Double>> = emptyList()

    fun submit(newRows: List<Pair<JournalLineEntity, Double>>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemStatementRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = rows[position].first
        val balance = rows[position].second

        holder.binding.tvDate.text = line.date
        holder.binding.tvDetails.text = line.details
        holder.binding.tvDebit.text = String.format(Locale.US, "%.2f", line.debit)
        holder.binding.tvCredit.text = String.format(Locale.US, "%.2f", line.credit)
        holder.binding.tvBalance.text = String.format(Locale.US, "%.2f", balance)
    }

    class ViewHolder(val binding: ItemStatementRowBinding) : RecyclerView.ViewHolder(binding.root)
}
