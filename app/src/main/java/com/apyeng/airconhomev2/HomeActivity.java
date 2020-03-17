package com.apyeng.airconhomev2;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;

public class HomeActivity extends AppCompatActivity {

    private int userId, tabSelected;
    private HomeItem homeItem;
    private ImageView menuIcon[];
    private MainFragment mainFragment;
    private static final String SELECTED = "selected";
    private static final int ADD_DEVICE_CODE = 1641;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //Get data
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        homeItem = getIntent().getParcelableExtra(Constant.HOME_DATA);



        if (userId==0 || homeItem==null){
            throw new IllegalArgumentException("Must pass user id and home data");
        }

        //Set menu array size
        menuIcon = new ImageView[2];

        //Declare object in layout
        TextView title = findViewById(R.id.title_toolbar);
        ImageView backIcon = findViewById(R.id.back_icon);
        menuIcon[0] = findViewById(R.id.add_ac_icon);
        menuIcon[1] = findViewById(R.id.add_member_icon);
        AHBottomNavigation bottomNav = findViewById(R.id.main_bottom_nav);

        //Set toolbar
        title.setText(homeItem.getName());

        //Back icon
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Back to HomeListActivity
                finish();
            }
        });

        //ADD device icon
        menuIcon[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Disconnect MQTT on MainFragment
                mainFragment.pauseService(true);
                //Set data
                Intent intent = new Intent(HomeActivity.this, AddDeviceActivity.class);
                intent.putExtra(Constant.USER_ID, userId);
                intent.putExtra(Constant.GROUP_ID, homeItem.getGroupId());
                intent.putExtra(Constant.NAME, homeItem.getName());
                //startActivity(intent);
                startActivityForResult(intent, ADD_DEVICE_CODE);
            }
        });

        //QR code
        menuIcon[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Create QR dialog
                Bundle bundle = new Bundle();
                bundle.putInt(Constant.GROUP_ID, homeItem.getGroupId());
                QRDialog dialog = new QRDialog();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                dialog.setArguments(bundle);
                dialog.show(transaction, QRDialog.TAG);
            }
        });

        //Set BottomNavigation
        AHBottomNavigationAdapter adapter = new AHBottomNavigationAdapter(this,
                R.menu.main_bottom_nav);
        adapter.setupWithBottomNavigation(bottomNav);
        // Change colors
        int[] colorIcon = getResources().getIntArray(R.array.colorIconBottomNav);
        bottomNav.setAccentColor(colorIcon[0]);
        bottomNav.setInactiveColor(colorIcon[1]);
        //Set notification on share item
        //bottomNav.setNotification("3", 1);
        //Add listener
        bottomNav.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                Log.w(TAG, "Tab select: "+position);

                tabSelected = position;
                selectFragment();

                return true;
            }
        });

        //Update active time
        HomeManager homeManager = new HomeManager(this);
        homeManager.updateRegisteredTime(userId, homeItem.getGroupId(), new HomeManager.OnListener() {
            @Override
            public void onSuccess() {
                Log.w(TAG, "Update registered time success...");
            }

            @Override
            public void onFailed(String error) {
                Log.w(TAG, "Update registered time failed..."+error);
            }
        });

        //Get last current tab
        if (savedInstanceState!=null){
            tabSelected = savedInstanceState.getInt(SELECTED, 0);
        }

        //Active tab from saving
        selectFragment();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Save tab
        outState.putInt(SELECTED, tabSelected);
        //
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==ADD_DEVICE_CODE){
            if (resultCode==RESULT_OK){
                //Refresh Main Fragment
                switchToMainFragment();
            }else {
                //Re-connect MQTT
                mainFragment.pauseService(false);
            }

        }

    }


    private void selectFragment(){
        switch (tabSelected){
            case 1: switchToLoggingFragment();  break;
            case 2: switchToPeopleFragment();   break;
            case 3: switchToMoreFragment(); break;
            default:switchToMainFragment(); break;
        }
    }

    private void switchToMainFragment(){
        //Set icon
        showMenu(R.id.add_ac_icon);
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(Constant.GROUP_ID, homeItem.getGroupId());
        //Create
        mainFragment = new MainFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        mainFragment.setArguments(bundle);
        transaction.replace(R.id.main_frame, mainFragment, MainFragment.TAG).commit();
    }

    private void switchToLoggingFragment(){
        //Set icon
        showMenu(-1);
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(Constant.GROUP_ID, homeItem.getGroupId());
        bundle.putString(Constant.REGISTERED, homeItem.getRegisteredTime());
        //Create
        LoggingFragment fragment = new LoggingFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        fragment.setArguments(bundle);
        transaction.replace(R.id.main_frame, fragment).commit();
    }


    private void switchToPeopleFragment(){
        //Set icon
        showMenu(R.id.add_member_icon);
        //Set data
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constant.HOME_DATA, homeItem);
        bundle.putInt(Constant.USER_ID, userId);
        //Create
        PeopleFragment fragment = new PeopleFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        fragment.setArguments(bundle);
        transaction.replace(R.id.main_frame, fragment).commit();
    }

    private void switchToMoreFragment(){
        //Set icon
        showMenu(-1);
        //Set data
        Bundle bundle = new Bundle();
        //Create
        MoreFragment fragment = new MoreFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        fragment.setArguments(bundle);
        transaction.replace(R.id.main_frame, fragment).commit();
    }

    private void showMenu(int iconId){
        //Find icon id
        for (ImageView icon : menuIcon){
            int id = icon.getId();
            if (iconId==id){
                icon.setVisibility(View.VISIBLE);
            }else {
                icon.setVisibility(View.GONE);
            }
        }

    }









}
