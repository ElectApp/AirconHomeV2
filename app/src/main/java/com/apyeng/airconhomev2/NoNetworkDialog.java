package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

public class NoNetworkDialog extends DialogFragment {

    private boolean isNoInternet; //False = No AC connected, True = No Internet
    public static final String TAG = "NoNetworkDialog"; //Unique TAG


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        //Get data
        Bundle bundle = getArguments();
        isNoInternet = bundle.getBoolean(Constant.NO_NETWORK, true);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_no_network, container, false);

        TextView title = view.findViewById(R.id.no_network_title);
        TextView detail = view.findViewById(R.id.no_network_detail);
        RoundButtonWidget action = view.findViewById(R.id.action_btn);

        if (!isNoInternet){
            title.setText(R.string.no_ac_connected);
            detail.setText(R.string.connect_wifi_note);
            action.setText(R.string.connect_wifi);
        }else {
            title.setText(R.string.no_internet);
            detail.setText(R.string.no_internet_detail);
            action.setText(R.string.retry);
        }

        action.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Check action
                if (isNoInternet){
                    //Recheck internet
                    if (Function.internetConnected(getDialog().getContext())){
                        dismiss();
                    }
                }else {
                    //Connect AC
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
                }
            }
        });

        return view;

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set animation here!
        //Thank: https://stackoverflow.com/questions/41869348/enter-and-exit-animations-not-working-in-dialog-fragment
        Dialog dialog = getDialog();
        if(dialog!=null){
            Window window = dialog.getWindow();
            if (window!=null){
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
            }
        }
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


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        //Callback listener when dialog dismiss
        //Thank: https://stackoverflow.com/questions/23786033/dialogfragment-and-ondismiss

        final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener){
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }

    }



}
