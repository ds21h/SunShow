package jb.sunmoon.show;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jan on 15-9-2017.
 */

public class Location {
    private static final String cVersion = "Version";
    private static final String cName = "Name";
    private static final String cLongitude = "Long";
    private static final String cLattitude = "Latt";
    private static final String cZone = "Zone";

    private static final int cCurrentVersion = 1;

    private final String mName;
    private double mLongitude;
    private double mLattitude;
    private String mZone;

    public Location(String pName, double pLongitude, double pLattitude, String pZone){
        mName = pName;
        mLongitude = pLongitude;
        mLattitude = pLattitude;
        mZone = pZone;
    }

    JSONObject xToJSON(){
        JSONObject lResult;

        lResult = new JSONObject();
        try{
            lResult.put(cVersion, cCurrentVersion);
            lResult.put(cName, mName);
            lResult.put(cLongitude, mLongitude);
            lResult.put(cLattitude, mLattitude);
            lResult.put(cZone, mZone);
        } catch (JSONException e) {
            lResult = new JSONObject();
        }
        return lResult;
    }

    Location(JSONObject pValue){
        int lVersion;

        lVersion = pValue.optInt(cVersion, 1);
        mName = pValue.optString(cName, "");
        mLongitude = pValue.optDouble(cLongitude, 0.0);
        mLattitude = pValue.optDouble(cLattitude, 0.0);
        mZone = pValue.optString(cZone, "");
    }

    public String xName(){
        return mName;
    }

    public double xLongitude(){
        return mLongitude;
    }

    public void xLongitude(double pLongitude){
        mLongitude = pLongitude;
    }

    public double xLattitude(){
        return mLattitude;
    }

    public void xLattitude(double pLattitude){
        mLattitude = pLattitude;
    }

    public String xZone(){
        return mZone;
    }

    public void xZone(String pZone){
        mZone = pZone;
    }
}
