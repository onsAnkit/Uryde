package uryde.passenger.navigation;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.view.ViewCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import uryde.passenger.R;
import uryde.passenger.util.PrefsHelper;


public class Navigator {
    private final boolean pickUp;
    private Activity activityContext;
    private LatLng destinationLatLng;
    private GoogleMap googleMap;
    private boolean isInReload = false;
    private List<LatLng> pointsList;
    private long reRouteInterval = TimeUnit.MILLISECONDS.toSeconds(50000);
    private int reloadCounter = 0;
    private Timer timer;
    private Marker trackingMarker;
    private long updateInterval = 100;

    private PrefsHelper mHelper;
    private Polyline routeLine;
    private PolylineOptions blackPolylineOptions, polylineOptions;

    class DrawPolyline implements NavigatorManager.DirectionListener {

        class PolyTimerTask extends TimerTask {

            class ExecuteNavigationTask implements Runnable {
                ExecuteNavigationTask() {
                }

                public void run() {
                    Navigator.this.executeNavigation();
                }
            }

            PolyTimerTask() {
            }

            public void run() {
                Navigator.this.activityContext.runOnUiThread(new ExecuteNavigationTask());
            }
        }

        DrawPolyline() {
        }

        public void onGetDirection(List<LatLng> latLngList) {
            try {
                if (latLngList != null) {
                    Navigator.this.pointsList = latLngList;
                   PolylineOptions polylineOptions = new PolylineOptions()
                           .width(Utility.convertDpToPixel(4.0f, Navigator.this.activityContext))
                            .color(ViewCompat.MEASURED_STATE_MASK);
                    for (LatLng latLng : latLngList) {
                        polylineOptions.add(latLng);
                    }
                    Navigator.this.routeLine = Navigator.this.googleMap.addPolyline(polylineOptions);
                    Navigator.this.routeLine.setColor(Color.BLUE);
                    Navigator.this.routeLine.setWidth(8.0f);

                    if (pickUp) {
                        googleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                                .position(pointsList.get(pointsList.size() - 1)).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_pickup)));
                    } else {
                        googleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                                .position(pointsList.get(pointsList.size() - 1)).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_dropoff)));
                    }


                   /* LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : pointsList) {
                        builder.include(latLng);
                    }
                    LatLngBounds bounds = builder.build();
                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                    googleMap.animateCamera(mCameraUpdate);

                    polylineOptions = new PolylineOptions();
                    polylineOptions.color(Color.GRAY);
                    polylineOptions.width(8);
                    polylineOptions.startCap(new SquareCap());
                    polylineOptions.endCap(new SquareCap());
                    polylineOptions.jointType(ROUND);
                    polylineOptions.addAll(pointsList);
                    greyPolyLine = googleMap.addPolyline(polylineOptions);

                    blackPolylineOptions = new PolylineOptions();
                    blackPolylineOptions.width(8);
                    blackPolylineOptions.color(Color.BLUE);
                    blackPolylineOptions.startCap(new SquareCap());
                    blackPolylineOptions.endCap(new SquareCap());
                    blackPolylineOptions.jointType(ROUND);
                    routeLine = googleMap.addPolyline(blackPolylineOptions);

                    if (pickUp) {
                        googleMap.addMarker(new MarkerOptions()
                                .position(pointsList.get(pointsList.size() - 1)).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_pickup)));
                    } else {
                        googleMap.addMarker(new MarkerOptions()
                                .position(pointsList.get(pointsList.size() - 1)).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_dropoff)));
                    }

                    ValueAnimator polylineAnimator = ValueAnimator.ofInt(0, 100);
                    polylineAnimator.setDuration(2000);
                    polylineAnimator.setInterpolator(new LinearInterpolator());
                    polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            List<LatLng> points = greyPolyLine.getPoints();
                            int percentValue = (int) valueAnimator.getAnimatedValue();
                            int size = points.size();
                            int newPoints = (int) (size * (percentValue / 100.0f));
                            List<LatLng> p = points.subList(0, newPoints);
                            routeLine.setPoints(p);
                        }
                    });
                    polylineAnimator.start();*/

                    Navigator.this.timer = new Timer();
                    Navigator.this.timer.schedule(new PolyTimerTask(), 500, Navigator.this.updateInterval);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class UpdatePolylinePoints implements NavigatorManager.DirectionListener {
        UpdatePolylinePoints() {
        }

        public void onGetDirection(List<LatLng> latLngList) {
            if (latLngList != null) {
                Navigator.this.resetReloadCounter();
                Navigator.this.pointsList = latLngList;
                if (Navigator.this.routeLine != null) {
                    Navigator.this.routeLine.setPoints(Navigator.this.pointsList);
//                    Navigator.this.greyPolyLine.setPoints(Navigator.this.pointsList);
                }
            }
            Navigator.this.isInReload = false;
        }
    }

    public Navigator(Activity activityContext, GoogleMap googleMap, boolean isPickUp) {
        this.pickUp = isPickUp;
        this.activityContext = activityContext;
        this.googleMap = googleMap;
        mHelper = new PrefsHelper(activityContext);
        resetReloadCounter();
    }

    public void startNavigation(Marker trackingMarker, LatLng sourceLatLng, LatLng destinationLatLng) {
        this.trackingMarker = trackingMarker;
        this.destinationLatLng = destinationLatLng;
        new NavigatorManager(sourceLatLng, destinationLatLng).getDirectionLatLngList(new DrawPolyline());
    }

    public boolean isInitialised() {
        if (this.timer == null || this.routeLine == null) {
            return false;
        }
        return true;
    }

    public void stopNavigation() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
            if (this.routeLine != null) {
                this.routeLine.remove();
                this.routeLine = null;
//                this.greyPolyLine.remove();
//                this.greyPolyLine = null;
            }
        }
    }

    private void executeNavigation() {
        if (this.pointsList.size() != 1 && !this.isInReload) {
           LatLng myLoc = getSourceLocation();
            if (myLoc != null) {
                LatLng secLatLng = this.pointsList.get(1);
                LatLng firstLatLng = this.pointsList.get(0);
                float distanceFirstSec = calculateDistance(firstLatLng, secLatLng);
                float distanceMyFirst = calculateDistance(myLoc, firstLatLng);
                float distanceMySec = calculateDistance(myLoc, secLatLng);
                if (distanceMySec <= distanceMyFirst) {
                    this.pointsList.remove(0);
                } else if (distanceMySec <= distanceFirstSec) {
                    this.pointsList.remove(0);
                } else {
                    this.reloadCounter--;
                    if (this.reloadCounter <= 0) {
                        reDrawRoute();
                    }
                }
                List<LatLng> latLngList = new ArrayList<>();
                latLngList.add(myLoc);
                latLngList.addAll(this.pointsList);
                if (this.routeLine != null) {
                    this.routeLine.setPoints(latLngList);
//                    this.greyPolyLine.setPoints(latLngList);
                }
            }
        } else if (!this.isInReload) {
            stopNavigation();
        }
    }

    private void reDrawRoute() {
        this.isInReload = true;
        new NavigatorManager(getSourceLocation(), this.destinationLatLng).getDirectionLatLngList(new UpdatePolylinePoints());
    }

    private Float calculateDistance(LatLng initialLatLng, LatLng finalLatLng) {
        float[] results = new float[1];
        Location.distanceBetween(initialLatLng.latitude, initialLatLng.longitude, finalLatLng.latitude, finalLatLng.longitude, results);
        return results[0];
    }

    private double lng = 0.0, lat = 0.0;

    private LatLng getSourceLocation() {
   /*     if (this.trackingMarker == null) {
            return null;
        }
        return this.trackingMarker.getPosition();
   */
        try {
            lat = Double.parseDouble(mHelper.getPref("driver_lat", "0.0"));
            lng = Double.parseDouble(mHelper.getPref("driver_long", "0.0"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LatLng(lat, lng);
    }

    private void resetReloadCounter() {
        this.reloadCounter = (int) (this.reRouteInterval / this.updateInterval);
    }

    public List<LatLng> getPolylineList() {
        return pointsList;
    }
}