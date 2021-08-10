package jb.sunmoon.show;

/**
 * Created by Jan on 15-9-2017.
 */

public class Location {
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
