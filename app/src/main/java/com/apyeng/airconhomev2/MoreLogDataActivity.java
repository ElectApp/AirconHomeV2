package com.apyeng.airconhomev2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apyeng.airconhomev2.adapters.GraphLogItemAdapter;
import com.apyeng.airconhomev2.helper.LogDataToCSVFile;
import com.apyeng.airconhomev2.models.GraphLogItem;
import com.github.mikephil.charting.data.Entry;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MoreLogDataActivity extends AppCompatActivity {

    //Devices
    private SheetItem cDevice;
    private ArrayList<SheetItem> devicesItems;
    private LinearLayout deviceLay;
    private TextView deviceTxt;
    //Date
    private Calendar cal;
    private TextView dateTxt;
    private final static SimpleDateFormat M_DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
    private final static SimpleDateFormat M2_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private final static String[] KEY_DATA = { Constant.TRIP_TYPE, Constant.ROOM_TEMP,
            Constant.AC_VOLTAGE, Constant.AC_CURRENT, Constant.AC_POWER,
            Constant.PV_VOLTAGE, Constant.PV_CURRENT, Constant.PV_POWER, Constant.PV_WH };
    private static final String[] KEY_POINT = { "0", "0.0",
            "0", "0.0", "0",
            "0", "0.0", "0", "0" };
    private GraphLogItemAdapter adapter;
    private List<GraphLogItem> items;
    private RecyclerView logRv;
    //Export CSV
    private ImageView expIcon;
    private List<ChartItem> allChart;
    private LogDataToCSVFile csvFile;
    private ArrayList<Integer> dataIndex;
    //Other
    private View sectionView[];
    private static final int PROGRESS = 0, CONTENT = 1, NO_AC = 2;
    private int groupId;
    private static final String TAG = "MoreLogDataActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_log_data);

        //Get data
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);

        //Layout
        deviceLay = findViewById(R.id.device_selected_lay);
        deviceTxt = findViewById(R.id.tv_device);
        expIcon = findViewById(R.id.exp_icon);
        dateTxt = findViewById(R.id.date_selected_txt);
        logRv = findViewById(R.id.log_rv);
        sectionView = new View[3];
        sectionView[0] = findViewById(R.id.circle_progress);
        sectionView[1] =  findViewById(R.id.container);
        sectionView[2] = findViewById(R.id.no_ac_content);

        //================ Back ==================//
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //================ Export ================//
        //Initial
        allChart = new ArrayList<>();
        //Action
        findViewById(R.id.exp_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save CSV file
                saveFile();
            }
        });

        //================ Device list ==============//
        //Initial
        devicesItems = new ArrayList<>();
        //Click to show list
        deviceLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeviceList();
            }
        });

        //================ Time Select Dialog =========//
        //Initial
        cal = Calendar.getInstance();
        updateDateSelected();
        //Pick btn
        findViewById(R.id.date_selected_lay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(MoreLogDataActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Log.w(TAG, "Pick Date: "+year+"-"+month+"-"+dayOfMonth);
                        //Save date and load data
                        cal.set(year, month, dayOfMonth);
                        updateDateSelected();
                        loadData();
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                dialog.show();
            }
        });

        //================= Logging Data ==============//
        //Initial
        LinearLayoutManager rvManager = new LinearLayoutManager(this);
        items = new ArrayList<>();
        items.add(new GraphLogItem(false, R.drawable.ic_error, "Trip Type",
                "Indoor or Outdoor's fault", "", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_temp, "Room Temperature",
                "It is measured by sensor of the return tube.", "\u2103", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_comp, "Compressor Voltage",
                "It is the output voltage of outdoor controller.", "V", null, null));
//        items.add(new GraphLogItem(false, R.drawable.ic_comp, "Compressor Current",
//                "It is the output current of outdoor controller.", "A", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_comp, "Inverter Input Current",
                "It is the input current of outdoor controller.", "A", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_comp, "Compressor Power",
                "It is the output power of outdoor controller.", "W", null, null));
