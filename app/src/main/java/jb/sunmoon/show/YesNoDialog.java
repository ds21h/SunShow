package jb.sunmoon.show;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


/**
 * Created by Jan on 22-10-2015.
 */
public class YesNoDialog extends DialogFragment {
    public static final String cTitle = "Title";
    public static final String cMessage = "Message";
    public static final String cYesButton = "YesButton";
    public static final String cNoButton = "NoButton";
    public static final String cYesResult = "Yes";
    public static final String cNoResult = "No";

    public interface YesNoDialogListener {
        void onYesNoDialogEnd(String pResult);
    }

    public YesNoDialog(){
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Bundle lParameters;
        String lTitle;
        String lMessage;
        String lYesButton;
        String lNoButton;

        lParameters = getArguments();
        lTitle = lParameters.getString(cTitle, "");
        lMessage = lParameters.getString(cMessage, "");
        lYesButton = lParameters.getString(cYesButton, "Yes");
        lNoButton = lParameters.getString(cNoButton, "No");

        return new AlertDialog.Builder(getActivity())
                .setTitle(lTitle)
                .setMessage(lMessage)
                .setPositiveButton(lYesButton, (dialog, which) -> {
                    YesNoDialogListener lListener = (YesNoDialogListener) getActivity();
                    lListener.onYesNoDialogEnd(cYesResult);
                })
                .setNegativeButton(lNoButton, (dialog, which) -> {
                    YesNoDialogListener lListener = (YesNoDialogListener) getActivity();
                    lListener.onYesNoDialogEnd(cNoResult);
                })
                .create();
    }
}
