package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

public class VolleyImageLoader implements Response.ErrorListener, Response.Listener<Bitmap> {
    private RequestQueue requestQueue;
    private OnLoadingListener listener;
    private Context context;

    public VolleyImageLoader(Context context){
        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
    }

    public Context getContext() {
        return context;
    }

    public void download(String url, int maxWidth, int maxHeight, OnLoadingListener listener){
        ImageRequest request = new ImageRequest(url, this, maxWidth, maxHeight,
                ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, this);
        requestQueue.add(request);
        this.listener = listener;
    }


    @Override
    public void onErrorResponse(VolleyError error) {
        if (listener!=null){
            listener.onFailed(error.getMessage());
        }
    }

    @Override
    public void onResponse(Bitmap response) {
        if (listener!=null){

            listener.onSuccess(response);
        }
    }

    interface OnLoadingListener{
        void onSuccess(Bitmap bitmap);
        void onFailed(String error);
    }

}
