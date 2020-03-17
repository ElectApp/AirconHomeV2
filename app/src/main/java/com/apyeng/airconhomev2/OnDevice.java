package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.SharedPreferences;

public class OnDevice {

    public static final String USER_FILE = "user_file";
    public static final String NETWORK_FILE = "network_file";
    private SharedPreferences preferences;

    public OnDevice(Context context){

    }

    public OnDevice(Context context, String fileName){
        preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public void saveSignIn(String email, String password){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.EMAIL, email);
        editor.putString(Constant.PASSWORD, password);
        editor.apply();
    }

    public String[] getSignIn(){
        String email = preferences.getString(Constant.EMAIL, null);
        String password = preferences.getString(Constant.PASSWORD, null);
        return new String[]{ email, password };
    }

    public void saveUserId(int id){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constant.USER_ID, id);
        editor.apply();
    }

    public void saveUsername(String name){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.USERNAME, name);
        editor.apply();
    }

    public String getUsername(){
        return preferences.getString(Constant.USERNAME, null);
    }

    public int getUserId(){
        return preferences.getInt(Constant.USER_ID, 0);
    }


    public String getWiFiPassword(String name){
        return preferences.getString(name, "");
    }

    public void saveWiFiPassword(String name, String password){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(name, password);
        editor.apply();
    }


}
