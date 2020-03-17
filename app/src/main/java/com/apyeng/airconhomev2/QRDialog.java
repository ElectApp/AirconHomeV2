package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.app.DialogFragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.sumimakito.awesomeqr.AwesomeQRCode;

import java.io.File;
import java.io.FileOutputStream;

public class QRDialog extends DialogFragment {

    private int id;
    private Bitmap qrBitmap;
    public static final String TAG = "QRDialog";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set theme
        setStyle(android.app.DialogFragment.STYLE_NORMAL, R.style.GeneralDialog);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_qr, container, false);

        ImageView qrImg = view.findViewById(R.id.qr_img);
        RoundButtonWidget saveBtn = view.findViewById(R.id.save_btn);

        //Get ID
        id = getArguments().getInt(Constant.GROUP_ID, 0);
        //Set background, can't use background bitmap to QR without resize
        Bitmap or = BitmapFactory.decodeResource(getResources(), R.drawable.logo_qr);
        Bitmap bg = Bitmap.createScaledBitmap(or, 200, 200, true); //Scale
        //Generate QR
        qrBitmap = AwesomeQRCode.create(Function.encodeQRDetail(id), 200, 24, 0.3f,
                Color.BLACK, Color.WHITE, bg, true, true);
        //Set to image and show
        qrImg.setImageBitmap(qrBitmap);

        //Save button
        saveBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Save
                trySaveFile();
            }
        });

        //Close icon
        view.findViewById(R.id.close_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set animation here!
        //Thank: https://stackoverflow.com/questions/41869348/enter-and-exit-animations-not-working-in-dialog-fragment
        Dialog dialog = getDialog();
        if(dialog!=null){
            Window window = dialog.getWindow();
            if (window!=null){
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
            }
        }
    }

    private void trySaveFile(){
        Context context = getDialog().getContext();
        //Check
        if (!Function.hasPermissions(context, Constant.WRITE_EXTERNAL)){
            //Request permission from user
            ActivityCompat.requestPermissions(getActivity(), Constant.WRITE_EXTERNAL, Constant.WRITE_CODE);
        }else {
            //Save to build-in flash memory
            //Thank: https://stackoverflow.com/questions/25608993/ioexception-no-such-file-or-directory-android
            //Set file name
            String fname = "AirconHome-QR-"+id+".jpg";
            //Create the parent path
            String path = Environment.getExternalStorageDirectory() + "/Aircon Home/";
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
                Toast.makeText(getDialog().getContext(),
                        R.string.denied_write, Toast.LENGTH_LONG).show();
            }
        }
    }
}
