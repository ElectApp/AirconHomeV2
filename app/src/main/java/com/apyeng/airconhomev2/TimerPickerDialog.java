package com.apyeng.airconhomev2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.app.DialogFragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class TimerPickerDialog extends DialogFragment {

    private Context context;
    private int timerType;
    private TimePicker picker;
    private OnSaveTimeListener listener;
    public static final int TIMER_ON = 1, TIMER_OFF = 2;
    public static final String TYPE = "type";
    public static final String TAG = "TimerPickerDialog";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle==null){
            throw new IllegalArgumentException("Must pass Timer Type");
        }

        //Set view
        View view = inflater.inflate(R.layout.dialog_timer_picker, container, false);

        TextView title = view.findViewById(R.id.timer_title);
        picker = view.findViewById(R.id.time_picker);

        //Set type
        timerType = bundle.getInt(TYPE, 0);
        if (timerType==0){
            throw new IllegalArgumentException("Must pass Timer Type");
        }

        String t = getResources().getString(timerType==TIMER_ON?
                R.string.set_timer_on:R.string.set_timer_off);
        title.setText(t);

        picker.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            picker.setHour(0); picker.setMinute(10);
        }else {
            picker.setCurrentHour(0); picker.setCurrentMinute(10);
        }

        //Close
        view.findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        //OK
        view.findViewById(R.id.ok_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Get time setting
                int hour, minute;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hour = picker.getHour(); minute = picker.getMinute();
                }else {
                    hour = picker.getCurrentHour(); minute = picker.getCurrentMinute();
                }
                //Check time
                if(hour==0 && minute==0){
                    //Alert
                    Toast.makeText(context, R.string.no_set_time, Toast.LENGTH_SHORT).show();
                }else {
                    //Callback
                    if(listener!=null){ listener.onSave(timerType, hour, minute); }
                    //Closed dialog
                    dismiss();
                }

            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
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

    public void addOnSaveTimeListener(OnSaveTimeListener listener){
        this.listener = listener;
    }

    interface OnSaveTimeListener{
        void onSave(int type, int hour, int minute);
    }
}
