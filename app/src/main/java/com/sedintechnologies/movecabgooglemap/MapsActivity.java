package com.sedintechnologies.movecabgooglemap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

  private GoogleMap mMap;
  private Marker marker;
  ArrayList<LatLng> latLngs = new ArrayList<>();
  private int currentPt;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    SupportMapFragment mapFragment =
        (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  @Override public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    LatLng sydney = new LatLng(13.038268, 80.213581);
    marker = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.5f));
    setValues();
  }

  private void setValues() {
    latLngs.add(new LatLng(13.038268, 80.213581));
    latLngs.add(new LatLng(13.037818, 80.213254));
    latLngs.add(new LatLng(13.037515, 80.213023));
    latLngs.add(new LatLng(13.037279, 80.212515));

    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override public void run() {
        if (currentPt < latLngs.size()) {
          Location targetLocation = new Location(LocationManager.GPS_PROVIDER);
          targetLocation.setLatitude(latLngs.get(currentPt).latitude);
          targetLocation.setLongitude(latLngs.get(currentPt).longitude);
          animateMarkerNew(targetLocation, marker);
          handler.postDelayed(this, 3000);
          currentPt++;
        } else {
          handler.removeCallbacks(this);
        }
      }
    }, 3000);
  }

  private void animateMarkerNew(final Location destination, final Marker marker) {

    if (marker != null) {

      final LatLng startPosition = marker.getPosition();
      final LatLng endPosition = new LatLng(destination.getLatitude(), destination.getLongitude());

      final float startRotation = marker.getRotation();
      final LatLngInterpolatorNew latLngInterpolator = new LatLngInterpolatorNew.LinearFixed();

      ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
      valueAnimator.setDuration(3000); // duration 3 second
      valueAnimator.setInterpolator(new LinearInterpolator());
      valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {
          try {
            float v = animation.getAnimatedFraction();
            LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition);
            marker.setPosition(newPosition);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder().target(newPosition).zoom(15.5f).build()));

            marker.setRotation(getBearing(startPosition,
                new LatLng(destination.getLatitude(), destination.getLongitude())));
          } catch (Exception ex) {
            //I don't care atm..
          }
        }
      });
      valueAnimator.addListener(new AnimatorListenerAdapter() {
        @Override public void onAnimationEnd(Animator animation) {
          super.onAnimationEnd(animation);
        }
      });
      valueAnimator.start();
    }
  }

  private interface LatLngInterpolatorNew {
    LatLng interpolate(float fraction, LatLng a, LatLng b);

    class LinearFixed implements LatLngInterpolatorNew {
      @Override public LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lngDelta = b.longitude - a.longitude;
        // Take the shortest path across the 180th meridian.
        if (Math.abs(lngDelta) > 180) {
          lngDelta -= Math.signum(lngDelta) * 360;
        }
        double lng = lngDelta * fraction + a.longitude;
        return new LatLng(lat, lng);
      }
    }
  }

  //https://stackoverflow.com/questions/35554796/rotate-marker-and-move-animation-on-map-like-uber-android
  //Method for finding bearing between two points
  private float getBearing(LatLng begin, LatLng end) {
    double lat = Math.abs(begin.latitude - end.latitude);
    double lng = Math.abs(begin.longitude - end.longitude);

    if (begin.latitude < end.latitude && begin.longitude < end.longitude) {
      return (float) (Math.toDegrees(Math.atan(lng / lat)));
    } else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) {
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
    } else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) {
      return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
    } else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) {
      return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
    }
    return -1;
  }
}
