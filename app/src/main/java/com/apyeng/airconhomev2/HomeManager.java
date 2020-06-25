package com.apyeng.airconhomev2;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeManager {

    private Context context;
    private Activity activity;
    private OnSetHomeListener onSetHomeListener;
    private OnSignInListener onSignInListener;
    private OnReadDeviceIdListener onReadDeviceIdListener;
    private static final String TAG = "HomeManager";

    public HomeManager(Context context){
        this.context = context;
    }

    public HomeManager(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
    }

    //In case create new home, groupId = 0
    public void setHome(int userId, int groupId, String homeName, String location, @NonNull OnSetHomeListener setHomeListener){
        //Check latLong
        if (location==null){ location = ""; }
        //Display progress
        Function.showLoadingDialog(activity);
        //Set listener
        onSetHomeListener = setHomeListener;
        //Run MySQL via PHP script
        Ion.with(context).load(Constant.ADD_EDIT_GROUP_URL)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.NAME, homeName)
                .setBodyParameter(Constant.LOCATION, location)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        //Close loading
                        Function.dismissDialogFragment(activity, PageLoadingDialog.TAG);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            //Check response
                            if (result.startsWith(Constant.SUCCESS)){
                                //Success
                                String id = result.substring(Constant.SUCCESS.length());
                                onSetHomeListener.onSuccess(Function.getInt(id));
                            }else {
                                //Failed
                                onSetHomeListener.onFailed(result);
                            }
                        }else if (e!=null){
                            //Show error
                            onSetHomeListener.onFailed(e.getMessage());
                        }else {
                            //No result
                            onSetHomeListener.onFailed(context.getResources().getString(R.string.no_result));
                        }
                    }
                });
    }

    interface OnSetHomeListener{
        void onSuccess(int groupId);
        void onFailed(String error);
    }


    public void signInWithUserId(final int userId, int statusFlag, @NonNull OnSignInListener listener){
        //Display progress
        Function.showLoadingDialog(activity);
        //Set listener
        onSignInListener = listener;
        //Run MySQL via PHP script
        Ion.with(context)
                .load(Constant.ID_SIGN_IN_URL)
                .setBodyParameter(Constant.LANGUAGE, Function.getLanguage())
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.STATUS, String.valueOf(statusFlag))
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //Close
                        Function.dismissDialogFragment(activity, PageLoadingDialog.TAG);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isJsonNull()){
                            //Check flag
                            if (result.get(Constant.SUCCESS).getAsBoolean()){
                                //Set data
                                String username = result.get(Constant.USERNAME).getAsString();
                                String email =result.get(Constant.EMAIL).getAsString();
                                String img = result.get(Constant.PROFILE_IMG).getAsString();
                                String registered = result.get(Constant.REGISTERED).getAsString();
                                //Convert Array string to int array
                                String buff = result.get(Constant.GROUP_ID).getAsString();
                                int groupId[] = null;
                                if (!buff.isEmpty()){ //Check no found group
                                    groupId = Function.getIntArray(buff);
                                }
                                /*
                                String buff2[] = buff.split(",");
                                int groupId[] = new int[buff2.length];
                                for (int i=0; i<groupId.length; i++){
                                    //Should use try catch for convert String to int
                                    groupId[i] = Function.getInt(buff2[i].trim());
                                }*/
                                //Set success
                                onSignInListener.onSuccess(
                                        new UserItem(userId, username, email, img, registered), groupId);
                            }else {
                                //Show error
                                onSignInListener.onFailed(result.get(Constant.CAUSE).getAsString());
                            }
                        }else if (e!=null){
                            onSignInListener.onFailed(e.getMessage());
                        }else {
                            onSignInListener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });

    }

    interface OnSignInListener{
        void onSuccess(UserItem userData, int groupId[]);
        void onFailed(String error);
    }

    public void readDeviceId(final int groupId[], @NonNull OnReadDeviceIdListener listener){
        Log.w(TAG, "Group: "+Function.toString(groupId));
        //Set listener
        onReadDeviceIdListener = listener;
        //Show loading
        Function.showLoadingDialog(activity);
        //Run MySQL via PHP script
        Ion.with(context)
                .load(Constant.GROUP_DEVICE_LIST)
                .setBodyParameter(Constant.GROUP_ID_LIST, Function.toString(groupId))
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray result) {
                        //Close loading
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if(result!=null && result.isJsonArray()){
                            List<HomeItem> items = new ArrayList<>();
                            for (int i=0; i<result.size(); i++){
                                JsonObject object = result.get(i).getAsJsonObject();
                                String error = object.get(Constant.ERROR).getAsString();
                                if (error.isEmpty()){
                                    int groupId = object.get(Constant.GROUP_ID).getAsInt();
                                    String name = object.get(Constant.NAME).getAsString();
                                    String profile = object.get(Constant.PROFILE_IMG).getAsString();
                                    String deviceId = object.get(Constant.DEVICE_ID).getAsString();
                                    String time = object.get(Constant.REGISTERED).getAsString();
                                    int dId[] = Function.getIntArray(deviceId);
                                    Log.w(TAG, "dId: "+ Arrays.toString(dId));
                                    //if (dId[0]==0){ dId = null; }
                                    HomeItem home = new HomeItem(groupId, name, profile, dId, time);
                                    items.add(home);
                                }
                            }
                            //Callback
                            onReadDeviceIdListener.onSuccess(items);
                        }else if (e!=null){
                            onReadDeviceIdListener.onFailed(e.getMessage());
                        }else {
                            onReadDeviceIdListener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });

    }


    interface OnReadDeviceIdListener{
        void onSuccess(List<HomeItem> homeItems);
        void onFailed(String error);
    }


    public void readFullDeviceDetail(final int groupId, final OnReadFullDeviceDetailListener listener){
        Log.w(TAG, "Read device full detail in group: "+groupId);
        //Loading
        Function.showLoadingDialog(activity);
        //Try read
        Ion.with(context)
                .load(Constant.DEVICE_LIST)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray result) {
                        //Close loading
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && result.isJsonArray()){
                            List<AirItem> airItems = new ArrayList<>();
                            for (int i=0; i<result.size(); i++){
                                JsonObject object = result.get(i).getAsJsonObject();
                                if (!object.has(Constant.ERROR)){
                                    int deviceId = object.get(Constant.DEVICE_ID).getAsInt();
                                    String name = object.get(Constant.ACTUAL_NAME).getAsString();
                                    String nickname = object.get(Constant.NICKNAME).getAsString();
                                    airItems.add(new AirItem(deviceId, name, nickname, null));
                                }
                            }
                            listener.onSuccess(airItems);
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    interface OnReadFullDeviceDetailListener{
        void onSuccess(List<AirItem> airItems);
        void onFailed(String error);
    }


    public void joinGroup(int userId, int groupId, @NonNull final OnListener listener){
        Log.w(TAG, "Join group ID: "+groupId+" by user ID: "+userId);
        //Loading
        Function.showLoadingDialog(activity);
        //Try join
        Ion.with(context)
                .load(Constant.JOIN_GROUP_URL)
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null){
                            if (result.equals(Constant.SUCCESS)){
                                listener.onSuccess();
                            }else {
                                listener.onFailed(result);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    interface OnListener{
        void onSuccess();
        void onFailed(String error);
    }

    public void uploadImage(String name, File fileToUpload, final OnUploadListener listener){
        //Name must set format G-x or U-x (G, U use for selection directory and x is unique code)
        Log.w(TAG, "Upload: "+name+" to server");
        //Loading
        Function.showLoadingDialog(activity);
        //Try upload
        Ion.with(context)
                .load(Constant.UPLOAD_IMG_URL)
                .setTimeout(2*60*1000)
                .setMultipartParameter(Constant.NAME, name)
                .setMultipartFile("pictures", "image/*", fileToUpload)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            if (result.startsWith(Constant.SUCCESS)){
                                int len = Constant.SUCCESS.length();
                                String fullPath = result.substring(len+1);
                                String fileName = FileHelder.getFileName(fullPath);
                                listener.onSuccess(fullPath, fileName);
                            }else {
                                listener.onFailed(result);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }

                    }
                });
    }

    interface OnUploadListener{
        void onSuccess(String fullPath, String fileName);
        void onFailed(String error);
    }

    public void leaveGroup(final int groupId, int userId, final OnSetHomeListener listener){
        Log.w(TAG, "Delete user ID "+userId+" from group ID "+groupId);
        //Loading
        Function.showLoadingDialog(activity);
        //Try
        Ion.with(context)
                .load(Constant.LEAVE_GROUP_URL)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            if (result.equals(Constant.SUCCESS)){
                                listener.onSuccess(groupId);
                            }else {
                                listener.onFailed(result);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }


    public void readMemberList(int userId, int groupId, final OnReadMemberListener listener){
        Log.w(TAG, "Read member list from "+groupId);
        Function.showLoadingDialog(activity);
        //Try
        Ion.with(context)
                .load(Constant.MEMBER_LIST_URL)
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray result) {
                        Function.dismissLoadingDialog(activity);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null){
                            List<MemberItem> items = new ArrayList<>();
                            String err = "";
                            for (int i=0; i<result.size(); i++){
                                JsonObject object = result.get(i).getAsJsonObject();
                                if (object.has(Constant.ERROR)){
                                    String b = object.get(Constant.ERROR).getAsString();
                                    if (!b.equals(Constant.NO_LIST)){ err = b; }
                                    break;
                                }else {
                                    String username = object.get(Constant.USERNAME).getAsString();
                                    int userId = object.get(Constant.USER_ID).getAsInt();
                                    String imgName = object.get(Constant.PROFILE_IMG).getAsString();
                                    String time = object.get(Constant.REGISTERED).getAsString();
                                    items.add(new MemberItem(userId, username, imgName, time));
                                }
                            }
                            if (err.isEmpty()){
                                listener.onSuccess(items);
                            }else {
                                listener.onFailed(err);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });

    }

    interface OnReadMemberListener{
        void onSuccess(List<MemberItem> memberItems);
        void onFailed(String error);
    }

    public void updateUserData(int userId, String username, final OnListener listener){
        Log.w(TAG, "Update user ID "+userId);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.EDIT_USER_DATA_URL)
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.USERNAME, username)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            if (result.equals(Constant.SUCCESS)){
                                listener.onSuccess();
                            }else {
                                listener.onFailed(result);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    public void updateRegisteredTime(int userId, int groupId, final OnListener listener){
        Log.w(TAG, "Update register time of user ID"+userId);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.UPDATE_TIME_USER_GROUP_URL)
                .setBodyParameter(Constant.USER_ID, String.valueOf(userId))
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            if (result.equals(Constant.SUCCESS)){
                                listener.onSuccess();
                            }else {
                                listener.onFailed(result);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });

    }


    //Keep date format
    public void readDeviceLogData(final int groupId, int deviceId, String date, final OnLogDataListener listener){
        Log.w(TAG, "read logging data of device ID: "+deviceId+" At "+date);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.DEVICE_LOG_DATA_URL)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.DEVICE_ID, String.valueOf(deviceId))
                .setBodyParameter(Constant.DATE, date)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        //Check result
                        if (result!=null){
                            if (result.has(Constant.ERROR)){
                                //Error
                                String err = result.get(Constant.ERROR).getAsString();
                                listener.onFailed(err);
                            }else {
                                //int group = result.get(Constant.GROUP_ID).getAsInt();
                                //int device = result.get(Constant.DEVICE_ID).getAsInt();
                                List<ChartItem> items = new ArrayList<>();
                                float sumEachId[] = new float[2]; //0=Ac, 1=PV
                                float total = 0, maxAtTime = 0;
                                try {
                                    // Can't use
                                    // JsonArray jsonArray = result.get(Constant.LOG_DATA).getAsJsonArray();
                                    // Due to result.get(Constant.LOG_DATA) is send format EX. below
                                    // "data":"[{\"device_id\":\"6\",\"pv_wh\":\"0\"},{\"device_id\":\"7\",\"pv_wh\":\"7916\"}]"}
                                    // It is not json google format
                                    String data = result.get(Constant.LOG_DATA).getAsString();
                                    JSONArray array = new JSONArray(data);
                                    int len = array.length();
                                    for (int i=0; i<len; i++){
                                        float values[] = new float[2];
                                        JSONObject object = array.getJSONObject(i);
                                        String time = object.getString(Constant.TIME); //Input: hh:mm:ss
                                        values[0] = (float) object.getDouble(Constant.AC_POWER);
                                        values[1] = (float) object.getDouble(Constant.PV_POWER);
                                        //Log.w(TAG, "Time: "+time+", AC: "+values[0]+", PV: "+values[1]);
                                        //Set cut only hh:mm
                                        time = time.substring(0, time.length()-3);
                                        //Sum each ID
                                        sumEachId[0] += values[0];
                                        sumEachId[1] += values[1];
                                        //Sum total
                                        total += values[0]+values[1];
                                        items.add(new ChartItem(time, values));
                                        //Find max
                                        if (values[0]>maxAtTime){ maxAtTime = values[0]; }
                                        if (values[1]>maxAtTime){ maxAtTime = values[1]; }
                                    }
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                                //Callback
                                listener.onSuccess(sumEachId, total, maxAtTime, items);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    interface OnLogDataListener{
        void onSuccess(float sumEachId[], float total, float maxValueAtTime, List<ChartItem> chartItems);
        void onFailed(String error);
    }

    //Keep date format
    public void readDeviceLogAdvanceData(final int groupId, int deviceId, String date,
                                         final String[] key, final OnLogAdvanceDataListener listener){
        //Set key list
        String keyList = Arrays.toString(key);
        keyList = keyList.replace("[", "");
        keyList = keyList.replace("]", "");
        Log.w(TAG, "read logging advance data of device ID: "+deviceId+" At "+date+" By "+keyList);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.DEVICE_LOG_ADVANCE_DATA_URL)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.DEVICE_ID, String.valueOf(deviceId))
                .setBodyParameter(Constant.DATE, date)
                .setBodyParameter(Constant.KEY_LIST, keyList)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        //Log.w(TAG, "Result: "+result);
                        //Check result
                        if (result!=null){
                            if (result.has(Constant.ERROR)){
                                //Error
                                String err = result.get(Constant.ERROR).getAsString();
                                listener.onFailed(err);
                            }else {
                                //int group = result.get(Constant.GROUP_ID).getAsInt();
                                //int device = result.get(Constant.DEVICE_ID).getAsInt();
                                List<ChartItem> items = new ArrayList<>();
                                try {
                                    // Can't use
                                    // JsonArray jsonArray = result.get(Constant.LOG_DATA).getAsJsonArray();
                                    // Due to result.get(Constant.LOG_DATA) is send format EX. below
                                    // "data":"[{\"device_id\":\"6\",\"pv_wh\":\"0\"},{\"device_id\":\"7\",\"pv_wh\":\"7916\"}]"}
                                    // It is not json google format
                                    String data = result.get(Constant.LOG_DATA).getAsString();
                                    JSONArray array = new JSONArray(data);
                                    final int len = array.length();
                                    final int keyLen = key.length;
                                    for (int i=0; i<len; i++){
                                        float values[] = new float[keyLen];
                                        JSONObject object = array.getJSONObject(i);
                                        String time = object.getString(Constant.TIME); //Input: hh:mm:ss
                                        //Set cut only hh:mm
                                        time = time.substring(0, time.length()-3);
                                        //Data
                                        for (int x=0; x<keyLen; x++){
                                            values[x] = (float) object.getDouble(key[x]);
                                            Log.w(TAG, time+" "+Arrays.toString(values));
                                        }
                                        //Add list
                                        items.add(new ChartItem(time, values));
                                    }
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                                //Callback
                                listener.onSuccess(items);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    interface OnLogAdvanceDataListener{
        void onSuccess(List<ChartItem> chartItems);
        void onFailed(String error);
    }

    //Date keep format: yyyy-MM-dd
    public void readDateRangLogData(int groupId, final SparseIntArray deviceArr, String startDate, String endDate, final OnLogDataListener listener){
        Log.w(TAG, "Read daily log data from: "+startDate+" to "+endDate+" At GroupID: "+groupId);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.DAILY_LOG_DATA)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.START_DATE, startDate)
                .setBodyParameter(Constant.END_DATE, endDate)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null){
                            if (result.has(Constant.ERROR)) {
                                //Error
                                String err = result.get(Constant.ERROR).getAsString();
                                //Callback
                                listener.onFailed(err);
                            }else {
                                // result.get(Constant.LOG_DATA) must keep format EX. below
                                // [{"device_id":"6","actual_name":"AC-E68F"},{"device_id":"7","actual_name":"AC-DB73"}]
                                // So, it can use JsonArray jsonArray = result.get(Constant.LOG_DATA).getAsJsonArray();
                                JsonArray jsonArray = result.get(Constant.LOG_DATA).getAsJsonArray();
                                List<ChartItem> chartItems = new ArrayList<>();
                                float sumEachId[] = new float[deviceArr.size()];
                                float total = 0, sumMaxAtTime = 0;
                                for(int i=0; i<jsonArray.size(); i++){
                                    JsonObject object = jsonArray.get(i).getAsJsonObject();
                                    String time = object.get(Constant.TIME).getAsString();
                                    // Due to object.get(Constant.DATA) sent format
                                    // "data":"[{\"device_id\":\"6\",\"pv_wh\":\"0\"},{\"device_id\":\"7\",\"pv_wh\":\"7916\"}]"}
                                    // So, can't use JsonArray jsonArray = object.get(Constant.DATA).getAsJsonArray();
                                    String data = object.get(Constant.DATA).getAsString();
                                    try {
                                        JSONArray dataArray = new JSONArray(data);
                                        int len = dataArray.length();
                                        //Log.w(TAG, "DATA Array: "+dataArray+", size: "+len);
                                        float point[] = new float[deviceArr.size()];
                                        float sum = 0;
                                        for (int n=0; n<len; n++){
                                            //Get deviceID
                                            JSONObject object2 = dataArray.getJSONObject(n);
                                            int dId = object2.getInt(Constant.DEVICE_ID);
                                            //Get index from id value
                                            int p = deviceArr.indexOfValue(dId);
                                            //Check index
                                            if (p>-1 && p<point.length){
                                                //Log.w(TAG, "Add value to index: "+p);
                                                point[p] = (float) object2.getDouble(Constant.PV_WH);
                                                //Sum value
                                                sumEachId[p] += point[p];
                                                //Sum total
                                                total += point[p];
                                                //Sum in same time
                                                sum += point[p];
                                            }
                                        }
                                        //Add result list
                                        chartItems.add(new ChartItem(time, point));
                                        //Find max
                                        if (sum>sumMaxAtTime){
                                            sumMaxAtTime = sum;
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }

                                }
                                //Add success
                                listener.onSuccess(sumEachId, total, sumMaxAtTime, chartItems);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }

                    }
                });
    }

    //Keep UK format: yyyy-mm
    public void readMonthRangLogData(int groupId, final SparseIntArray deviceArr, String startMonth, String endMonth, final OnLogDataListener listener){
        Log.w(TAG, "Read Monthly log data At "+groupId+" from "+startMonth+" to "+endMonth);
        Function.showLoadingDialog(activity);
        //Do
        Ion.with(context)
                .load(Constant.MONTHLY_LOG_DATA)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.START_MONTH, startMonth)
                .setBodyParameter(Constant.END_MONTH, endMonth)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        Function.dismissLoadingDialog(activity);
                        //Check data
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null){
                            //Check no list error
                            if (result.has(Constant.ERROR)) {
                                //Error
                                String err = result.get(Constant.ERROR).getAsString();
                                //Callback
                                listener.onFailed(err);
                            }else {
                                JsonArray jsonArray = result.get(Constant.LOG_DATA).getAsJsonArray();
                                List<ChartItem> chartItems = new ArrayList<>();
                                float sumEachId[] = new float[deviceArr.size()];
                                float total = 0, sumMaxAtTime = 0;
                                for (int i=0; i<jsonArray.size(); i++){
                                    JsonObject object = jsonArray.get(i).getAsJsonObject();
                                    String time = object.get(Constant.TIME).getAsString();
                                    JsonArray data = object.get(Constant.DATA).getAsJsonArray();
                                    float point[] = new float[deviceArr.size()];
                                    float sum = 0;
                                    for (int n=0; n<data.size(); n++){
                                        JsonObject object1 = data.get(n).getAsJsonObject();
                                        //Get device id
                                        int id = object1.get(Constant.DEVICE_ID).getAsInt();
                                        //Get position in device array
                                        int p = deviceArr.indexOfValue(id);
                                        if (p>-1 && p<point.length){
                                            //Log.w(TAG, "Set value to index: "+p);
                                            //Set value
                                            point[p] = object1.get(Constant.PV_WH).getAsFloat();
                                            //Sum value
                                            sumEachId[p] += point[p];
                                            //Sum total
                                            total += point[p];
                                            //Sum in same time
                                            sum += point[p];
                                        }
                                    }
                                    //Add result list
                                    chartItems.add(new ChartItem(time, point));
                                    //Find max
                                    if (sum>sumMaxAtTime){
                                        sumMaxAtTime = sum;
                                    }
                                }
                                //Add success
                                listener.onSuccess(sumEachId, total, sumMaxAtTime, chartItems);
                            }
                        }else if (e!=null){
                            listener.onFailed(e.getMessage());
                        }else {
                            listener.onFailed(context.getString(R.string.no_result));
                        }
                    }
                });
    }

    public void insertUpdateAnyGroupTable(final int groupId, final String sqlMessage, final OnSingleStringCallback callback){
        Ion.with(context)
                .load(Constant.INSERT_UPDATE_ANY_GROUP_TABLE)
                .setBodyParameter(Constant.GROUP_ID, String.valueOf(groupId))
                .setBodyParameter(Constant.SQL_MESSAGE, sqlMessage)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null){
                            //success<last-id>
                            if (result.startsWith(Constant.SUCCESS)){
                                //Callback lastId
                                callback.onSuccess(result.substring(7));
                            }else {
                                callback.onFailed(result);
                            }
                        }else if(e!=null && !e.getMessage().isEmpty()){
                            callback.onFailed(e.getMessage());
                        }else {
                            callback.onFailed(context.getString(R.string.no_result));
                        }

                    }
                });
    }

    public interface OnSingleStringCallback{
        void onSuccess(String value);
        void onFailed(String error);
    }



}
