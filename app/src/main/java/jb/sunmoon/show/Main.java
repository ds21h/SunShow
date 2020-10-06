package jb.sunmoon.show;
//*
//*  Uses the JSR-310 backport for Android (java.time.* package in Java 8)
//*
//*  See https://github.com/JakeWharton/ThreeTenABP
//*

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.json.JSONObject;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main extends Activity implements YesNoDialog.YesNoDialogListener {
    private final Context mContext = this;
    private static final String cDateFormat = "EEEE dd - MM - yyyy";
    private static final String cTimeFormat = "HH:mm:ss";
    private static final String cTimeNull = "--:--:--";

    private Data mData;
    private AppData mAppData;

    private int mTextColor;

    private int mSelection;
    private LatLng mLocation;
    private String mTimeZoneId;
    private LinearLayout mLyoSelection;
    private LinearLayout mLyoName;
    private OwnSpinner mSpSelection;
    ArrayAdapter<String> mAdpSelection;
    private EditText mEdtName;
    private TextView mTxtLongitude;
    private TextView mTxtLattitude;
    private TextView mTxtTimeZone;
    private TextView mTxtRise;
    private TextView mTxtSet;
    private Button mBtnDate;
    private Button mBtnSave;
    private Button mBtnNameOk;
    private boolean mNameEntry;
    private boolean mLocationInit;
    private boolean mSpinnerInit;
    private String mRise;
    private String mSet;
    private int mMoonResource;
    private int mDay;
    private int mMonth;
    private int mYear;
    private String mName;

    private int mLocationStatus;
    //* 0: Init
    //* 1: Start
    //* 2: Last location
    //* 3: No Last location
    //* 4: Start get actual location
    //* 5: Actual location
    //* 9: No location services

    private FusedLocationProviderClient mLocationClient;

    Handler mLocationHandler = new Handler(Looper.getMainLooper());
    Runnable mLocationRunnable = new Runnable() {
        @Override
        public void run() {
            boolean lReRun;

            lReRun = true;
            switch (mLocationStatus){
                case 0:
                    sInitLocation();
                    break;
                case 2:
                case 3:
                    sGetActualLocation();
                    break;
                case 5:
                    sStopUpdates();
                case 9:
                    lReRun = false;
                    break;
            }
            if (lReRun){
                mLocationHandler.postDelayed(this, 200);
            }
        }
    };

    private LocationCallback mLocationCallback;

    private AdapterView.OnItemSelectedListener mSelectionListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pPosition, long id) {
            Location lLocation;
            String lName;

            mSelection = pPosition;
            switch (pPosition) {
                case 0:
                    mAppData.xModus(AppData.ModusCurrent);
                    mLocation = mAppData.xCurrentLocation();
                    mTimeZoneId = mAppData.xCurrentZone().toString();
                    sFillLongitudeLattitude();
                    sRiseSet();
                    break;
                case 1:
                    mAppData.xModus(AppData.ModusMap);
                    if (!mSpinnerInit) {
                        sToMaps();
                    }
                    break;
                default:
                    mAppData.xModus(AppData.ModusStorage);
                    lName = mSpSelection.getItemAtPosition(pPosition).toString();
                    mAppData.xSelection(lName);
                    lLocation = mData.xLocation(lName);
                    mLocation = new LatLng(lLocation.xLattitude(), lLocation.xLongitude());
                    mTimeZoneId = lLocation.xZone();
                    sFillLongitudeLattitude();
                    sRiseSet();
                    break;
            }
            mSpinnerInit = false;
            sSetScreen();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mSelection = -1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        AndroidThreeTen.init(this);

        LocalDate lDate;

        mSpinnerInit = true;

        mData = Data.getInstance(mContext);
        mAppData = AppData.getInstance();

        mLyoSelection = findViewById(R.id.lyoSelection);
        mLyoName = findViewById(R.id.lyoName);
        mSpSelection = findViewById(R.id.spSelection);
        mEdtName = findViewById(R.id.edtName);
        mTxtLongitude = findViewById(R.id.txtLongitude);
        mTxtLattitude = findViewById(R.id.txtLattitude);
        mTxtTimeZone = findViewById(R.id.txtTimeZone);
        mTxtRise = findViewById(R.id.txtRise);
        mTxtSet = findViewById(R.id.txtSet);
        mBtnDate = findViewById(R.id.btnDate);
        mBtnSave = findViewById(R.id.btnSave);
        mBtnNameOk = findViewById(R.id.btnNameOk);

        mTextColor = mTxtLongitude.getCurrentTextColor();

        if (savedInstanceState == null) {
            mAppData.xInitData();
            mSelection = 0;
            lDate = LocalDate.now();
            mDay = lDate.getDayOfMonth();
            mMonth = lDate.getMonthValue();
            mYear = lDate.getYear();
            mTimeZoneId = "";
            mLocation = null;
            mNameEntry = false;
            mLocationInit = true;
            mRise = "";
            mSet = "";
            mMoonResource = R.drawable.moon_00;
            mName = "";
            sCheckPermissions();
        } else {
            mSelection = savedInstanceState.getInt("Selection");
            mLocation = savedInstanceState.getParcelable("Location");
            mNameEntry = savedInstanceState.getBoolean("NameEntry");
            mLocationInit = savedInstanceState.getBoolean("Init");
            mRise = savedInstanceState.getString("Rise");
            mSet = savedInstanceState.getString("Set");
            mMoonResource = savedInstanceState.getInt("MoonResource");
            mDay = savedInstanceState.getInt("Day");
            mMonth = savedInstanceState.getInt("Month");
            mYear = savedInstanceState.getInt("Year");
            mName = savedInstanceState.getString("Name");
            mTimeZoneId = savedInstanceState.getString("TimeZoneId");

            lDate = LocalDate.of(mYear, mMonth, mDay);
        }

        sFillBackground();

        mAdpSelection = new ArrayAdapter<>(this, R.layout.ownspinner_item);
        mAdpSelection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpSelection.setAdapter(mAdpSelection);
        sFillSelection();
        mSpSelection.setOnItemSelectedListener(mSelectionListener);

        mEdtName.setText(mName);
        mBtnDate.setText(lDate.format(DateTimeFormatter.ofPattern(cDateFormat)));

        sSetScreen();
        sFillRiseSet();
        sFillLongitudeLattitude();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        mName = mEdtName.getText().toString();
        mSelection = mSpSelection.getSelectedItemPosition();
        savedInstanceState.putInt("Selection", mSelection);
        savedInstanceState.putParcelable("Location", mLocation);
        savedInstanceState.putBoolean("NameEntry", mNameEntry);
        savedInstanceState.putBoolean("Init", mLocationInit);
        savedInstanceState.putString("Rise", mRise);
        savedInstanceState.putString("Set", mSet);
        savedInstanceState.putInt("MoonResource", mMoonResource);
        savedInstanceState.putInt("Day", mDay);
        savedInstanceState.putInt("Month", mMonth);
        savedInstanceState.putInt("Year", mYear);
        savedInstanceState.putString("Name", mName);
        savedInstanceState.putString("TimeZoneId", mTimeZoneId);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAppData.xModus() == AppData.ModusMap) {
            mLocation = mAppData.xMapLocation();
            if (!mAppData.xMapZone().equals("")) {
                mTimeZoneId = mAppData.xMapZone();
            }
            sFillLongitudeLattitude();
            sRiseSet();
            if (mAppData.xMapZone().equals("")) {
                new SelectTimeZone(this).execute();
            }
        }
    }

    private void sCheckPermissions() {
        List<String> lPermissions;

        lPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            lPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            lPermissions.add(Manifest.permission.INTERNET);
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            lPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (lPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    lPermissions.toArray(new String[0]),
                    1);
        } else {
            sGetLocation();
        }
    }


    @Override
    public void onRequestPermissionsResult(int pRequest, @NonNull String[] permissions, @NonNull int[] pResults) {
        int lCount;
        boolean lRefused;

        if (pRequest == 1) {
            lRefused = false;
            for (lCount = 0; lCount < pResults.length; lCount++) {
                if (pResults[lCount] != PackageManager.PERMISSION_GRANTED) {
                    lRefused = true;
                    break;
                }
            }
            if (lRefused) {
                Toast.makeText(mContext, R.string.msg_permissions, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                sGetLocation();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mNameEntry) {
            mNameEntry = false;
            sSetScreen();
        } else {
            finish();
        }
    }

    private void sProcessTimeZone(int pStatus, String pMessage, JSONObject pResult) {
        String lResult;
        String lMessage;
        String lTimeZoneId;

        switch (pStatus) {
            case Result.cResultOK:
                lResult = pResult.optString("status", "wrong JSON answer");
                if (lResult.equals("OK")) {
                    lTimeZoneId = pResult.optString("timeZoneId", "");
                    if (!lTimeZoneId.equals("")) {
                        mAppData.xMapZone(lTimeZoneId);
                        mTimeZoneId = lTimeZoneId;
                    }
                } else {
                    lMessage = pResult.optString("error_message", "");
                    Toast.makeText(mContext, lResult + " / " + lMessage, Toast.LENGTH_SHORT).show();
                }
                break;
            case Result.cResultConnectTimeOut:
                Toast.makeText(mContext, "Connect Time-Out", Toast.LENGTH_SHORT).show();
                break;
            case Result.cResultReadTimeOut:
                Toast.makeText(mContext, "Read Time-Out", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(mContext, pMessage, Toast.LENGTH_SHORT).show();
                break;
        }
        sFillLongitudeLattitude();
        sRiseSet();
    }

    private void sFillSelection() {
        List<Location> lLocations;
        Location lLocation;
        int lLocationCount;
        int lSelectionPos;

        mAdpSelection.clear();
        mAdpSelection.add(getString(R.string.sel_current_location));
        mAdpSelection.add(getString(R.string.sel_choose_map));
        lSelectionPos = -2;
        lLocations = mData.xLocations();
        for (lLocationCount = 0; lLocationCount < lLocations.size(); lLocationCount++) {
            lLocation = lLocations.get(lLocationCount);
            if (lLocation.xName().equals(mAppData.xSelection())) {
                lSelectionPos = lLocationCount;
            }
            mAdpSelection.add(lLocation.xName());
        }
        switch (mAppData.xModus()) {
            case AppData.ModusCurrent:
                lSelectionPos = 0;
                break;
            case AppData.ModusMap:
                lSelectionPos = 1;
                break;
            default:
                lSelectionPos += 2;
        }
        mSpSelection.setSelection(lSelectionPos);
    }

    private void sGetLocation() {
        mLocationStatus = 0;
        mLocationHandler.postDelayed(mLocationRunnable, 10);
    }

    private void sInitLocation(){
        GoogleApiAvailability lAvailable;

        lAvailable = GoogleApiAvailability.getInstance();
        if (lAvailable.isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS) {
            mLocationStatus = 1;
            mLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
            try {
                mLocationClient.getLastLocation()
                        .addOnCompleteListener(this, new OnCompleteListener<android.location.Location>() {
                            @Override
                            public void onComplete(Task pTask) {
                                android.location.Location lLocation;

                                if (pTask.isSuccessful()){
                                    lLocation = (android.location.Location) pTask.getResult();
                                    if (lLocation == null) {
                                        mLocationStatus = 3;
                                    } else {
                                        mLocationStatus = 2;
                                        mAppData.xCurrentLocation(new LatLng(lLocation.getLatitude(), lLocation.getLongitude()));
                                        mAppData.xLocationStatus(AppData.LocationLast);
                                        if (mAppData.xModus() == AppData.ModusCurrent) {
                                            mLocation = mAppData.xCurrentLocation();
                                            sRiseSet();
                                            sFillLongitudeLattitude();
                                        }
                                    }
                                } else {
                                    mLocationStatus = 3;
                                }
                            }
                        });
            } catch (SecurityException ignored) {
            }
        } else {
            mLocationStatus = 9;
        }
    }

    private void sGetActualLocation(){
        LocationRequest lRequest;

        lRequest = LocationRequest.create();
        lRequest.setInterval(2000);
        lRequest.setFastestInterval(2000);
        lRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                boolean lUpdate;

                if (locationResult == null) {
                    return;
                }
                lUpdate = false;
                for (android.location.Location lLocation : locationResult.getLocations()) {
                    mAppData.xCurrentLocation(new LatLng(lLocation.getLatitude(), lLocation.getLongitude()));
                    lUpdate = true;
                }
                if (lUpdate){
                    mLocationStatus = 5;
                    mAppData.xLocationStatus(AppData.LocationFix);
                    if (mAppData.xModus() == AppData.ModusCurrent) {
                        mLocation = mAppData.xCurrentLocation();
                        sRiseSet();
                        sFillLongitudeLattitude();
                    }
                }
            }
        };
        try {
            mLocationClient.requestLocationUpdates(lRequest, mLocationCallback, Looper.getMainLooper());
            mLocationStatus = 4;
        } catch (SecurityException pExc){
            mLocationStatus = 9;
        }
    }

    private void sStopUpdates(){
        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void sFillLongitudeLattitude() {
        if (mLocation == null) {
            mTxtLongitude.setText("");
            mTxtLattitude.setText("");
            mTxtTimeZone.setText("");
        } else {
            mTxtLongitude.setText(String.valueOf(mLocation.longitude));
            mTxtLattitude.setText(String.valueOf(mLocation.latitude));
            mTxtTimeZone.setText(mTimeZoneId);
        }
        if (mAppData.xLocationStatus() == AppData.LocationInit){
            mTxtLongitude.setTextColor(Color.RED);
            mTxtLattitude.setTextColor(Color.RED);
        } else {
            mTxtLongitude.setTextColor(mTextColor);
            mTxtLattitude.setTextColor(mTextColor);
        }
        if (mAppData.xLocationStatus() == AppData.LocationFix) {
            mTxtLongitude.setTypeface(null, Typeface.NORMAL);
            mTxtLattitude.setTypeface(null, Typeface.NORMAL);
        } else {
            mTxtLongitude.setTypeface(null, Typeface.ITALIC);
            mTxtLattitude.setTypeface(null, Typeface.ITALIC);
        }
    }

    private void sFillRiseSet() {
        mTxtRise.setText(mRise);
        mTxtSet.setText(mSet);
        if (mAppData.xDisplay() == AppData.DisplayMoon) {
            sFillBackground();
        }
    }

    public void sSun_Click(View Vw) {
        if (mAppData.xDisplay() != AppData.DisplaySun) {
            mAppData.xDisplay(AppData.DisplaySun);
            sFillBackground();
            sRiseSet();
            sFillRiseSet();
        }
    }

    public void sMoon_Click(View Vw) {
        if (mAppData.xDisplay() != AppData.DisplayMoon) {
            mAppData.xDisplay(AppData.DisplayMoon);
            sRiseSet();
            sFillRiseSet();
        }
    }

    private void sFillBackground() {
        Display lDisplay;
        Point lSizePoint;
        int lSize;
        ImageView lImgBackground;
        Bitmap lBackground;
        int lResourceId;

        lDisplay = getWindowManager().getDefaultDisplay();
        lSizePoint = new Point();
        lDisplay.getSize(lSizePoint);
        if (lSizePoint.x > lSizePoint.y) {
            lSize = lSizePoint.x;
        } else {
            lSize = (int) (lSizePoint.y * 1.02);
        }
        if (mAppData.xDisplay() == AppData.DisplaySun) {
            lResourceId = R.drawable.sunrise;
        } else {
            lResourceId = mMoonResource;
        }
        lBackground = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), lResourceId), lSize, lSize, true);
        lImgBackground = findViewById(R.id.imgBackground);
        lImgBackground.setImageBitmap(lBackground);
    }

    public void sSetDate(View Vw) {
        DatePicker lPick;

        DatePickerDialog lPicker = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int pYear, int pMonth, int pDay) {
                LocalDate lDate;
                mYear = pYear;
                mMonth = pMonth + 1;
                mDay = pDay;

                lDate = LocalDate.of(mYear, mMonth, mDay);
                mBtnDate.setText(lDate.format(DateTimeFormatter.ofPattern(cDateFormat)));
                sRiseSet();
            }
        }, mYear, mMonth - 1, mDay);
        lPick = lPicker.getDatePicker();
        lPick.setCalendarViewShown(true);
        lPick.setSpinnersShown(false);
        lPicker.show();
    }

    public void sSaveLocation(View Vw) {
        mNameEntry = true;
        mName = "";
        mEdtName.setText(mName);
        sSetScreen();
    }

    public void sNameOk(View Vw) {
        String lName;
        int lResult;

        mName = mEdtName.getText().toString();
        lName = mName.trim();
        if (lName.equals("")) {
            Toast.makeText(mContext, R.string.msg_noname, Toast.LENGTH_SHORT).show();
        } else {
            lResult = sSaveLocation(lName);
            if (lResult == Result.cResultOK) {
                mNameEntry = false;
                mAppData.xModus(AppData.ModusStorage);
                mAppData.xSelection(lName);
                sSetScreen();
                sFillSelection();
            }
        }
    }

    private int sSaveLocation(String pName) {
        DialogFragment lDialog;
        Bundle lParameters;
        List<Location> lLocations;
        Location lLocation;
        int lLocationCount;
        boolean lFound;
        int lResult;

        lLocations = mData.xLocations();
        lFound = false;
        for (lLocationCount = 0; lLocationCount < lLocations.size(); lLocationCount++) {
            lLocation = lLocations.get(lLocationCount);
            if (lLocation.xName().equals(pName)) {
                lFound = true;
                break;
            }
        }
        if (lFound) {
            lDialog = new YesNoDialog();
            lParameters = new Bundle();
            lParameters.putString(YesNoDialog.cTitle, getString(R.string.dia_location_exists));
            lParameters.putString(YesNoDialog.cMessage, getString(R.string.dia_location_replace).replace("**Loc**", pName));
            lParameters.putString(YesNoDialog.cYesButton, getString(R.string.btn_OK));
            lParameters.putString(YesNoDialog.cNoButton, getString(R.string.btn_cancel));
            lDialog.setArguments(lParameters);
            lDialog.show(getFragmentManager(), "ReplaceLocation");
            lResult = Result.cResultExists;
        } else {
            lLocation = new Location(pName, mLocation.longitude, mLocation.latitude, mTimeZoneId);
            lResult = mData.xNewLocation(lLocation);
        }
        return lResult;
    }

    @Override
    public void onYesNoDialogEnd(String pResult) {
        int lResult;
        String lName;
        List<Location> lLocations;
        Location lLocation = null;
        int lLocationCount;
        boolean lFound;

        if (pResult.equals(YesNoDialog.cYesResult)) {
            lName = mName.trim();
            lLocations = mData.xLocations();
            lFound = false;
            for (lLocationCount = 0; lLocationCount < lLocations.size(); lLocationCount++) {
                lLocation = lLocations.get(lLocationCount);
                if (lLocation.xName().equals(lName)) {
                    lFound = true;
                    break;
                }
            }
            if (lFound) {
                lLocation.xLongitude(mLocation.longitude);
                lLocation.xLattitude(mLocation.latitude);
                lLocation.xZone(mTimeZoneId);
                lResult = mData.xModifyLocation(lLocation);
                if (lResult == Result.cResultOK) {
                    mNameEntry = false;
                    mAppData.xModus(AppData.ModusStorage);
                    mAppData.xSelection(lName);
                    sSetScreen();
                    sFillSelection();
                }
            }
        }
    }

    private void sSetScreen() {
        LinearLayout.LayoutParams lParVisible;
        LinearLayout.LayoutParams lParInvisible;

        lParVisible = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParInvisible = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        if (mNameEntry) {
            mLyoSelection.setLayoutParams(lParInvisible);
            mLyoName.setLayoutParams(lParVisible);
            mSpSelection.setEnabled(false);
            mBtnDate.setEnabled(false);
            mBtnSave.setEnabled(false);
            mBtnSave.setVisibility(View.INVISIBLE);
            mBtnNameOk.setEnabled(true);
            mEdtName.setEnabled(true);
        } else {
            mLyoSelection.setLayoutParams(lParVisible);
            mLyoName.setLayoutParams(lParInvisible);
            mSpSelection.setEnabled(true);
            mBtnDate.setEnabled(true);
            mBtnNameOk.setEnabled(false);
            mEdtName.setEnabled(false);
            if (mAppData.xModus() == AppData.ModusStorage) {
                mBtnSave.setEnabled(false);
                mBtnSave.setVisibility(View.INVISIBLE);
            } else {
                mBtnSave.setEnabled(true);
                mBtnSave.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sRiseSet() {
        ZonedDateTime lLocalTime;
        ZonedDateTime lUTCTime;
        SunMoonCalculator lCalc;
        double lRise;
        double lSet;
        int lMoonAge;

        if (mLocation != null) {
            try {
                lLocalTime = ZonedDateTime.of(mYear, mMonth, mDay, 12, 0, 0, 0, ZoneId.of(mTimeZoneId));
                lUTCTime = lLocalTime.withZoneSameInstant(ZoneId.of("UTC"));
                lCalc = sInitCalculator(lUTCTime);
                if (mAppData.xDisplay() == AppData.DisplaySun) {
                    lRise = lCalc.sun.rise;
                    lLocalTime = sLocalTime(lRise);
                    mRise = lLocalTime.format(DateTimeFormatter.ofPattern(cTimeFormat));
                    lSet = lCalc.sun.set;
                    lLocalTime = sLocalTime(lSet);
                    mSet = lLocalTime.format(DateTimeFormatter.ofPattern(cTimeFormat));
                    lMoonAge = 0;
                } else {
                    lRise = lCalc.moon.rise;
                    if (lRise == -1) {
                        mRise = cTimeNull;
                        mSet = cTimeNull;
                        lMoonAge = 0;
                    } else {
                        lMoonAge = (int) lCalc.moonAge;
                        lLocalTime = sLocalTime(lRise);
                        if (lLocalTime.getHour() < 12) {
                            if (lMoonAge > 14) {
                                if (lLocalTime.getDayOfMonth() == mDay) {
                                    lUTCTime = lUTCTime.plusDays(1);
                                    lCalc = sInitCalculator(lUTCTime);
                                    lRise = lCalc.moon.rise;
                                    lLocalTime = sLocalTime(lRise);
                                }
                            }
                        }
                        mRise = lLocalTime.format(DateTimeFormatter.ofPattern(cTimeFormat));
                        if (lLocalTime.getDayOfMonth() != mDay) {
                            mRise = mRise + "(+)";
                        }
                        lSet = lCalc.moon.set;
                        if (lSet < lRise) {
                            lUTCTime = lUTCTime.plusDays(1);
                            lCalc = sInitCalculator(lUTCTime);
                            lSet = lCalc.moon.set;
                        }
                        lLocalTime = sLocalTime(lSet);
                        mSet = lLocalTime.format(DateTimeFormatter.ofPattern(cTimeFormat));
                        if (lLocalTime.getDayOfMonth() != mDay) {
                            mSet = mSet + "(+)";
                        }
                    }
                }
            } catch (Exception pExc) {
                mRise = cTimeNull;
                mSet = cTimeNull;
                lMoonAge = 0;
            }
            switch (lMoonAge) {
                case 0:
                    mMoonResource = R.drawable.moon_00;
                    break;
                case 1:
                    mMoonResource = R.drawable.moon_02;
                    break;
                case 2:
                    mMoonResource = R.drawable.moon_02;
                    break;
                case 3:
                    mMoonResource = R.drawable.moon_03;
                    break;
                case 4:
                    mMoonResource = R.drawable.moon_04;
                    break;
                case 5:
                    mMoonResource = R.drawable.moon_05;
                    break;
                case 6:
                    mMoonResource = R.drawable.moon_06;
                    break;
                case 7:
                    mMoonResource = R.drawable.moon_07;
                    break;
                case 8:
                    mMoonResource = R.drawable.moon_08;
                    break;
                case 9:
                    mMoonResource = R.drawable.moon_09;
                    break;
                case 10:
                    mMoonResource = R.drawable.moon_10;
                    break;
                case 11:
                    mMoonResource = R.drawable.moon_11;
                    break;
                case 12:
                    mMoonResource = R.drawable.moon_12;
                    break;
                case 13:
                    mMoonResource = R.drawable.moon_13;
                    break;
                case 14:
                    mMoonResource = R.drawable.moon_14;
                    break;
                case 15:
                    mMoonResource = R.drawable.moon_15;
                    break;
                case 16:
                    mMoonResource = R.drawable.moon_16;
                    break;
                case 17:
                    mMoonResource = R.drawable.moon_17;
                    break;
                case 18:
                    mMoonResource = R.drawable.moon_18;
                    break;
                case 19:
                    mMoonResource = R.drawable.moon_19;
                    break;
                case 20:
                    mMoonResource = R.drawable.moon_20;
                    break;
                case 21:
                    mMoonResource = R.drawable.moon_21;
                    break;
                case 22:
                    mMoonResource = R.drawable.moon_22;
                    break;
                case 23:
                    mMoonResource = R.drawable.moon_23;
                    break;
                case 24:
                    mMoonResource = R.drawable.moon_24;
                    break;
                case 25:
                    mMoonResource = R.drawable.moon_25;
                    break;
                case 26:
                    mMoonResource = R.drawable.moon_26;
                    break;
                default:
                    mMoonResource = R.drawable.moon_00;
                    break;
            }

            sFillRiseSet();
        }
    }

    private SunMoonCalculator sInitCalculator(ZonedDateTime pMoment) {
        SunMoonCalculator lCalc;

        try {
            lCalc = new SunMoonCalculator(pMoment.getYear(), pMoment.getMonthValue(), pMoment.getDayOfMonth(), pMoment.getHour(), pMoment.getMinute(), 0, mLocation.longitude * SunMoonCalculator.DEG_TO_RAD, mLocation.latitude * SunMoonCalculator.DEG_TO_RAD);
            lCalc.setTwilight(SunMoonCalculator.TWILIGHT.HORIZON_34arcmin);
            lCalc.calcSunAndMoon();
        } catch (Exception pExc) {
            lCalc = null;
        }

        return lCalc;
    }

    private ZonedDateTime sLocalTime(Double pTimeStamp) {
        ZonedDateTime lLocalTime;
        ZonedDateTime lUTCTime;
        int[] lCalcMoment;

        try {
            lCalcMoment = SunMoonCalculator.getDate(pTimeStamp);
            lUTCTime = ZonedDateTime.of(lCalcMoment[0], lCalcMoment[1], lCalcMoment[2], lCalcMoment[3], lCalcMoment[4], lCalcMoment[5], 0, ZoneId.of("UTC"));
            lLocalTime = lUTCTime.withZoneSameInstant(ZoneId.of(mTimeZoneId));
        } catch (Exception pExc) {
            lLocalTime = ZonedDateTime.now();
        }

        return lLocalTime;
    }

    private void sToMaps() {
        Intent lInt;

        lInt = new Intent();
        lInt.setClass(this, LocationMaps.class);
        startActivity(lInt);
    }

    private static class SelectTimeZone extends AsyncTask<Void, Void, RestAPI.RestResult> {
        private WeakReference<Main> mRefMain;

        private SelectTimeZone(Main pMain) {
            mRefMain = new WeakReference<>(pMain);
        }

        @Override
        protected RestAPI.RestResult doInBackground(Void... params) {
            Main lMain;
            String lRequest;
            String lAction;
            RestAPI.RestResult lOutput;
            RestAPI lRestAPI;
            long lTimeStamp;

            lMain = mRefMain.get();
            if (lMain == null) {
                return null;
            } else {
                lTimeStamp = new Date().getTime() / 1000;
                lRequest = "https://maps.googleapis.com/maps/api/timezone/json";
                lAction = "location=" + lMain.mLocation.latitude + "," + lMain.mLocation.longitude
                        + "&timestamp=" + lTimeStamp
                        + "&key=" + lMain.getString(R.string.google_api_key);
                lRestAPI = new RestAPI();
                lRestAPI.xMethod(RestAPI.cMethodGet);
                lRestAPI.xMediaRequest(RestAPI.cMediaText);
                lRestAPI.xMediaReply(RestAPI.cMediaJSON);
                lRestAPI.xUrl(lRequest);
                lRestAPI.xAction(lAction);
                lOutput = lRestAPI.xCallApi();
                return lOutput;
            }
        }

        protected void onPostExecute(RestAPI.RestResult pOutput) {
            Main lMain;

            if (pOutput != null) {
                lMain = mRefMain.get();
                if (lMain != null) {
                    lMain.sProcessTimeZone(pOutput.xResult(), pOutput.xText(), pOutput.xRelyJ());
                }
            }
        }
    }
}
