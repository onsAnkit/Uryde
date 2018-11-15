package uryde.passenger.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Timer;
import java.util.TimerTask;

import uryde.passenger.LandingActivity;
import uryde.passenger.Login;
import uryde.passenger.R;
import uryde.passenger.model.GooglePlacesResultData;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

public class TimerService extends Service {

    private Timer myTimer;
    private PrefsHelper mHelper;
    private TimerTask myTimerTask;
    private static final String TAG = TimerService.class.getSimpleName();
    private CountDownTimer timer1 = null;
    public static final String COUNTDOWN_BR = Constants.BROADCAST_COUNTER;
    Intent bi = new Intent(COUNTDOWN_BR);
    private String t0 = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHelper = new PrefsHelper(TimerService.this);

    }

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        manageTimer();
        return START_STICKY;
    }*/

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        manageTimer();
    }

    private void manageTimer() {
        if (!t0.equals("")) {
            if (Integer.parseInt(t0) <= 2) {
                startTimer(Long.parseLong(mHelper.getPref(Constants.TIMER_VALUE, "0")) * 1000);
            }
        } else {
            if (myTimer == null) {
                myTimer = new Timer();
                myTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (!mHelper.getPref("isTimerRunning", false)) {
                            new CallTimeServices().execute();
                        } else {
                            Handler refresh = new Handler(Looper.getMainLooper());
                            refresh.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mHelper.getPref(Constants.TIMER_VALUE, "0").equals("")) {
                                        bi.putExtra("finish", 1);
                                        sendBroadcast(bi);
                                        stopTimerTask();
                                    } else {
                                        startTimer(Long.parseLong(mHelper.getPref(Constants.TIMER_VALUE, "0")) * 1000);
                                        stopTimerTask();
                                    }
                                }
                            });
                        }
                    }
                };
                myTimer.schedule(myTimerTask, 0, 30000);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CallTimeServices extends AsyncTask<String, Void, GooglePlacesResultData> {
        @Override
        protected GooglePlacesResultData doInBackground(String... params) {
            GooglePlacesResultData parseData = null;
            try {
                String url = null;
                if (mHelper.getPref(Constants.DRIVER_ON_THE_WAY, false) || mHelper.getPref(Constants.DRIVER_ARRIVED, false)) {
                    url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("driver_lat", "") + ","
                            + mHelper.getPref("driver_long", "") + "&destination=" + mHelper.getPref("pick_lat", "")
                            + "," + mHelper.getPref("pick_long", "") + "&key=" + Constants.MAPS_DIRECTION;

                    System.out.println("google url=" + url);
                } else if (mHelper.getPref(Constants.BEGIN_JOURNEY, false)) {
                    url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("pick_lat", "") + ","
                            + mHelper.getPref("pick_long", "") + "&destination=" + mHelper.getPref("drop_lat", "") + ","
                            + mHelper.getPref("drop_long", "") + "&key=" + Constants.MAPS_DIRECTION;
                    System.out.println("google url=" + url);
                }

                String data = null;
                try {
                    // Fetching the data from web service in background
                    if (url != null)
                        data = CommonMethods.callhttpRequest(url);
                } catch (Exception e) {
                    System.out.println("Background Task" + e.toString());
                }

                if (data != null) {
                    Gson gson = new Gson();
                    parseData = gson.fromJson(data, GooglePlacesResultData.class);
                }
                return parseData;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(GooglePlacesResultData result) {
            super.onPostExecute(result);
            try {
                if (result != null) {
                    if (result.getRoutes() != null && result.getRoutes().size() > 0) {
                        String[] ti = result.getRoutes().get(0).getLegs().get(0).getDuration().getText().split("\\s+");
                        t0 = ti[0];
                        if (Integer.parseInt(t0) <= 2) {
                            if (mHelper.getPref(Constants.TIMER_VALUE, "0").equals("")) {
                                startTimer(120000);
                            } else {
                                startTimer(Long.parseLong(mHelper.getPref(Constants.TIMER_VALUE, "0")) * 1000);
                                stopTimerTask();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void startTimer(final long time) {
        timer1 = new CountDownTimer(time, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long l) {
                long i = l / 1000;

                stopTimerTask();
                if (Integer.parseInt(mHelper.getPref("app_status", "0")) > 3) {
                    mHelper.savePref(Constants.TIMER_VALUE, String.format("%02d", i % 240));
                    Intent bi = new Intent(COUNTDOWN_BR);
                    bi.putExtra("countdown", String.format("%02d", i % 240));
                    bi.putExtra("finish", 0);
                    sendBroadcast(bi);

                    Log.v("Log_tag", "Tick of Progress" + l + "  " + i);
                } else {
                    timer1.cancel();
                    stopTimerTask();
                }
            }

            @Override
            public void onFinish() {
                if (Integer.parseInt(mHelper.getPref("app_status", "0")) > 3) {
                    sendNotification("Driver Arrived at your location.");
                    bi.putExtra("finish", 1);
                    sendBroadcast(bi);
                }
            }
        }.start();
    }

    private void sendNotification(String message) {
        Intent intent = null;
        if (mHelper.getPref("user_login", false)) {
            intent = new Intent(this, LandingActivity.class);
        } else {
            intent = new Intent(this, Login.class);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Uri defaultSoundUri = Uri.parse("android.resource://uryde.passenger/" + R.raw.sonido_ejecutivo);
        String channelId = "ChannelID$packageName";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            mChannel.enableVibration(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            mChannel.setSound(defaultSoundUri, audioAttributes);

            Notification notification = new Notification.Builder(this, channelId)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSmallIcon(getNotificationIcon())
                    .setContentIntent(pendingIntent)
                    .build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.createNotificationChannel(mChannel);
            notificationManager.notify(0 /* ID of notification */, notification);

        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(getNotificationIcon())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setColor(Color.argb(0x1, 0x33, 0x33, 0x33))
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
        if (useWhiteIcon) {
            return R.drawable.ic_transparent_icon;
        } else {
            return R.mipmap.ic_launcher;
        }
    }

    private void stopTimerTask() {
        if (myTimer != null) {
            myTimer.cancel();
            myTimer = null;
        }

        if (myTimerTask != null) {
            myTimerTask.cancel();
            myTimerTask = null;
        }

    }

    @Override
    public boolean stopService(Intent name) {
        t0 = "";
        stopTimer();
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void stopTimer() {
        Log.d(TAG, "service stopped");
        if (timer1 != null) {
            timer1.cancel();
        }
        stopTimerTask();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "on Task");
        if (!t0.equals("")) {
            if (Integer.parseInt(t0) <= 2) {
                startTimer(Long.parseLong(mHelper.getPref(Constants.TIMER_VALUE, "0")) * 1000);
            }
        } else {
            if (myTimer == null) {
                myTimer = new Timer();
                myTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (!mHelper.getPref("isTimerRunning", false)) {
                            new CallTimeServices().execute();
                        } else {
                            Handler refresh = new Handler(Looper.getMainLooper());
                            refresh.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mHelper.getPref(Constants.TIMER_VALUE, "0").equals("00")) {
                                        bi.putExtra("finish", 1);
                                        sendBroadcast(bi);
                                        stopTimerTask();
                                    } else {
                                        startTimer(Long.parseLong(mHelper.getPref(Constants.TIMER_VALUE, "0")) * 1000);
                                        stopTimerTask();
                                    }
                                }
                            });
                        }
                    }
                };
                myTimer.schedule(myTimerTask, 0, 30000);
            }
        }
    }

}
