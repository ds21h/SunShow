package jb.sunmoon.show;

/**
 * Created by Jan on 11-7-2016.
 */
class Result {
    private int mResult;

    static final int cResultOK = 0;

    static final int cResultNotFound = 10;
    static final int cResultExists = 11;

    static final int cResultServiceNotAvailable = 20;
    static final int cResultServerError = 21;
    static final int cResultConnectTimeOut = 22;
    static final int cResultReadTimeOut = 23;
    static final int cResultOutputFout = 24;

    static final int cResultError = 99;

    Result(){
        mResult = cResultOK;
    }

    void xResult(int pResult){
        mResult = pResult;
    }

    int xResult(){
        return mResult;
    }
}
