package com.apyeng.airconhomev2;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

public class PageLoadingDialog extends DialogFragment {

    public static final String TAG = "PageLoadingDialog"; //Unique TAG

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.LoadingDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.loading_dialog, container, false);

        //Set Animation to Aircon Icon
        //Thank: https://www.youtube.com/watch?v=scZYIAZrMWk
        ImageView icon = view.findViewById(R.id.load_icon);
        icon.setBackgroundResource(R.drawable.loading_animation);
        AnimationDrawable anim = (AnimationDrawable)icon.getBackground();
        anim.start();

        return view;

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
                window.getAttributes().windowAnimations = R.style.LoadingDialogAnimation;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        //Set size dialog
        Dialog dialog = getDialog();
        if(dialog!=null){
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window!=null){
                window.setLayout(width, height);
            }
            //Block cancel from Back button on navigator bar
            //dialog.setCancelable(false);
        }

    }


}
