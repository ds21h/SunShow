package jb.sunmoon.show;
//*
//*  Uses the JSR-310 backport for Android (java.time.* package in Java 8)
//*
//*  See https://github.com/JakeWharton/ThreeTenABP
//*

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
//import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;
import com.jakewharton.threetenabp.AndroidThreeTen;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main extends AppCompatActivity {
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
//    private boolean mUseTimeZoneDb;

    private int mLocationStatus;
    //* 0: Init
    //* 1: Start
    //* 2: Last location
    //* 3: No Last location
    //* 4: Start get actual location
    //* 5: Actual location
    //* 9: No location services

    private FusedLocationProviderClient mLocationClient;

    Handler mTimeZoneHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message pMessage) {
            String lTimeZoneId;
            String lErrMsg;

            if ((pMessage.what & HandlerCode.cTimeZone) != 0) {
                lErrMsg = HandlerCode.xCheckCode(mContext, pMessage.what);
                if (lErrMsg == null) {
                    if (pMessage.obj != null){
                        lTimeZoneId = (String) pMessage.obj;
                        mAppData.xMapZone(lTimeZoneId);
                        mTimeZoneId = lTimeZoneId;
                    }
                } else {
                    Toast.makeText(mContext, lErrMsg, Toast.LENGTH_SHORT).show();
                }
                sFillLongitudeLattitude();
                sRiseSet();
            }
            return true;
        }
    });

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

    private final AdapterView.OnItemSelectedListener mSelectionListener = new AdapterView.OnItemSelectedListener() {
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
/*                case 1:
                    mAppData.xModus(AppData.ModusMap);
                    if (!mSpinnerInit) {
                        mUseTimeZoneDb = false;
                        sToMaps();
                    }
                    break; */
                case 1:
                    mAppData.xModus(AppData.ModusMap);
                    if (!mSpinnerInit) {
//                        mUseTimeZoneDb = true;
                        sToMapsOSM();
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
        WindowCompat.enableEdgeToEdge(this.getWindow());
        setContentView(R.layout.main_layout);
        AndroidThreeTen.init(this);

        LinearLayout lLyoMain = findViewById(R.id.lyoMain);
        ViewCompat.setOnApplyWindowInsetsListener(lLyoMain, (pView, pInsets) -> {
            Insets lInsets;
            ViewGroup.MarginLayoutParams lLayoutParms;

            lInsets = pInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            lLayoutParms = (ViewGroup.MarginLayoutParams) pView.getLayoutParams();
            lLayoutParms.topMargin = lInsets.top;
            lLayoutParms.leftMargin = lInsets.left;
            lLayoutParms.bottomMargin = lInsets.bottom;
            lLayoutParms.rightMargin = lInsets.right;
            pView.setLayoutParams(lLayoutParms);

            return WindowInsetsCompat.CONSUMED;
        });

        com.google.android.material.button.MaterialButton lBtnDB = findViewById(R.id.btnDB);
        lBtnDB.setOnClickListener(pView -> sDB_menu());
        mBtnNameOk = findViewById(R.id.btnNameOk);
        mBtnNameOk.setOnClickListener(pView -> sNameOk());
        mBtnSave = findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(pView -> sSaveLocation());
        mBtnDate = findViewById(R.id.btnDate);
        mBtnDate.setOnClickListener(pView -> sSetDate());
        android.widget.ImageButton lImbSun = findViewById(R.id.imbSun);
        lImbSun.setOnClickListener(pView -> sSun_Click());
        android.widget.ImageButton lImbMoon = findViewById(R.id.imbMoon);
        lImbMoon.setOnClickListener(pView -> sMoon_Click());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back is pressed... Finishing the activity
                sBackPressed();
            }
        });

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
//            mUseTimeZoneDb = false;
            sCheckPermissions();
        } else {
            mSelection = savedInstanceState.getInt("Selection");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                mLocation = savedInstanceState.getParcelable("Location", LatLng.class);

            } else {
                mLocation = savedInstanceState.getParcelable("Location");
            }
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
//            mUseTimeZoneDb = savedInstanceState.getBoolean("UseTimeZoneDb");

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
//        savedInstanceState.putBoolean("UseTimeZoneDb", mUseTimeZoneDb);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        TimeZoneRunnable lTimeZoneRunnable;

        if (mAppData.xModus() == AppData.ModusMap) {
            mLocation = mAppData.xMapLocation();
            if (!mAppData.xMapZone().isEmpty()) {
                mTimeZoneId = mAppData.xMapZone();
            }
            sFillLongitudeLattitude();
            if (mAppData.xMapZone().isEmpty()) {
                lTimeZoneRunnable = new TimeZoneRunnable(mTimeZoneHandler, mLocation, getString(R.string.timezonedb_key));
                SunMoonApp.getInstance().xExecutor.execute(lTimeZoneRunnable);
            } else {
                sRiseSet();
            }
        }
    }

    private void sCheckPermissions() {
        List<String> lPermissions;

        lPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            lPermissions.add(Manifest.permission.INTERNET);
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            lPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!lPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    lPermissions.toArray(new String[0]),
                    1);
        } else {
            sGetLocation();
        }
    }

    @Override
    public void onStop() {
        mTimeZoneHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int pRequest, @NonNull String[] permissions, @NonNull int[] pResults) {
        super.onRequestPermissionsResult(pRequest, permissions, pResults);
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

    private void sBackPressed(){
        if (mNameEntry) {
            mNameEntry = false;
            sSetScreen();
        } else {
            finish();
        }
    }

/*    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mNameEntry) {
            mNameEntry = false;
            sSetScreen();
        } else {
            finish();
        }
    } */

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
                        .addOnCompleteListener(this, pTask -> {
                            android.location.Location lLocation;

                            if (pTask.isSuccessful()){
                                lLocation = pTask.getResult();
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
                        });
            } catch (SecurityException ignored) {
            }
        } else {
            mLocationStatus = 9;
        }
    }

    private void sGetActualLocation(){
        LocationRequest lRequest;
        LocationRequest.Builder lBuilder;

        lBuilder = new LocationRequest.Builder(2000);
        lRequest = lBuilder.setMinUpdateIntervalMillis(2000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult pLocationResult) {
                boolean lUpdate;

                lUpdate = false;
                for (android.location.Location lLocation : pLocationResult.getLocations()) {
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

    private void sDB_menu(){
        Intent lInt;

        lInt = new Intent();
        lInt.setClass(this, DBMenu.class);
        startActivity(lInt);
    }

    private void sSun_Click() {
        if (mAppData.xDisplay() != AppData.DisplaySun) {
            mAppData.xDisplay(AppData.DisplaySun);
            sFillBackground();
            sRiseSet();
            sFillRiseSet();
        }
    }

    private void sMoon_Click() {
        if (mAppData.xDisplay() != AppData.DisplayMoon) {
            mAppData.xDisplay(AppData.DisplayMoon);
            sRiseSet();
            sFillRiseSet();
        }
    }

    private void sFillBackground() {
        Display lDisplay;
        Point lSizePoint;
        Rect lRect;
        int lSize;
        ImageView lImgBackground;
        Bitmap lBackground;
        int lResourceId;
        WindowMetrics lMetrics;
        WindowManager lManager;


        lSizePoint = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lManager = this.getWindowManager();
            lMetrics = lManager.getCurrentWindowMetrics();
            lRect = lMetrics.getBounds();
            lSizePoint = new Point(lRect.right - lRect.left, lRect.bottom - lRect.top);
        } else {
            lDisplay = getWindowManager().getDefaultDisplay();
            lDisplay.getSize(lSizePoint);
        }
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

    private void sSetDate() {
        DatePickerDialog lPicker = new DatePickerDialog(this, (datePicker, pYear, pMonth, pDay) -> {
            LocalDate lDate;
            mYear = pYear;
            mMonth = pMonth + 1;
            mDay = pDay;

            lDate = LocalDate.of(mYear, mMonth, mDay);
            mBtnDate.setText(lDate.format(DateTimeFormatter.ofPattern(cDateFormat)));
            sRiseSet();
        }, mYear, mMonth - 1, mDay);
        lPicker.show();
    }

    private void sSaveLocation() {
        mNameEntry = true;
        mName = "";
        mEdtName.setText(mName);
        sSetScreen();
    }

    private void sNameOk() {
        String lName;
        int lResult;

        mName = mEdtName.getText().toString();
        lName = mName.trim();
        if (lName.isEmpty()) {
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
        List<Location> lLocations;
        Location lLocation = null;
        Location fLocation;
        int lLocationCount;
        boolean lFound;
        int lResult;
        Result lDialogResult;
        DialogInterface.OnClickListener dialogClickListener;

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
            fLocation = lLocation; // The location needs to be final for use in the nested class!
            lDialogResult = new Result();
            dialogClickListener = (dialog, which) -> {
                int bResult;

                switch (which) {
                    // on below line we are setting a click listener
                    // for our positive button
                    case DialogInterface.BUTTON_POSITIVE:
                            fLocation.xLongitude(mLocation.longitude);
                            fLocation.xLattitude(mLocation.latitude);
                            fLocation.xZone(mTimeZoneId);
                            bResult = mData.xModifyLocation(fLocation);
                            if (bResult == Result.cResultOK) {
                                lDialogResult.xResult(Result.cResultOK);
                                mNameEntry = false;
                                mAppData.xModus(AppData.ModusStorage);
                                mAppData.xSelection(pName);
                                sSetScreen();
                                sFillSelection();
                            } else {
                                lDialogResult.xResult(Result.cResultError);
                            }
                        break;
                    // on below line we are setting click listener
                    // for our negative button.
                    case DialogInterface.BUTTON_NEGATIVE:
                        // on below line we are dismissing our dialog box.
                        lDialogResult.xResult(Result.cResultExists);
                        dialog.dismiss();

                }
            };
            // on below line we are creating a builder variable for our alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
//            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            // on below line we are setting message for our dialog box.
            builder.setMessage(getString(R.string.dia_location_replace).replace("**Loc**", pName))
                    .setTitle(R.string.dia_location_exists)
                    // on below line we are setting positive button
                    // and setting text to it.
                    .setPositiveButton(R.string.btn_OK, dialogClickListener)
                    // on below line we are setting negative button
                    // and setting text to it.
                    .setNegativeButton(R.string.btn_cancel, dialogClickListener)
                    // on below line we are calling
                    // show to display our dialog.
                    .show();
            lResult = lDialogResult.xResult();
        } else {
            lLocation = new Location(pName, mLocation.longitude, mLocation.latitude, mTimeZoneId);
            lResult = mData.xNewLocation(lLocation);
        }
        return lResult;
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
            lCalc = new SunMoonCalculator(pMoment.getYear(), pMoment.getMonthValue(), pMoment.getDayOfMonth(), pMoment.getHour(), pMoment.getMinute(), 0, mLocation.longitude * SunMoonCalculator.DEG_TO_RAD, mLocation.latitude * SunMoonCalculator.DEG_TO_RAD, 0);
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

/*    private void sToMaps() {
        Intent lInt;

        lInt = new Intent();
        lInt.setClass(this, LocationMaps.class);
        startActivity(lInt);
    } */

    private void sToMapsOSM() {
        Intent lInt;

        lInt = new Intent();
        lInt.setClass(this, LocationOSM.class);
        startActivity(lInt);
    }
}
