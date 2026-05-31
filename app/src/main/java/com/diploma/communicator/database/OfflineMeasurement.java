package com.diploma.communicator.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сутність, що редставляє таблицю для зберігання вимірів при відсутності мережевого з'єднання.
 */
@Entity(tableName = "offline_measurements")
public class OfflineMeasurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int smoke;
    public int gas;
    public float radiation;
    public String timestamp;

    /**
     * Конструктор класу OfflineMeasurement.
     *
     * @param smoke     рівень диму
     * @param gas       рівень газу
     * @param radiation рівень радіації
     * @param timestamp час вимірювання
     */
    public OfflineMeasurement(int smoke, int gas, float radiation, String timestamp) {
        this.smoke = smoke;
        this.gas = gas;
        this.radiation = radiation;
        this.timestamp = timestamp;
    }
}
