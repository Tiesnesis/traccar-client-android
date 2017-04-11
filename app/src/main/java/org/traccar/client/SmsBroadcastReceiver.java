package org.traccar.client;

/**
 * Created by ulvisgoldbergs on 07/02/2017.
 */

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


import static org.traccar.client.PositionProvider.TAG;


public class SmsBroadcastReceiver extends BroadcastReceiver {

    public static final String SMS_BUNDLE = "pdus";
    private SharedPreferences preferences;
    private String alarmPhone;
    public boolean armed = false;


    @TargetApi(Build.VERSION_CODES.DONUT)
    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();
        if (intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            String smsMessageStr = "";
            for (int i = 0; i < sms.length; ++i) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i]);

                String smsBody = smsMessage.getMessageBody().toString();
                smsMessageStr += smsBody;
            }


            if (smsMessageStr.equals("arm")) {
                Log.i(TAG, "Arming..");
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
                alarmPhone = preferences.getString(MainActivity.KEY_ALARM_PHONE, null);
                Toast.makeText(context, "Arming..", Toast.LENGTH_LONG).show();
                armed = true;
                Log.i(TAG, "Armed");
                Toast.makeText(context, "Armed", Toast.LENGTH_LONG).show();
                try {
                    SmsManager smsManager = SmsManager.getDefault();
//                    smsManager.sendTextMessage(alarmPhone, null, "Armed", null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
            else if (smsMessageStr.equals("disarm")) {
                Log.i(TAG, "Disarming..");
                Toast.makeText(context, "Disarming..", Toast.LENGTH_LONG).show();
                armed = false;
                Log.i(TAG, "Disarmed");
                Toast.makeText(context, "Disarmed", Toast.LENGTH_LONG).show();
            }

        }
    }
}