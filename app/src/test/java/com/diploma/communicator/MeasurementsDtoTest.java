package com.diploma.communicator;

import com.diploma.communicator.http.MeasurementsDto;

import org.junit.Assert;
import org.junit.Test;

public class MeasurementsDtoTest {
    int expectedSmoke = 1500;
    int expectedGas = 1200;
    float expectedRad = 0.15f;
    String expectedTime = "2026-01-01T10:00:00";

    @Test
    public void testConstructorAndGetters() {
        MeasurementsDto dto = new MeasurementsDto(expectedSmoke, expectedGas, expectedRad, expectedTime);

        Assert.assertEquals(expectedSmoke, dto.getSmokeLevel());
        Assert.assertEquals(expectedGas, dto.getGasLevel());
        Assert.assertEquals(expectedRad, dto.getRadiationLevel(), 0.001);
        Assert.assertEquals(expectedTime, dto.getTimestamp());
    }

    @Test
    public void testSettersAndGetters() {
        MeasurementsDto dto = new MeasurementsDto(0, 0, 0f, "");
        dto.setSmokeLevel(expectedSmoke);
        dto.setGasLevel(expectedGas);
        dto.setRadiationLevel(expectedRad);
        dto.setTimestamp(expectedTime);

        Assert.assertEquals(expectedSmoke, dto.getSmokeLevel());
        Assert.assertEquals(expectedGas, dto.getGasLevel());
        Assert.assertEquals(expectedRad, dto.getRadiationLevel(), 0.001);
        Assert.assertEquals(expectedTime, dto.getTimestamp());
    }
}
