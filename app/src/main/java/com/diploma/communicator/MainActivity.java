package com.diploma.communicator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.diploma.communicator.http.ApiInterface;
import com.diploma.communicator.http.RetrofitInstance;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;

/**
 * Головна Activity застосунку.
 * Відповідає за запит дозволів, ініціалізацію сканування BLE, авторизацію
 * та відображення отриманих показників.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView smokeValue;
    private TextView gasValue;
    private TextView radiationValue;
    private TextView connectionValue;

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private boolean isDeviceFound = false;

    private final int SMOKE_THRESHOLD = 2000;
    private final int GAS_THRESHOLD = 2000;
    private final float RADIATION_THRESHOLD = 0.3f;

    private static final String ACTION_SERVER_STATUS = "com.diploma.communicator.ACTION_SERVER_STATUS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        smokeValue = findViewById(R.id.smokeValue);
        gasValue = findViewById(R.id.gasValue);
        radiationValue = findViewById(R.id.radiationValue);
        connectionValue = findViewById(R.id.connectionValue);
        connectionValue.setTextColor(Color.RED);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPermissionsAndStart();
    }

    /**
     * Перевіряє системні дозволи на Bluetooth та Геолокацію залежно від версії ОС Android.
     */
    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 1);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
                return;
            }
        }
        performLogin();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performLogin();
        } else {
            Log.e(TAG, "Дозволи на Bluetooth не надано!");
        }
    }

    /**
     * Виконує авторизацію через API, отримує JWT-токен та запускає сканування BLE.
     */
    private void performLogin() {
        ApiInterface api = RetrofitInstance.getClient(this).create(ApiInterface.class);

        Map<String, String> credentials = new java.util.HashMap<>();
        credentials.put("username", SharedPrefs.getUsername(this));
        credentials.put("password", SharedPrefs.getPassword(this));

        api.login(credentials).enqueue(new retrofit2.Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, retrofit2.Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().get("token");
                    RetrofitInstance.setToken(token);
                    Log.i("AUTH", "Успішна авторизація! Токен отримано. Починаємо сканування...");
                    Toast.makeText(MainActivity.this, "Успішна авторизація!", Toast.LENGTH_LONG).show();
                    startBleScan();
                } else {
                    Log.e("AUTH", "Помилка авторизації: " + response.code());
                    Toast.makeText(MainActivity.this, "Помилка авторизації!", Toast.LENGTH_LONG).show();
                    startBleScan();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e("AUTH", "Помилка мережі при логіні: " + t.getMessage());
                startBleScan();
            }
        });
    }

    /**
     * Починає сканування BLE пристроїв.
     */
    @SuppressLint("MissingPermission")
    private void startBleScan() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ba = bm.getAdapter();
        boolean isBtOn = (ba != null && ba.isEnabled());

        boolean isGpsOn = true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            isGpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        if (!isBtOn || !isGpsOn) {
            Log.d(TAG, "Очікування: BT=" + isBtOn + ", GPS=" + isGpsOn);
            return;
        }

        if (isScanning || isDeviceFound) return;

        bluetoothLeScanner = ba.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) return;

        try {
            bluetoothLeScanner.stopScan(scanCallback);
        } catch (Exception ignored) {
        }

        UUID currentServiceUuid = UUID.fromString(SharedPrefs.getServiceUuid(this));
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(currentServiceUuid)).build();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        isScanning = true;
        bluetoothLeScanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        Log.i(TAG, "Сканування запущено успішно.");
    }

    /**
     * Обробник результатів сканування.
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (isDeviceFound) return;
            isDeviceFound = true;

            String foundMacAddress = result.getDevice().getAddress();
            Log.i(TAG, "Знайдено ESP32. MAC: " + foundMacAddress);

            try {
                bluetoothLeScanner.stopScan(this);
            } catch (Exception ignored) {
            }
            isScanning = false;

            startBleService(foundMacAddress);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Помилка сканування BLE: " + errorCode);
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                isScanning = true;
            } else {
                isScanning = false;
            }
        }
    };

    /**
     * Запускає BleForegroundService, передаючи MAC-адресу знайденого пристрою.
     *
     * @param macAddress MAC-адреса цільового BLE пристрою
     */
    private void startBleService(String macAddress) {
        Intent serviceIntent = new Intent(this, BleForegroundService.class);
        serviceIntent.putExtra("MAC_ADDRESS", macAddress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * Слухча для відстеження системних станів Bluetooth та Локації.
     */
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.i(TAG, "Bluetooth увімкнено. Спроба сканування...");
                    startBleScan();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    resetBleFlags();
                }
            } else if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "Локацію змінено. Спроба сканування...");
                startBleScan();
            }
        }
    };

    /**
     * Скидає прапор сканування та зупиняє BluetoothLeScanner.
     */
    private void resetBleFlags() {
        isDeviceFound = false;
        isScanning = false;
        if (bluetoothLeScanner != null) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Слухач для отримання даних датчиків з BleForegroundService, а також статусу з'єднання з сервером.
     */
    private final BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BleForegroundService.ACTION_DATA_RECEIVED.equals(intent.getAction())) {
                int smoke = intent.getIntExtra(BleForegroundService.EXTRA_SMOKE, 0);
                int gas = intent.getIntExtra(BleForegroundService.EXTRA_GAS, 0);
                float rad = intent.getFloatExtra(BleForegroundService.EXTRA_RAD, 0);

                smokeValue.setText(String.valueOf(smoke));
                checkThreshold(smoke, SMOKE_THRESHOLD, smokeValue);
                gasValue.setText(String.valueOf(gas));
                checkThreshold(gas, GAS_THRESHOLD, gasValue);
                radiationValue.setText(String.valueOf(rad));
                checkThreshold(rad, RADIATION_THRESHOLD, radiationValue);
            } else if (ACTION_SERVER_STATUS.equals(action)) {
                boolean isConnected = intent.getBooleanExtra("EXTRA_IS_CONNECTED", false);

                if (isConnected) {
                    connectionValue.setText("є");
                    connectionValue.setTextColor(Color.GREEN);
                } else {
                    connectionValue.setText("немає");
                    connectionValue.setTextColor(Color.RED);
                }
            }
        }
    };

    /**
     * Перевіряє чи значення перевищує поріг і змінює колір тексту на червоний.
     *
     * @param val       поточне значення (дим, газ, радіація)
     * @param threshold поріг, після якої сигналізується небезпека
     * @param textView  елемент UI для зміни кольору
     */
    public void checkThreshold(Number val, Number threshold, TextView textView) {
        if (val.floatValue() > threshold.floatValue()) {
            textView.setTextColor(Color.RED);
        } else {
            textView.setTextColor(Color.BLACK);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_login) {
            showLoginDialog();
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Відображає діалогове вікно авторизації.
     * При збереженні оновлює логін/пароль у SharedPrefs та ініціює вхід.
     */
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Авторизація");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null);
        EditText editUsername = view.findViewById(R.id.editUsername);
        EditText editPassword = view.findViewById(R.id.editPassword);

        editUsername.setText(SharedPrefs.getUsername(this));
        editPassword.setText(SharedPrefs.getPassword(this));

        builder.setView(view);
        builder.setPositiveButton("Зберегти та Увійти", (dialog, which) -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            SharedPrefs.setUsername(this, username);
            SharedPrefs.setPassword(this, password);

            RetrofitInstance.clearToken();

            performLogin();
        });
        builder.setNegativeButton("Скасувати", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Відображає діалог для зміни налаштувань підключення.
     * Оновлює конфігурації у SharedPrefs.
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Налаштування");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        EditText editBaseUrl = view.findViewById(R.id.editBaseUrl);
        EditText editService = view.findViewById(R.id.editServiceUuid);
        EditText editChar = view.findViewById(R.id.editCharUuid);

        editBaseUrl.setText(SharedPrefs.getBaseUrl(this));
        editService.setText(SharedPrefs.getServiceUuid(this));
        editChar.setText(SharedPrefs.getCharUuid(this));

        builder.setView(view);
        builder.setPositiveButton("Зберегти", (dialog, which) -> {
            String newService = editService.getText().toString().trim();
            String newChar = editChar.getText().toString().trim();
            String newBaseUrl = editBaseUrl.getText().toString().trim();

            try {
                UUID.fromString(newService);
                UUID.fromString(newChar);

                SharedPrefs.setBaseUrl(this, newBaseUrl);
                SharedPrefs.setServiceUuid(this, newService);
                SharedPrefs.setCharUuid(this, newChar);

                RetrofitInstance.resetClient();
                sendBroadcast(new Intent("com.diploma.communicator.ACTION_RELOAD_CONFIG"));
                Toast.makeText(this, "Налаштування оновлені!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Налаштування оновлені.");
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Помилка: Невірний формат UUID!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Помилка: Невірний формат UUID!");
            }
        });
        builder.setNegativeButton("Скасувати", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filterData = new IntentFilter();
        filterData.addAction(BleForegroundService.ACTION_DATA_RECEIVED);
        filterData.addAction(ACTION_SERVER_STATUS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filterData, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(bleReceiver, filterData);
        }

        IntentFilter filterState = new IntentFilter();
        filterState.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filterState.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(stateReceiver, filterState);

        startBleScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bleReceiver);
        unregisterReceiver(stateReceiver);
    }
}