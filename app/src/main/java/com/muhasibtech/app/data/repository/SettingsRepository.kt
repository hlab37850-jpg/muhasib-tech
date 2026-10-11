package com.muhasibtech.app.data.repository

import com.muhasibtech.app.data.local.dao.SettingDao
import com.muhasibtech.app.data.local.entity.SettingEntity
import com.muhasibtech.app.data.model.SettingKeys

class SettingsRepository(
    private val settingDao: SettingDao
) {

    suspend fun save(key: String, value: String) {
        settingDao.upsert(SettingEntity(key = key, value = value))
    }

    suspend fun getString(key: String, default: String = ""): String {
        return settingDao.getByKey(key)?.value ?: default
    }

    suspend fun getLong(key: String, default: Long = 0L): Long {
        val value = getString(key, default.toString())
        return value.toLongOrNull() ?: default
    }

    suspend fun getInt(key: String, default: Int = 0): Int {
        val value = getString(key, default.toString())
        return value.toIntOrNull() ?: default
    }

    suspend fun getBoolean(key: String, default: Boolean = true): Boolean {
        val value = getString(key, if (default) "1" else "0")
        return value == "1"
    }

    suspend fun setCurrentWarehouseId(id: Long) {
        save(SettingKeys.CURRENT_WAREHOUSE_ID, id.toString())
    }

    suspend fun getCurrentWarehouseId(): Long {
        return getLong(SettingKeys.CURRENT_WAREHOUSE_ID, 0L)
    }

    suspend fun setCurrentCurrencyId(id: Long) {
        save(SettingKeys.CURRENT_CURRENCY_ID, id.toString())
    }

    suspend fun getCurrentCurrencyId(): Long {
        return getLong(SettingKeys.CURRENT_CURRENCY_ID, 0L)
    }
}
