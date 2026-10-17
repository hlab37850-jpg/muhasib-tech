package com.muhasibtech.app.data.service

import org.junit.Assert.assertTrue
import org.junit.Test

class StockTransferValidatorTest {

    @Test
    fun acceptsDifferentPositiveWarehouses() {
        StockTransferValidator.validate(sourceWarehouseId = 1L, targetWarehouseId = 2L)
    }

    @Test
    fun rejectsMissingSourceWarehouse() {
        val error = runCatching {
            StockTransferValidator.validate(sourceWarehouseId = 0L, targetWarehouseId = 2L)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("مخزن المصدر") == true)
    }

    @Test
    fun rejectsMissingTargetWarehouse() {
        val error = runCatching {
            StockTransferValidator.validate(sourceWarehouseId = 1L, targetWarehouseId = 0L)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun rejectsTransferToSameWarehouse() {
        val error = runCatching {
            StockTransferValidator.validate(sourceWarehouseId = 3L, targetWarehouseId = 3L)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("المخزن نفسه") == true)
    }
}
