package com.diploma.communicator;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.diploma.communicator.http.RetrofitInstance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import retrofit2.Retrofit;

@RunWith(RobolectricTestRunner.class)
public class RetrofitInstanceTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        RetrofitInstance.resetClient();
        RetrofitInstance.clearToken();
    }

    @Test
    public void getClient_returnsNonNullRetrofitInstance() {
        Retrofit retrofit = RetrofitInstance.getClient(context);

        Assert.assertNotNull(retrofit);
        Assert.assertEquals("http://192.168.1.102:8080/", retrofit.baseUrl().toString());
    }

    @Test
    public void resetClient_createsNewInstanceOnNextCall() {
        Retrofit firstInstance = RetrofitInstance.getClient(context);
        RetrofitInstance.resetClient();
        Retrofit secondInstance = RetrofitInstance.getClient(context);

        Assert.assertNotSame(firstInstance, secondInstance);
    }
}
