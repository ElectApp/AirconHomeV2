package com.apyeng.airconhomev2.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.apyeng.airconhomev2.Constant;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;


public class LogDataToCSVFile {

    private CSVWriter writer;
    private File file;
    private OnWriteListener onWriteListener = null;
    private int cLine = 0;
    private static final String TAG = "LogData";

    public LogDataToCSVFile(Context context, String fileNameWithoutType){
        File path = new File(Constant.MAIN_FILE_PATH+"Logging Data/");
        path.mkdirs();
        file = new File(path, fileNameWithoutType+".csv");
        Log.w(TAG, "Path: "+file.getAbsolutePath());
    }

    public LogDataToCSVFile(Context context){
        File path = new File(Constant.MAIN_FILE_PATH+"Logging Data/");
        path.mkdirs();
        file = new File(path, getFileName(context.getClass().getSimpleName()));
        Log.w(TAG, "Path: "+file.getAbsolutePath());
    }

    public void setOnWriteListener(OnWriteListener onWriteListener) {
        this.onWriteListener = onWriteListener;
    }

    private String getFileName(String name){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        return name+"_"+format.format(calendar.getTime())+".csv";
    }

    public void writeNextLine(String[]data){
        if (file!=null){
            Write write = new Write();
            write.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, data);
            cLine++;
        }
    }


    public String getFilePath(){
        if (file!=null){
            return file.getAbsolutePath();
        }
        return null;
    }

    public File getFile(){
        return file;
    }

    public void removeFile(){
        if (file!=null && file.exists()){
            file.delete();
        }
    }

    private class Write extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(final String... strings) {
            //Set thread
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (file.exists()){
                            FileWriter fileWriter = new FileWriter(file, true);
                            writer = new CSVWriter(fileWriter);
                        }else {
                            writer = new CSVWriter(new FileWriter(file));
                        }
                        Log.w(TAG, "Write: "+ Arrays.toString(strings));
                        writer.writeNext(strings);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                thread.join();
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (onWriteListener!=null){
                onWriteListener.onEnd(cLine);
            }
        }
    }

    public interface OnWriteListener{
        void onEnd(int lineNumber);
    }


}
