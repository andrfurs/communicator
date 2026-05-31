package com.diploma.communicator;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.diploma.communicator.database.AppDatabase;
import com.diploma.communicator.database.MeasurementDao;
import com.diploma.communicator.database.OfflineMeasurement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MeasurementDaoTest {
    private AppDatabase db;
    private MeasurementDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.measurementDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndGetOldestMeasurement() {
        OfflineMeasurement m1 = new OfflineMeasurement(1100, 1200, 0.1f, "2026-01-01T10:00:00");
        OfflineMeasurement m2 = new OfflineMeasurement(1300, 1400, 0.2f, "2026-02-02T10:00:00");

        dao.insert(m1);
        dao.insert(m2);
        OfflineMeasurement retrieved = dao.getOldestMeasurement();

        Assert.assertNotNull(retrieved);
        Assert.assertEquals(1100, retrieved.smoke);
        Assert.assertEquals(1200, retrieved.gas);
        Assert.assertEquals(0.1f, retrieved.radiation, 0.001);
        Assert.assertEquals("2026-01-01T10:00:00", retrieved.timestamp);
    }

    @Test
    public void deleteMeasurement() {
        OfflineMeasurement m1 = new OfflineMeasurement(1100, 1200, 0.1f, "2026-01-01T10:00:00");
        dao.insert(m1);

        OfflineMeasurement retrieved = dao.getOldestMeasurement();
        Assert.assertNotNull(retrieved);

        dao.delete(retrieved);
        OfflineMeasurement afterDelete = dao.getOldestMeasurement();

        Assert.assertNull(afterDelete);
    }
}
