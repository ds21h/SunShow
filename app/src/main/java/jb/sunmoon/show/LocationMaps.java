package jb.sunmoon.show;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.view.View;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.MapsInitializer.Renderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationMaps extends FragmentActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback {
    private AppData mAppData;

    private GoogleMap mMap;
    private LatLng mLocation;
    private boolean mMapPresent;
    private CameraPosition mPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);
        setContentView(R.layout.locationmaps_layout);

        mAppData = AppData.getInstance();

        SupportMapFragment lFragment;

        mMapPresent = false;
        if (savedInstanceState == null){
            mLocation = mAppData.xMapLocation();
            mPosition = new CameraPosition.Builder().target(mLocation).tilt(0).zoom(10).build();
        } else {
            // Note: These getParcelable requests are deprecated in Api 33. The implementation however is buggy so it is recommended to keep on using these.
            mLocation = savedInstanceState.getParcelable("Location");
            mPosition = savedInstanceState.getParcelable("CamPosition");
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        lFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frgMap);
        if (lFragment != null) {
            lFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapsSdkInitialized(@NonNull Renderer renderer) {
    }

    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mMapPresent){
            mPosition = mMap.getCameraPosition();
        }
        savedInstanceState.putParcelable("Location", mLocation);
        savedInstanceState.putParcelable("CamPosition", mPosition);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap pMap) {
        mMap = pMap;
        mMapPresent = true;

        mMap.setOnMapClickListener(pLocation -> {
            mLocation = pLocation;
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(mLocation).title("Location"));
        });

        if (mLocation != null){
            mMap.addMarker(new MarkerOptions().position(mLocation).title("Location"));
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mPosition));
        }
    }

    public void sSelect(View pView){
        mAppData.xMapLocation(mLocation);
        mAppData.xMapZone("");
        finish();
    }
}
