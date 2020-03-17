package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

public class AsyncTaskImageLoader extends AsyncTask<LoaderItem, Integer, LoaderItem> {

    private OnDownloadListener downloadListener;
    private VolleyImageLoader loader;
    private LoaderItem resultItem;
    private String loadError;
    private boolean flag;
    private static final String TAG = "AsyncTaskImageLoader";


    public AsyncTaskImageLoader(Context context){
        loader = new VolleyImageLoader(context);
    }

    public void load(int id, String url, OnDownloadListener listener){
        //Set item
        LoaderItem item = new LoaderItem(id, url, null);
        //Start new object by use Context from previously object
        AsyncTaskImageLoader imageLoader = new AsyncTaskImageLoader(loader.getContext());
        imageLoader.downloadListener = listener;
        imageLoader.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, item);
    }

    @Override
    protected LoaderItem doInBackground(final LoaderItem... item) {
        //Copy some result
        resultItem = item[0];
        //Create new thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "Try to download: "+item[0].getUrl());
                //Download
                loader.download(item[0].getUrl(), 0, 0,
                        new VolleyImageLoader.OnLoadingListener() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        //Update result
                        resultItem.setBitmap(bitmap);
                        loadError = null;
                        Log.w(TAG, "success");
                        flag = true;
                    }

                    @Override
                    public void onFailed(String error) {
                        //Set error
                        loadError = error;
                        resultItem.setBitmap(null);
                        Log.w(TAG, "Failed: "+error);
                        flag = true;
                    }
                });
                //Waiting
                while (!flag);
            }
        });
        thread.start();
        Log.w(TAG, "Waiting download finish...");
        //Waiting until finish
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            loadError = e.getMessage();
        }
        Log.w(TAG, "download finish...");

        return resultItem;
    }


    @Override
    protected void onPostExecute(LoaderItem item) {
        super.onPostExecute(item);
        //Call back result
        if (resultItem.getBitmap()!=null){
            downloadListener.onSuccess(resultItem);
        }else {
            downloadListener.onFailed(resultItem, loadError);
        }

    }

    interface OnDownloadListener{
        void onSuccess(LoaderItem item);
        void onFailed(LoaderItem item, String error);
    }
}
