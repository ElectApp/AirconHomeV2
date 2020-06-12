package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.apyeng.airconhomev2.Interfaces.StringCallback;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;
import static com.android.volley.Request.Method.GET;

public class Function {


    //Change background color of EditText and vibrate device for request enter data
    public static void setRequestEnter(Context context, @NonNull EditText editText){
        Function.setOneShortVibrator(context);
        editText.setSelected(true);
    }

    //Set device vibrator
    public static void setOneShortVibrator(Context context){
        //Thank: https://stackoverflow.com/questions/13950338/how-to-make-an-android-device-vibrate
        Vibrator v = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(300);
            }
        }
    }


    //Get current device language
    public static String getLanguage(){
        return Locale.getDefault().getDisplayLanguage();
    }

    //Get int from String
    public static int getInt(String value){
        try {
            return Integer.valueOf(value.trim());
        }catch (NumberFormatException e){
            e.printStackTrace();
            return -1;
        }
    }

    //Convert message ex. 123,12,12,35 to int array
    public static int[] getIntArray(String message){
        String d[] = message.split(",");
        int size = d.length;
        if (size<1){
            return new int[]{ getInt(message) };
        }
        int num[] = new int[size];
        for (int i=0; i<d.length; i++){
            num[i] = getInt(d[i]);
        }
        return num;
    }

    public static String toString(int num[]){
        StringBuilder builder = new StringBuilder("");
        for (int i=0; i<num.length; i++){
            builder.append(num[i]);
            if (i<num.length-1){
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static void showLoadingDialog(Activity activity){
        if (activity!=null){
            PageLoadingDialog dialog = new PageLoadingDialog();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            dialog.show(transaction, PageLoadingDialog.TAG);
        }
    }

    public static void dismissLoadingDialog(Activity activity){
        dismissDialogFragment(activity, PageLoadingDialog.TAG);
    }

    public static void showNoNetworkDialog(Activity activity, boolean isNoInternet){
        if (activity!=null){
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constant.NO_NETWORK, isNoInternet);
            NoNetworkDialog dialog = new NoNetworkDialog();
            dialog.setArguments(bundle);
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            dialog.show(transaction, NoNetworkDialog.TAG);
        }
    }

    public static void showNoResultDialog(
            @NonNull Activity activity,
            @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        //Set content
        Bundle bundle = new Bundle();
        bundle.putInt(AppErrorDialog.TITLE_ID, R.string.no_result);
        bundle.putInt(AppErrorDialog.DETAIL_ID, R.string.no_result_detail);
        bundle.putInt(AppErrorDialog.BUTTON_ID, R.string.retry);
        //Show
        showAppErrorDialog(activity, bundle).setButtonListener(listener);

    }

    public static void showNoInternetDialog(
            @NonNull Activity activity,
            @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        //Set content
        Bundle bundle = new Bundle();
        bundle.putInt(AppErrorDialog.TITLE_ID, R.string.no_internet);
        bundle.putInt(AppErrorDialog.DETAIL_ID, R.string.no_internet_detail);
        bundle.putInt(AppErrorDialog.BUTTON_ID, R.string.retry);
        //Show
        showAppErrorDialog(activity, bundle).setButtonListener(listener);
    }

    public static void showUploadImageError(
            @NonNull Activity activity,
            @NonNull String error,
            @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        //Set content
        Bundle bundle = new Bundle();
        bundle.putInt(AppErrorDialog.TITLE_ID, R.string.upload_failed_title);
        bundle.putString(AppErrorDialog.DETAIL_TXT, error);
        bundle.putInt(AppErrorDialog.BUTTON_ID, R.string.retry);
        //Show
        showAppErrorDialog(activity, bundle).setButtonListener(listener);
    }

    public static void showAppErrorDialog(Activity activity, int titleID, int detailID, int buttonID,
                                          @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        //Set content
        Bundle bundle = new Bundle();
        bundle.putInt(AppErrorDialog.TITLE_ID, titleID);
        bundle.putInt(AppErrorDialog.DETAIL_ID, detailID);
        bundle.putInt(AppErrorDialog.BUTTON_ID, buttonID);
        //Show
        showAppErrorDialog(activity, bundle).setButtonListener(listener);
    }

    public static void showAppErrorDialog(Activity activity, int titleID, String detailTxt, int buttonID,
                                          @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        //Set content
        Bundle bundle = new Bundle();
        bundle.putInt(AppErrorDialog.TITLE_ID, titleID);
        bundle.putString(AppErrorDialog.DETAIL_TXT, detailTxt);
        bundle.putInt(AppErrorDialog.BUTTON_ID, buttonID);
        //Show
        showAppErrorDialog(activity, bundle).setButtonListener(listener);
    }

    public static void dismissAppErrorDialog(Activity activity){
        dismissDialogFragment(activity, AppErrorDialog.TAG);
    }

    public static void showAppErrorDialog(Activity activity, int detailID, int buttonID, @NonNull AppErrorDialog.OnClickActionButtonListener listener){
        Function.showAppErrorDialog(activity, R.string.error, detailID, buttonID, listener);
    }

    private static AppErrorDialog showAppErrorDialog(@NonNull Activity activity, @NonNull Bundle bundle){
        AppErrorDialog dialog = new AppErrorDialog();
        dialog.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        dialog.show(transaction, AppErrorDialog.TAG);
        return dialog;
    }

    public static void dismissNoNetworkDialog(Activity activity){
        dismissDialogFragment(activity, NoNetworkDialog.TAG);
    }

    public static void dismissDialogFragment(Activity activity, String tag){
        if (activity!=null){
            Fragment fragment = activity.getFragmentManager().findFragmentByTag(tag);
            if(fragment!=null){
                DialogFragment dialogFragment = (DialogFragment)fragment;
                dialogFragment.dismiss();
            }
        }
    }

    public static void showToast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showToast(Context context, int message){
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getSSID(Context context){
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(WIFI_SERVICE);
        if (wifiManager!=null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                String ssid = wifiInfo.getSSID();//Here you can access your SSID
                return Function.cutQuoted(ssid);
            }
        }
        return "";
    }

    public static String cutQuoted(String name){
        char []input = name.toCharArray();
        StringBuilder builder = new StringBuilder("");
        for (char a : input){
            if (a != '"'){
                builder.append(a);
            }
        }
        return builder.toString();
    }

    public static void disconnectWiFi(Context context){
        WifiManager manager = (WifiManager)context.getApplicationContext()
                .getSystemService(WIFI_SERVICE);
        if (manager!=null){
            manager.disconnect();
        }
    }

    //Work only Wi-Fi or Cellular connected but can't check internet
    public static boolean internetConnected(Context context){
        return getConnectionType(context)>0;
    }

    public static void checkInternet(Context context, @NonNull final StringCallback callback){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(GET, "https://www.youtube.com/",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onResponse("Internet is available.");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error.getMessage());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    //Thank: https://stackoverflow.com/a/53243938
    @IntRange(from = 0, to = 2)
    public static int getConnectionType(Context context) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1;
                    }
                }
            }
        } else {
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        result = 2;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        result = 1;
                    }
                }
            }
        }
        return result;
    }

    public static String get1Digit(int value){
        DecimalFormat format = new DecimalFormat("0.0");
        return format.format(value/10.0f);
    }

    public static String get1Digit(double value){
        DecimalFormat format = new DecimalFormat("0.0");
        return format.format(value);
    }

    public static int getSumOf(int value[]){
        int v = 0;
        for (int i : value){ v += i; }
        return v;
    }

    public static int getLevel(float minValue, float maxValue, int maxLevel, float compareValue){
        float step = (maxValue-minValue)/maxLevel;
        if (compareValue<minValue){
            return 0;
        }else if (compareValue>=maxValue){
            return maxLevel+1;
        }else {
            for (int i=0; i<maxLevel; i++){
                float x1 = minValue+(step*i);
                float x2 = minValue+(step*(i+1));
                if (compareValue>=x1 && compareValue<x2){
                    return i+1;
                }
            }
            return 0;
        }
    }

    public static String addComma(String message[]){
        StringBuilder builder = new StringBuilder("");
        for (String m : message){
            builder.append(m.concat(","));
        }
        return builder.toString();
    }


    public static String getUserImageUrl(String fileName){
        return Constant.DOWNLOAD_URL+"?path="+Constant.USER_IMG_DIR+"/"+fileName+Constant.JPEG;
    }

    public static String getGroupImageUrl(String fileName){
        return Constant.DOWNLOAD_URL+"?path="+Constant.GROUP_IMG_DIR+"/"+fileName+Constant.JPEG;
    }

    /**
     * A one color image.
     * @param width
     * @param height
     * @param color
     * @return A one color image with the given width and height.
     * https://gist.github.com/catehstn/6fc1a9ab7388a1655175
     */
    public static Bitmap createImage(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }


    public static byte[] getByteArray(Bitmap bitmap){
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        //Use PNG format due to it return same dimensions as original
        //Detail: https://stackoverflow.com/questions/8417034/how-to-make-bitmap-compress-without-change-the-bitmap-size
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream);
        return bStream.toByteArray();
    }

    public static Bitmap getBitmap(byte[] byteArray){
        if (byteArray==null){ return null; }
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    //Thank: http://androidcodesonhands.blogspot.com/2018/07/how-to-get-uri-from-bitmap-image-in.html
    public static Uri getImageUri(Activity inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public static void showFullScreenPicture(Activity activity, Bitmap bitmap){
        //Allow show when bitmap != null
        if (activity!=null && bitmap!=null){
            //Show full picture
            Bundle bundle = new Bundle();
            bundle.putByteArray(Constant.PROFILE_IMG, getByteArray(bitmap));
            FullScreenPictureDialog dialog = new FullScreenPictureDialog();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            dialog.setArguments(bundle);
            dialog.show(transaction, FullScreenPictureDialog.TAG);
        }
    }

    public static String encodeQRDetail(int id){
        //Detail: <id>x<sum of byte of id>
        String idTxt = String.valueOf(id);
        return idTxt+"x"+getSumOf(idTxt);
    }

    public static int decodeQRDetail(String detail){
        String[] n = detail.split("x");
        if (n.length!=2){ return -1; }
        int id = -1;
        try {
            long sum1 = Long.valueOf(n[1]);
            long sum2 = getSumOf(n[0]);
            if (sum1==sum2){ return getInt(n[0]); }
        }catch (NumberFormatException e){
            e.printStackTrace();
        }
        return id;
    }

    private static long getSumOf(String string){
        byte[] bytes = string.getBytes();
        long sum = 0;
        for (byte b : bytes){ sum += b; }
        return sum;
    }

    public static void showAlertDialog(
            Activity activity, int titleId, int detailId, int buttonId, MyAlertDialog.OnButtonClickListener listener){
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(MyAlertDialog.TITLE_ID, titleId);
        bundle.putInt(MyAlertDialog.DETAIL_ID, detailId);
        bundle.putInt(MyAlertDialog.BUTTON_ID, buttonId);
        //Create dialog
        createMyAlertDialog(activity, bundle, listener);
    }

    public static void showAlertDialog(
            Activity activity, int titleId, String detailTxt, int buttonId, MyAlertDialog.OnButtonClickListener listener){
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(MyAlertDialog.TITLE_ID, titleId);
        bundle.putString(MyAlertDialog.DETAIL_TXT, detailTxt);
        bundle.putInt(MyAlertDialog.BUTTON_ID, buttonId);
        //Create dialog
        createMyAlertDialog(activity, bundle, listener);
    }

    public static void showPasswordDialog(
            Activity activity, String titleTxt, int buttonId, MyAlertDialog.OnButtonClickListener listener){
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(MyAlertDialog.TITLE_ID, 0);
        bundle.putString(MyAlertDialog.TITLE_TXT, titleTxt);
        bundle.putInt(MyAlertDialog.BUTTON_ID, buttonId);
        bundle.putInt(MyAlertDialog.VISIBLE_OBJ, MyAlertDialog.FILL_PASSWORD_ONLY);
        //Create dialog
        createMyAlertDialog(activity, bundle, listener);
    }

    private static void createMyAlertDialog(Activity activity, Bundle bundle, MyAlertDialog.OnButtonClickListener listener){
        MyAlertDialog dialog = new MyAlertDialog();
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        dialog.setArguments(bundle);
        dialog.setClickListener(listener);
        dialog.show(transaction, MyAlertDialog.TAG);
    }

    //n = number of day from today
    public static String getDate(int n){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, n);
        return Constant.DATE_FORMAT.format(cal.getTime());
    }

    //Thank: https://stackoverflow.com/questions/8940438/number-of-days-in-particular-month-of-particular-year
    public static String getEndDateInMonth(Calendar calendar){
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
        return Constant.DATE_FORMAT.format(calendar.getTime());
    }

    public static String getStartDateInMonth(Calendar calendar){
        int startDays = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, startDays);
        return Constant.DATE_FORMAT.format(calendar.getTime());
    }

    public static String getCurrentMonth(){
        Calendar calendar = Calendar.getInstance();
        return Constant.MONTH_FORMAT.format(calendar.getTime());
    }

    public static String getPowerAndUnit(float value){
        DecimalFormat format = new DecimalFormat("###,###,###,##0.00");
        String unit = " W";
        if (value>999.9f){
            unit = " kW";
            value /= 1000.0f;
        }
        return format.format(value) + unit;
    }

    public static int getResWiFiLevelIcon(int rssi){
        //Max = -50, Min = -80, 4 level
        //More: https://eyesaas.com/wi-fi-signal-strength/
        int level = (int) ((0.1f*rssi) + 9);
        if (level<0){ level = 0; }
        switch (level){
            case 0: return R.drawable.wifi_level0_icon;
            case 1: return R.drawable.wifi_level1_icon;
            case 2: return R.drawable.wifi_level2_icon;
            case 3: return R.drawable.wifi_level3_icon;
            default: return R.drawable.wifi_level4_icon;
        }
    }


}
