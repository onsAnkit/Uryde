package uryde.passenger.notificationService

import android.app.*
import android.util.Log

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import org.json.JSONObject
import uryde.passenger.LandingActivity
import uryde.passenger.Login
import uryde.passenger.R
import uryde.passenger.RideInfo
import uryde.passenger.util.Constants
import uryde.passenger.util.PrefsHelper
import android.media.AudioAttributes


class FireBaseNotificationService : FirebaseMessagingService() {

    var mHelper: PrefsHelper? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: " + remoteMessage!!.from)
        mHelper = PrefsHelper(this@FireBaseNotificationService)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val mObject = JSONObject(remoteMessage.data)
            val message = mObject.getString("message")
            if (mObject.has("action")) {
                val action = mObject.getString("action")
                val app_appointment_id = mObject.getString("app_appointment_id")
                mHelper?.savePref(Constants.APPOINTMENT_ID, app_appointment_id)

                val isbkrnd = isApplicationSentToBackground(this@FireBaseNotificationService)
                if (action != null) {
                    when {
                        action == "8" -> {
                            val intent = Intent(Constants.CANCEL_APPOINTMENT_BROADCAST)
                            intent.putExtra("response", action)
                            intent.putExtra("message", message)
                            sendBroadcast(intent)
                        }
                        RideInfo.visibilityStatus() -> {
                            val intent = Intent(Constants.NEW_APPOINTMENT_BROADCAST)
                            intent.putExtra("status", action)
                            intent.putExtra("message", message)
                            sendBroadcast(intent)
                        }
                        !isbkrnd -> when (action) {
                            "2" -> {
                                val i = Intent()
                                i.setClassName("uryde.passenger", "uryde.passenger.Invoice")
                                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(i)
                            }
                            else -> {
                                val i = Intent()
                                i.setClassName("uryde.passenger", "uryde.passenger.RideInfo")
                                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(i)
                            }
                        }
                    }
                }
                sendNotification(message)
            }
            sendNotification(message)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
            val mObject = JSONObject(remoteMessage.notification?.body)
            val message = mObject.getString("message")
            sendNotification(message)
        }

    }

    private fun sendCancelBroadcast(action: String?, message: String?) {
        val intent = Intent(Constants.CANCEL_APPOINTMENT_BROADCAST)
        intent.putExtra("response", action)
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    /**
     * Create and show a simple notification containing the received GCM message.

     * @param message GCM message received.
     */
    private fun sendNotification(message: String) {
            var intent: Intent? = null
            intent = if (mHelper?.getPref("user_login", false) == true) {
                Intent(this, LandingActivity::class.java)
            } else {
                Intent(this, Login::class.java)
            }
            val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT)

            val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val defaultSoundUri = Uri.parse("android.resource://uryde.passenger/" + R.raw.sonido_ejecutivo)
            val channelId = "ChannelID$packageName"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mChannel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
                mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            mChannel.enableVibration(true)

            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()

            mChannel.setSound(defaultSoundUri, audioAttributes)

            val notification = Notification.Builder(this, channelId)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSmallIcon(getNotificationIcon())
                    .setContentIntent(pendingIntent)
                    .build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(mChannel)
            notificationManager.notify(0 /* ID of notification */, notification)

        } else {
            val notificationBuilder = NotificationCompat.Builder(this)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(getNotificationIcon())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setColor(Color.argb(0x1, 0x33, 0x33, 0x33))
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        }
    }

    private fun getNotificationIcon(): Int {
        val useWhiteIcon = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
        return if (useWhiteIcon) R.drawable.ic_transparent_icon else R.mipmap.ic_launcher
    }

    companion object {

        private val TAG = FireBaseNotificationService::class.java.simpleName
    }

    private fun isApplicationSentToBackground(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.getRunningTasks(1)
        if (!tasks.isEmpty()) {
            val topActivity = tasks[0].topActivity
            if (topActivity.packageName != context.packageName) {
                return true
            }
        }
        return false
    }
}
