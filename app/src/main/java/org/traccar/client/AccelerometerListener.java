package org.traccar.client;

import android.Manifest;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import static org.traccar.client.PositionProvider.TAG;

/**
 * Created by ulvisgoldbergs on 05/04/2017.
 */

public class AccelerometerListener implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private float xAccelCur;
    private float yAccelCur;
    private float zAccelCur;
    private float xAccelLast;
    private float yAccelLast;
    private float zAccelLast;

    private float xRotCur;
    private float yRotCur;
    private float zRotCur;
    private float xRotLast;
    private float yRotLast;
    private float zRotLast;

    public boolean status;
    boolean firstAccDelta;
    boolean firstGyroDelta;

    private SharedPreferences preferences;
    private String alarmPhone;
    private int accSensitivity;
    private int gyroSensitivity;

    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private CallBr br_call;
    private SmsBroadcastReceiver br_sms;
    Context service;

    public AccelerometerListener(Context service) {
        firstAccDelta = true;
        firstGyroDelta = true;
        this.service = service;
        preferences = PreferenceManager.getDefaultSharedPreferences(service);

        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI, new Handler());

        mGyroscope = mSensorManager
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mGyroscope,
                SensorManager.SENSOR_DELAY_UI, new Handler());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
//            Log.w(TAG, "x" + x + "|y" + y + "|z" + z);
            xAccelLast = xAccelCur;
            yAccelLast = yAccelCur;
            zAccelLast = zAccelCur;
            xAccelCur = x;
            yAccelCur = y;
            zAccelCur = z;
            float xdelta = Math.abs(xAccelCur - xAccelLast);
            float ydelta = Math.abs(yAccelCur - yAccelLast);
            float zdelta = Math.abs(zAccelCur - zAccelLast);
            accSensitivity = Integer.parseInt(preferences.getString(MainActivity.KEY_ACC_SENSITIVITY, "1"));
            if ((xdelta > (0.1 * accSensitivity) || ydelta > (0.1 * accSensitivity) || zdelta > (0.1 * accSensitivity)) && !firstAccDelta) {
                Log.w(TAG, "AccSens:" + preferences.getString(MainActivity.KEY_ACC_SENSITIVITY, "1"));
                Log.w(TAG, "AccSens:xg" + xdelta + "|yg" + ydelta + "|zg" + zdelta);
                startCall();
            }
            firstAccDelta = false;
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
//            Log.w(TAG, "xg" + x + "|yg" + y + "|zg" + z);
            xRotLast = xRotCur;
            yRotLast = yRotCur;
            zRotLast = zRotCur;
            xRotCur = x;
            yRotCur = y;
            zRotCur = z;
            float xdelta = Math.abs(xRotCur - xRotLast);
            float ydelta = Math.abs(yRotCur - yRotLast);
            float zdelta = Math.abs(zRotCur - zRotLast);
            gyroSensitivity = Integer.parseInt(preferences.getString(MainActivity.KEY_GYRO_SENSITIVITY, "1"));
            if ((xdelta > (0.001 * gyroSensitivity) || ydelta > (0.001 * gyroSensitivity) || zdelta > (0.001 * gyroSensitivity)) && !firstGyroDelta) {
                Log.w(TAG,"GyroSens" + preferences.getString(MainActivity.KEY_GYRO_SENSITIVITY, "1"));
                Log.w(TAG, "GyroSens:xg" + xdelta + "|yg" + ydelta + "|zg" + zdelta);
                startCall();
            }
            firstGyroDelta = false;
        }
    }

    private void startCall() {
        if (br_call.idle && (br_sms.armed || preferences.getBoolean(MainActivity.KEY_ALARM_STATUS, false))) {
            alarmPhone = preferences.getString(MainActivity.KEY_ALARM_PHONE, null);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            callIntent.setData(Uri.parse("tel:" + alarmPhone));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (service.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    return;
                }
            }
            service.getApplicationContext().startActivity(callIntent);;
        }
    }

    public void registerReceivers(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_IN);
        this.br_call = new CallBr();
        this.br_sms = new SmsBroadcastReceiver();
        service.registerReceiver(this.br_call, filter);
        final IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_SMS_RECEIVED);
        service.registerReceiver(this.br_sms, filter2);

    }
    public void unregisterReceivers() {
        status = false;
        service.unregisterReceiver(this.br_call);
        service.unregisterReceiver(this.br_sms);

    }

    public class CallBr extends BroadcastReceiver
    {
        Bundle bundle;
        String state;
        public boolean idle = true;


        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ACTION_IN))
            {
                if ((bundle = intent.getExtras()) != null)
                {
                    state = bundle.getString(TelephonyManager.EXTRA_STATE);
                    Log.w(TAG, state);
                    if (state.equals(TelephonyManager.EXTRA_STATE_IDLE))
                    {
                        idle = true;
                    }
                    else {
                        idle = false;
                    }
                    }
                }
            }
        }

}

