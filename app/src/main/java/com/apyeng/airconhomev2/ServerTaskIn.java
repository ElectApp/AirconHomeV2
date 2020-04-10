package com.apyeng.airconhomev2;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;

public class ServerTaskIn {

    private int tcpPort;
    private long tcpTimeout;
    private ArrayList<String> subTopics;
    private ArrayList<String> pubTopics;

    public ServerTaskIn(int tcpPort, long tcpTimeout, ArrayList<String> subTopics, ArrayList<String> pubTopics){
        this.tcpPort = tcpPort;
        this.tcpTimeout = tcpTimeout;
        this.subTopics = subTopics;
        this.pubTopics = pubTopics;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpTimeout(long tcpTimeout) {
        this.tcpTimeout = tcpTimeout;
    }

    public long getTcpTimeout() {
        return tcpTimeout;
    }

    public void setSubTopics(ArrayList<String> subTopics) {
        this.subTopics = subTopics;
    }

    public ArrayList<String> getSubTopics() {
        return subTopics;
    }

    public void setPubTopics(ArrayList<String> pubTopics) {
        this.pubTopics = pubTopics;
    }

    public ArrayList<String> getPubTopics() {
        return pubTopics;
    }
}
