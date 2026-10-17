package com.muhasibtech.app.data.service

import kotlin.math.abs

/**
 * Guards the double-entry invariant before journal lines are persisted.
 */
object JournalBalanceValidator {
    const val TOLERANCE = 0.001

    fun validate(totalDebit: Double, totalCredit: Double) {
        require(totalDebit.isFinite() && totalCredit.isFinite()) {
            "مجاميع القيد غير صالحة"
        }
        require(totalDebit >= 0.0 && totalCredit >= 0.0) {
            "لا يمكن أن تكون مجاميع القيد سالبة"
        }
        check(abs(totalDebit - totalCredit) <= TOLERANCE) {
            "القيد الناتج عن المستند غير متوازن: المدين $totalDebit والدائن $totalCredit"
        }
    }
}
