package uryde.passenger.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uryde.passenger.R;


public class CommonMethods {

    static ProgressDialog dialog;
    private static String timeUpdate = "", aTime;

    public static void hideKeyboard(Context ctx) {
        InputMethodManager inputManager = (InputMethodManager) ctx
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View v = ((Activity) ctx).getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static boolean isEmailValid(String Email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = Email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }
    public static String getRealPathFromURI(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }
    public static ProgressDialog GetDialog(Context context, String title, String Message) {


        if (dialog != null) {
            dialog = null;
        }
        dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(Message);
        dialog.show();
        return dialog;
    }

    public static void dismissDialog() {
        dialog.dismiss();
    }

    public static void errormessageon_Edittext(String message, EditText view) {

        int ecolor = Color.parseColor("#ff0000"); // whatever color you want
        String estring = message;
        ForegroundColorSpan fgcspan = new ForegroundColorSpan(ecolor);
        SpannableStringBuilder ssbuilder = new SpannableStringBuilder(estring);
        ssbuilder.setSpan(fgcspan, 0, estring.length(), 0);
        view.setError(ssbuilder);
        view.requestFocus();
    }

    /**
     * method used to get current time
     *
     * @return date and time both
     */
    public static String getFormattedCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        String dateTime = formatter.format(date);
        return dateTime;
    }

