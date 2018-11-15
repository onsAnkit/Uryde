package uryde.passenger.searching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import uryde.passenger.R;
import uryde.passenger.util.Constants;


public class SearchAddressGooglePlacesActivity extends Activity {
    AutoCompleteTextView atvPlaces;
    PlacesTask placesTask;
    ParserTask parserTask;

    private ListView searchAddressListview;
    double currentLatitude, currentLongitude;
    private String HintText;
    private AppLocationService appLocationService;
    private ArrayList<String> reference_id_list = new ArrayList<String>();
    private ArrayList<String> address_list = new ArrayList<String>();
    ParserTask placeDetailsParserTask, placesParserTask;
    final int PLACES = 0;
    final int PLACES_DETAILS = 1;
    DownloadTask placeDetailsDownloadTask;
    private static int clicked_index = 0;
    private DatabasePickupHandler dbPickup;
    private DatabaseDropHandler dbDrop;
    private List<DBLocations> dbLocations;
    private List<String> resultDataList = new ArrayList<String>();
    private ImageView cancelText;
    private static final String TAG = SearchAddressGooglePlacesActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_search);

        TextView back =  findViewById(R.id.back);
        TextView title =  findViewById(R.id.title);
        cancelText =  findViewById(R.id.cancel_text);
        atvPlaces =  findViewById(R.id.atv_places);
        atvPlaces.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        searchAddressListview =  findViewById(R.id.search_address_listview);
        atvPlaces.setThreshold(0);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            HintText = extras.getString("chooser");
            title.setText(HintText);
        }

        atvPlaces.setHint(HintText);
        atvPlaces.setThreshold(1);

        if (HintText.equals(getString(R.string.pickup_location))) {
            dbPickup = new DatabasePickupHandler(this);
            dbLocations = dbPickup.getAllPickupLocations();

            for (int i = 0; i < dbLocations.size() && i < 7; i++) {
                resultDataList.add(dbLocations.get(i).getFormattedAddress());
            }
        } else if (HintText.equals(getString(R.string.drop_off))) {
            dbDrop = new DatabaseDropHandler(this);
            dbLocations = dbDrop.getAllDropLocations();

            for (int i = 0; i < dbLocations.size(); i++) {
                resultDataList.add(dbLocations.get(i).getFormattedAddress());
            }
        }

        if (resultDataList.size() > 0) {
            Log.d("dbLocations size = ", dbLocations.size() + "");
            AddressAdapterNew adapter = new AddressAdapterNew(SearchAddressGooglePlacesActivity.this, resultDataList);
            searchAddressListview.setAdapter(adapter);
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        appLocationService = new AppLocationService(SearchAddressGooglePlacesActivity.this);
        Location gpsLocation = appLocationService.getLocation(LocationManager.GPS_PROVIDER);
        if (gpsLocation != null) {
            currentLatitude = gpsLocation.getLatitude();
            currentLongitude = gpsLocation.getLongitude();
        } else {
            Location nwLocation = appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);

            if (nwLocation != null) {
                currentLatitude = nwLocation.getLatitude();
                currentLongitude = nwLocation.getLongitude();
            }
        }

        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                atvPlaces.setText("");
                cancelText.setVisibility(View.GONE);
            }
        });

        atvPlaces.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cancelText.setVisibility(View.VISIBLE);
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0) {
                    cancelText.setVisibility(View.GONE);
                }
            }
        });

        searchAddressListview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {


                clicked_index = arg2;
                placeDetailsDownloadTask = new DownloadTask(PLACES_DETAILS);

                if (reference_id_list.size() > 0 && reference_id_list.get(arg2) != null) {
                    String url = getPlaceDetailsUrl(reference_id_list.get(arg2));
                    Log.d("ParserTask url=", url);
                    placeDetailsDownloadTask.execute(url);
                    return;
                }

                if (dbLocations.size() > 0) {
                    Log.d(TAG, "ParserTask inside PLACES_DETAILS dbLocations.size()>0");
                    Log.d(TAG, "inside listResult size < 0");
                    Log.d(TAG, "inside dbLocations PICKUP index=" + clicked_index + "  " + dbLocations.get(clicked_index).getAddressName());
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("LATITUDE_SEARCH", dbLocations.get(clicked_index).getLatitude());
                    returnIntent.putExtra("LONGITUDE_SEARCH", dbLocations.get(clicked_index).getLongitude());
                    returnIntent.putExtra("SearchAddress", dbLocations.get(clicked_index).getFormattedAddress());
                    returnIntent.putExtra("ADDRESS_NAME", dbLocations.get(clicked_index).getAddressName());
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }

            }
        });
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        private int downloadType = 0;

        // Constructor
        DownloadTask(int type) {
            this.downloadType = type;
        }

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            switch (downloadType) {
                case PLACES:
                    // Creating ParserTask for parsing Google Places
                    placesParserTask = new ParserTask(PLACES);

                    // Start parsing google places json data
                    // This causes to execute doInBackground() of ParserTask class
                    placesParserTask.execute(result);

                    break;

                case PLACES_DETAILS:
                    // Creating ParserTask for parsing Google Places
                    placeDetailsParserTask = new ParserTask(PLACES_DETAILS);

                    // Starting Parsing the JSON string
                    // This causes to execute doInBackground() of ParserTask class
                    placeDetailsParserTask.execute(result);
            }
        }
    }

    private String getPlaceDetailsUrl(String ref) {

        // Obtain browser key from https://code.google.com/apis/console
        String key = "key=" + Constants.SEARCH_ADDRESS_API_KEY;

        // reference of place
        String reference = "reference=" + ref;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = reference + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/details/" + output + "?" + parameters;
        Log.i("", "getPlaceDetailsUrl = " + url);

        return url;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SearchAddressGooglePlacesActivity.this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS should be enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();

            }
        });

        // Showing Alert Message
        alertDialog.show();
    }


    private class AddressAdapterNew extends ArrayAdapter<String> {
        List<String> objects;
        Activity activity;

        public AddressAdapterNew(Activity activity, List<String> objects) {
            super(activity, R.layout.activity_main_search, objects);
            this.objects = objects;
            this.activity = activity;
            // TODO Auto-generated constructor stub
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            if (convertView == null) {
                convertView = activity.getLayoutInflater().inflate(R.layout.address_item, null);
            }

            TextView locationName = (TextView) convertView.findViewById(R.id.location_name);
            TextView addressTextview = (TextView) convertView.findViewById(R.id.address_textview);
            String[] total_addressStrings = objects.get(position).split(",");

            if (total_addressStrings.length > 0) {
                String first_name = total_addressStrings[0];

                String last_name = "";
                for (int i = 1; i < total_addressStrings.length; i++) {
                    last_name = last_name + total_addressStrings[i];
                }
                locationName.setText(first_name);
                addressTextview.setText(last_name);
            }
            return convertView;
        }
    }


    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception Durl", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    // Fetches all places from GooglePlaces AutoComplete Web Service
    private class PlacesTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            // Obtain browser key from https://code.google.com/apis/console
            String key = "key=" + Constants.SEARCH_ADDRESS_API_KEY;

            String input = "";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            // place type to be searched
            //String types = "types=geocode";

