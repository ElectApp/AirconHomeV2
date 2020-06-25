package com.apyeng.airconhomev2;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sumimakito.awesomeqr.AwesomeQRCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PeopleFragment extends Fragment {

    private int userId;
    private HomeItem homeItem;
    private View sectionView[];
    private ImageView qrImg;
    private Bitmap qrBitmap;    //Save QR code
    private MemberItemAdapter adapter;
    private TextView numMemberTxt;
    private Context context;
    private Activity activity;
    private static final int LOADING = 0, NO_MEMBER = 1, CONTENT = 2;
    public static final String TAG = "PeopleFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get data
        homeItem = getArguments().getParcelable(Constant.HOME_DATA);
        userId = getArguments().getInt(Constant.USER_ID);
        if (homeItem==null || userId==0){
            throw new IllegalArgumentException("Must pass home data!");
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_people, container, false);

        numMemberTxt = view.findViewById(R.id.total_list);

        //Save section view
        sectionView = new View[3];
        sectionView[0] = view.findViewById(R.id.circle_progress);
        sectionView[1] = view.findViewById(R.id.no_member_lay);
        sectionView[2] = view.findViewById(R.id.member_content);
        qrImg = view.findViewById(R.id.qr_img);

        RoundButtonWidget saveBtn = view.findViewById(R.id.save_btn);
        saveBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Save file
                trySaveFile();
            }
        });

        RoundButtonWidget shareBtn = view.findViewById(R.id.share_btn);
        shareBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Share to another app
                shareImageUri(saveImage(qrBitmap));
            }
        });

        //Initial member list
        LinearLayoutManager rvManager = new LinearLayoutManager(context);
        adapter = new MemberItemAdapter(context, new ArrayList<MemberItem>());
        adapter.setClickItemListener(new MemberItemAdapter.OnClickItemListener() {
            @Override
            public void onClick(View view, MemberItem memberItem) {
                Log.w(TAG, "Click.."+memberItem.getUsername());
                //Show full screen
                Bitmap bitmap = memberItem.getImageDownloaded();
                Function.showFullScreenPicture(activity, bitmap);
            }
        });
        RecyclerView recyclerView = view.findViewById(R.id.member_rv);
        recyclerView.setLayoutManager(rvManager);
        recyclerView.setAdapter(adapter);

        return view;

    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Read member list
        tryReadMemberList();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }



    private void tryReadMemberList(){
        //Hide dialog
        Function.dismissAppErrorDialog(activity);
        //Check internet
        if (Function.internetConnected(context)){
            //Loading
            showContent(LOADING);
            //Read people list
            HomeManager homeManager = new HomeManager(context);
            homeManager.readMemberList(
                    userId, homeItem.getGroupId(), new HomeManager.OnReadMemberListener() {
                        @Override
                        public void onSuccess(List<MemberItem> memberItems) {
                            //Check list
                            int no = memberItems.size();
                            if (no<1){
                                //Generate QR code
                                setQrBitmap();
                            }else {
                                //Set total text
                                String txt = adapter.getContext().getString(R.string.member)
                                        +" ("+no+")";
                                numMemberTxt.setText(txt);
                                //Show list
                                adapter.addList(memberItems);
                                //Show member list
                                showContent(CONTENT);
                            }
                        }

                        @Override
                        public void onFailed(String error) {
                            Function.showDBErrorDialog(getActivity(), error, errorListener);
                        }
                    });

        }else {
            Function.showNoInternetDialog(getActivity(), errorListener);
        }

    }

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    //Thank: https://recordnotfound.com/AwesomeQRCode-SumiMakito-152234
    private void setQrBitmap(){
        //Set background, can't use background bitmap to QR without resize
        Bitmap or = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_qr);
        Bitmap bg = Bitmap.createScaledBitmap(or, 200, 200, true); //Scale
        //Generate QR
        qrBitmap = AwesomeQRCode.create(Function.encodeQRDetail(homeItem.getGroupId()), 200, 24, 0.3f,
                Color.BLACK, Color.WHITE, bg, true, true);
        //Set to image and show
        qrImg.setImageBitmap(qrBitmap);
        //Show content
        showContent(NO_MEMBER);
    }

    private void trySaveFile(){
        //Check
        if (!Function.hasPermissions(context, Constant.WRITE_EXTERNAL)){
            //Request permission from user
            ActivityCompat.requestPermissions(activity, Constant.WRITE_EXTERNAL, Constant.WRITE_CODE);
        }else {
            //Save to build-in flash memory
            //Thank: https://stackoverflow.com/questions/25608993/ioexception-no-such-file-or-directory-android
            //Set file name
            String fname = "AirconHome-QR-"+homeItem.getGroupId()+".jpg";
            //Create the parent path
            String path = Environment.getExternalStorageDirectory()+"/Aircon Home/";
            File dir = new File(path);
            if (!dir.exists()) { dir.mkdirs(); }
            //Set full path
            String fullName = path + fname;
            File file = new File (fullName);
            Log.w(TAG, "Path: "+file.getAbsolutePath());
            try {
                //Try save
                FileOutputStream out = new FileOutputStream(file);
                qrBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                Toast.makeText(context, R.string.saved, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * Saves the image as PNG to the app's cache directory.
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    private Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "qr-shared.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, Constant.MY_FILE_PROVIDER, file);

        } catch (IOException e) {
            Log.e(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    /**
     * Shares the PNG image from Uri.
     * @param uri Uri of image to share.
     *
     * https://stackoverflow.com/a/50924037
     */
    private void shareImageUri(Uri uri){
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/png");
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Check result
        if(requestCode == Constant.WRITE_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //User allowed writing, so re-save
                trySaveFile();
            }else {
                //Show message
                Function.showToast(context, R.string.denied_write);
            }
        }
    }


    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            switch (titleId){
                case R.string.no_result:
                case R.string.no_internet:
                    //Retry
                    tryReadMemberList();
                    break;
            }

        }
    };

}
