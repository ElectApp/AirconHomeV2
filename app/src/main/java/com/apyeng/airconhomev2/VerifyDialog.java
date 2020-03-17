package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class VerifyDialog extends DialogFragment {

    private String email;
    public static final String TAG = "VerifyDialog"; //Unique TAG


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        //Get data
        Bundle bundle = getArguments();
        email = bundle.getString(Constant.EMAIL, "");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.verify_dialog, container, false);

        TextView emailTxt = view.findViewById(R.id.email_txt);

        RoundButtonWidget okBtn = view.findViewById(R.id.ok_btn);
        RoundButtonWidget resendBtn = view.findViewById(R.id.resend_btn);

        emailTxt.setText(email);

        okBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //close
                if (isVisible()){ dismiss(); }
            }
        });

        resendBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //resend email
                resendEmailVerification();
            }
        });

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
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
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


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        //Callback listener when dialog dismiss
        //Thank: https://stackoverflow.com/questions/23786033/dialogfragment-and-ondismiss

        final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener){
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }

    }


    private void resendEmailVerification(){
        //Show loading
        Function.showLoadingDialog(getActivity());
        //Run MySQL via PHP script
        Ion.with(this)
                .load(Constant.RESEND_EMAIL_VERIFY)
                .setBodyParameter(Constant.EMAIL, email)
                .setBodyParameter(Constant.LANGUAGE, Function.getLanguage())
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        //Close
                        Function.dismissDialogFragment(getActivity(), PageLoadingDialog.TAG);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            showToast(result);
                        }else if (e!=null){
                            showToast(e.getMessage());
                        }else {
                            showToast(R.string.no_result);
                        }
                    }
                });
    }

    private void showToast(String message){
        final Context context = getDialog().getContext();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Function.setOneShortVibrator(context);
    }

    private void showToast(int messageId){
        final Context context = getDialog().getContext();
        Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
        Function.setOneShortVibrator(context);
    }

}
