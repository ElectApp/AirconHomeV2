package com.apyeng.airconhomev2;






import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class LogTabFragment extends Fragment {

    public static final String FLAG = "Flag";
    public static final String TAG = "LogTabFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_log_tab, container, false);

        TextView textView = view.findViewById(R.id.test_txt);

        int test = getArguments().getInt(FLAG);
        textView.setText(String.valueOf(test));

        return view;
    }

    private Context context(){
        return getActivity().getBaseContext();
    }



}
