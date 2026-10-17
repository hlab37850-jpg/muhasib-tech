package com.muhasibtech.app.data.service

/**
 * Validates warehouse transfer endpoints before any stock balance is changed.
 */
object StockTransferValidator {
    fun validate(sourceWarehouseId: Long, targetWarehouseId: Long) {
        if (sourceWarehouseId <= 0L || targetWarehouseId <= 0L) {
            throw IllegalStateException("يجب تحديد مخزن المصدر ومخزن الوجهة")
        }
        if (sourceWarehouseId == targetWarehouseId) {
            throw IllegalStateException("لا يمكن تحويل المخزون إلى المخزن نفسه")
        }
    }
}
