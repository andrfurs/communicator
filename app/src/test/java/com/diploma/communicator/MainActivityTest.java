package com.diploma.communicator;

import android.graphics.Color;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    private MainActivity activity;
    private TextView testTextView;

    @Before
    public void setUp() {
        activity = new MainActivity();
        testTextView = new TextView(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void checkThreshold_valBelowThreshold_colorIsBlack() {
        int value = 1000;
        int threshold = 2000;

        activity.checkThreshold(value, threshold, testTextView);

        Assert.assertEquals(Color.BLACK, testTextView.getCurrentTextColor());
    }

    @Test
    public void checkThreshold_valAboveThreshold_colorIsRed() {
        int value = 2500;
        int threshold = 2000;

        activity.checkThreshold(value, threshold, testTextView);

        Assert.assertEquals(Color.RED, testTextView.getCurrentTextColor());
    }

    @Test
    public void checkThreshold_valAboveThreshold_float_colorIsRed() {
        float radiationVal = 0.5f;
        float radiationThreshold = 0.3f;

        activity.checkThreshold(radiationVal, radiationThreshold, testTextView);

        Assert.assertEquals(Color.RED, testTextView.getCurrentTextColor());
    }
}
