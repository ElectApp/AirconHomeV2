package com.apyeng.airconhomev2;

import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.apyeng.airconhomev2.adapters.LabelDetailAdapter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.util.ArrayList;
import java.util.List;

public class EditLabelActivity extends AppCompatActivity {


    //Layout
    private ImageView actionImg;
    private ClearableEditText nameEnter;
    private TextView selectedTxt, selectAllTxt;
    //Data
    private int groupId, userId, cTotalSelected = 0;
    private LabelItem labelItem;
    private List<LabelItem> acItems;
    private LabelDetailAdapter adapter;
    //Other
    private HomeManager homeManager;
    private View sectionView[];
    private static final int LOADING = 0, CONTENT = 1, NO_CONTENT = 2;
    private static final String TAG = "EditLabelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_label);

        //Layout
        TextView titleTxt = findViewById(R.id.title_toolbar);
        actionImg = findViewById(R.id.action_icon);
        nameEnter = findViewById(R.id.name_enter);
        selectedTxt = findViewById(R.id.selected_tv);
        selectAllTxt = findViewById(R.id.select_all_tv);
        RecyclerView acRv = findViewById(R.id.ac_rv);

        sectionView = new View[3];
        sectionView[0] = findViewById(R.id.circle_progress);
        sectionView[1] = findViewById(R.id.container);
        sectionView[2] = findViewById(R.id.no_content);

        //Get data
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        labelItem = getIntent().getParcelableExtra(Constant.DATA);
        //Check data
        if(groupId==0 || userId==0){
            throw new IllegalArgumentException("Must pass groupId and userId...");
        }

