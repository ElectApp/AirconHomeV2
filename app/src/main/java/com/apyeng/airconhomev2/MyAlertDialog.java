package com.apyeng.airconhomev2;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MyAlertDialog extends DialogFragment {

    private int titleId, detailId, btnId, vObj;
    private String detailTxt="", titleTxt="";
    private OnButtonClickListener clickListener;
    public static final String BUTTON_ID = "button-id", TITLE_ID = "title-id", TITLE_TXT = "title-txt",
            DETAIL_ID = "detail-id", DETAIL_TXT = "detail-txt", VISIBLE_OBJ = "visible-object";
    public static final int DETAIL_ONLY = 0, FILL_DATA_ONLY = 1, FILL_PASSWORD_ONLY = 2, ALL = 3;
    public static final String TAG = "MyAlertDialog"; //Unique TAG

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set theme
        setStyle(DialogFragment.STYLE_NORMAL, R.style.GeneralDialog);

        //Get data
        Bundle bundle = getArguments();
        titleId = bundle.getInt(TITLE_ID, R.string.error);
        titleTxt = bundle.getString(TITLE_TXT);
        detailId = bundle.getInt(DETAIL_ID, 0);
        detailTxt = bundle.getString(DETAIL_TXT);
        btnId = bundle.getInt(BUTTON_ID, R.string.ok);
        vObj = bundle.getInt(VISIBLE_OBJ, DETAIL_ONLY);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //Set view
        View view = inflater.inflate(R.layout.dialog_alert, container, false);

        TextView title = view.findViewById(R.id.alert_title);
        TextView detail = view.findViewById(R.id.alert_detail);
        final ClearableEditText eData = view.findViewById(R.id.enter_data);
        final PasswordEditText ePass = view.findViewById(R.id.enter_password);
        TextView btn = view.findViewById(R.id.alert_btn);

        if(titleId!=0){
            title.setText(titleId);
        }else {
            title.setText(titleTxt);
        }

        switch (vObj){
            case DETAIL_ONLY:
                if (detailId!=0){
                    detail.setText(detailId);
                }else {
                    detail.setText(detailTxt);
                }
                ePass.setVisibility(View.GONE);
                break;

            case FILL_DATA_ONLY:
                detail.setVisibility(View.GONE);
                ePass.setVisibility(View.GONE);
                break;

            case FILL_PASSWORD_ONLY:
                detail.setVisibility(View.GONE);
                eData.setVisibility(View.GONE);
                break;

            case ALL:
                if (detailId!=0){
                    detail.setText(detailId);
                }else {
                    detail.setText(detailTxt);
                }
                break;
        }

        btn.setText(btnId);

        view.findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (clickListener!=null){
                    clickListener.onClose();
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // dismiss();
                if (clickListener!=null){
                    clickListener.onAction(String.valueOf(eData.getText()), String.valueOf(ePass.getText()));
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
    public void onResume() {
        // Store access variables for window and blank point
        Window window = getDialog().getWindow();
        Point size = new Point();
        if (window!=null){
            // Store dimensions of the screen in `size`
            Display display = window.getWindowManager().getDefaultDisplay();
            display.getSize(size);
            // Set the width of the dialog proportional to 95% of the screen width
            // Thank: https://guides.codepath.com/android/using-dialogfragment
            window.setLayout((int) (size.x * 0.95), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            //Set color TRANSPARENT to outside round layout of dialog
            //Thank: https://www.codingdemos.com/android-custom-dialog-animation/
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        super.onResume();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (clickListener!=null){
            clickListener.onClose();
        }
    }

    public void setClickListener(OnButtonClickListener clickListener){
        this.clickListener = clickListener;
    }

    interface OnButtonClickListener{
        void onClose();
        void onAction(String data, String password);
    }


}