//            String types = "establishment|geocode&location=" + currentLatitude + "," + currentLongitude + "&radius=500po&language=en&components=country:ind";
            String types = "establishment|geocode&location=" + currentLatitude + "," + currentLongitude + "&radius=500po&language=en";

            // Sensor enabled
            //String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input + "&" + types + "&" + key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/" + output + "?" + parameters;

            Log.d(TAG, url);

            try {
                // Fetching the data from web service in background
                data = downloadUrl(url);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Creating ParserTask
            parserTask = new ParserTask(PLACES);

            // Starting Parsing the JSON string returned by Web Service
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {
        JSONObject jObject;

        int parserType = 0;

        public ParserTask(int type) {
            this.parserType = type;
        }

        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {
            List<HashMap<String, String>> places = null;

			/*PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
            	jObject = new JSONObject(jsonData[0]);

            	// Getting the parsed data as a List construct
            	places = placeJsonParser.parse(jObject);

            	Utility.printLog("ParserTask jObject="+jObject);
            	Utility.printLog("ParserTask places="+places);

            }catch(Exception e)
            {
            	Utility.printLog("ParserTask Exception: "+e.toString());
            }*/

            try {

                if (jsonData[0] != null) {
                    jObject = new JSONObject(jsonData[0]);
                    switch (parserType) {
                        case PLACES:
                            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
                            // Getting the parsed data as a List construct
                            places = placeJsonParser.parse(jObject);
                            break;
                        case PLACES_DETAILS:
                            PlaceDetailsJSONParser placeDetailsJsonParser = new PlaceDetailsJSONParser();
                            // Getting the parsed data as a List construct
                            places = placeDetailsJsonParser.parse(jObject);
                    }

                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "ParserTask Exception: " + e.toString());
            }

            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            switch (parserType) {
                case PLACES:

                    if (result != null) {
                        Log.i("Results", "ParserTask Result Size" + result.size());

                        reference_id_list.clear();
                        address_list.clear();

                        for (int i = 0; i < result.size(); i++) {
                            Log.i("", "ParserTask description json =" + result.get(i));
                            address_list.add(result.get(i).get("description"));
                            reference_id_list.add(result.get(i).get("reference"));
                            Log.i("", "ParserTask description name =" + result.get(i).get("description"));
                        }

                        Log.i("Results", "ParserTask address_list Size" + address_list.size());

                        AddressAdapterNew adapter = new AddressAdapterNew(SearchAddressGooglePlacesActivity.this, address_list);
                        searchAddressListview.setAdapter(adapter);
                    }

                    break;
                case PLACES_DETAILS:

                    if (result != null && result.size() > 0) {
                        double latitude = Double.parseDouble(result.get(0).get("lat"));
                        double longitude = Double.parseDouble(result.get(0).get("lng"));
                        Log.i("Results", "ParserTask latitude=" + latitude + " ,longitude=" + longitude);


                        if (HintText.equals(getString(R.string.pickup_location))) {
                            dbPickup.addPickupLocation("",
                                    address_list.get(clicked_index),
                                    "" + latitude
                                    , "" + longitude);
                        } else if (HintText.equals(getString(R.string.drop_off))) {
                            dbDrop.addDropLocation("",
                                    address_list.get(clicked_index),
                                    "" + latitude
                                    , "" + longitude);
                        }

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("SearchAddress", address_list.get(clicked_index));
                        returnIntent.putExtra("ADDRESS_NAME", "");
                        returnIntent.putExtra("LATITUDE_SEARCH", "" + latitude);
                        returnIntent.putExtra("LONGITUDE_SEARCH", "" + longitude);
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    } else if (dbLocations.size() > 0) {

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("LATITUDE_SEARCH", dbLocations.get(clicked_index).getLatitude());
                        returnIntent.putExtra("LONGITUDE_SEARCH", dbLocations.get(clicked_index).getLongitude());
                        returnIntent.putExtra("SearchAddress", dbLocations.get(clicked_index).getFormattedAddress());
                        returnIntent.putExtra("ADDRESS_NAME", dbLocations.get(clicked_index).getAddressName());
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        finish();
        overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //	getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}