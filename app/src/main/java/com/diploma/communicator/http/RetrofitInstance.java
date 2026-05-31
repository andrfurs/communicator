package com.diploma.communicator.http;

import android.content.Context;

import com.diploma.communicator.SharedPrefs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Клас для створення та налаштування клієнта Retrofit.
 * Забезпечує додавання JWT-токена до кожного мережевого запиту.
 */
public class RetrofitInstance {
    private static String currentBaseUrl = "";
    private static Retrofit retrofit;
    private static String authToken = null;

    /**
     * Встановлює токен авторизації.
     *
     * @param token JWT-токен
     */
    public static void setToken(String token) {
        authToken = token;
    }

    /**
     * Повертає налаштований екземпляр Retrofit.
     *
     * @param context контекст застосунку
     * @return клієнт Retrofit
     */
    public static Retrofit getClient(Context context) {
        String savedUrl = SharedPrefs.getBaseUrl(context);
        if (retrofit == null || !currentBaseUrl.equals(savedUrl)) {
            currentBaseUrl = savedUrl;

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder();

                        if (authToken != null) {
                            requestBuilder.header("Authorization", "Bearer " + authToken);
                        }

                        return chain.proceed(requestBuilder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Скидає поточний клієнт Retrofit.
     */
    public static void resetClient() {
        retrofit = null;
        currentBaseUrl = "";
    }

    /**
     * Очищає збережений токен авторизації та скидає клієнт.
     */
    public static void clearToken() {
        authToken = null;
        retrofit = null;
    }
}
