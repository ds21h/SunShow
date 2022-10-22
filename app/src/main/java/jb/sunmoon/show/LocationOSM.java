package jb.sunmoon.show;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

public class LocationOSM extends Activity {
    private AppData mAppData;
    private MapView mMap = null;
    private LatLng mLocation;
    private GeoPoint mPosition;
    private final ArrayList<OverlayItem> mItems = new ArrayList<>();
    private ItemizedIconOverlay<OverlayItem> mItemizedIconOverlay;

    private final MapEventsReceiver mEventReceiver = new MapEventsReceiver() {

        @Override
        public boolean singleTapConfirmedHelper(GeoPoint pLocation) {
            return false;
        }

        @Override
        public boolean longPressHelper(GeoPoint pLocation) {
            OverlayItem lItem;

            mLocation = new LatLng(pLocation.getLatitude(), pLocation.getLongitude());
            lItem = new OverlayItem("", "", pLocation);
            mItemizedIconOverlay.removeAllItems();
            mItemizedIconOverlay.addItem(lItem);
            mMap.invalidate();
            return true;
        }
    };

    private final View.OnClickListener mSelectListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mAppData.xMapLocation(mLocation);
            mAppData.xMapZone("");
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.location_osm);

        double lZoom;
        IMapController lController;
        MapEventsOverlay lOverlay;
        Button lBtnSelect;
        OverlayItem lItem;

        mAppData = AppData.getInstance();
        if (savedInstanceState == null){
            mLocation = mAppData.xMapLocation();
            mPosition = new GeoPoint(mLocation.latitude, mLocation.longitude);
            lZoom = 9.5;
        } else {
            // Note: These getParcelable requests are deprecated in Api 33. The implementation however is buggy so it is recommended to keep on using these.
            mLocation = savedInstanceState.getParcelable("Location");
            mPosition = savedInstanceState.getParcelable("Position");
            lZoom = savedInstanceState.getDouble("Zoom");
        }

        mMap = findViewById(R.id.map);
        lBtnSelect = findViewById(R.id.btnSelect);

        mMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setMultiTouchControls(true);
        mMap.setTilesScaledToDpi(true);
        lController = mMap.getController();
        lController.setZoom(lZoom);
        lController.setCenter(mPosition);

        lBtnSelect.setOnClickListener(mSelectListener);
        lOverlay = new MapEventsOverlay(mEventReceiver);
        mMap.getOverlays().add(0,lOverlay);

//        mMap.addMapListener(new DelayedMapListener(mMapListener, 1000));
        mItemizedIconOverlay = new ItemizedIconOverlay<>(mItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, getApplicationContext());
        mMap.getOverlays().add(0,mItemizedIconOverlay);

        lItem = new OverlayItem("", "", mPosition);
        mItemizedIconOverlay.addItem(lItem);

        mMap.invalidate();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        mPosition = (GeoPoint) mMap.getMapCenter();
        savedInstanceState.putParcelable("Location", mLocation);
        savedInstanceState.putParcelable("Position", mPosition);
        savedInstanceState.putDouble("Zoom", mMap.getZoomLevelDouble());
    }

    @Override
    public void onResume() {
        super.onResume();
        mMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMap.onPause();
    }
}