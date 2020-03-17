package com.apyeng.airconhomev2;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;

public class LimitCharOnEditText implements TextWatcher {

    private TextView progressText = null;
    private int maxChar;
    private OnAfterChanged changed = null;
    private final static String TAG = "LimitChar";

    public LimitCharOnEditText(int maxChar, OnAfterChanged changed){
        this.maxChar = maxChar;
        this.changed = changed;
    }

    public LimitCharOnEditText(int maxChar, TextView progressText){
        this.maxChar = maxChar;
        this.progressText = progressText;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.e(TAG, "After: "+s.length()+" Text: "+s.toString());
        int currentMax = s.length();
        if (currentMax>maxChar){ s.delete(currentMax-1, currentMax); }  //Limit number of char
        //Update current char count on editText
        if (changed!=null){ changed.onChanged(s.length()); }
        //Set on ProgressText
        if (progressText!=null){
            //ex. 250/400
            String p = String.valueOf(s.length())+"/"+ String.valueOf(maxChar);
            progressText.setText(p);
        }

    }

    interface OnAfterChanged{
        void onChanged(int count);
    }
}
