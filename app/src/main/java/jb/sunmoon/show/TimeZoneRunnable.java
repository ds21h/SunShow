package jb.sunmoon.show;

import android.os.Handler;
import android.os.Message;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

class TimeZoneRunnable implements Runnable{
    private final Handler mHandler;
    private final LatLng mLocation;
    private final String mKey;

    TimeZoneRunnable(Handler pHandler, LatLng pLocation, String pKey){
        mHandler = pHandler;
        mLocation = pLocation;
        mKey = pKey;
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
        lTimeStamp = new Date().getTime() / 1000;
        lRequest = "https://maps.googleapis.com/maps/api/timezone/json";
        lAction = "location=" + mLocation.latitude + "," + mLocation.longitude
                + "&timestamp=" + lTimeStamp
                + "&key=" + mKey;
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
                lTimeZone = lOutput.xReplyJ().optString("timeZoneId", "");
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
