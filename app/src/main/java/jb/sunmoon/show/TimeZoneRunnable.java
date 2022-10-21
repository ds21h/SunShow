package jb.sunmoon.show;

import android.os.Handler;
import android.os.Message;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

class TimeZoneRunnable implements Runnable{
    private final Handler mHandler;
    private final LatLng mLocation;
    private final String mKey;
    private final boolean mUseTimeZoneDb;

// https://timezonedb.com/references/get-time-zone  http://api.timezonedb.com/v2.1/get-time-zone?key=A5TGO2DQ7F6G&format=json&by=position&lat=52.1362213&lng=4.6645597

    TimeZoneRunnable(Handler pHandler, LatLng pLocation, String pKey, boolean pTimeZoneDb){
        mHandler = pHandler;
        mLocation = pLocation;
        mKey = pKey;
        mUseTimeZoneDb = pTimeZoneDb;
    }

    @Override
    public void run() {
        String lRequest;
        String lAction;
        RestAPI.RestResult lOutput;
        RestAPI lRestAPI;
        long lTimeStamp;
        int lResult;
        String lTimeZone = "";
        Message lMessage;

        lResult = HandlerCode.cTimeZone;
        if (mUseTimeZoneDb){
            lRequest = "http://api.timezonedb.com/v2.1/get-time-zone";
            lAction = "format=json&by=position"
                    + "&lat=" + mLocation.latitude
                    + "&lng=" + mLocation.longitude
                    + "&key=" + mKey;
        } else {
            lTimeStamp = new Date().getTime() / 1000;
            lRequest = "https://maps.googleapis.com/maps/api/timezone/json";
            lAction = "location=" + mLocation.latitude + "," + mLocation.longitude
                    + "&timestamp=" + lTimeStamp
                    + "&key=" + mKey;
        }
        lRestAPI = new RestAPI();
        lRestAPI.xMethod(RestAPI.cMethodGet);
        lRestAPI.xMediaRequest(RestAPI.cMediaText);
        lRestAPI.xMediaReply(RestAPI.cMediaJSON);
        lRestAPI.xUrl(lRequest);
        lRestAPI.xAction(lAction);
        lResult |= HandlerCode.cRequestOK;
        lOutput = lRestAPI.xCallApi();
        if (lOutput.xResult() == Result.cResultOK){
            lResult |= HandlerCode.cCommunicationOK;
            if (lOutput.xReplyJ().optString("status", "wrong JSON answer").equals("OK")){
                if (mUseTimeZoneDb){
                    lTimeZone = lOutput.xReplyJ().optString("zoneName", "");
                } else {
                    lTimeZone = lOutput.xReplyJ().optString("timeZoneId", "");
                }
                if (!lTimeZone.equals("")){
                    lResult |= HandlerCode.cProcessOK;
                }
            }
        }
        lMessage = Message.obtain(mHandler, lResult);
        if ((lResult & HandlerCode.cProcessOK) != 0){
            lMessage.obj = lTimeZone;
        }
        lMessage.sendToTarget();
    }
}
