package com.apyeng.airconhomev2;

import android.app.DialogFragment;
import android.content.Context;
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
import android.widget.Toast;

import com.applikeysolutions.cosmocalendar.dialog.CalendarDialog;
import com.applikeysolutions.cosmocalendar.dialog.OnDaysSelectionListener;
import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.selection.RangeSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.SingleSelectionManager;
import com.applikeysolutions.cosmocalendar.utils.SelectionType;
import com.applikeysolutions.cosmocalendar.view.CalendarView;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatePickerDialog extends DialogFragment {

    private CalendarView calendarView;
    private String startDate, endDate;;
    public static final String TAG = "DatePickerDialog";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_date_picker, container, false);

        calendarView = view.findViewById(R.id.date_picker);

        view.findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        view.findViewById(R.id.ok_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Callback date picked and dismiss dialog

                if (calendarView.getSelectionManager() instanceof RangeSelectionManager) {
                    RangeSelectionManager rangeSelectionManager = (RangeSelectionManager) calendarView.getSelectionManager();
                    if(rangeSelectionManager.getDays() != null) {
                        startDate = String.valueOf(rangeSelectionManager.getDays().first);
                        endDate = String.valueOf(rangeSelectionManager.getDays().second);
                    } else {
                        Toast.makeText(context(), "Invalid Selection", Toast.LENGTH_SHORT).show();
                    }
                }

                List<Day> days = calendarView.getSelectedDays();

                if (days!=null){
                    for (Day d : days){
                        Log.w(TAG, "Selected: "+d.getDayNumber());
                    }
                }


                dismiss();


            }
        });

        //============ Initial CalendarView =============//


        //Set First day of the week
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);
        //Set Orientation 0 = Horizontal | 1 = Vertical
        calendarView.setCalendarOrientation(0);

        //Set type
        calendarView.setSelectionType(SelectionType.RANGE);


        return view;

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

    private Context context(){
        return getActivity().getBaseContext();
    }


}
