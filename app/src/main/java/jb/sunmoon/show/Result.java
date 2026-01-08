package jb.sunmoon.show;

/**
 * Created by Jan on 11-7-2016.
 */
public class Result {
    private int mResult;

    public static final int cResultOK = 0;

    public static final int cResultNotFound = 10;
    public static final int cResultExists = 11;

    public static final int cResultServiceNotAvailable = 20;
    public static final int cResultServerError = 21;
    public static final int cResultConnectTimeOut = 22;
    public static final int cResultReadTimeOut = 23;
    public static final int cResultOutputFout = 24;

    public static final int cResultError = 99;

    public Result(){
        mResult = cResultOK;
    }

    void xResult(int pResult){
        mResult = pResult;
    }

    int xResult(){
        return mResult;
    }
}
