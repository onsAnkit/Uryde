/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uryde.passenger;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import java.util.List;

public class MyGcmListenerService extends GcmListenerService {

    private PrefsHelper mHelper;
    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        mHelper = new PrefsHelper(MyGcmListenerService.this);

        String message = data.getString("message");
        String action = data.getString("action");
        String appointmentId = data.getString("app_appointment_id");

        mHelper.savePref(Constants.APPOINTMENT_ID, appointmentId);
        boolean isbkrnd = isApplicationSentToBackground(MyGcmListenerService.this);

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "question_counter: " + action);

        String str = "";
        for (String key : data.keySet()) {
            str = String.format("%s %s=>%s;", str, key, data.get(key));
        }
        Log.d(TAG, str);
        if (action != null) {
            if (action.equals("8")) {
                Intent intent = new Intent(Constants.CANCEL_APPOINTMENT_BROADCAST);
                intent.putExtra("response", action);
                intent.putExtra("message", message);
                sendBroadcast(intent);
            } else if (RideInfo.visibilityStatus()) {
                Intent intent = new Intent(Constants.NEW_APPOINTMENT_BROADCAST);
                intent.putExtra("status", action);
                intent.putExtra("message", message);
                sendBroadcast(intent);
            } else if (!isbkrnd) {
                switch (action) {
                    case "2": {
                        Intent i = new Intent();
                        i.setClassName("uryde.passenger", "uryde.passenger.Invoice");
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        break;
                    }
                    default: {
                        Intent i = new Intent();
                        i.setClassName("uryde.passenger", "uryde.passenger.RideInfo");
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        break;
                    }
                }
            }
        }

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */

        sendNotification(message);

        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = null;
        if (mHelper.getPref("user_login", false)) {
            intent = new Intent(this, LandingActivity.class);
        } else {
            intent = new Intent(this, Login.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri defaultSoundUri = Uri.parse("android.resource://uryde.passenger/" + R.raw.sonido_ejecutivo);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "channelId")
                .setLargeIcon(largeIcon)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setColor(Color.argb(0x1, 0x33, 0x33, 0x33))
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.drawable.ic_transparent_icon : R.mipmap.ic_launcher;
    }

    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
