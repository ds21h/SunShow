package jb.sunmoon.show;

import android.content.Context;

class HandlerCode {
    //  Runnable ID
    static final int cTimeZone = 0x0100;

    //  Process code
    static final int cRequestOK = 0x01;
    static final int cCommunicationOK = 0x02;
    static final int cProcessOK = 0x04;

    private HandlerCode(){}

    static String xCheckCode(Context pContext, int pWhat){
        String lResult;

        if ((pWhat & HandlerCode.cRequestOK) != 0) {
            if ((pWhat & HandlerCode.cCommunicationOK) != 0) {
                if ((pWhat & HandlerCode.cProcessOK) != 0) {
                    lResult = null;
                } else {
                    lResult = pContext.getString(R.string.msg_server_error);
                }
            } else {
                lResult = pContext.getString(R.string.msg_comm_error);
            }
        } else {
            lResult = pContext.getString(R.string.msg_int_error);
        }
        return lResult;
    }
}
