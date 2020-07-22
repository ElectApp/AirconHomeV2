package com.apyeng.airconhomev2;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.apyeng.airconhomev2.adapters.LabelDetailAdapter;

import java.util.ArrayList;
import java.util.List;

public class ManageLabelActivity extends AppCompatActivity {

    private int groupId, userId;
    private ImageView actionImg;
    private HomeManager homeManager;
    private List<LabelItem> labelItems;
    private LabelDetailAdapter adapter;
    private View sectionView[];
    private int resultCode;
    private ArrayList<Integer> deletedId;
    private static final int LOADING = 0, CONTENT = 1, NO_CONTENT = 2, CHANGE_LABEL_CODE = 1027;
    private static final String TAG = "ManageLabelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_label);

        //Layout
        actionImg = findViewById(R.id.action_icon);
        RecyclerView labelRv = findViewById(R.id.label_rv);

        sectionView = new View[3];
        sectionView[0] = findViewById(R.id.circle_progress);
        sectionView[1] = labelRv;
        sectionView[2] = findViewById(R.id.no_label_content);

        //Get data
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);

        //Back
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.isEditMode()){
                    closeDeleteMode();
                }else {
                    backToPreviously();
                }
            }
        });

        //Action
        actionImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionImg.isSelected()){
                    //Delete label
                    deleteLabel();
                }else {
                    //Add label
                    startEditLabelActivity(null);
                }
            }
        });

        //Initial
        homeManager = new HomeManager(this);
        showDeleteIcon(false);
        resultCode = RESULT_CANCELED;
        //List
        labelItems = new ArrayList<>();
        adapter = new LabelDetailAdapter(this, labelItems);
        LinearLayoutManager rvManager = new LinearLayoutManager(this);
        labelRv.setLayoutManager(rvManager);
        labelRv.setAdapter(adapter);
        //Click
        adapter.setOnLabelClickListener(new LabelDetailAdapter.OnLabelClickListener() {
            @Override
            public void onClick(View view, LabelItem labelItem, int position) {
                if (adapter.isEditMode()){
                    //Toggle Select or Not select
                    labelItem.setSelected(!labelItem.isSelected());
                    labelItems.set(position, labelItem);
                    adapter.notifyItemChanged(position);
                }else {
                    //Go to EditLabelActivity
                    startEditLabelActivity(labelItem);
                }
            }
        });
        //Enable edit mode
        adapter.setOnLabelLongClickListener(new LabelDetailAdapter.OnLabelLongClickListener() {
            @Override
            public void onClick(View view, LabelItem labelItem, int position) {
                //Toggle edit mode
                if (adapter.isEditMode()){
                    closeDeleteMode();
                }else {
                    adapter.setEditMode(true);
                    showDeleteIcon(true);
                }
                adapter.notifyDataSetChanged();
            }
        });

        //Load label details
        tryReadLabelDetail();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Reload label details
        if (resultCode==RESULT_OK){
            this.resultCode |= RESULT_OK;
            tryReadLabelDetail();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Back
        backToPreviously();
    }

    private void backToPreviously(){
        //Set result
        Log.w(TAG, "Result Code: "+resultCode);
        setResult(resultCode);
        finish();
    }

    private void showDeleteIcon(boolean show){
        actionImg.setImageResource(show? R.drawable.ic_delete : R.drawable.ic_add_circle);
        actionImg.setSelected(show);
    }

    private void tryReadLabelDetail(){
        showContent(LOADING);
        if (Function.internetConnected(this)){
            Log.w(TAG, "Read label list...");
            homeManager.readLabelDetail(groupId, new HomeManager.OnReadLabelCallback() {
                @Override
                public void onSuccess(List<LabelItem> items) {
                    Log.w(TAG, "Read success...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(ManageLabelActivity.this);
                    //Show label
                    labelItems.clear();
                    labelItems.addAll(items);
                    adapter.notifyDataSetChanged();
                    showContent(labelItems.size()>0? CONTENT:NO_CONTENT);
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "Read failed..."+error);
                    Function.showDBErrorDialog(ManageLabelActivity.this, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(this, errorListener);
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            tryReadLabelDetail();
        }
    };

    public void deleteLabel(){
        //Selected
        deletedId = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean f = false; LabelItem item;
        for (int i=0; i<labelItems.size(); i++){
            item = labelItems.get(i);
            Log.w(TAG, "["+i+"] "+item.isSelected());
            if (item.isSelected()){
                if (!f){
                    f = true;
                }else {
                    builder.append(" OR ");
                }
                builder.append("label_id=");
                builder.append(item.getId());
                //Add deleted list
                deletedId.add(item.getId());
            }
        }
        //Selected?
        if (!f){ return; }

        //Start
        showContent(LOADING);
        //Action
        if (Function.internetConnected(this)){
            String sql = "DELETE FROM label_data WHERE "+builder.toString();
            Log.w(TAG, sql);
            homeManager.insertUpdateAnyGroupTable(groupId, sql, new HomeManager.OnSingleStringCallback() {
                @Override
                public void onSuccess(String value) {
                    Log.w(TAG, "Delete success...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(ManageLabelActivity.this);
                    //Back to normal mode
                    showDeleteIcon(false);
                    adapter.setEditMode(false);
                    //Update list
                    for (int i : deletedId){
                        for (int x=0; x<labelItems.size(); x++){
                            if (labelItems.get(x).getId()==i){ labelItems.remove(x); break; }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    //Show Data
                    showContent(labelItems.size()>0? CONTENT:NO_CONTENT);
                    //Save flag
                    resultCode |= RESULT_OK;
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "Delete failed..."+error);
                    Function.showDBErrorDialog(ManageLabelActivity.this, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(this, errorListener);
        }
    }

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    private void closeDeleteMode(){
        showDeleteIcon(false);
        adapter.setEditMode(false);
        for (LabelItem item : labelItems){ item.setSelected(false); }
        adapter.notifyDataSetChanged();
    }

    private void startEditLabelActivity(LabelItem labelItem){
        Intent intent = new Intent(ManageLabelActivity.this, EditLabelActivity.class);
        intent.putExtra(Constant.DATA, labelItem);
        intent.putExtra(Constant.GROUP_ID, groupId);
        intent.putExtra(Constant.USER_ID, userId);
        startActivityForResult(intent, CHANGE_LABEL_CODE);
    }



}
