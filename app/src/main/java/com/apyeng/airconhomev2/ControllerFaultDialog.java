package com.apyeng.airconhomev2;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class ControllerFaultDialog extends DialogFragment {

    public static final String TAG = "ControllerFaultDialog"; //Unique TAG
    public static final String CODE = "Code";
    private Context context;
    private String title = "Normal", detail = "";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        int code = bundle.getInt(CODE, 0);
        if(code!=0){
            if(code<100){
              //Set title
              title = "Fault: od"+get2Digit(code);
              //Set detail
              int a = code-1;
              String buff[] = context.getResources().getStringArray(R.array.outdoor_fault_description);
              if(a<buff.length){
                  detail = buff[a];
              }else {
                  detail = "Unknown the details";
              }

            }else {
              //Set title
              title = "Fault: id"+get2Digit(code-100);
              //Set detail
              int a = code-101;
              String buff[] = context.getResources().getStringArray(R.array.indoor_fault_description);
              if(a<buff.length){
                  detail = buff[a];
              }else {
                  detail = "Unknown the details";
              }
            }
        }

    }

    private String get2Digit(int number){
        if (number<10){
            return "0"+ String.valueOf(number);
        }else {
            return String.valueOf(number);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.controller_fault_dialog, container, false);

        view.findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        TextView titleText = view.findViewById(R.id.fault_code);
        titleText.setText(title);

        TextView detailText = view.findViewById(R.id.fault_detail);
        detailText.setText(detail);

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



}
