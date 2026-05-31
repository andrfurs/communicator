package com.diploma.communicator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.diploma.communicator.http.Sender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Фонова служба для постійного з'єднання з BLE-пристроєм.
 * Отримує дані через GATT, парсить їх та делегує оновлення UI і відправку на сервер.
 */
public class BleForegroundService extends Service {
    private static final String TAG = "BLE_CLIENT";
    private static final String CHANNEL_ID = "BleServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_DATA_RECEIVED = "com.diploma.communicator.ACTION_DATA_RECEIVED";
    public static final String EXTRA_SMOKE = "EXTRA_SMOKE";
    public static final String EXTRA_GAS = "EXTRA_GAS";
    public static final String EXTRA_RAD = "EXTRA_RAD";
    private UUID SERVICE_UUID;
    private UUID CHAR_UUID;
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceMacAddress;

    private Sender sender;

    /**
     * Ініціалізує сервіс, зчитує налаштування з SharedPrefs та реєструє BroadcastReceiver.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();

        SERVICE_UUID = UUID.fromString(SharedPrefs.getServiceUuid(this));
        CHAR_UUID = UUID.fromString(SharedPrefs.getCharUuid(this));

        createNotificationChannel();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        IntentFilter filter = new IntentFilter("com.diploma.communicator.ACTION_RELOAD_CONFIG");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(configReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(configReceiver, filter);
        }

        sender = new Sender(this);
    }

    /**
     * Слухач зміни стану Bluetooth системи.
     */
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(TAG, "Bluetooth вимкнено. Очищення з'єднання...");
                        closeGatt();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "Bluetooth увімкнено. Спроба перепідключення...");
                        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                        if (bm != null) bluetoothAdapter = bm.getAdapter();
                        if (deviceMacAddress != null) {
                            connectToDevice(deviceMacAddress);
                        }
                        break;
                }
            }
        }
    };

    /**
     * Слухач оновлення конфігураційних даних для перезавантаження BLE-з'єднання.
     */
    private final BroadcastReceiver configReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.diploma.communicator.ACTION_RELOAD_CONFIG".equals(intent.getAction())) {
                Log.i(TAG, "Отримано сигнал оновлення налаштувань. Оновлюємо BLE...");

                try {
                    UUID oldCharUuid = CHAR_UUID;

                    SERVICE_UUID = UUID.fromString(SharedPrefs.getServiceUuid(BleForegroundService.this));
                    CHAR_UUID = UUID.fromString(SharedPrefs.getCharUuid(BleForegroundService.this));

                    if (bluetoothGatt != null && hasBluetoothPermission()) {
                        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
                        if (service != null) {
                            BluetoothGattCharacteristic oldChar = service.getCharacteristic(oldCharUuid);
                            if (oldChar != null) {
                                bluetoothGatt.setCharacteristicNotification(oldChar, false);
                            }
                        }
                    }

                    if (deviceMacAddress != null) {
                        connectToDevice(deviceMacAddress);
                    }

                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "ПОМИЛКА: Некоректний формат UUID! " + e.getMessage());
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("З'єднання активне")
                .setContentText("Отримання даних у фоні...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        if (intent != null && intent.hasExtra("MAC_ADDRESS")) {
            deviceMacAddress = intent.getStringExtra("MAC_ADDRESS");
            connectToDevice(deviceMacAddress);
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "Додаток закрито користувачем. Зупинка служби...");

        stopForeground(true);
        stopSelf();
    }

    /**
     * Підключається до GATT-сервера пристрою за вказаною MAC-адресою.
     *
     * @param address MAC-адреса BLE-пристрою
     */
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null || !bluetoothAdapter.isEnabled()) return;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        closeGatt();

        if (hasBluetoothPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(this, false, gattCallback);
            }
        }
    }

    /**
     * Закриває та очищує GATT з'єднання.
     */
    private void closeGatt() {
        if (bluetoothGatt != null) {
            if (hasBluetoothPermission()) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
    }

    /**
     * Callbacks для управління GATT подіями.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Підключено до GATT сервера.");
                if (hasBluetoothPermission()) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Відключено від GATT сервера.");
                closeGatt();
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && deviceMacAddress != null) {
                    Log.i(TAG, "Спроба автоматичного перепідключення...");
                    connectToDevice(deviceMacAddress);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHAR_UUID);
                    if (characteristic != null) {
                        if (hasBluetoothPermission()) {
                            gatt.setCharacteristicNotification(characteristic, true);

                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            processData(characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            processData(value);
        }

        /**
         * Обробляє та розбирає сирі байтові дані від BLE-пристрою.
         *
         * @param value масив байтів, що містить значення з датчиків
         */
        private void processData(byte[] value) {
            if (value != null && value.length > 0) {
                String receivedString = new String(value);
                Log.d(TAG, "Отримано дані: " + receivedString);

                try {
                    String[] parts = receivedString.split(",");
                    if (parts.length == 3) {
                        int smoke = Integer.parseInt(parts[0].trim());
                        int gas = Integer.parseInt(parts[1].trim());
                        float rad = Float.parseFloat(parts[2].trim());

                        broadcastData(smoke, gas, rad);

                        long currentTime = System.currentTimeMillis();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                Locale.getDefault());
                        String time = sdf.format(new Date(currentTime));

                        sender.sendDataToServer(smoke, gas, rad, time);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Помилка парсингу: " + receivedString, e);
                }
            }
        }
    };

    /**
     * Перевірка дозволів на використання Bluetooth.
     *
     * @return наявність дозволів
     */
    @SuppressLint("MissingPermission")
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Розсилає показники датчиків.
     *
     * @param smoke рівень диму
     * @param gas   рівень газу
     * @param rad   рівень радіації
     */
    private void broadcastData(int smoke, int gas, float rad) {
        Intent intent = new Intent(ACTION_DATA_RECEIVED);
        intent.putExtra(EXTRA_SMOKE, smoke);
        intent.putExtra(EXTRA_GAS, gas);
        intent.putExtra(EXTRA_RAD, rad);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeGatt();
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(configReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Створює канал нотифікацій, необхідний для запуску сервіса.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
