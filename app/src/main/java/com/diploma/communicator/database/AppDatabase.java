package com.diploma.communicator.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Головний клас локальної бази даних Room.
 */
@Database(entities = {OfflineMeasurement.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    /**
     * Отримання доступу до інтерфейсу вимірювань.
     *
     * @return екземпляр MeasurementDao
     */
    public abstract MeasurementDao measurementDao();

    /**
     * Реалізація бпзи даних.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * Повертає єдиний екземпляр AppDatabase.
     *
     * @param context контекст застосунку
     * @return екземпляр локальної бази даних
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "offline_data.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
