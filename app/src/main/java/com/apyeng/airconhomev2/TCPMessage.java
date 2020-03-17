package com.apyeng.airconhomev2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class TCPMessage extends AsyncTask<String[], Integer, Task> {

    private OnTCPListener listener;
    private boolean enableCommaSplitter;
    private static final String TAG = "TCPMessage";

    public void run(String[] request, OnTCPListener listener){
        //this.listener = listener;
        //this.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
        TCPMessage tcpMessage = new TCPMessage();
        tcpMessage.enableCommaSplitter = true;
        tcpMessage.listener = listener;
        tcpMessage.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
    }

    public void run(String[] request, boolean enableCommaSplitter, OnTCPListener listener){
        //this.listener = listener;
        //this.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
        TCPMessage tcpMessage = new TCPMessage();
        tcpMessage.enableCommaSplitter = enableCommaSplitter;
        tcpMessage.listener = listener;
        tcpMessage.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, request);
    }

    @Override
    protected Task doInBackground(String[]... strings) {
        final String[]command = strings[0];
        String error = null;
        Task task = new Task(Indoor.SETUP_IP, Indoor.PORT, command);
        try {
            Socket socket = new Socket();
            Log.w(TAG, "Try connect...");
            // Create PrintWriter object for sending messages to server.
            socket.connect(new InetSocketAddress(Indoor.SETUP_IP, Indoor.PORT), Indoor.CONNECT_TIMEOUT);
            // Create PrintWriter object for sending messages to server.
            PrintWriter outWrite = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            //Send message
            for (String c : command){
                outWrite.print(c+",");
            }
            outWrite.flush();
            Log.w(TAG, "Send request success :: "+ Arrays.toString(command));
            //Create object for receiving message response from server.
            InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
            long start = System.currentTimeMillis(); boolean timeout = false;
            //Waiting
            while (!inputStream.ready()){
                timeout = (System.currentTimeMillis() - start)>Indoor.RESPONSE_TIMEOUT;
                if (timeout){ break; }
            }
            //Check timeout
            if(!timeout){
                //Reset start time
                start = System.currentTimeMillis();
                //Read response message
                StringBuilder s = new StringBuilder("");
                int i=0; String[]buffer = new String[Indoor.BUFF_MAX];
                while (inputStream.ready()){
                    //Int value
                    int a = inputStream.read();
                    //Convert int to char
                    char data = (char)a;
                    //Check value
                    // if (a>255){ data = '\0'; } //Empty
                    //Check syntax ',' mean ending each word. So Server must send Ex: Hello,You,
                    if (enableCommaSplitter && data==','){
                        buffer[i] = s.toString(); ///Add message to String array
                        s = new StringBuilder(""); ///Clear string
                        i++;
                    }else {
                        s.append(data); //Create word string
                    }
                    //Check InputSteam read finish
                    if (!inputStream.ready()) {
                        buffer = i > 0 ? Arrays.copyOf(buffer, i) : new String[]{s.toString()}; //Re-size buffer
                        Log.w(TAG, "Server response :: " + Arrays.toString(buffer));
                        //return new Task(Indoor.SETUP_IP, Indoor.PORT, command, buffer);
                        task.setResponse(buffer);
                    }else {
                        //Check timeout
                        timeout = (System.currentTimeMillis() - start)>Indoor.RESPONSE_TIMEOUT;
                        if (timeout){ break; }
                    }
                }

            }
            if (timeout){
                error = "Response timeout";
                Log.e(TAG, error);
                //return new Task(Indoor.SETUP_IP, Indoor.PORT, command, new String[]{ error });
                //task.setResponse(new String[]{ error });
            }
            socket.close(); //Disconnect server
        }catch (IOException e){
            e.printStackTrace();
            error = "Socket failed: "+e.getMessage();
            Log.e(TAG, error);
            //return new Task(Indoor.SETUP_IP, Indoor.PORT, command, new String[]{ error });
        }
        if (error!=null){ task.setResponse(new String[]{ error }); }
        //return new Task(Indoor.SETUP_IP, Indoor.PORT, command, new String[]{ "No response!" });
        return task;
    }

    @Override
    protected void onPostExecute(Task task) {
        super.onPostExecute(task);
        //Log.w(TAG, "Set result..."+Arrays.toString(task.response));
        if(listener!=null){
            listener.onResult(task);
        }
    }

    interface OnTCPListener{
        void onResult(Task task);
    }


}
