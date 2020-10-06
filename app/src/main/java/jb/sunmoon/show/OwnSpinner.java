package jb.sunmoon.show;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by Jan on 19-9-2017.
 */

public class OwnSpinner extends androidx.appcompat.widget.AppCompatSpinner {
    int mPreviousPos = -1;

    public OwnSpinner(Context pContext) {
        super(pContext);
    }

    public OwnSpinner(Context pContext, AttributeSet pAttrs) {
        super(pContext, pAttrs);
    }

    public OwnSpinner(Context pContext, AttributeSet pAttrs, int pDefStyle) {
        super(pContext, pAttrs, pDefStyle);
    }

    @Override
    public void setSelection(int pPosition) {
        super.setSelection(pPosition);
        if (pPosition == getSelectedItemPosition() && mPreviousPos == pPosition) {
            getOnItemSelectedListener().onItemSelected(null, null, pPosition, 0);
        }
        mPreviousPos = pPosition;
    }}
