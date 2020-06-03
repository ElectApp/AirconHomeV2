package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private int userId; //Use only homeItem != null
    private HomeItem homeItem;
    private UserItem userItem;
    private boolean editFlag;
    private ProgressBar savingBar;
    private TextView registerTxt;
    private SelectableRoundedImageView profileImg;
    private ClearableEditText nameEnter;
    private File imgFile;
    private List<File> cacheFileList;
    private HomeManager homeManager;
    private String fileName, name;
    private int resultCode;
    private byte[] imgByteArray;
    private static final String TAG = "EditProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Get data
        imgByteArray = getIntent().getByteArrayExtra(Constant.PROFILE_IMG);
        homeItem = getIntent().getParcelableExtra(Constant.HOME_DATA);
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        if (homeItem==null && userId==0){
            userItem = getIntent().getParcelableExtra(Constant.USER_DATA);
            if (userItem==null){
                throw new IllegalArgumentException("Must pass home data or user data...");
            }
        }

        //Declare element on layout
        savingBar = findViewById(R.id.circle_progress);
        profileImg = findViewById(R.id.profile_img);
        nameEnter = findViewById(R.id.name_enter);
        registerTxt = findViewById(R.id.registered_time);
        TextView title = findViewById(R.id.title_toolbar);
        RoundButtonWidget signOutBtn = findViewById(R.id.sign_out_btn);

        title.setText(R.string.edit_profile);

        //Initial
        cacheFileList = new ArrayList<>();
        homeManager = new HomeManager(this);

        //Initial view
        if (homeItem!=null){
            //Image
            setProfileImg(Function.getBitmap(imgByteArray), R.drawable.home_img_icon);
            //Name
            nameEnter.setText(homeItem.getName());
            nameEnter.setHint(R.string.enter_home_name);
            //Time
            setRegisterTxt(R.string.built, homeItem.getRegisteredTime());
            //Sign out btn
            signOutBtn.setVisibility(View.GONE);
            //set filename
            fileName = "G-"+homeItem.getGroupId();
        }else {
            //Image
            setProfileImg(Function.getBitmap(imgByteArray), R.drawable.user_icon);
            //Name
            nameEnter.setText(userItem.getUsername());
            nameEnter.setHint(R.string.enter_username);
            //Time
            setRegisterTxt(R.string.registered, userItem.getRegisteredTime());
            //Sign out btn
            signOutBtn.setVisibility(View.VISIBLE);
            //set filename
            fileName = "U-"+userItem.getUserId();
        }

        //Back icon
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check flag
                checkBeforeBack();
            }
        });

        //Pick image icon
        findViewById(R.id.camera_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPic = new Intent(Intent.ACTION_PICK);
                photoPic.setType("image/*");
                startActivityForResult(photoPic, Constant.SELECT_PHOTO);
            }
        });

        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Show full picture
                Bitmap bitmap = Function.getBitmap(imgByteArray);
                if (bitmap!=null){
                    Function.showFullScreenPicture(EditProfileActivity.this, bitmap);
                }
            }
        });

        signOutBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Show confirm dialog
                showConfirmSignOut();
            }
        });

        //Set Keyboard listener
        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                //Update flag
                editFlag = true;
                //Hide cursor
                nameEnter.setCursorVisible(isOpen);
            }
        });


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        checkBeforeBack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case Constant.SELECT_PHOTO: //Pick img result
                if (resultCode==RESULT_OK && data!=null){
                    Uri selectedImage = data.getData();
                    if (selectedImage!=null){
                        //Set img file and preview
                        setImgFile(selectedImage);
                        //Update flag
                        editFlag = true;
                    }
                }
                break;
        }

    }


    private void checkBeforeBack(){
        //Check flag
        if (editFlag){
            saveData();
        }else {
            finish();
        }
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
        profileImg.setImageBitmap(bMap);
        //Save imgArray
        imgByteArray = Function.getByteArray(bMap);

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

    private void saveData(){
        //Check data
        name = String.valueOf(nameEnter.getText()).trim();
        if (name.isEmpty()){
            //Alert
            Function.setRequestEnter(this, nameEnter);
        }else {
            //check internet
            if (Function.internetConnected(this)){
                //Saving
                savingBar.setVisibility(View.VISIBLE);
                //Check flag
                if (homeItem!=null){
                    //Save home data
                    saveGroupData();
                }else {
                    //Save user data
                    saveUserData();
                }
            }else {
                //Show dialog
                Function.showNoInternetDialog(this, errorListener);
            }

        }
    }

    private void saveGroupData(){
        //Work
        homeManager.setHome(userId, homeItem.getGroupId(), name, null, new HomeManager.OnSetHomeListener() {
            @Override
            public void onSuccess(int groupId) {
                Log.w(TAG, "Update home success...");
                //Save name
                homeItem.setName(name);
                //Upload
                checkBeforeUploadImg();
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "Update home failed..."+error);
                //Show
                Function.showNoResultDialog(
                        EditProfileActivity.this, errorListener);
            }
        });

    }

    private void checkBeforeUploadImg(){
        //Update code
        if (imgFile!=null){
            // Continuous upload image
            tryUploadImg();
        }else {
            resultCode = RESULT_OK;
            //Set data back to previously activity
            setDataBack();
        }
    }

    private void saveUserData(){
        //Work
        homeManager.updateUserData(userItem.getUserId(), name, new HomeManager.OnListener() {
            @Override
            public void onSuccess() {
                Log.w(TAG, "Update user data success...");
                //Save name
                userItem.setUsername(name);
                //Upload img
                checkBeforeUploadImg();
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "Update user data failed..."+error);
                //Show
                Function.showNoResultDialog(
                        EditProfileActivity.this, errorListener);
            }
        });
    }


    private void tryUploadImg(){
        //Name: G-<group-id> for saving to images/group
        homeManager.uploadImage(fileName, imgFile,
                new HomeManager.OnUploadListener() {
                    @Override
                    public void onSuccess(String fullPath, String fileName) {
                        Log.w(TAG, "upload success..."+fileName);
                        resultCode = RESULT_OK;
                        //Set data back to previously activity
                        setDataBack();
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e(TAG, "upload failed..."+error);
                        //Show
                        Function.showUploadImageError(EditProfileActivity.this,
                                error, errorListener);
                    }
                });
    }

    private void setDataBack(){
        //Hide
        savingBar.setVisibility(View.GONE);
        //Pass data
        if (resultCode==RESULT_OK){
            Intent intent = new Intent();
            if (homeItem!=null){
                homeItem.setProfileImg(fileName);
                intent.putExtra(Constant.HOME_DATA, homeItem);
            }else {
                userItem.setProfileImg(fileName);
                intent.putExtra(Constant.USER_DATA, userItem);
            }
            intent.putExtra(Constant.PROFILE_IMG, imgByteArray);
            setResult(resultCode, intent);
        }
        //Delete file
        deleteImgCacheFile();
        //Back
        finish();
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener =
            new AppErrorDialog.OnClickActionButtonListener() {
                @Override
                public void onClick(View view, int titleId, int buttonId) {
                    //Dismiss
                    Function.dismissAppErrorDialog(EditProfileActivity.this);
                    //Check title
                    switch (titleId){
                        case R.string.no_internet:
                            //Re-upload
                            saveData();
                            break;
                        case R.string.upload_failed_title:
                            //Retry to upload
                            tryUploadImg();
                            break;
                    }

                }
            };

    private void setProfileImg(Bitmap bitmap, int noImgId){
        if (bitmap!=null){
            profileImg.setImageBitmap(bitmap);
        }else {
            profileImg.setImageResource(noImgId);
        }
    }

    private void showConfirmSignOut(){
        Function.showAlertDialog(this, R.string.sign_out, R.string.sign_out_confirm,
                R.string.confirm, new MyAlertDialog.OnButtonClickListener() {
                    @Override
                    public void onClose() {

                    }

                    @Override
                    public void onAction(String data, String password) {
                        //Hide dialog
                        Function.dismissDialogFragment(EditProfileActivity.this, MyAlertDialog.TAG);
                        //Sign out
                        final  Context context = EditProfileActivity.this;
                        savingBar.setVisibility(View.VISIBLE); //Show saving
                        homeManager.signInWithUserId(userItem.getUserId(),
                                Constant.SIGN_OUT_FLAG, new HomeManager.OnSignInListener() {
                                    @Override
                                    public void onSuccess(UserItem userData, int[] groupId) {
                                        savingBar.setVisibility(View.GONE);
                                        Function.showToast(context, R.string.sign_out_success);
                                        //Clear userId
                                        OnDevice onDevice = new OnDevice(context, OnDevice.USER_FILE);
                                        onDevice.saveUserId(0);
                                        //Close all the running activities and start SplashActivity
                                        //Thank: https://stackoverflow.com/questions/35510182/how-to-clear-previous-activities-on-a-button-click
                                        Intent intent = new Intent(context, SplashActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onFailed(String error) {
                                        savingBar.setVisibility(View.GONE);
                                        Function.showToast(context, R.string.sign_out_failed);
                                    }
                                });

                    }
                });

    }

    //dateTime input: YYYY-MM-DD HH:mm:ss
    private void setRegisterTxt(int typeId, String dateTime){
        //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat format = Constant.REGISTER_FORMAT;
        try {
            Date date = format.parse(dateTime);
            //Set new format
            format = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            dateTime = format.format(date);
        }catch (ParseException e) {
            e.printStackTrace();
        }
        String txt = getString(typeId)+" "+dateTime;
        registerTxt.setText(txt);
    }


}
