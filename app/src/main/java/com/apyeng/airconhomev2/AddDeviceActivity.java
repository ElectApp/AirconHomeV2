package com.apyeng.airconhomev2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddDeviceActivity extends AppCompatActivity {

    private ImageView backIcon;
    private SelectableRoundedImageView homeIcon;
    private ClearableEditText homeNameEnter, nicknameEnter;
    private PasswordEditText passwordEnter;
    private TextView actionNoteTxt, barTxt, ssidTxt;
    private TextView stepTxt[] = new TextView[3];
    private RoundButtonWidget actionBtn, doneBtn;
    private static final int LAY_MAX = 5;
    private LinearLayout stepLay[] = new LinearLayout[LAY_MAX];
    private LinearLayout progressLay;
    private int countStep, groupId, userId, minStep, resultCode;
    private String nickname, actualName="", latLong="", homeName="";
    private HomeManager home;
    private OnDevice onDevice;
    private AccessLocation location;
    private File imgFile;   //File for upload to server
    private List<File> cacheFileList;
    private Bitmap profileBitmap;
    private TCPMessage tcpMessage;
    private WiFiConnectionDialog wiFiConnectionDialog;
    private static final int CONNECT_AC = 2023;
    private static final String TAG = "AddDeviceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        //Get data
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);
        homeName = getIntent().getStringExtra(Constant.NAME);
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        //Check
        if (userId<=0){
            throw new IllegalArgumentException("Must pass user id...");
        }
        if (homeName==null){ homeName = ""; }

        //Save last step
        minStep = groupId==0? 0 : 1;
        countStep = minStep;

        //Declare view object on layout
        homeNameEnter = findViewById(R.id.home_name_enter);
        nicknameEnter = findViewById(R.id.ac_name_enter);
        passwordEnter = findViewById(R.id.enter_password);

        actionNoteTxt = findViewById(R.id.button_note);
        barTxt = findViewById(R.id.title_toolbar);
        //ssidEnter = findViewById(R.id.ssid_enter);
        ssidTxt = findViewById(R.id.tv_ssid);
        stepTxt[0] = findViewById(R.id.step1);
        stepTxt[1] = findViewById(R.id.step2);
        stepTxt[2] = findViewById(R.id.step3);

        actionBtn = findViewById(R.id.action_btn);
        doneBtn = findViewById(R.id.done_btn);

        progressLay = findViewById(R.id.progress_lay);
        stepLay[0] = findViewById(R.id.set_home_name_lay);
        stepLay[1] = findViewById(R.id.connect_ac_lay);
        stepLay[2] = findViewById(R.id.set_ac_name_lay);
        stepLay[3] = findViewById(R.id.connect_router_lay);
        stepLay[4] = findViewById(R.id.finish_lay);

        backIcon = findViewById(R.id.back_icon);
        homeIcon = findViewById(R.id.profile_img);

        actionBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Check step
                if (countStep==4){
                    countStep = minStep;
                    showLayout();
                }else {
                    runAction();
                }

            }
        });

        doneBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Pass data and back to previously activity
                setResultActivity();
            }
        });

        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Count down
                countDownStep();
            }
        });


        homeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Show full picture
                Function.showFullScreenPicture(AddDeviceActivity.this, profileBitmap);
            }
        });

        //Pick img
        findViewById(R.id.camera_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPic = new Intent(Intent.ACTION_PICK);
                photoPic.setType("image/*");
                startActivityForResult(photoPic, Constant.SELECT_PHOTO);
            }
        });

        findViewById(R.id.tv_ssid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNetworkDialog();
            }
        });
        findViewById(R.id.select_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNetworkDialog();
            }
        });

        //Set char counter to EditText
        homeNameEnter.addTextChangedListener(
                new LimitCharOnEditText(40, (TextView)findViewById(R.id.home_name_counter)));
        nicknameEnter.addTextChangedListener(
                new LimitCharOnEditText(20, (TextView)findViewById(R.id.ac_name_counter)));

        //Set Keyboard listener
        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                homeNameEnter.setCursorVisible(isOpen);
                nicknameEnter.setCursorVisible(isOpen);
                passwordEnter.setCursorVisible(isOpen);
            }
        });

        //Location
        location = new AccessLocation(this, new AccessLocation.OnLocationListener() {
            @Override
            public void onChanged(Location location) {
                //Get position
                latLong = location.getLatitude()+","+location.getLongitude();
                Log.w(TAG, "Get location success: "+latLong);
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "Get location failed: "+error);
            }
        });

        //Initial home manager
        home = new HomeManager(this);
        //Set OnDevice object to network file
        onDevice = new OnDevice(this, OnDevice.NETWORK_FILE);
        //Initial list
        cacheFileList = new ArrayList<>();
        //TCP message
        tcpMessage = new TCPMessage();
        //Show
        showLayout();
        //Get location
        tryGetLocation();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case CONNECT_AC: //Try ping for checking (it is AC?)
                Log.w(TAG, "Check AC is connected...");
                checkACConnected();
                break;
            case Constant.SELECT_PHOTO: //Pick img result
                if (resultCode==RESULT_OK && data!=null){
                    Uri selectedImage = data.getData();
                    if (selectedImage!=null){
                        //Set img file and preview
                        setImgFile(selectedImage);
                    }
                }
                break;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Stop get location
        location.stop();
    }

    @Override
    public void onBackPressed() {
        //Count down
        countDownStep();
    }

    private void tryGetLocation(){
        //Check permission
        if (!Function.hasPermissions(this, Constant.LOCATION_PERMISSION)){
            //Request permission from user
            ActivityCompat.requestPermissions(this, Constant.LOCATION_PERMISSION, Constant.LOCATION_CODE);
        }else {
            //Get location
            if (groupId==0){ location.start(); }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case Constant.LOCATION_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //User allowed the location and you can read it now
                    tryGetLocation();
                }else {
                    //back to previously
                    setResultActivity();
                }
                break;
            case WiFiConnectionDialog.PERMISSION_CODE:
                if (wiFiConnectionDialog!=null && wiFiConnectionDialog.isVisible()){
                    wiFiConnectionDialog.onPermissionResult(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                }
                break;
        }
    }

    private void showNetworkDialog(){
        //Show Network Scanning by WiFi Module
        ModuleScanWiFiDialog.show(AddDeviceActivity.this,
                new NetworkItemAdapter.OnClickItemListener() {
                    @Override
                    public void onClick(int position, NetworkItem item) {
                        //Update SSID
                        ssidTxt.setText(item.ssid);
                        //Clear
                        passwordEnter.setText(onDevice.getWiFiPassword(item.ssid));
                    }
                });
    }

    private void countDownStep(){
        //Check
        if (countStep==minStep){
            //Pass data and back to previously activity
            setResultActivity();
        }else {
            //count down
            countStep--;
            showLayout();
        }
    }

    private void setResultActivity(){
        //Delete cache file
        deleteImgCacheFile();
        //Thank: https://stackoverflow.com/questions/12293884/how-can-i-send-back-data-using-finish
        Intent intent = new Intent();
        //intent.putExtra(Constant.HOME_DATA, homeItem);
        intent.putExtra(Constant.GROUP_ID, groupId);
        setResult(resultCode, intent);
       // Log.w(TAG, "Now group ID: "+groupId);
        //Back to previously activity
        finish();
    }

    private void showLayout(){
        //Set toolbar
        String title = getString(R.string.build_home);
        if (countStep>0){
            title = getString(R.string.add_ac_to)+" "+homeName;
        }
        barTxt.setText(title);
        //Show step layout
        switch (countStep){
            case 0:
                actionBtn.setText(R.string.save);
                break;
            case 1:
                setStepTxt(0);
                actionBtn.setText(R.string.connect_wifi);
                actionNoteTxt.setText(R.string.connect_wifi_note);
                break;
            case 2:
                setStepTxt(1);
                actionBtn.setText(R.string.next);
                break;
            case 3:
                //Close dialog
                Function.dismissNoNetworkDialog(this);
                //Set layout
                setStepTxt(2);
                actionBtn.setText(R.string.save_connect);
                actionNoteTxt.setText(R.string.save_connect_note);
                break;
            case 4:
                actionBtn.setText(R.string.add_new_ac);
                break;
        }
        //Set Other
        doneBtn.setVisibility(countStep==4? View.VISIBLE : View.GONE);
        actionNoteTxt.setVisibility(countStep==1 || countStep==3? View.VISIBLE : View.GONE);
        backIcon.setVisibility(countStep<4? View.VISIBLE : View.INVISIBLE);
        progressLay.setVisibility(countStep!=4 && countStep>0? View.VISIBLE : View.INVISIBLE);
        for (int i=0; i<LAY_MAX; i++){
            stepLay[i].setVisibility(i==countStep? View.VISIBLE : View.GONE);
        }


    }

    private void setStepTxt(int n){
        for (int i=0; i<stepTxt.length; i++){
            setStepComplete(stepTxt[i], i<=n);
        }
    }

    private void setStepComplete(TextView textView, boolean complete){
        int color1 = getResources().getColor(R.color.colorBlue);
        int color2 = getResources().getColor(R.color.colorTextNormal);
        if (complete){
            textView.setBackgroundColor(color1);
            textView.setTextColor(Color.WHITE);
        }else {
            textView.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(color2);
        }
    }

    private void runAction(){
        switch (countStep){
            case 0: //Add or Edit Home (Such as Group)
                //Clear bg
                homeNameEnter.setSelected(false);
                //Check
                homeName = String.valueOf(homeNameEnter.getText());
                if (homeName.isEmpty()){
                    //Request
                    Function.setRequestEnter(this, homeNameEnter);
                }else {
                    //Check internet
                    if (Function.internetConnected(this)){
                        //Loading
                        Function.showLoadingDialog(this);
                        //Work
                        home.setHome(userId, groupId, homeName, latLong, new HomeManager.OnSetHomeListener() {
                            @Override
                            public void onSuccess(int groupId) {
                                Log.w(TAG, "Group ID: "+groupId);
                                //Update group id
                                AddDeviceActivity.this.groupId = groupId;
                                //Update code
                                resultCode = RESULT_OK;
                                if (imgFile!=null){
                                    // Continuous upload image
                                    tryUploadImg();
                                }else {
                                    //Hide loading
                                    Function.dismissLoadingDialog(AddDeviceActivity.this);
                                    //Update minStep so can't edit home name
                                    minStep = 1;
                                    //Next step
                                    countStep++;
                                    showLayout();
                                }
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e(TAG, "Create home failed..."+error);
                                //Show
                                Function.showNoResultDialog(
                                        AddDeviceActivity.this, errorListener);
                            }
                        });
                    }else {
                        //Show dialog
                        Function.showNoInternetDialog(AddDeviceActivity.this, errorListener);
                    }
                }
                break;
            case 1: //Connect to AC WiFi
                if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){ //Settings.ACTION_WIFI_SETTINGS
                    //My Wi-Fi connectivity
                    wiFiConnectionDialog = new WiFiConnectionDialog();
                    wiFiConnectionDialog.setSsidFilter("AC-");
                    wiFiConnectionDialog.setWiFiCallback(new WiFiConnectionDialog.WiFiCallback() {
                        @Override
                        public void onRequestPermission(@NonNull String[] permissions) {
                            ActivityCompat.requestPermissions(AddDeviceActivity.this,
                                    permissions, WiFiConnectionDialog.PERMISSION_CODE);
                        }

                        @Override
                        public void onDone(NetworkItem item) {
                            if (item!=null){
                                Log.w(TAG, "Try check A/C..."+item.ssid);
                                checkACConnected();
                            }
                        }
                    });
                    wiFiConnectionDialog.show(getSupportFragmentManager(), WiFiConnectionDialog.TAG);
                }else{
                    //Setting panel for Android 10
                    startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), CONNECT_AC);
                }
                break;
            case 2: //Set nickname of AC
                nickname = String.valueOf(nicknameEnter.getText()).trim();
                if(nickname.isEmpty()){
                    Function.setRequestEnter(AddDeviceActivity.this, nicknameEnter);
                }else {
                    countStep++;
                    showLayout();
                }
                break;
            case 3: //Set SSID and Password of router
                String ssid = String.valueOf(ssidTxt.getText());
                String password = String.valueOf(passwordEnter.getText());
                //Save password
                onDevice.saveWiFiPassword(ssid, password);
                //Check network
                if(actualName.equals(Function.getSSID(this))){
                    //Do connect
                    configAndConnectRouter(ssid, password);
                }else {
                    Function.showNoNetworkDialog(this, false);
                }
                break;
        }
    }

    private void checkACConnected(){
        //Show progress
        Function.showLoadingDialog(this);
        //Checking it is AC?
        tcpMessage.run( new String[]{Indoor.PING_TEST}, new TCPMessage.OnTCPListener() {
            @Override
            public void onResult(Task task) {
                //Close loading
                Function.dismissDialogFragment(AddDeviceActivity.this, PageLoadingDialog.TAG);
                //Check response
                Log.w(TAG, "Result: "+ Arrays.toString(task.response));
                if (task.response!=null && task.response[0].equals(Indoor.ON_TEST)){
                    //Save actual name
                    actualName = Function.getSSID(AddDeviceActivity.this);
                    //Next step
                    countStep++;
                    showLayout();
                }
            }
        });
    }
    
    private void configAndConnectRouter(String ssid, String password){
        //Show progress
        Function.showLoadingDialog(this);
        //Config and connect AC to router
        String request[] = new String[]{ Indoor.CONFIG_CONNECT_ROUTER, nickname, ssid, password,
                            String.valueOf(groupId) };
        tcpMessage.run( request, new TCPMessage.OnTCPListener() {
            @Override
            public void onResult(Task task) {
                //Close loading
                Function.dismissDialogFragment(AddDeviceActivity.this, PageLoadingDialog.TAG);
                //Check response
                Log.w(TAG, "Result: "+ Arrays.toString(task.response));
                if (task.response[0].equals(Indoor.ON_SUCCESS)){
                    //Save flag
                    resultCode = RESULT_OK;
                    //Show next step
                    countStep++;
                    showLayout();
                }else {
                    Function.showToast(AddDeviceActivity.this, R.string.failed);
                }
            }
        });
    }


    private void setImgFile(Uri uri){
        Log.w(TAG, "Pick path: "+uri.getPath());
        //Create Bitmap
        InputStream imageStream = null;
        try {
            //getting the image
            imageStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //decoding bitmap
        Bitmap bMap = BitmapFactory.decodeStream(imageStream);
        Log.w(TAG, "Original WxH: "+bMap.getWidth()+"x"+bMap.getHeight());
        //Cal height size by fixed ratio and width size
        //If don't convert divider to float, ratio value is always return 0.0
        float ratio = bMap.getHeight()/(bMap.getWidth()*1.0f);
        Log.w(TAG, "Ratio H/W: "+ratio);
        int newH = (int)(Constant.IMG_W*ratio);
        Log.w(TAG, "New WxH: "+Constant.IMG_W+" x "+newH);
        //Reduce size
        bMap = Bitmap.createScaledBitmap(bMap, Constant.IMG_W, newH, true);
        // bMap = ThumbnailUtils.extractThumbnail(bMap, IMG_W, newH);
        //Create cache file
        imgFile = new File(getCacheDir(), "re_"+System.currentTimeMillis()+".jpeg");
        try {
            //Try save
            FileOutputStream out = new FileOutputStream(imgFile);
            bMap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            //Add to list
            cacheFileList.add(imgFile);
            Log.w(TAG, "Save cache file success..."+imgFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Show image
        homeIcon.setImageBitmap(bMap);
        //Save bitmap
        profileBitmap = bMap;
    }

    private void tryUploadImg(){
        //Name: G-<group-id> for saving to images/group
        home.uploadImage( "G-"+groupId, imgFile,
                new HomeManager.OnUploadListener() {
            @Override
            public void onSuccess(String fullPath, String fileName) {
                Log.w(TAG, "upload success..."+fileName);
                //Hide loading
                Function.dismissLoadingDialog(AddDeviceActivity.this);
                //Update minStep so can't edit home name
                minStep = 1;
                //Next step
                countStep++;
                showLayout();
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "upload failed..."+error);
                //Show
                Function.showUploadImageError(AddDeviceActivity.this,
                        error, errorListener);
            }
        });
    }

    private void deleteImgCacheFile(){
        for (File file : cacheFileList){
            if (file!=null && file.exists()){
                String name = file.getAbsolutePath();
                if (file.delete()){
                    Log.w(TAG, "Deleted..."+name);
                }else {
                    Log.w(TAG, "Delete failed..."+name);
                }
            }else {
                Log.w(TAG, "Not found file...");
            }
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener =
            new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            //Dismiss
            Function.dismissAppErrorDialog(AddDeviceActivity.this);
            //Check title
            switch (titleId){
                case R.string.no_internet:
                    //Re-upload
                    runAction();
                    break;
                case R.string.upload_failed_title:
                    //Retry to upload
                    tryUploadImg();
                    break;
            }

        }
    };


}