//        items.add(new GraphLogItem(false, R.drawable.ic_comp, "Compressor Energy",
//                "It is the output energy of outdoor controller.", "Wh", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_pv, "PV Voltage",
                "It is the output voltage of solar panel (PV).", "V", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_pv, "PV Current",
                "It is the output current of solar panel (PV).", "A", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_pv, "PV Power",
                "It is the output power of solar panel (PV).", "W", null, null));
        items.add(new GraphLogItem(false, R.drawable.ic_pv, "PV Energy",
                "It is the output energy of solar panel (PV).", "Wh", null, null));
        adapter = new GraphLogItemAdapter(this, items);
        adapter.setOnItemClickListener(new GraphLogItemAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int p, String title, boolean checked) {
                //Add selected item
                GraphLogItem item = items.get(p);
                item.checked = !item.checked;
                items.set(p, item);
                adapter.notifyItemChanged(p);
            }
        });
        logRv.setLayoutManager(rvManager);
        logRv.setAdapter(adapter);
        //Load data
        tryReadDeviceDetail();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case Constant.WRITE_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    saveFile();
                }
                break;
        }
    }

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    private void tryReadDeviceDetail(){
        //Loading
        showContent(PROGRESS);
        //Action
        if (Function.internetConnected(this)){
            HomeManager homeManager = new HomeManager(this);
            homeManager.readFullDeviceDetail(groupId, new HomeManager.OnReadFullDeviceDetailListener() {
                @Override
                public void onSuccess(List<AirItem> airItems) {
                    Log.w(TAG, "OnSuccess...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(MoreLogDataActivity.this);
                    //Add list
                    devicesItems.clear();
                    for (AirItem ac : airItems){
                        devicesItems.add(new SheetItem(R.drawable.ic_ac,
                                ac.getNickname()+" | "+ac.getActualName(), ac.getDeviceId()));
                        Log.w(TAG, "Found ["+ac.getDeviceId()+"] "+ac.getActualName());
                    }
                    //Show
                    boolean ok = airItems.size()>0;
                    showContent(ok? CONTENT:NO_AC);
                    //Enable
                    expIcon.setEnabled(ok);
                    deviceLay.setEnabled(ok);
                    //Read log data
                    if(ok){
                        //Save initial device
                        setCurrentDevice(devicesItems.get(0));
                        loadData();
                    }
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showDBErrorDialog(MoreLogDataActivity.this, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(MoreLogDataActivity.this, errorListener);
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            if (buttonId==R.string.retry){
                tryReadDeviceDetail();
            }else {
                Function.dismissAppErrorDialog(MoreLogDataActivity.this);
                showContent(CONTENT);
            }
        }
    };

    private void showDeviceList(){
        //Layout
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        RecyclerView rv = view.findViewById(R.id.menu_rv);
        //Add to list
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SheetItemAdapter adapter = new SheetItemAdapter(groupId, devicesItems, new SheetItemAdapter.ItemListener() {
            @Override
            public void onItemClick(int id, int numberSelected, SheetItem itemSelected) {
                //Hide
                dialog.dismiss();
                //Reload
                setCurrentDevice(itemSelected);
                loadData();
            }
        });
        rv.setAdapter(adapter);
        //Behavior and show
        BottomSheetBehavior.from((View)view.getParent());
        dialog.show();
    }

    private void setCurrentDevice(SheetItem item){
        cDevice = item;
        deviceTxt.setText(item.getName());
    }

    private void updateDateSelected(){
        String d = M_DATE_FORMAT.format(cal.getTime());
        dateTxt.setText(d);
    }

    //Date must in format: YYYY-MM-DD
    private void loadData(){
        if (cDevice==null){ return; }
        //Loading
        showContent(PROGRESS);
        //Try
        HomeManager homeManager = new HomeManager(this);
        homeManager.readDeviceLogAdvanceData(groupId, cDevice.getItemId(),
                Constant.DATE_FORMAT.format(cal.getTime()), KEY_DATA, new HomeManager.OnLogAdvanceDataListener() {
                    @Override
                    public void onSuccess(List<ChartItem> chartItems) {
                        //Prepare data to set chart
                        ArrayList<Entry> d1 = new ArrayList<>();
                        ArrayList<Entry> d2 = new ArrayList<>();
                        ArrayList<Entry> d3 = new ArrayList<>();
                        ArrayList<Entry> d4 = new ArrayList<>();
                        ArrayList<Entry> d5 = new ArrayList<>();
                        ArrayList<Entry> d6 = new ArrayList<>();
                        ArrayList<Entry> d7 = new ArrayList<>();
                        ArrayList<Entry> d8 = new ArrayList<>();
                        ArrayList<Entry> d9 = new ArrayList<>();
                        ArrayList<String> time = new ArrayList<>();
                        for (int i=0; i<chartItems.size(); i++){
                            ChartItem item = chartItems.get(i);
                            d1.add(new Entry(i, item.values[0]));
                            d2.add(new Entry(i, item.values[1]));
                            d3.add(new Entry(i, item.values[2]));
                            d4.add(new Entry(i, item.values[3]));
                            d5.add(new Entry(i, item.values[4]));
                            d6.add(new Entry(i, item.values[5]));
                            d7.add(new Entry(i, item.values[6]));
                            d8.add(new Entry(i, item.values[7]));
                            d9.add(new Entry(i, item.values[8]));
                            time.add(i, item.time);
                        }
                        //Show data
                        //Trip type
                        GraphLogItem g1 = items.get(0);
                        g1.details = Constant.NONE_POINT.format(d1.get(d1.size()-1).getY())+" "+g1.unit;
                        g1.graphLabel = time;
                        g1.graphData = d1;
                        //Room temp
                        GraphLogItem g2 = items.get(1);
                        g2.details = Constant.ONE_POINT.format(d2.get(d2.size()-1).getY())+" "+g2.unit;
                        g2.graphLabel = time;
                        g2.graphData = d2;
                        //Comp V
                        GraphLogItem g3 = items.get(2);
                        g3.details = Constant.NONE_POINT.format(d3.get(d3.size()-1).getY())+" "+g3.unit;
                        g3.graphLabel = time;
                        g3.graphData = d3;
                        //Comp A
                        GraphLogItem g4 = items.get(3);
                        g4.details = Constant.ONE_POINT.format(d4.get(d4.size()-1).getY())+" "+g4.unit;
                        g4.graphLabel = time;
                        g4.graphData = d4;
                        //Comp W
                        GraphLogItem g5 = items.get(4);
                        g5.details = Constant.NONE_POINT.format(d5.get(d5.size()-1).getY())+" "+g5.unit;
                        g5.graphLabel = time;
                        g5.graphData = d5;
                        //PV V
                        GraphLogItem g6 = items.get(5);
                        g6.details = Constant.NONE_POINT.format(d6.get(d6.size()-1).getY())+" "+g6.unit;
                        g6.graphLabel = time;
                        g6.graphData = d6;
                        //PV A
                        GraphLogItem g7 = items.get(6);
                        g7.details = Constant.ONE_POINT.format(d7.get(d7.size()-1).getY())+" "+g7.unit;
                        g7.graphLabel = time;
                        g7.graphData = d7;
                        //PV W
                        GraphLogItem g8 = items.get(7);
                        g8.details = Constant.NONE_POINT.format(d8.get(d8.size()-1).getY())+" "+g8.unit;
                        g8.graphLabel = time;
                        g8.graphData = d8;
                        //PV Wh
                        GraphLogItem g9 = items.get(8);
                        g9.details = Constant.NONE_POINT.format(d9.get(d9.size()-1).getY())+" "+g9.unit;
                        g9.graphLabel = time;
                        g9.graphData = d9;
                        //Update adapter and show list
                        adapter.notifyDataSetChanged();
                        showContent(CONTENT);
                        logRv.setVisibility(View.VISIBLE);
                        //Save
                        allChart.clear();
                        allChart.addAll(chartItems);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e(TAG, "Download advance data failed: "+error);
                        if (error.equals(Constant.NO_LIST)){
                            Function.showAppErrorDialog(MoreLogDataActivity.this,
                                    R.string.no_data, "", R.string.ok, errorListener);
                            logRv.setVisibility(View.INVISIBLE);
                        }else {
                            Function.showDBErrorDialog(MoreLogDataActivity.this, error, errorListener);
                        }
                    }
                });
    }

    private void saveFile(){
        //Check permission
        if (!Function.hasPermissions(this, Constant.WRITE_EXTERNAL)){
            //Request permission from user
            ActivityCompat.requestPermissions(this, Constant.WRITE_EXTERNAL, Constant.WRITE_CODE);
        }else {
            //Create CSV file
            //Title
            GraphLogItem logItem;
            dataIndex = new ArrayList<>();
            List<String> line = new ArrayList<>();
            line.add("Time");
            for(int x=0; x<items.size(); x++){
                logItem = items.get(x);
                if(logItem.checked){
                    dataIndex.add(x);
                    line.add(logItem.title+" ("+logItem.unit+")");
                }
            }
            //Action
            if(dataIndex.size()>0){
                String[] d = line.toArray(new String[0]);
                String n = cDevice.getName()+"_"+M2_DATE_FORMAT.format(cal.getTime());
                csvFile = new LogDataToCSVFile(this, n);
                Log.w(TAG, "Export "+n);
                //Clear exits file
                csvFile.removeFile();
                //Listener
                csvFile.setOnWriteListener(new LogDataToCSVFile.OnWriteListener() {
                    @Override
                    public void onEnd(int lineNumber) {
                        Log.w(TAG, "Write end..."+lineNumber);
                        if (lineNumber>allChart.size()){
                            //End
                            String fp = csvFile.getFilePath();
                            Log.w(TAG, "Saved: "+fp);
                            if (fp!=null){
                                Function.showToast(MoreLogDataActivity.this, "Saved at "+fp);
                                //Share to another app
                                shareFile();
                            }else {
                                Function.showToast(MoreLogDataActivity.this, "Save failed!");
                            }
                            //Clear
                            csvFile.setOnWriteListener(null);
                        }else {
                            //Data
                            String[] d2 = new String[dataIndex.size()+1];
                            int r = 0;
                            ChartItem c = allChart.get(lineNumber-1);
                            d2[r++] = c.time;
                            for (int y : dataIndex){
                                d2[r++] = new DecimalFormat(KEY_POINT[y]).format(c.values[y]);
                            }
                            csvFile.writeNextLine(d2);
                            Log.w(TAG, Arrays.toString(d2));
                        }
                    }
                });
                //Header
                csvFile.writeNextLine(d);
                Log.w(TAG, Arrays.toString(d));
            }else {
                Function.showToast(this, R.string.no_sel);
            }
        }
    }

    private void shareFile(){
        Uri uri = FileProvider.getUriForFile(this, Constant.MY_FILE_PROVIDER, csvFile.getFile());
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("text/csv");
        startActivity(intent);
    }

}
