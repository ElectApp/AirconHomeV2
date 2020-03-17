package com.apyeng.airconhomev2;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.core.ViewFinderView;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

//Thank: https://github.com/dm77/barcodescanner/blob/master/zxing-sample/src/main/java/me/dm7/barcodescanner/zxing/sample/CustomViewFinderScannerActivity.java
public class JoinActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private SelectableRoundedImageView homeImg;
    private TextView title, action, homeName, errorNote;
    private View sectionView[];
    private ZXingScannerView mScannerView;
    private int contentNumber, userId, resultCode;
    private int groupId[];
    private HomeItem homeItem; //Join Home
    private HomeManager homeManager;
    private Bitmap profileBitMap;
    private static final int SCANNER=0, LOADING=1, FOUND=2;
    private static final String TAG = "JoinActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        //Get data
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        groupId = getIntent().getIntArrayExtra(Constant.GROUP_ID);
        if (userId==0){
            throw new IllegalArgumentException("Must pass user id...");
        }

        //Set view
        ViewGroup contentFrame = findViewById(R.id.scanner_frame);

        title = findViewById(R.id.title_toolbar);
        action = findViewById(R.id.action_txt);
        homeImg = findViewById(R.id.home_icon);
        homeName = findViewById(R.id.home_name);
        errorNote = findViewById(R.id.error_note);

        //Add scanner layout to contentFrame
        mScannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new CustomViewFinderView(context);
            }
        };
        contentFrame.addView(mScannerView);

        //Back icon
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Back to previously
                if (contentNumber==FOUND){
                    showContent(SCANNER);
                }else {
                    finish();
                }

            }
        });

        //Show full picture
        homeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Function.showFullScreenPicture(JoinActivity.this, profileBitMap);
            }
        });

        //Action btn
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (contentNumber){
                    case SCANNER: //Pick QR code from gallery
                        Intent photoPic = new Intent(Intent.ACTION_PICK);
                        photoPic.setType("image/*");
                        startActivityForResult(photoPic, Constant.SELECT_PHOTO);
                        break;
                    case FOUND: //Join Membership
                        //Check already member?
                        String t = action.getText().toString();
                        if (t.equals(getString(R.string.ok))){
                            //Update result code
                            resultCode = RESULT_OK;
                            //Back to list
                            backToPreviouslyActivity();
                        }else {
                            //Join group
                            showContent(LOADING);
                            homeManager.joinGroup(userId, homeItem.getGroupId(), new HomeManager.OnListener() {
                                @Override
                                public void onSuccess() {
                                    setAction(true);
                                }

                                @Override
                                public void onFailed(String error) {
                                    setAction(false);
                                    Function.showToast(JoinActivity.this, R.string.no_result);
                                }
                            });
                        }
                        break;
                }

            }
        });

        //Initial
        homeManager = new HomeManager(this);

        //Set section view
        sectionView = new View[3];
        sectionView[0] = contentFrame;
        sectionView[1] = findViewById(R.id.circle_progress);
        sectionView[2] = findViewById(R.id.found_item_lay);

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backToPreviouslyActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Restart content
        showContent(contentNumber);
    }

    @Override
    public void onPause() {
        super.onPause();
        //Stop camera
        mScannerView.stopCamera();
    }

    private void backToPreviouslyActivity(){
        //Set data
        Intent intent = new Intent();
        intent.putExtra(Constant.HOME_DATA, homeItem);
        setResult(resultCode, intent);
        //Back
        finish();
    }

    private void tryScanQR(){
        //Check permission
        if (!Function.hasPermissions(this, Constant.CAMERA_PERMISSION)){
            //Request permission from user
            ActivityCompat.requestPermissions(this, Constant.CAMERA_PERMISSION, Constant.CAMERA_CODE);
        }else {
            mScannerView.setResultHandler(this);
            mScannerView.setAutoFocus(true);
            mScannerView.startCamera();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Check result (Not necessary but work -> Unreasonable)
        if(requestCode == Constant.CAMERA_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //User allowed writing
                tryScanQR();
            }else {
                //Back
                finish();
            }
        }
    }

    @Override
    public void handleResult(Result rawResult) {

        //Toast.makeText(this, "Contents = " + rawResult.getText() +
        //        ", Format = " + rawResult.getBarcodeFormat().toString(), Toast.LENGTH_SHORT).show();

        checkQRDetail(rawResult.getText());

    }

    private void restartScanner(){
        // Note:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(JoinActivity.this);
            }
        }, 1000);
    }


    private static class CustomViewFinderView extends ViewFinderView {

        public CustomViewFinderView(Context context) {
            super(context);
            init();
        }

        public CustomViewFinderView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private void init() {
            setSquareViewFinder(true);
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Check photo
        if (requestCode==Constant.SELECT_PHOTO && resultCode==RESULT_OK && data!=null && data.getData()!=null){

            Uri selectedImage = data.getData();
            InputStream imageStream = null;
            try {
                //getting the image
                imageStream = getContentResolver().openInputStream(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                //Show failed
                showAlertDialog(R.string.error, R.string.read_failed, null);
            }
            //decoding bitmap
            Bitmap bMap = BitmapFactory.decodeStream(imageStream);
            homeImg.setImageURI(selectedImage);// To display selected image in image view
            int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
            // copy pixel data from the Bitmap into the 'intArray' array
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(),
                    bMap.getHeight());
            //Create binary bitmap
            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(),
                    bMap.getHeight(), intArray);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            //Try read
            Reader reader = new QRCodeMultiReader();
            try {
                //Get result and check detail
                Result result = reader.decode(bitmap);
                checkQRDetail(result.getText());

            } catch (NotFoundException e) {
                e.printStackTrace();
                showAlertDialog(R.string.error, R.string.read_failed, null);

            } catch (ChecksumException e) {
                e.printStackTrace();
                showAlertDialog(R.string.error, R.string.read_failed, null);

            } catch (FormatException e) {
                e.printStackTrace();
                showAlertDialog(R.string.error, R.string.read_failed, null);

            }

        }

    }

    private void checkQRDetail(String detail){
        Log.w(TAG, "QR Code detail: "+ detail);
        final int groupId = Function.decodeQRDetail(detail);
        //Check
        if (groupId>-1){
            //Show loading
            showContent(LOADING);
            //Try read home detail
            homeManager.readDeviceId(new int[]{groupId}, new HomeManager.OnReadDeviceIdListener() {
                @Override
                public void onSuccess(List<HomeItem> homeItems) {
                    //Load image
                    if (homeItems!=null && homeItems.size()>=1){
                        //Update home item
                        homeItem = homeItems.get(0);
                        //Set home name
                        homeName.setText(homeItem.getName());
                        //Download images
                        String fileName = homeItem.getProfileImg();

                        if (!fileName.isEmpty()){
                            VolleyImageLoader loader = new VolleyImageLoader(JoinActivity.this);
                            loader.download(Function.getGroupImageUrl(fileName), 0, 0,
                                    new VolleyImageLoader.OnLoadingListener() {
                                        @Override
                                        public void onSuccess(Bitmap bitmap) {
                                            //Update image
                                            homeImg.setImageBitmap(bitmap);
                                            setAction(isMember());
                                            //Save
                                            profileBitMap = bitmap;
                                        }

                                        @Override
                                        public void onFailed(String error) {
                                            Log.e(TAG, "Load image failed: "+error);
                                            setColorToHomeImg();
                                            setAction(isMember());
                                        }
                                    });
                        }else {
                            setColorToHomeImg();
                            setAction(isMember());
                        }

                    }else {
                        //Show not found home and back start scanner
                        showAlertDialog(R.string.error, R.string.not_found_home, null);
                        showContent(SCANNER);
                    }
                }

                @Override
                public void onFailed(String error) {
                    //Function.showToast(JoinActivity.this, R.string.no_result);
                    showAlertDialog(R.string.no_result, R.string.no_result_detail, null);
                    showContent(SCANNER);
                }
            });
        }else {
            showAlertDialog(R.string.qr_code_details, 0, detail);
        }
    }

    private void setColorToHomeImg(){
        //Set img
        homeImg.setImageBitmap(null);
        homeImg.setImageResource(R.drawable.home_img_icon);
        //Show found content
        showContent(FOUND);
    }

    private void showAlertDialog(int titleId, int detailId, String detail){
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(MyAlertDialog.TITLE_ID, titleId);
        bundle.putInt(MyAlertDialog.DETAIL_ID, detailId);
        bundle.putString(MyAlertDialog.DETAIL_TXT, detail);
        //Create dialog
        MyAlertDialog dialog = new MyAlertDialog();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        dialog.setArguments(bundle);
        dialog.setClickListener(new MyAlertDialog.OnButtonClickListener() {
            @Override
            public void onClose() {
                Log.w(TAG, "Close dialog...");
                restartScanner();
            }

            @Override
            public void onAction(View view) {
                Log.w(TAG, "Click action on dialog...");
                Function.dismissDialogFragment(JoinActivity.this, MyAlertDialog.TAG);
                restartScanner();
            }
        });
        dialog.show(transaction, MyAlertDialog.TAG);
    }

    private boolean isMember(){
        if (groupId!=null){
            for (int i : groupId){
                if (i==homeItem.getGroupId()){ return true; }
            }
        }
        return false;
    }

    private void setAction(boolean success){
        //Set text on action button
        if (success){
            action.setText(R.string.ok);
            errorNote.setVisibility(View.VISIBLE);
        }else {
            action.setText(R.string.join);
            errorNote.setVisibility(View.GONE);
        }
        //Show content
        showContent(FOUND);
    }

    private void showContent(int n){
        //Show content
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
        //Set other detail
        switch (n){
            case SCANNER:
                title.setText(R.string.scan_title);
                action.setText(R.string.photo_gallery);
                //Start scanner
                tryScanQR();
                break;
            case LOADING:
            case FOUND:
                title.setText(R.string.join_member);
                break;
        }
        action.setVisibility(n==LOADING? View.INVISIBLE : View.VISIBLE);

        //Save number
        contentNumber = n;
    }


}
