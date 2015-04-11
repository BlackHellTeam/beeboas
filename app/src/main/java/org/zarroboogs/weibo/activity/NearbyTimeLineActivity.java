
package org.zarroboogs.weibo.activity;

//import com.google.android.gms.maps.CameraUpdate;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;

import org.zarroboogs.util.net.WeiboException;
import org.zarroboogs.weibo.BeeboApplication;
import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.asynctask.MyAsyncTask;
import org.zarroboogs.weibo.bean.GeoBean;
import org.zarroboogs.weibo.bean.MessageBean;
import org.zarroboogs.weibo.bean.data.NearbyStatusListBean;
import org.zarroboogs.weibo.dao.NearbyTimeLineDao;
import org.zarroboogs.weibo.support.utils.Utility;

import com.umeng.analytics.MobclickAgent;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * User: qii Date: 13-3-8
 */
public class NearbyTimeLineActivity extends AbstractAppActivity {

    // private GoogleMap mMap;

    private double lat;

    private double lon;

    private String locationStr;

    // private Marker melbourne;

    // private Map<Marker, MessageBean> bindEvent = new HashMap<Marker,
    // MessageBean>();

    private GetGoogleLocationInfo locationTask;

    private FetchWeiboMsg fetchWeiboMsg;

    private MenuItem refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        // getActionBar().setDisplayShowHomeEnabled(false);
        // getActionBar().setDisplayShowTitleEnabled(true);
        // getActionBar().setDisplayHomeAsUpEnabled(false);
        // getActionBar().setTitle(getString(R.string.nearby));
        addLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(this.getClass().getName());
        MobclickAgent.onResume(this);

        if (TextUtils.isEmpty(locationStr)) {
            if (Utility.isTaskStopped(locationTask)) {
                GeoBean geoBean = new GeoBean();
                geoBean.setLatitude(lat);
                geoBean.setLongitude(lon);
                locationTask = new GetGoogleLocationInfo(geoBean);
                locationTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(this.getClass().getName());
        MobclickAgent.onPause(this);

        Utility.cancelTasks(locationTask);
    }

    private void setUpMapIfNeeded() {
        // if (mMap == null) {
        // mMap = ((SupportMapFragment)
        // getSupportFragmentManager().findFragmentById(R.id.map))
        // .getMap();
        // if (mMap != null) {
        // mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //
        // mMap.setOnInfoWindowClickListener(new
        // GoogleMap.OnInfoWindowClickListener() {
        // public void onInfoWindowClick(Marker marker) {
        // MessageBean msg = bindEvent.get(marker);
        // if (msg != null) {
        // startActivityForResult(BrowserWeiboMsgActivity.newIntent(msg,
        // GlobalContext.getInstance().getSpecialToken()), 0);
        // }
        // }
        // });
        //
        // final LatLng MELBOURNE = new LatLng(lat, lon);
        // melbourne = mMap.addMarker(new MarkerOptions()
        // .position(MELBOURNE)
        // .title(GlobalContext.getInstance().getCurrentAccountName())
        // .snippet(String.format("[%f,%f]", lat, lon)
        // ));
        // melbourne.showInfoWindow();
        // LatLng latLng = new LatLng(lat, lon);
        // CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        // mMap.moveCamera(update);
        //
        // }
        // }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu_nearbytimelineactivity, menu);
        refresh = menu.findItem(R.id.refresh);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.refresh) {
		}
        return super.onOptionsItemSelected(item);
    }

    private class GetGoogleLocationInfo extends MyAsyncTask<Void, String, String> {

        GeoBean geoBean;

        public GetGoogleLocationInfo(GeoBean geoBean) {
            this.geoBean = geoBean;

        }

        @Override
        protected String doInBackground(Void... params) {

            Geocoder geocoder = new Geocoder(NearbyTimeLineActivity.this, Locale.getDefault());

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(geoBean.getLat(), geoBean.getLon(), 1);
            } catch (IOException e) {
                cancel(true);
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);

                StringBuilder builder = new StringBuilder();
                int size = address.getMaxAddressLineIndex();
                for (int i = 0; i < size; i++) {
                    builder.append(address.getAddressLine(i));
                }
                return builder.toString();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            // if (!TextUtils.isEmpty(s) && melbourne != null) {
            // melbourne.showInfoWindow();
            // getActionBar().setSubtitle(s);
            // }
            super.onPostExecute(s);
        }
    }

    private void addLocation() {
        LocationManager locationManager = (LocationManager) NearbyTimeLineActivity.this
                .getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(NearbyTimeLineActivity.this, getString(R.string.please_open_gps), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(NearbyTimeLineActivity.this, getString(R.string.gps_is_searching), Toast.LENGTH_SHORT).show();

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }

    private void updateWithNewLocation(Location result) {
        GeoBean geoBean = new GeoBean();
        lat = result.getLatitude();
        lon = result.getLongitude();
        setUpMapIfNeeded();
        geoBean.setLatitude(lat);
        geoBean.setLongitude(lon);
        if (Utility.isTaskStopped(locationTask)) {
            locationTask = new GetGoogleLocationInfo(geoBean);
            locationTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (Utility.isTaskStopped(fetchWeiboMsg)) {
            fetchWeiboMsg = new FetchWeiboMsg();
            fetchWeiboMsg.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }

        ((LocationManager) NearbyTimeLineActivity.this.getSystemService(Context.LOCATION_SERVICE))
                .removeUpdates(locationListener);

    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);

        }

        public void onProviderDisabled(String provider) {

        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private class FetchWeiboMsg extends MyAsyncTask<Void, Void, NearbyStatusListBean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ProgressBar pb = (ProgressBar) inflater.inflate(R.layout.editmyprofileactivity_refresh_actionbar_view_layout,
                    null);
            refresh.setActionView(pb);
        }

        @Override
        protected NearbyStatusListBean doInBackground(Void... params) {

            try {
                return new NearbyTimeLineDao(BeeboApplication.getInstance().getAccessToken(), lat, lon).get();
            } catch (WeiboException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(NearbyStatusListBean nearbyStatusListBean) {
            super.onPostExecute(nearbyStatusListBean);
            if (refresh.getActionView() != null) {
                refresh.getActionView().clearAnimation();
                refresh.setActionView(null);
            }

            if (nearbyStatusListBean == null) {
                return;
            }
            List<MessageBean> messageBeanList = nearbyStatusListBean.getItemList();
            for (MessageBean msg : messageBeanList) {
                GeoBean g = msg.getGeo();
                if (g == null) {
                    continue;
                }
                // final LatLng MELBOURNE = new LatLng(g.getLat(), g.getLon());
                // Marker melbourne = mMap.addMarker(new MarkerOptions()
                // .position(MELBOURNE)
                // .title(msg.getUser().getScreen_name())
                // .snippet(msg.getText())
                // );
                // melbourne.showInfoWindow();
                // bindEvent.put(melbourne, msg);
            }
        }
    }
}