        //Back
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        //Action
        actionImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save to DB
                saveLabel();
            }
        });
        selectAllTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toggle Select All
                boolean nMode = !selectAllTxt.isSelected();
                for (LabelItem item : acItems){
                    item.setSelected(nMode);
                }
                adapter.notifyDataSetChanged();
                cTotalSelected = nMode? acItems.size():0;
                countNumSelected(0);
                //Text
                selectAllTxt.setText(nMode? R.string.unselect_all:R.string.select_all);
                //Save state
                selectAllTxt.setSelected(nMode);
            }
        });

        //Initial
        //Title
        titleTxt.setText(labelItem==null? R.string.add_label:R.string.edit_label);
        //Name
        nameEnter.setText(labelItem==null? "":labelItem.getText());
        //Set char counter to usernameEnter
        nameEnter.addTextChangedListener(
                new LimitCharOnEditText(20, (TextView)findViewById(R.id.name_counter)));
        //Set Keyboard listener
        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                nameEnter.setCursorVisible(isOpen);
            }
        });
        //Summary
        cTotalSelected = labelItem==null? 0:labelItem.getTotalDevices();
        countNumSelected(0);
        //Manager
        homeManager = new HomeManager(this);
        //List
        ItemOffsetDecoration decoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        acItems = new ArrayList<>();
        adapter = new LabelDetailAdapter(this, acItems);
        adapter.setEditMode(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,
                Function.getGirdColumn(this, this, 220),
                GridLayoutManager.VERTICAL, false);
        acRv.setLayoutManager(gridLayoutManager);
        acRv.setAdapter(adapter);
        //Set space
        acRv.addItemDecoration(decoration);
        //Set RecyclerView smooth scrolling with NestScrollView
        ViewCompat.setNestedScrollingEnabled(acRv, false);
        //Click
        adapter.setOnLabelClickListener(new LabelDetailAdapter.OnLabelClickListener() {
            @Override
            public void onClick(View view, LabelItem labelItem, int position) {
                //Update selected item
                LabelItem labelItem1 = acItems.get(position);
                if (labelItem1.isSelected()){
                    countNumSelected(-1);
                    labelItem1.setSelected(false);
                }else {
                    countNumSelected(1);
                    labelItem1.setSelected(true);
                }
                acItems.set(position, labelItem1);
                adapter.notifyItemChanged(position);
            }
        });
        //Get A/C list
        readAllDevice();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private void readAllDevice(){
        showContent(LOADING);
        if (Function.internetConnected(this)){
            homeManager.readFullDeviceDetail(groupId, 0,
                    new HomeManager.OnReadFullDeviceDetailListener() {
                @Override
                public void onSuccess(List<AirItem> airItems) {
                    Log.w(TAG, "OnSuccess...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(EditLabelActivity.this);
                    //Show full list
                    acItems.clear();
                    for (AirItem item : airItems){
                        acItems.add(new LabelItem(item.getDeviceId(), item.getNickname(),
                                item.getActualName(), false));
                    }
                    //Update select item
                    if (acItems.size()>0){
                        if (labelItem!=null){
                            //Next to read device in label
                            readDeviceInLabel();
                        }else {
                            //Show data
                            showContent(CONTENT);
                        }
                    }else {
                        showContent(NO_CONTENT);
                    }
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showDBErrorDialog(EditLabelActivity.this, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(EditLabelActivity.this, errorListener);
        }
    }

    private void readDeviceInLabel(){
        if (Function.internetConnected(this)){
            homeManager.readFullDeviceDetail(groupId, labelItem.getId(),
                    new HomeManager.OnReadFullDeviceDetailListener() {
                        @Override
                        public void onSuccess(List<AirItem> airItems) {
                            Log.w(TAG, "OnSuccess...");
                            //Hide dialog
                            Function.dismissAppErrorDialog(EditLabelActivity.this);
                            //Update list
                            for (AirItem itemS : airItems){
                                for (LabelItem itemA : acItems){
                                    if (itemA.getId()==itemS.getDeviceId()){
                                        itemA.setSelected(true);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                            //Show data
                            showContent(CONTENT);
                        }

                        @Override
                        public void onFailed(String error) {
                            Log.w(TAG, "OnFailed..."+error);
                            Function.showDBErrorDialog(EditLabelActivity.this, error, errorListener);
                        }
                    });
        }else {
            Function.showNoInternetDialog(EditLabelActivity.this, errorListener);
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            readAllDevice();
        }
    };

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    private void countNumSelected(int upDown){
        cTotalSelected += upDown;
        String t = getString(R.string.selected)+" "+cTotalSelected+" "+getString(R.string.devices);
        selectedTxt.setText(t);
        //Enable Save Icon
        actionImg.setEnabled(cTotalSelected>0);
    }

    private void saveLabel(){
        //Hide keyboard
        KeyboardUtils.hideKeyboard(this);
        //Check data
        nameEnter.setSelected(false);
        String name = String.valueOf(nameEnter.getText());
        if (name.isEmpty()){
            Function.setRequestEnter(this, nameEnter);
            return;
        }
        //Try save
        showContent(LOADING);
        if (Function.internetConnected(this)){
            //Label details
            if (labelItem==null){
                labelItem = new LabelItem(0, name, 0, false);
            }else {
                labelItem.setText(name);
            }
            //Devices
            int idD[] = new int[acItems.size()];
            int z = 0;
            for (LabelItem item : acItems){
                if (item.isSelected()){
                    idD[z++] = item.getId();
                }
            }
            //Save
            homeManager.addOrEditDeviceInLabel(groupId, userId, labelItem, idD, new HomeManager.OnListener() {
                @Override
                public void onSuccess() {
                    Log.w(TAG, "Save label success...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(EditLabelActivity.this);
                    //Short delay for waiting DB updated 'total_devices'
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Set result and back
                            setResult(RESULT_OK);
                            finish();
                        }
                    }, 300);
                }

                @Override
                public void onFailed(String error) {
                    Log.e(TAG, "Save label failed..."+error);
                    Function.showDBErrorDialog(EditLabelActivity.this, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(EditLabelActivity.this, errorListener);
        }
    }


}