    /**
     * method used to show alert dialog
     *
     * @param string get alert message
     */
    public static void showAlert(Context context, String title, String string) {
        // TODO Auto-generated method stub
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);
        alert.setMessage(string);
        alert.setCancelable(false);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        alert.show();
    }

    /**
     * method used to validate email
     *
     * @param email get email
     * @return true or false
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * method used to get age from date of birth
     *
     * @param _month month
     * @param _day   day
     * @param _year  year
     * @return age
     */
    public static int getAge(int _month, int _day, int _year) {

        GregorianCalendar cal = new GregorianCalendar();
        int year, month, date, age;

        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        date = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(_year, _month, _day);
        age = year - cal.get(Calendar.YEAR);
        if ((month < cal.get(Calendar.MONTH))
                || ((month == cal.get(Calendar.MONTH)) && (date < cal.get(Calendar.DAY_OF_MONTH)))) {
            --age;
        }
        if (age < 0)
            throw new IllegalArgumentException("Age < 0");
        return age;
    }

    /**
     * method used to change date format
     *
     * @param date date
     * @return change format date
     */
    public static String changeDateFormat(String date) {
        SimpleDateFormat input = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat output = new SimpleDateFormat("MM-dd-yyyy");
        try {
            Date oneWayTripDate = input.parse(date);  // parse input
            date = output.format(oneWayTripDate);    // format output
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * method used to change the font
     *
     * @param context get context
     * @return changed font
     */
    public static Typeface headerFont(Context context) {
        Typeface headerFont = Typeface.createFromAsset(context.getAssets(), "font/OpenSans-Regular.ttf");

        return headerFont;
    }

    /**
     * method used to change the font
     *
     * @param context get context
     * @return changed font
     */
    public static Typeface boldFont(Context context) {
        Typeface headerFont = Typeface.createFromAsset(context.getAssets(), "font/Lato-Bold.ttf");

        return headerFont;
    }

    /**
     * method used to change the font
     *
     * @param context get context
     * @return changed font
     */
    public static Typeface normalText(Context context) {
        Typeface headerFont = Typeface.createFromAsset(context.getAssets(), "font/Lato-Light.ttf");

        return headerFont;
    }

    /**
     * method used to remove a comma from last in string
     *
     * @param str contain comma seprated string
     * @return string
     */
    public static String method(String str) {
        if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == ',') {
            str = str.substring(0, str.length() - 1);
        }
        Log.d("String is" + " String", str);
        return str;
    }

    public static String callhttpRequest(String url) {
        System.out.println("utility url..." + url);
        url = url.replaceAll(" ", "%20");
        String resp = null;
        org.apache.http.client.methods.HttpGet httpRequest;
        try {
            httpRequest = new org.apache.http.client.methods.HttpGet(url);
            HttpParams httpParameters = new BasicHttpParams();
            int timeoutConnection = 60000;
            HttpConnectionParams.setConnectionTimeout(httpParameters,
                    timeoutConnection);
            int timeoutSocket = 60000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            org.apache.http.client.HttpClient httpClient = new org.apache.http.impl.client.DefaultHttpClient(httpParameters);
            org.apache.http.HttpResponse response = httpClient.execute(httpRequest);
            org.apache.http.HttpEntity entity = response.getEntity();
            org.apache.http.entity.BufferedHttpEntity bufHttpEntity = new org.apache.http.entity.BufferedHttpEntity(entity);
            final long contentLength = bufHttpEntity.getContentLength();
            if ((contentLength >= 0)) {
                InputStream is = bufHttpEntity.getContent();
                int tobeRead = is.available();

                ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
                int ch;

                while ((ch = is.read()) != -1) {
                    bytestream.write(ch);
                }

                resp = new String(bytestream.toByteArray());
            }
        } catch (MalformedURLException e) {
            System.out.println("Utility callhttpRequest.." + e);
            e.printStackTrace();
        } catch (org.apache.http.client.ClientProtocolException e) {
            System.out.println("Utility callhttpRequest.." + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Utility callhttpRequest.." + e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Utility Exception.." + e);
        }
        return resp;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static int getWidth(Context mContext) {
        int width = 0;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT > 12) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
        } else {
            width = display.getWidth();  // deprecated
        }
        return width;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static int getHeight(Context mContext) {
        int height = 0;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT > 12) {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
        } else {
            height = display.getHeight();  // deprecated
        }
        return height;
    }

    public static void showAlertAddActivity(final Activity context, String title, String string) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);
        alert.setMessage(string);
        alert.setCancelable(false);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.finish();

            }
        });
        alert.show();
    }

    /**
     * method used to get image host url
     *
     * @param activity contain context
     * @return url
     */
    public static String getImageHostUrl(Activity activity) {
        //////Log.i(TAG, "getImageHeightAndWidth");

        String devieytype = "";
        int screenHeight = getHeight(activity);
        int screenWidth = getWidth(activity);
        //////Log.i(TAG, "getImageHeightAndWidth  screenHeight "+screenHeight);
        //////Log.i(TAG, "getImageHeightAndWidth  screenWidth  "+screenWidth);
        //	int imagehiegth;
        //int imagewidth;
        if ((screenHeight <= 500 && screenHeight >= 480) && (screenWidth <= 340 && screenWidth >= 300)) {
            //////Log.i(TAG, "getImageHeightAndWidth mdpi");
            devieytype = Constants.CAR_IMAGE_URL + "mdpi/";

        } else if ((screenHeight <= 400 && screenHeight >= 300) && (screenWidth <= 240 && screenWidth >= 220))

        {

            //////Log.i(TAG, "getImageHeightAndWidth ldpi");
            devieytype = Constants.CAR_IMAGE_URL + "mdpi/";

        } else if ((screenHeight <= 840 && screenHeight >= 780) && (screenWidth <= 500 && screenWidth >= 440)) {

            //////Log.i(TAG, "getImageHeightAndWidth hdpi");
            devieytype = Constants.CAR_IMAGE_URL + "hdpi/";

        } else if ((screenHeight <= 1280 && screenHeight >= 840) && (screenWidth <= 720 && screenWidth >= 500)) {

            //////Log.i(TAG, "getImageHeightAndWidth xdpi");

            devieytype = Constants.CAR_IMAGE_URL + "xhdpi/";
        } else if ((screenHeight <= 1920 && screenHeight >= 1280) && (screenWidth <= 1080 && screenWidth >= 720)) {

            devieytype = Constants.CAR_IMAGE_URL + "xxhdpi/";

        } else {
            devieytype = Constants.CAR_IMAGE_URL + "xxxhdpi/";
        }

        return devieytype;
    }

    public static String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy/MM/dd ");
        String strDate = mdformat.format(calendar.getTime());

        Log.d("Current Date:- ", strDate);

        return strDate;
    }

    public static String getDateAndTime() {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        String formattedDate = df.format(c.getTime());

        Log.d("Current Date:- ", formattedDate);
        return formattedDate;
    }

    public static String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("hh:mm a", Locale.US);
        String formattedDate = df.format(c.getTime());

        Log.d("Current Date:- ", formattedDate);
        return formattedDate;
    }

    public static org.apache.http.HttpResponse doPost(String url, Map<String, String> kvPairs) throws org.apache.http.client.ClientProtocolException, IOException {
        // HttpClient httpclient = new DefaultHttpClient();

        org.apache.http.client.HttpClient httpclient = new org.apache.http.impl.client.DefaultHttpClient();

        HttpPost httppost = new HttpPost(url);

        if (kvPairs != null || kvPairs.isEmpty() == false) {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
                    kvPairs.size());
            String k, v;
            Iterator<String> itKeys = kvPairs.keySet().iterator();

            while (itKeys.hasNext()) {
                k = itKeys.next();
                v = kvPairs.get(k);
                nameValuePairs.add(new BasicNameValuePair(k, v));
            }

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        }

        org.apache.http.HttpResponse response;
        response = httpclient.execute(httppost);
        Log.i("TAG", "doPost response........." + response);
        return response;
    }

    /**
     * method used to get date from date picker
     *
     * @param textView contain text view to set text
     */
    public static void getDate(final Context context, final TextView textView, int year, int month, int calenderDate, final boolean forLater, final TextView selectedMonth, final TextView selectedYear) {
        final DatePickerDialog dpd = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Date now = new Date();
                Date normal = null;
                String normalAsString = "";
                String nowAsString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
                Log.d("Now date", nowAsString);
                try {
                    normal = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
                    normalAsString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(normal);
                    Log.d("Selected Date", normalAsString);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (forLater) {
                    if (nowAsString.equals(normalAsString)) {
                        String getMonthDate = CommonMethods.getMonthDate(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth, true);
                        Log.d("After Select :- ", getMonthDate);
                        String splitArray[] = getMonthDate.split("-");
                        String date = splitArray[0];
                        String month = splitArray[1];
                        String sYear = splitArray[2];

                        textView.setText(date);
                        selectedYear.setText(sYear);
                        selectedMonth.setText(month);
                    } else if (normal.before(now)) {
                        Toast.makeText(context, context.getString(R.string.valid_date_in_history), Toast.LENGTH_SHORT).show();
                    } else {
                        String getMonthDate = CommonMethods.getMonthDate(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth, true);
                        String splitArray[] = getMonthDate.split("-");
                        String date = splitArray[0];
                        String month = splitArray[1];
                        String sYear = splitArray[2];

                        textView.setText(date);
                        selectedMonth.setText(month);
                        selectedYear.setText(sYear);
                    }
                } else {
                    if (normal.equals(now)) {
                        textView.setText(CommonMethods.getMonthDate(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth, false));
                    } else if (normal.before(now)) {
                        Toast.makeText(context, context.getString(R.string.valid_date_in_history), Toast.LENGTH_SHORT).show();
                    } else {
                        textView.setText(CommonMethods.getMonthDate(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth, false));
                    }
                }
            }
        },
                year,
                month,
                calenderDate);

        if (forLater) {
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        } else {
            dpd.show();
        }
    }

    /**
     * method used to get time from timer
     *
     * @param textView contain text view to set text
     */
    public static void getTimer(Context context, final TextView textView, int hour, int minute, final boolean forLater, final TextView selectedAMPM) {
        final Calendar mcurrentTime = Calendar.getInstance();
        hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {

                timeUpdate = selectedHour + ":" + selectedMinute + ":00";
                updateTime(selectedHour, selectedMinute, textView, forLater, selectedAMPM);

            }
        }, hour, minute, false);//Yes 24 hour time
        mTimePicker.setTitle(context.getString(R.string.select_time));
        mTimePicker.show();
    }

    /**
     * method used to update date and time am pm
     *
     * @param hours    contain hours
     * @param mins     contain minutes
     * @param textView for set time on text view
     */
    private static void updateTime(int hours, int mins, TextView textView, boolean forLater, TextView selectedAMPM) {

        String timeSet = "";
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";

        String minutes = "";
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        // Append in a StringBuilder
        aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();
        if (forLater) {
            String time[] = aTime.split("\\s+");
            textView.setText(time[0]);
            selectedAMPM.setText(time[1]);
        } else {
            textView.setText(aTime);
        }
        Log.d("before time", " " + aTime);

    }

    /**
     * method used to convert date format
     *
     * @param oldDate contain old date
     * @return converted date
     */
    public static String getMonthDate(String oldDate, boolean forLater) {
        Date date = null;
        SimpleDateFormat write = null;
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        if (forLater) {
            write = new SimpleDateFormat("dd-MMMM-yyyy", Locale.US);
        } else {
            write = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
        }
        try {
            date = parser.parse(oldDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return write.format(date);
    }

    /**
     * method used to get distance in kilometers
     *
     * @param distance contain distance
     * @return distance in kilometer
     */
    public static double getDistanceInKilometers(double distance) {
        return distance / 1000;
    }

    /**
     * method used to get distance in meters
     *
     * @param distance contain distance
     * @return distance in meter
     */
    public static double getDistanceInMeters(double distance) {
        return distance * 1609.34;
    }

    /**
     * method used to get distance in meters
     *
     * @param distance contain distance
     * @return distance in meter
     */
    public static double metersToMiles(double distance) {
        return distance / 1609.34;
    }

    /**
     * method used to convert date format
     *
     * @param oldDate contain old date
     * @return converted date
     */
    public static String getConvertedDate(String oldDate) {
        Date date = null;
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        SimpleDateFormat write = new SimpleDateFormat("dd-MMM-yyyy",Locale.US);
        try {
            date = parser.parse(oldDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return write.format(date);
    }

    /**
     * method used to convert String value to 2 decimal format
     *
     * @param value contain string value
     * @return string with 2 decimal
     */
    public static String getDecimalValue(String value) {
        Float sFee = Float.parseFloat(value);
        DecimalFormat format = new DecimalFormat("0");
        format.setMaximumFractionDigits(0);
        value = format.format(sFee);

        return value;
    }

    public static void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();

    }


    /**
     * method used to check is service started
     *
     * @param serviceClass contain service class
     * @param activity     contain activity object
     * @return true or false
     */
    public static boolean isServiceRunning(Class<?> serviceClass, Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
