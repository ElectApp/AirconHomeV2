package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.DialogFragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class FullScreenPictureDialog extends DialogFragment {

    public static final String TAG = "FullScreenPictureDialog";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set theme
        setStyle(DialogFragment.STYLE_NORMAL, R.style.GeneralDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_full_screen_picture, container, false);

        byte[] imgByteArray = getArguments().getByteArray(Constant.PROFILE_IMG);
        Bitmap bitmap = Function.getBitmap(imgByteArray);

        ImageView imageView = view.findViewById(R.id.image_view);
        imageView.setImageBitmap(bitmap);

        return view;
    }

    @Override
    public void onResume() {
        //Set size dialog layout
        Window window = getDialog().getWindow();
        if (window!=null){
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            //Set color TRANSPARENT to outside round layout of dialog
            //Thank: https://www.codingdemos.com/android-custom-dialog-animation/
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        super.onResume();
    }


}
