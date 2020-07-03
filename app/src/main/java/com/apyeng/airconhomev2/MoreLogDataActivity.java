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
import android.support.v4.widget.SwipeRefreshLayout;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MoreLogDataActivity extends AppCompatActivity {

    //Devices
    private SheetItem cDevice;
    private ArrayList<SheetItem> devicesItems;
    private LinearLayout deviceLay;
    private TextView deviceTxt;
    //Data
    private Calendar cal;
    private TextView dateTxt, selectedTxt;
    private final static SimpleDateFormat M_DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
    private final static SimpleDateFormat M2_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private static String[] KEY_DATA, KEY_POINT;
    private GraphLogItemAdapter adapter;
    private List<GraphLogItem> items;
    private RecyclerView logRv;
    private int countSelected;
    private SwipeRefreshLayout swipeLay;
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
        swipeLay = findViewById(R.id.log_swipe);
        selectedTxt = findViewById(R.id.count_tv);
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

        //================ Refresh ==============//
        swipeLay.setEnabled(true);
        swipeLay.setColorSchemeResources(R.color.colorPrimary);
        swipeLay.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.w(TAG, "Refresh...");
                loadData();
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
        items.add(new GraphLogItem(Constant.SET_POINT, false, R.drawable.ic_temp, "Set Point Temperature",
                "Command by user", "0.0", "\u2103", null, null));
        items.add(new GraphLogItem(Constant.ROOM_TEMP, false, R.drawable.ic_temp, "Room Temperature",
                "It is measured by sensor of the return tube.", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.STATUS, false, R.drawable.ic_machine, "Indoor Status",
                "Real status of Indoor", "0", "",null, null));
        items.add(new GraphLogItem(Constant.STALL, false, R.drawable.ic_machine, "Stall Prevention",
                "Stall prevention of Outdoor", "0","", null, null));
        items.add(new GraphLogItem(Constant.TRIP_TYPE, false, R.drawable.ic_error, "Trip Type",
                "Indoor or Outdoor's fault", "0", "",null, null));
        items.add(new GraphLogItem(Constant.PFC_CURRENT, false, R.drawable.ic_pcb, "Invert Input Current",
                "", "0.0","A", null, null));
        items.add(new GraphLogItem(Constant.DC_BUS_VOLTAGE, false, R.drawable.ic_pcb, "DC Bus Voltage",
                "", "0","V", null, null));
        items.add(new GraphLogItem(Constant.PFC_TEMP, false, R.drawable.ic_temp, "PFC Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.HEAT_SINK_TEMP, false, R.drawable.ic_temp, "Heat Sink Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.OPERATING_FREQ, false, R.drawable.ic_comp, "Compressor Speed",
                "", "0.00","Hz", null, null));
        items.add(new GraphLogItem(Constant.COMP_VOLTAGE, false, R.drawable.ic_comp, "Compressor Voltage",
                "", "0","V", null, null));
        items.add(new GraphLogItem(Constant.COMP_CURRENT, false, R.drawable.ic_comp, "Compressor Current",
                "", "0.0","A", null, null));
        items.add(new GraphLogItem(Constant.COMP_IN_POWER, false, R.drawable.ic_comp, "Compressor Input Power",
                "It is the output power of outdoor controller.", "0","W", null, null));
        items.add(new GraphLogItem(Constant.EXV_POSITION, false, R.drawable.ic_machine, "EXV Position",
                "", "0","", null, null));
        items.add(new GraphLogItem(Constant.BLDC_FAN1, false, R.drawable.ic_machine, "BLDC Fan 1",
                "", "0","rpm", null, null));
        items.add(new GraphLogItem(Constant.BLDC_FAN2, false, R.drawable.ic_machine, "BLDC Fan 2",
                "", "0","rpm", null, null));
        items.add(new GraphLogItem(Constant.DISCHARGE_TEMP, false, R.drawable.ic_temp, "Discharge Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.SUCTION_TEMP, false, R.drawable.ic_temp, "Suction Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.CONDENSER_TEMP, false, R.drawable.ic_temp, "Condenser Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.AMBIENT_TEMP, false, R.drawable.ic_temp, "Ambient Temperature",
                "", "0.0","\u2103", null, null));
        items.add(new GraphLogItem(Constant.PV_VOLTAGE, false, R.drawable.ic_pv, "PV Voltage",
                "It is the output voltage of solar panel (PV).", "0","V", null, null));
        items.add(new GraphLogItem(Constant.PV_CURRENT, false, R.drawable.ic_pv, "PV Current",
                "It is the output current of solar panel (PV).", "0.0","A", null, null));
        items.add(new GraphLogItem(Constant.PV_POWER, false, R.drawable.ic_pv, "PV Power",
                "It is the output power of solar panel (PV).", "0.0","W", null, null));
        items.add(new GraphLogItem(Constant.PV_WH, false, R.drawable.ic_pv, "PV Energy Today",
                "It is the output energy of solar panel (PV).", "0","Wh", null, null));
        items.add(new GraphLogItem(Constant.PV_TOTAL_KWH, false, R.drawable.ic_pv, "PV Total Energy",
                "It is the output energy of solar panel (PV).", "0","kWh", null, null));
        adapter = new GraphLogItemAdapter(this, items);
        adapter.setOnItemClickListener(new GraphLogItemAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int p, String title, boolean checked) {
                //Add selected item
                GraphLogItem item = items.get(p);
                item.checked = !item.checked;
                items.set(p, item);
                adapter.notifyItemChanged(p);
                //Show total of item selected
                setSelectedTxt(item.checked? 1:-1);
            }
        });
        logRv.setLayoutManager(rvManager);
        logRv.setAdapter(adapter);
        //Initial counter
        setSelectedTxt(0);
        //KEY DATA and POINT
        GraphLogItem logItem;
        KEY_DATA = new String[items.size()];
        KEY_POINT = new String[items.size()];
        for (int k=0; k<items.size(); k++){
            logItem = items.get(k);
            KEY_DATA[k] = logItem.logKey;
            KEY_POINT[k] = logItem.format;
        }
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

    private void setSelectedTxt(int add){
        countSelected += add;
        selectedTxt.setText(String.valueOf(countSelected));
        selectedTxt.setVisibility(countSelected>0? View.VISIBLE:View.INVISIBLE);
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
                        //Log.w(TAG, "Chart list..."+chartItems.size());

                        //Prepare data to set chart
                        ArrayList<Entry>[] dAll = new ArrayList[items.size()];
                        ArrayList<String> time = new ArrayList<>();
                        float min[] = new float[items.size()];
                        for (int i=0; i<chartItems.size(); i++){
                            ChartItem item = chartItems.get(i);
                            for (int k=0; k<dAll.length; k++){
                                if (dAll[k]==null){ dAll[k] = new ArrayList<>(); }
                                dAll[k].add(new Entry(i, item.values[k]));
                                //Found min value
                                if (item.values[k]<min[k]){ min[k] = item.values[k]; }
                            }
                            time.add(i, item.time);
                        }

                        //Show Data
                        for (int w=0; w<items.size(); w++){
                            GraphLogItem g = items.get(w);
                            float v = dAll[w].get(dAll[w].size()-1).getY();
                            if (g.logKey.equals(Constant.STATUS)||g.logKey.equals(Constant.STALL)){
                                g.details = Function.getStringValueFormat((int)v, Function.BINARY_FORM);
                            }else {
                                g.details = new DecimalFormat(KEY_POINT[w]).format(v)+" "+g.unit;
                            }
                            g.graphLabel = time;
                            g.graphData = dAll[w];
                            g.min = min[w];
                            Log.w(TAG, "Updated chart...name = "+g.title+", min = "+g.min);
                        }

                        //Update adapter and show list
                        adapter.notifyDataSetChanged();
                        showContent(CONTENT);
                        logRv.setVisibility(View.VISIBLE);
                        //Clear
                        if (swipeLay.isRefreshing()){ swipeLay.setRefreshing(false); }
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
