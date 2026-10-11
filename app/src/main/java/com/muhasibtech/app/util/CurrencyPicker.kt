package com.muhasibtech.app.util

import android.content.Context
import android.widget.RadioButton
import android.widget.RadioGroup
import com.muhasibtech.app.data.local.entity.CurrencyEntity

object CurrencyPicker {

    fun build(
        context: Context,
        radioGroup: RadioGroup,
        currencies: List<CurrencyEntity>,
        selectedId: Long,
        onPick: (Long) -> Unit
    ) {
        radioGroup.removeAllViews()

        currencies.forEachIndexed { index, currency ->
            val button = RadioButton(context).apply {
                id = 100 + index
                text = currency.name
                isChecked = currency.id == selectedId
            }
            radioGroup.addView(button)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId >= 100 && checkedId - 100 < currencies.size) {
                onPick(currencies[checkedId - 100].id)
            }
        }
    }
}
