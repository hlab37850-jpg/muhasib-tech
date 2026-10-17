package com.muhasibtech.app.data.service

import org.junit.Assert.assertThrows
import org.junit.Test

class StockAvailabilityValidatorTest {
    @Test
    fun acceptsQuantityWithinAvailableStock() {
        StockAvailabilityValidator.validate(10.0, 4.0)
    }

    @Test
    fun acceptsExactAvailableQuantity() {
        StockAvailabilityValidator.validate(5.0, 5.0)
    }

    @Test
    fun toleratesFloatingPointNoise() {
        StockAvailabilityValidator.validate(1.0, 1.0000005)
    }

    @Test
    fun rejectsActualShortage() {
        assertThrows(IllegalStateException::class.java) {
            StockAvailabilityValidator.validate(3.0, 3.1)
        }
    }

    @Test
    fun rejectsNegativeAvailableQuantity() {
        assertThrows(IllegalStateException::class.java) {
            StockAvailabilityValidator.validate(-1.0, 1.0)
        }
    }

    @Test
    fun rejectsNonFiniteValues() {
        assertThrows(IllegalStateException::class.java) {
            StockAvailabilityValidator.validate(Double.NaN, 1.0)
        }
    }
}
