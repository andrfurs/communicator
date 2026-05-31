package com.diploma.communicator.http;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.diploma.communicator.database.AppDatabase;
import com.diploma.communicator.database.MeasurementDao;
import com.diploma.communicator.database.OfflineMeasurement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Клас для відправки показників на сервер та керування процесом синхронізації офлайн-даних.
 */
public class Sender {
    private static final String TAG = "API";
    private boolean isSyncing = false;
    private final Context context;
    private final MeasurementDao dao;
    private final ExecutorService executor;

    public static final String ACTION_SERVER_STATUS = "com.diploma.communicator.ACTION_SERVER_STATUS";
    public static final String EXTRA_IS_CONNECTED = "EXTRA_IS_CONNECTED";

    /**
     * Конструктор Sender.
     *
     * @param context контекст застосунку для ініціалізації бази даних та Broadcast
     */
    public Sender(Context context) {
        this.context = context.getApplicationContext();
        this.dao = AppDatabase.getDatabase(this.context).measurementDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Відправляє системне повідомлення щодо стану підключення до сервера.
     *
     * @param isConnected статус підключення
     */
    private void sendStatusBroadcast(boolean isConnected) {
        Intent intent = new Intent(ACTION_SERVER_STATUS);
        intent.putExtra(EXTRA_IS_CONNECTED, isConnected);
        context.sendBroadcast(intent);
    }

    /**
     * Відправляє поточні дані датчиків на сервер. У разі помилки зберігає їх локально у базі даних Room.
     *
     * @param smoke     рівень диму
     * @param gas       рівень газу
     * @param radiation рівень радіації
     * @param time      час вимірювання
     */
    public void sendDataToServer(int smoke, int gas, float radiation, String time) {
        MeasurementsDto data = new MeasurementsDto(smoke, gas, radiation, time);

        ApiInterface api = RetrofitInstance.getClient(context).create(ApiInterface.class);

        Call<MeasurementsDto> call = api.sendMeasurements(data);
        call.enqueue(new Callback<MeasurementsDto>() {
            @Override
            public void onResponse(Call<MeasurementsDto> call, Response<MeasurementsDto> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Дані успішно відправлені. ID: " + response.body().getId());
                    sendStatusBroadcast(true);
                    syncOfflineData();
                } else {
                    Log.e(TAG, "Помилка сервера. Код: " + response.code());
                    sendStatusBroadcast(false);
                    saveToRoom(smoke, gas, radiation, time);
                }
            }

            @Override
            public void onFailure(Call<MeasurementsDto> call, Throwable t) {
                Log.e(TAG, "Помилка мережі: " + t.getMessage());
                sendStatusBroadcast(false);
                saveToRoom(smoke, gas, radiation, time);
            }
        });
    }

    /**
     * Асинхронно та відправляє на сервер збережені офлайн-дані з локальної бази.
     */
    private void syncOfflineData() {
        if (isSyncing) return;
        isSyncing = true;

        executor.execute(() -> {
            MeasurementDao dao = AppDatabase.getDatabase(context).measurementDao();

            OfflineMeasurement oldData = dao.getOldestMeasurement();

            if (oldData != null) {
                MeasurementsDto dto = new MeasurementsDto(oldData.smoke, oldData.gas, oldData.radiation,
                        oldData.timestamp);
                ApiInterface api = RetrofitInstance.getClient(context).create(ApiInterface.class);

                api.sendMeasurements(dto).enqueue(new Callback<MeasurementsDto>() {
                    @Override
                    public void onResponse(Call<MeasurementsDto> call, Response<MeasurementsDto> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Дані успішно відправлені. ID: " + response.body().getId());
                            sendStatusBroadcast(true);
                            executor.execute(() -> {
                                dao.delete(oldData);
                                isSyncing = false;
                                syncOfflineData();
                            });
                        } else {
                            isSyncing = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<MeasurementsDto> call, Throwable t) {
                        Log.e(TAG, "Помилка мережі: " + t.getMessage());
                        sendStatusBroadcast(false);
                        isSyncing = false;
                    }
                });
            } else {
                isSyncing = false;
            }
        });
    }

    /**
     * Зберігає дані у локальну базу даних у фоновому потоці.
     *
     * @param smoke рівень диму
     * @param gas   рівень газу
     * @param rad   рівень радіації
     * @param time  час вимірювання
     */
    private void saveToRoom(int smoke, int gas, float rad, String time) {
        executor.execute(() -> {
            dao.insert(new OfflineMeasurement(smoke, gas, rad, time));
            Log.d("ROOM", "Дані збережено в базу даних." + smoke + " " + gas + " " + rad +
                    " " + time);
        });
    }
}
