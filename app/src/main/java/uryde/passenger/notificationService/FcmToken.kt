package uryde.passenger.notificationService

import android.content.Context
import android.util.Log

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import uryde.passenger.util.Constants

class FcmToken : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val mHelper = this@FcmToken.getSharedPreferences(Constants.DEVICE_TOKEN, Context.MODE_PRIVATE)
        val editor = mHelper.edit()
        val refreshToken = FirebaseInstanceId.getInstance().token
        Log.d("Token", refreshToken)
        editor.putString(Constants.DEVICE_TOKEN, refreshToken)
        editor.apply()

    }
}
