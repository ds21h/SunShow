package jb.sunmoon.show;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class DBMenu extends Activity {
    private final Context mContext = this;
    private static final int cReqExportFile = 1;
    private static final int cReqImportFile = 2;
    private static final String cLineType = "Type";
    private static final String cTypeLocation = "Location";
    private static final String cValue = "Value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.db_menu_layout);
    }

    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int pRequest, int pResult, Intent pInt) {
        Uri lUri;
//        Thread lAsync;

        if (pResult == RESULT_OK) {
            switch (pRequest) {
                case cReqExportFile:
                    lUri = pInt.getData();
                    sExportDB(lUri);
                    break;
                case cReqImportFile:
                    lUri = pInt.getData();
                    sImportDB(lUri);
                    break;
            }
        }
    }

    public void sExportDB(View Vw) {
        Intent lIntent;

        lIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        lIntent.addCategory(Intent.CATEGORY_OPENABLE);
        lIntent.setType("application/dbb");
        lIntent.putExtra(Intent.EXTRA_TITLE, "SunshowDB.dbb");

        startActivityForResult(lIntent, cReqExportFile);
    }

    public void sImportDB(View Vw) {
        Intent lIntent;

        lIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        lIntent.addCategory(Intent.CATEGORY_OPENABLE);
        lIntent.setType("*/*");

        startActivityForResult(lIntent, cReqImportFile);
    }

    private void sExportDB(Uri pFileName) {
        OutputStream lOutStream;
        BufferedWriter lBuffer;
        JSONObject lResult;
        List<Location> lLocations;

        try {
            lOutStream = mContext.getContentResolver().openOutputStream(pFileName);
            lBuffer = new BufferedWriter(new OutputStreamWriter(lOutStream));
            lLocations = Data.getInstance(this).xLocations();
            for (Location bLocation : lLocations) {
                lResult = new JSONObject();
                try {
                    lResult.put(cLineType, cTypeLocation);
                    lResult.put(cValue, bLocation.xToJSON());
                } catch (JSONException e) {
                    lResult = new JSONObject();
                }
                lBuffer.write(lResult.toString());
                lBuffer.newLine();
            }
            lBuffer.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finish();
    }

    private void sImportDB(Uri pFileName) {
        InputStream lInStream;
        BufferedReader lBuffer;
        JSONObject lResult;
        JSONObject lValue;
        String lLine;
        String lLineType;
        int lPhase;
        Location lLocation;
//        String lName;

        lPhase = 0;
        try {
            lInStream = mContext.getContentResolver().openInputStream(pFileName);
            lBuffer = new BufferedReader(new InputStreamReader(lInStream));
            lLine = lBuffer.readLine();
            while (lLine != null) {
                lResult = new JSONObject(lLine);
                lLineType = lResult.optString(cLineType, "");
                if (!lLineType.isEmpty()) {
                    if (lLineType.equals(cTypeLocation)){
                        if (lPhase < 1) {
                            Data.getInstance(this).xCleanLocations();
                            lPhase = 1;
                        }
                        lValue = lResult.optJSONObject(cValue);
                        if (lValue != null) {
                            lLocation = new Location(lValue);
                            Data.getInstance(this).xNewLocation(lLocation);
                        }
                    }
/*                    switch (lLineType) {
                        case cTypeLocation: {
                            if (lPhase < 1) {
                                Data.getInstance(this).xCleanLocations();
                                lPhase = 1;
                            }
                            if (lPhase == 1) {
                                lValue = lResult.optJSONObject(cValue);
                                if (lValue != null) {
                                    lLocation = new Location(lValue);
                                    Data.getInstance(this).xNewLocation(lLocation);
                                }
                            }
                            break;
                        }
                    } */
                }
                lLine = lBuffer.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finish();
    }
}
