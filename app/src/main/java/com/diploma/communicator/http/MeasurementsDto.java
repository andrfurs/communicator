package com.diploma.communicator.http;

/**
 * Клас передачі даних, що містить показники датчиків для відправки на сервер.
 */
public class MeasurementsDto {
    private Long id;
    private int smokeLevel;
    private int gasLevel;
    private float radiationLevel;
    private String timestamp;

    /**
     * Конструктор для створення об'єкта показників.
     *
     * @param smokeLevel     рівень диму
     * @param gasLevel       рівень газу
     * @param radiationLevel рівень радіації
     * @param timestamp      часова мітка
     */
    public MeasurementsDto(int smokeLevel, int gasLevel, float radiationLevel, String timestamp) {
        this.smokeLevel = smokeLevel;
        this.gasLevel = gasLevel;
        this.radiationLevel = radiationLevel;
        this.timestamp = timestamp;
    }

    public int getSmokeLevel() {
        return smokeLevel;
    }

    public void setSmokeLevel(int smokeLevel) {
        this.smokeLevel = smokeLevel;
    }

    public int getGasLevel() {
        return gasLevel;
    }

    public void setGasLevel(int gasLevel) {
        this.gasLevel = gasLevel;
    }

    public float getRadiationLevel() {
        return radiationLevel;
    }

    public void setRadiationLevel(float radiationLevel) {
        this.radiationLevel = radiationLevel;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }
}
