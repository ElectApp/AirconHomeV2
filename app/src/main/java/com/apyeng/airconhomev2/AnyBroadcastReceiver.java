package com.apyeng.airconhomev2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AnyBroadcastReceiver extends BroadcastReceiver {

    private OnCallback callback;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action==null){return;}
        if(callback!=null){
            callback.onReceive(context, intent);
        }
    }

    public void setOnCallback(OnCallback callback){
        this.callback = callback;
    }

    public interface OnCallback{
        void onReceive(Context context, Intent intent);
    }

}
