package uryde.passenger.navigation;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Utility {
    private static int b;

    public static final boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }



    public static float convertDpToPixel(float dp, Context context) {
        return dp * (((float) context.getResources().getDisplayMetrics().densityDpi) / 160.0f);
    }

    public static String getFormatedDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            dateString = new SimpleDateFormat("EEE M,yyyy").format(dateFormat.parse(dateString));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateString;
    }

    public static void setCustomTitleFont(Context context, Toolbar toolbar) {
        try {
            Field f = toolbar.getClass().getDeclaredField("mTitleTextView");
            f.setAccessible(true);
            TextView yourTextView = (TextView) f.get(toolbar);
            yourTextView.setTextColor(-1);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        }
    }

    public static String generateImageName() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + random() + ".jpg";
    }

    public static String random() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) {
            salt.append(SALTCHARS.charAt((int) (rnd.nextFloat() * ((float) SALTCHARS.length()))));
        }
        return salt.toString();
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int size) {
        int bitmapSize = bitmap.getByteCount();
        int requiredSizeInBytes = size * 1024;
        while (bitmapSize > requiredSizeInBytes) {
            bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth() * 2) / 3, (bitmap.getHeight() * 2) / 3, false);
            bitmapSize = bitmap.getByteCount();
        }
        return bitmap;
    }

    public static String getTimeString(long time) {
        int h = (int) (time / 3600000);
        int m = ((int) (time - ((long) (h * 3600000)))) / 60000;
        int s = ((int) ((time - ((long) (h * 3600000))) - ((long) (60000 * m)))) / 1000;
        String hh = h < 10 ? "0" + h : h + "";
        return hh + ":" + (m < 10 ? "0" + m : m + "") + ":" + (s < 10 ? "0" + s : s + "");
    }

    public static List<LatLng> decodePolyLine(String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList();
        int lat = 0;
        int lng = 0;
        while (index < len) {
            int index2;
            int shift = 0;
            int result = 0;
            while (true) {
                index2 = index + 1;
                int b = poly.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lat += (result & 1) != 0 ? (result >> 1) ^ -1 : result >> 1;
            shift = 0;
            result = 0;
            index = index2;
            while (true) {
                index2 = index + 1;
                b = poly.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lng += (result & 1) != 0 ? (result >> 1) ^ -1 : result >> 1;
            decoded.add(new LatLng(((double) lat) / 100000.0d, ((double) lng) / 100000.0d));
            index = index2;
        }
        return decoded;
    }


    public static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        while (index < len) {
            int index2;
            int shift = 0;
            int result = 0;
            while (true) {
                index2 = index + 1;
                int b = encoded.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lat += (result & 1) != 0 ? (result >> 1) ^ -1 : result >> 1;
            shift = 0;
            result = 0;
            index = index2;
            while (true) {
                index2 = index + 1;
                b = encoded.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lng += (result & 1) != 0 ? (result >> 1) ^ -1 : result >> 1;
            poly.add(new LatLng(((double) lat) / 100000.0d, ((double) lng) / 100000.0d));
            index = index2;
        }
        return poly;
    }
}
