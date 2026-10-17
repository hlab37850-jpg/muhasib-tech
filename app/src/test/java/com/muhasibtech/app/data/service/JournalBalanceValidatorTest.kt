package com.muhasibtech.app.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalBalanceValidatorTest {

    @Test
    fun acceptsBalancedJournal() {
        JournalBalanceValidator.validate(totalDebit = 125.5, totalCredit = 125.5)
    }

    @Test
    fun acceptsSmallFloatingPointDifferenceWithinTolerance() {
        JournalBalanceValidator.validate(totalDebit = 10.0005, totalCredit = 10.0)
    }

    @Test
    fun rejectsUnbalancedJournal() {
        val error = runCatching {
            JournalBalanceValidator.validate(totalDebit = 100.0, totalCredit = 99.0)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("غير متوازن") == true)
    }

    @Test
    fun rejectsNonFiniteTotals() {
        val error = runCatching {
            JournalBalanceValidator.validate(totalDebit = Double.NaN, totalCredit = 0.0)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun rejectsNegativeTotals() {
        val error = runCatching {
            JournalBalanceValidator.validate(totalDebit = -1.0, totalCredit = 1.0)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
