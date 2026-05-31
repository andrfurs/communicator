package com.diploma.communicator;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SharedPrefsTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        context.getSharedPreferences("DiplomaPrefs", Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Test
    public void getBaseUrl_returnsDefaultValue_whenEmpty() {
        String url = SharedPrefs.getBaseUrl(context);
        Assert.assertEquals("http://192.168.1.102:8080/", url);
    }

    @Test
    public void setBaseUrl_savesValueSuccessfully() {
        String newUrl = "http://localhost:3000/";

        SharedPrefs.setBaseUrl(context, newUrl);

        Assert.assertEquals(newUrl, SharedPrefs.getBaseUrl(context));
    }

    @Test
    public void getUsername_returnsDefaultValue_whenEmpty() {
        String username = SharedPrefs.getUsername(context);
        Assert.assertEquals("", username);
    }

    @Test
    public void setServiceUuid_savesValueSuccessfully() {
        String testUuid = "12345678-1234-1234-1234-123456789abc";
        SharedPrefs.setServiceUuid(context, testUuid);

        Assert.assertEquals(testUuid, SharedPrefs.getServiceUuid(context));
    }
}
