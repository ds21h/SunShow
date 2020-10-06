package jb.sunmoon.show;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jan on 14-5-2017.
 */

class Data extends SQLiteOpenHelper {
    private static Data mInstance = null;

    private static final String cDBName = "SunShow.db";
    private static final int cDBVersion = 1;

    private static final String cTableLocation = "Location";
    private static final String cClmName = "Name";
    private static final String cClmLongitude = "Longitude";
    private static final String cClmLattitude = "Lattitude";
    private static final String cClmZone = "Zone";

    static Data getInstance(Context pContext) {
        /*
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information:
         * http://developer.android.com/resources/articles/avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new Data(pContext.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private Data(Context pContext) {
        super(pContext, pContext.getExternalFilesDir(null).getAbsolutePath() + "/" + cDBName, null, cDBVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase pDB) {
        sDefLocation(pDB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase pDB, int pOldVersion, int pNewVersion) {
        pDB.execSQL("DROP TABLE IF EXISTS " + cTableLocation);
        onCreate(pDB);
    }

    private void sDefLocation(SQLiteDatabase pDB){
        pDB.execSQL(
                "CREATE TABLE " + cTableLocation +
                        " (_ID Integer primary key, " +
                        cClmName + " Text Not Null Unique, " +
                        cClmLongitude + " Double Not Null, " +
                        cClmLattitude + " Double Not Null, " +
                        cClmZone + " Text Not Null)"
        );
    }

    List<Location> xLocations(){
        List<Location> lLocations;
        Location lLocation;
        SQLiteDatabase lDB;
        Cursor lCursor;
        String[] lColumns;
        String lSequence;
        String lName;
        double lLongitude;
        double lLattitude;
        String lZone;

        lColumns = new String[] {cClmName, cClmLongitude, cClmLattitude, cClmZone};
        lSequence = cClmName;

        lLocations = new ArrayList<>();

        lDB = this.getReadableDatabase();

        lCursor = lDB.query(cTableLocation, lColumns, null, null, null, null, lSequence);
        while (lCursor.moveToNext()){
            lName = lCursor.getString(0);
            lLongitude = lCursor.getDouble(1);
            lLattitude = lCursor.getDouble(2);
            lZone = lCursor.getString(3);
            lLocation = new Location(lName, lLongitude, lLattitude, lZone);
            lLocations.add(lLocation);
        }
        lCursor.close();
        lDB.close();

        return lLocations;
    }

    Location xLocation(String pName){
        Location lLocation = null;
        SQLiteDatabase lDB;
        Cursor lCursor;
        String[] lColumns;
        String lSelection;
        String[] lSelectionArgs;
        String lName;
        double lLongitude;
        double lLattitude;
        String lZone;

        lColumns = new String[] {cClmName, cClmLongitude, cClmLattitude, cClmZone};
        lSelection = cClmName + " = ?";
        lSelectionArgs = new String[] {pName};

        lDB = this.getReadableDatabase();

        lCursor = lDB.query(cTableLocation, lColumns, lSelection, lSelectionArgs, null, null, null);
        if (lCursor.moveToNext()){
            lName = lCursor.getString(0);
            lLongitude = lCursor.getDouble(1);
            lLattitude = lCursor.getDouble(2);
            lZone = lCursor.getString(3);
            lLocation = new Location(lName, lLongitude, lLattitude, lZone);
        }
        lCursor.close();
        lDB.close();

        return lLocation;
    }

    int xModifyLocation(Location pLocation){
        SQLiteDatabase lDB;
        ContentValues lValues;
        String lSelection;
        String[] lSelectionArgs;
        int lNumber;
        int lResult;

        lValues = new ContentValues();
        lValues.put(cClmLongitude, pLocation.xLongitude());
        lValues.put(cClmLattitude, pLocation.xLattitude());
        lValues.put(cClmZone, pLocation.xZone());
        lSelection = cClmName + " = ?";
        lSelectionArgs = new String[] {pLocation.xName()};

        lDB = this.getWritableDatabase();

        lNumber = lDB.update(cTableLocation, lValues, lSelection, lSelectionArgs);
        if (lNumber == 1){
            lResult = Result.cResultOK;
        } else {
            lResult = Result.cResultError;
        }

        lDB.close();

        return  lResult;
    }

    int xNewLocation(Location pLocation){
        SQLiteDatabase lDB;
        ContentValues lValues;
        long lRow;
        int lResult;

        lDB = this.getWritableDatabase();

        lValues = new ContentValues();
        lValues.put(cClmName, pLocation.xName());
        lValues.put(cClmLongitude, pLocation.xLongitude());
        lValues.put(cClmLattitude, pLocation.xLattitude());
        lValues.put(cClmZone, pLocation.xZone());
        lRow = lDB.insert(cTableLocation, null, lValues);
        if (lRow < 0){
            lResult = Result.cResultError;
        } else {
            lResult = Result.cResultOK;
        }
        lDB.close();

        return lResult;
    }
}
