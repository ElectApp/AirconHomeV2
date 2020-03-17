package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class AppErrorDialog extends DialogFragment {

    private int titleId, detailId, buttonId;
    private int count;
    private String detailTxt;
    private Context context;
    private Activity activity;
    private OnClickActionButtonListener buttonListener;

    public static final String TITLE_ID = "title", DETAIL_ID = "detail", DETAIL_TXT = "detail-txt",
            BUTTON_ID = "button";
    public static final String TAG = "AppErrorDialog"; //Unique TAG

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        //Get data
        Bundle bundle = getArguments();
        titleId = bundle.getInt(TITLE_ID);
        detailId = bundle.getInt(DETAIL_ID);
        buttonId = bundle.getInt(BUTTON_ID);
        detailTxt = bundle.getString(DETAIL_TXT);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_app_error, container, false);

        TextView title = view.findViewById(R.id.title);
        TextView detail = view.findViewById(R.id.detail);
        RoundButtonWidget action = view.findViewById(R.id.action_btn);

        //Set title
        if (titleId!=0){ title.setText(titleId); }
        //Set detail
        if (detailId!=0){
            detail.setText(detailId);
        }else {
            detail.setText(detailTxt);
        }
        //Set button
        if (buttonId!=0){ action.setText(buttonId); }

        //Set click listener
        action.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {

                if (buttonId==R.string.connect_ac_wifi){
                    //Connect AC
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
                }

                if (buttonListener!=null){
                    buttonListener.onClick(view, titleId, buttonId);
                }

            }
        });

        return view;

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
/*
        //Set animation here!
        //Thank: https://stackoverflow.com/questions/41869348/enter-and-exit-animations-not-working-in-dialog-fragment
        Dialog dialog = getDialog();
        if(dialog!=null){
            Window window = dialog.getWindow();
            if (window!=null){
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
            }
        }*/
    }

    @Override
    public void onStart() {
        super.onStart();

        //Set size dialog
        Dialog dialog = getDialog();
        if(dialog!=null){
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window!=null){
                window.setLayout(width, height);
            }
            //Block cancel from Back button on navigator bar
            //dialog.setCancelable(false);
        }

    }

    //Detect back button but don't dismiss
    //Thank: https://stackoverflow.com/questions/21307858/detect-back-button-but-dont-dismiss-dialogfragment
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        return new Dialog(activity, getTheme()){
            @Override
            public void onBackPressed() {
                //super.onBackPressed();
                Log.w(TAG, "Press back");
                //Count
                count++;
                if(count<2){
                    Toast.makeText(context, R.string.close_app,
                            Toast.LENGTH_LONG).show();
                }else {
                    count = 0;
                    System.exit(0);
                }
            }
        };
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        //Callback listener when dialog dismiss
        //Thank: https://stackoverflow.com/questions/23786033/dialogfragment-and-ondismiss

        //final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener){
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }

    }

    public void setButtonListener(OnClickActionButtonListener listener){
        buttonListener = listener;
    }


    interface OnClickActionButtonListener{
        void onClick(View view, int titleId, int buttonId);
    }



}
