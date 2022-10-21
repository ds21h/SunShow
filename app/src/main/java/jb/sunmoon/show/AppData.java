package jb.sunmoon.show;

import com.google.android.gms.maps.model.LatLng;

import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

/**
 * Created by Jan on 26-8-2017.
 */

class AppData {
    private static AppData mInstance = null;

    static final int ModusCurrent = 0;
    static final int ModusMap = 1;
    static final int ModusStorage = 2;

    static final int DisplaySun = 0;
    static final int DisplayMoon = 1;

    static final int LocationInit = 0;
    static final int LocationLast = 1;
    static final int LocationFix = 2;

    private int mModus;
    private int mDisplay;
    private LatLng mCurrentLocation;
    private ZoneId mCurrentZone;
    private int mLocationStatus;
    private LatLng mMapLocation;
    private String mMapZone;
    private String mSelection;

    static AppData getInstance(){
        /*
         * use double-check locking for thread-safe initialization.
         * see https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/
         */
        if (mInstance == null) {
            synchronized(AppData.class){
                if (mInstance == null){
                    mInstance = new AppData();
                }
            }
        }
        return mInstance;
    }

    AppData(){
        xInitData();
    }

    void xInitData(){
        mModus = ModusCurrent;
        mCurrentLocation = new LatLng(0.0, 0.0);
        mCurrentZone = ZonedDateTime.now().getZone();
        mLocationStatus = LocationInit;
        mMapLocation = null;
        mMapZone = "";
        mDisplay = DisplaySun;
        mSelection = "";
    }

    int xModus(){
        return mModus;
    }

    void xModus(int pModus){
        mModus = pModus;
    }

    LatLng xCurrentLocation(){
        return mCurrentLocation;
    }

    void xCurrentLocation(LatLng pLocation){
        mCurrentLocation = pLocation;
    }

    ZoneId xCurrentZone(){
        return mCurrentZone;
    }

    int xLocationStatus(){
        return mLocationStatus;
    }

    void xLocationStatus(int pStatus){
        mLocationStatus = pStatus;
    }

    LatLng xMapLocation(){
        if (mMapLocation == null){
            return mCurrentLocation;
        } else {
            return mMapLocation;
        }
    }

    void xMapLocation(LatLng pLocation){
        mMapLocation = pLocation;
    }

    String xMapZone(){
        return mMapZone;
    }

    void xMapZone(String pZone){
        mMapZone = pZone;
    }

    int xDisplay(){
        return mDisplay;
    }

    void xDisplay(int pDisplay){
        mDisplay = pDisplay;
    }

    String xSelection(){
        return mSelection;
    }

    void xSelection(String pSelection){
        mSelection = pSelection;
    }
}
