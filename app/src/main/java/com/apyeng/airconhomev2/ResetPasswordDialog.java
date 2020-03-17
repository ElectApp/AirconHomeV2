package com.apyeng.airconhomev2;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;


public class ResetPasswordDialog extends DialogFragment {

    private ClearableEditText emailEnter;
    private RoundButtonWidget actionBtn;
    private TextView emailTxt;
    private View sView[];
    private Context context;
    private Activity activity;
    private boolean sentFinish;
    private static final int INITIAL = 0, COMPLETE = 1;
    public static final String TAG = "ResetPasswordDialog";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set style
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_reset_password, container, false);
        //Declare object in layout
        emailTxt = v.findViewById(R.id.email_txt);
        emailEnter = v.findViewById(R.id.enter_email);
        actionBtn = v.findViewById(R.id.reset_action_btn);
        sView = new View[2];
        sView[0] = v.findViewById(R.id.lay_email);
        sView[1] = v.findViewById(R.id.lay_sent_finish);

        actionBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                if (sentFinish){
                    //Close dialog
                    dismiss();
                }else {
                    //Clear alert
                    emailEnter.setSelected(false);
                    //Get data
                    String email = String.valueOf(emailEnter.getText());
                    if (email.isEmpty()){
                        Function.setRequestEnter(context, emailEnter);
                    }else {
                        //Send email
                        sendEmail(email);
                    }
                }
            }
        });

        //Initial
        setSentFinish(false);

        return v;
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
                //Set animation
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
                //Set resize layout when keyboard is visible
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }


    @Override
    public void onResume() {
        //Set size dialog layout
        Window window = getDialog().getWindow();
        if (window!=null){
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.activity = getActivity();
    }


    private void showState(int n){
        for (int i=0; i<sView.length; i++){
            sView[i].setVisibility(n==i? View.VISIBLE : View.GONE);
        }
    }

    private void setSentFinish(boolean finish){
        //Set flag
        sentFinish = finish;
        //Set text on button
        actionBtn.setText(finish? R.string.ok : R.string.reset_password);
        //Layout
        showState(finish? COMPLETE : INITIAL);
    }

    private void sendEmail(final String email){
        //Show loading
        Function.showLoadingDialog(activity);
        //Run MySQL via PHP script
        Ion.with(this)
                .load(Constant.SEND_RESET_PASSWORD_EMAIL)
                .setBodyParameter(Constant.EMAIL, email)
                .setBodyParameter(Constant.LANGUAGE, Function.getLanguage())
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        //Close
                        Function.dismissDialogFragment(activity, PageLoadingDialog.TAG);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isEmpty()){
                            //Check result
                            if (result.equals(Constant.SUCCESS)){
                                setSentFinish(true);
                                emailTxt.setText(email);
                            }else {
                                showError(result);
                            }
                        }else if (e!=null){
                            showError(e.getMessage());
                        }else {
                            showError(context.getResources().getString(R.string.no_result));
                        }

                    }
                });
    }

    private void showError(String message){
        Function.showToast(context, message);
        Function.setOneShortVibrator(context);
        setSentFinish(false);
    }


}
