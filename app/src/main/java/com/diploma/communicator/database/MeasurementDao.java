package com.diploma.communicator.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Інтерфейс доступу до даних сутності OfflineMeasurement.
 * Містить SQL-команди для роботи з локальною базою даних.
 */
@Dao
public interface MeasurementDao {
    /**
     * Додає новий запис вимірювання в таблицю.
     *
     * @param measurement об'єкт вимірювання
     */
    @Insert
    void insert(OfflineMeasurement measurement);

    /**
     * Отримує найстаріший запис з бази даних для його подальшої синхронізації з сервером.
     *
     * @return найстаріший об'єкт OfflineMeasurement
     */
    @Query("SELECT * FROM offline_measurements ORDER BY id ASC LIMIT 1")
    OfflineMeasurement getOldestMeasurement();

    /**
     * Видаляє вказаний запис із бази даних.
     *
     * @param measurement об'єкт, який потрібно видалити
     */
    @Delete
    void delete(OfflineMeasurement measurement);
}
