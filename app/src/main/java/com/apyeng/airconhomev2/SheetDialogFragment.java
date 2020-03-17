package com.apyeng.airconhomev2;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

public class SheetDialogFragment extends BottomSheetDialogFragment {

    SheetItemAdapter.ItemListener listener;
    public static final String TAG = "SheetDialogFragment";

    public SheetDialogFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set style
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //Get data
        int id = 0;
        Bundle bundle = getArguments();
        if (bundle!=null){
            id = bundle.getInt(Constant.GROUP_ID);
        }

        //Set view
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.menu_rv);
        //Set list
        List<SheetItem> menuItems = new ArrayList<>();
        menuItems.add(new SheetItem(R.drawable.ic_edit, getString(R.string.edit_profile)));
        menuItems.add(new SheetItem(R.drawable.ic_leave, getString(R.string.leave_home)));
        //Add to list
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SheetItemAdapter adapter = new SheetItemAdapter(id, menuItems, new SheetItemAdapter.ItemListener() {
            @Override
            public void onItemClick(int id, int numberSelected, SheetItem itemSelected) {
                //Hide
                dismiss();
                //Call back listener
                if (listener!=null){
                    listener.onItemClick(id, numberSelected, itemSelected);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        return view;
    }


    public void setListener(SheetItemAdapter.ItemListener listener){
        this.listener = listener;
    }







}
