package com.kidsplay.app.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class LightSensorManager implements SensorEventListener {

    public interface OnLightChangedListener {
        void onLightChanged(boolean isDark);
    }

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final OnLightChangedListener listener;

    public LightSensorManager(SensorManager sensorManager, OnLightChangedListener listener) {
        this.sensorManager = sensorManager;
        this.lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        this.listener = listener;
    }

    public void start() {
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        float lux = event.values[0];
        if (listener != null) {
            listener.onLightChanged(lux < 10f);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
