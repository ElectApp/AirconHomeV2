package com.apyeng.airconhomev2;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class MoreFragment extends Fragment {

    private Context context;
    private int groupId;
    public static final String TAG = "MoreFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //Set view
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        //Get data
        Bundle bundle = getArguments();
        groupId = bundle.getInt(Constant.GROUP_ID, 0);

        RecyclerView recyclerView = view.findViewById(R.id.more_rv);
        //Set list
        List<SheetItem> menuItems = new ArrayList<>();
        menuItems.add(new SheetItem(R.drawable.tcp_icon, getString(R.string.modbus_tcp)));
        //Add to list
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        SheetItemAdapter adapter = new SheetItemAdapter(0, menuItems, new SheetItemAdapter.ItemListener() {
            @Override
            public void onItemClick(int id, int numberSelected, SheetItem itemSelected) {
                switch (numberSelected){
                    case 0: //Modbus
                        Intent intent = new Intent(context, ModbusActivity.class);
                        intent.putExtra(Constant.GROUP_ID, groupId);
                        startActivity(intent);
                        break;
                }
            }
        });
        recyclerView.setAdapter(adapter);

        return view;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //This function work before onCreate()
        //Thank: https://stackoverflow.com/questions/8215308/using-context-in-a-fragment
        //Save context
        this.context = context;
    }




}
