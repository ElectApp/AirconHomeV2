package com.apyeng.airconhomev2;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;


public class SplashActivity extends AppCompatActivity implements DialogInterface.OnDismissListener{

    private ScrollView scrollView;
    private LinearLayout usernameLay, rootLay;
    private RoundButtonWidget signAction, signInTab, signUpTab;
    private ClearableEditText usernameEnter, emailEnter;
    private PasswordEditText passEnter;
    private View triLeft, triRight;
    private String name, email, password;
    private int userId;
    private OnDevice onDevice;
    private TextView forgetBtn;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        scrollView = findViewById(R.id.scroll_view);

        signInTab = findViewById(R.id.sign_in_tab);
        signUpTab = findViewById(R.id.sign_up_tab);
        signAction = findViewById(R.id.sign_action);
        forgetBtn = findViewById(R.id.forget_action);

        rootLay = findViewById(R.id.root_lay);
        usernameLay = findViewById(R.id.username_lay);

        usernameEnter = findViewById(R.id.enter_username);
        emailEnter = findViewById(R.id.enter_email);
        passEnter = findViewById(R.id.enter_password);

        triLeft = findViewById(R.id.triangle_left);
        triRight = findViewById(R.id.triangle_right);

        signInTab.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                setSignAction(true);
            }
        });

        signUpTab.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                setSignAction(false);
            }
        });

        signAction.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Check data
                if (hasData()){
                    //Check action
                    if (usernameEnter.getVisibility()==View.VISIBLE){
                        //Sign Up
                        signUpWithEmail();
                    }else {
                        //Sign in
                        signInWithEmail();
                    }
                }

            }
        });

        forgetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Show ResetPasswordDialog
                ResetPasswordDialog dialog = new ResetPasswordDialog();
                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                dialog.show(transaction, ResetPasswordDialog.TAG);

            }
        });

        //Get device data
        onDevice = new OnDevice(this, OnDevice.USER_FILE);
        userId = onDevice.getUserId();
        Log.w(TAG, "ID: "+userId);

        //Set char counter to usernameEnter
        usernameEnter.addTextChangedListener(
                new LimitCharOnEditText(20, (TextView)findViewById(R.id.counter)));

        //Set Keyboard listener
        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                usernameEnter.setCursorVisible(isOpen);
                emailEnter.setCursorVisible(isOpen);
                passEnter.setCursorVisible(isOpen);
                if (isOpen){
                    //Scroll up to top without set adjustResize at Manifest
                    //Must move sign in or sign up button to above Soft Keyboard
                    scrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //replace this line to scroll up or down
                            //For this code render the cursor on EditText will change to the last item
                            //scrollView.fullScroll(ScrollView.FOCUS_DOWN);

                            //My method set position > maximum height content layout
                            scrollView.scrollTo(0, 500);
                        }
                    }, 20L);
                }
            }
        });


        //Initial
        setSignAction(true);

    }


    @Override
    protected void onStart() {
        super.onStart();

        //Check userId from saving on device
        if (userId>0){
            //Start HomeListActivity
            intentHomeListActivity();
        }else {
            //Show Sign in, sign up layout
            rootLay.setVisibility(View.VISIBLE);
        }

    }


    private void showVerifyDialog(){
        //Pass data to dialog
        Bundle bundle = new Bundle();
        bundle.putString(Constant.EMAIL, email);
        //Create dialog
        VerifyDialog dialog = new VerifyDialog();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        dialog.setArguments(bundle);
        dialog.show(transaction, VerifyDialog.TAG);
    }

    private void setSignAction(boolean signIn){
        //Hide keyboard
        KeyboardUtils.hideKeyboard(this);
        //Clear
        clearAll();
        //Color
        int ca = getResources().getColor(R.color.colorAccent);
        float eva = getResources().getDimension(R.dimen.g_eva);
        //Set display
        if (signIn){
            //Sign In display
            signInTab.setColor(Color.WHITE, ca);
            signInTab.setCardElevation(eva);
            signUpTab.setColor(Color.TRANSPARENT, Color.WHITE);
            triLeft.setVisibility(View.VISIBLE);
            triRight.setVisibility(View.INVISIBLE);
            usernameLay.setVisibility(View.GONE);
            usernameEnter.setVisibility(View.GONE);
            signAction.setText(R.string.sign_in);
            //Show forget button
            forgetBtn.setVisibility(View.VISIBLE);
        }else {
            //Sign Up display
            signUpTab.setColor(Color.WHITE, ca);
            signUpTab.setCardElevation(eva);
            signInTab.setColor(Color.TRANSPARENT, Color.WHITE);
            triLeft.setVisibility(View.INVISIBLE);
            triRight.setVisibility(View.VISIBLE);
            usernameLay.setVisibility(View.VISIBLE);
            usernameEnter.setVisibility(View.VISIBLE);
            signAction.setText(R.string.sign_up);
            //Hide forget button
            forgetBtn.setVisibility(View.GONE);
        }
    }

    private void clearAll(){
        clear(usernameEnter);
        clear(emailEnter);
        clear(passEnter);
        passEnter.showPassword(false);
    }

    private void clear(ClearableEditText editText){
        editText.setText("");
        editText.setSelected(false);
    }

    private void clear(PasswordEditText editText){
        editText.setText("");
        editText.setSelected(false);
    }

    private boolean hasData(){
        //Clear all
        usernameEnter.setSelected(false);
        emailEnter.setSelected(false);
        passEnter.setSelected(false);
        //Get data for user
        name = String.valueOf(usernameEnter.getText());
        email = String.valueOf(emailEnter.getText());
        password = String.valueOf(passEnter.getText());
        //Check empty
        if (usernameEnter.getVisibility()==View.VISIBLE && name.isEmpty()){
            Function.setRequestEnter(this, usernameEnter);
            return false;
        }else if (email.isEmpty()){
            Function.setRequestEnter(this, emailEnter);
            return false;
        }else if (password.isEmpty()){
            Function.setRequestEnter(this, passEnter);
            return false;
        }
        return true;
    }

    private void signUpWithEmail(){
            //Show progress
            Function.showLoadingDialog(this);
            //Run MySQL via PHP script
            Ion.with(this)
                    .load(Constant.SIGN_UP_URL)
                    .setBodyParameter(Constant.LANGUAGE, Function.getLanguage())
                    .setBodyParameter(Constant.USERNAME, name)
                    .setBodyParameter(Constant.EMAIL, email)
                    .setBodyParameter(Constant.PASSWORD, password)
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            //Closed progress
                            Function.dismissDialogFragment(SplashActivity.this, PageLoadingDialog.TAG);
                            //Check result
                            Log.e(TAG, "Error: "+e);
                            Log.w(TAG, "Result: "+result);
                            if (result!=null && !result.isEmpty()){
                                if (result.equals(Constant.SUCCESS)){
                                    //Show Verify Dialog
                                    showVerifyDialog();
                                }else {
                                    showError(result);
                                }
                            }else if (e!=null){
                                //Show error
                                showError(e.getMessage());
                            }else {
                                //No result
                                showError(R.string.no_result);
                            }
                        }
                    });

    }

    private void signInWithEmail(){
        //Show progress
        Function.showLoadingDialog(this);
        //Run MySQL via PHP script
        Ion.with(this)
                .load(Constant.EMAIL_SIGN_IN_URL)
                .setBodyParameter(Constant.LANGUAGE, Function.getLanguage())
                .setBodyParameter(Constant.EMAIL, email)
                .setBodyParameter(Constant.PASSWORD, password)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //Close
                        Function.dismissDialogFragment(SplashActivity.this, PageLoadingDialog.TAG);
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isJsonNull()){
                            //Get result
                            if (result.get(Constant.SUCCESS).getAsBoolean()){
                                //Get some data
                                int status = result.get(Constant.STATUS).getAsInt();
                                //Check status
                                if (status==0){
                                    //Not verify
                                    showVerifyDialog();
                                }else {
                                    //Update and save data
                                    userId = result.get(Constant.USER_ID).getAsInt();
                                    onDevice.saveUserId(userId);
                                    //Start HomeListActivity
                                    intentHomeListActivity();
                                }
                            }else {
                                showError(result.get(Constant.CAUSE).getAsString());
                            }
                        }else if (e!=null){
                            showError(e.getMessage());
                        }else {
                            showError(R.string.no_result);
                        }
                    }
                });
    }


    private void showError(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Function.setOneShortVibrator(this);
    }

    private void showError(int messageId){
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        Function.setOneShortVibrator(this);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        //VerifyDialog and NoNetworkDialog dismiss will call this here
        //Change tab to Sign in
        setSignAction(true);
    }


    private void intentHomeListActivity(){
        Intent intent = new Intent(this, HomeListActivity.class);
        intent.putExtra(Constant.USER_ID, userId);
        startActivity(intent);
        finish();
    }




}
