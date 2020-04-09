package com.apyeng.airconhomev2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class RenameACActivity extends AppCompatActivity {

    private AirItem airItem;
    private int groupId, positionId;
    private ClearableEditText nicknameEnter;
    private static final String TAG = "RenameACActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename_ac);

        //Get data
        airItem = getIntent().getParcelableExtra(Constant.AC_DATA);
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);
        positionId = getIntent().getIntExtra(Constant.POSITION_ID, positionId);
        if(airItem==null || groupId==0){
            throw new IllegalArgumentException("Must pass AC data...");
        }

        //Set Title
        TextView titleTxt = findViewById(R.id.title_toolbar);
        titleTxt.setText(R.string.rename);

        //Nickname
        nicknameEnter = findViewById(R.id.ac_name_enter);
        nicknameEnter.addTextChangedListener(
                new LimitCharOnEditText(20, (TextView)findViewById(R.id.ac_name_counter)));

        //Current name
        String name = airItem.getNickname();
        if (name!=null){ nicknameEnter.setText(name); }

        //Back icon
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Save btn
        RoundButtonWidget saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //New nickname
                String n = String.valueOf(nicknameEnter.getText()).trim();
                if(n.isEmpty()){
                    Function.setRequestEnter(RenameACActivity.this, nicknameEnter);
                }else {
                    //Save to DB
                    airItem.setNickname(n);
                    saveNickname();
                }
            }
        });
    }

    private void saveNickname(){
        String sql = "UPDATE device_data SET nickname='"+airItem.getNickname()+"' WHERE device_id="+airItem.getDeviceId();
        HomeManager manager = new HomeManager(this, this);
        manager.insertUpdateAnyGroupTable(groupId, sql, new HomeManager.OnSingleStringCallback() {
            @Override
            public void onSuccess(String value) {
                Log.w(TAG, "Save nickname success...");
                Function.showToast(RenameACActivity.this, R.string.saved);
                //Set data
                Intent intent = new Intent();
                intent.putExtra(Constant.POSITION_ID, positionId);
                intent.putExtra(Constant.AC_DATA, airItem);
                setResult(RESULT_OK, intent);
                //Back
                finish();
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "Save new nickname error: "+error);
                Function.showToast(RenameACActivity.this, R.string.no_result);
            }
        });
    }



}
