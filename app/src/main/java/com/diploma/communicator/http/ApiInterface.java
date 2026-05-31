package com.diploma.communicator.http;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Інтерфейс Retrofit, що описує кінцеві точки REST API сервера.
 */
public interface ApiInterface {
    /**
     * Виконує запит на авторизацію користувача.
     *
     * @param credentials мапа, що містить логін та пароль
     * @return об'єкт Call, що у разі успіху поверне мапу з токеном авторизації
     */
    @POST("auth/login")
    Call<Map<String, String>> login(@Body Map<String, String> credentials);

    /**
     * Відправляє поточні показники датчиків на сервер.
     *
     * @param measurements об'єкт DTO з рівнями диму, газу, радіації та часом
     * @return об'єкт Call, що містить відправлені дані
     */
    @POST("server/measurements-values")
    Call<MeasurementsDto> sendMeasurements(@Body MeasurementsDto measurements);
}
