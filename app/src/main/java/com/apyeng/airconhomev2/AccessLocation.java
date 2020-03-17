package com.apyeng.airconhomev2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class AccessLocation implements LocationListener {

    //Thank:
    // https://www.viralandroid.com/2015/12/how-to-get-current-gps-location-programmatically-in-android.html

    private Context context;
    private OnLocationListener listener;
    private LocationManager manager;

    private static final String TAG = "AccessLocation";

    public AccessLocation(Context context, OnLocationListener listener) {
        this.listener = listener;
        this.context = context;
    }

    public void start(){
        //Initial
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        //Get provider
        String provider;
        if (manager != null) {
            provider = manager.getBestProvider(criteria, false);

            if (provider != null && !provider.isEmpty()) {
                //Check permission
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    //No grant
                    listener.onFailed("No grant access to locations");
                }else {
                    //Grant, so get location
                    Location location = manager.getLastKnownLocation(provider);
                    manager.requestLocationUpdates(provider, 5000, 1, this);
                    if (location!=null){
                        onLocationChanged(location);
                    }else {
                        listener.onFailed("Can't get location");
                    }

                }
            }

        }else {
            listener.onFailed("LocationManager is null.");
        }
    }

    public void stop(){
        if (manager!=null){
            manager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.w(TAG, "LocationChanged: "+location.getLatitude()+", "+location.getLongitude());
        if (listener!=null){
            listener.onChanged(location);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.w(TAG, "StatusChanged: "+s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.w(TAG, "ProviderEnabled: "+s);
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.w(TAG, "ProviderDisable: "+s);
    }

    interface OnLocationListener{
        void onChanged(Location location);
        void onFailed(String error);
    }



}
