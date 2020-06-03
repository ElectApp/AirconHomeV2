package com.apyeng.airconhomev2;

public class NetworkItem {

    public String ssid, bssid, password, ips, status;
    public int rssi, auth, netID;

    public NetworkItem(){
        this.auth = -1;
    }

    public NetworkItem(String ssid, String bssid, int rssi){
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.auth = -1;
    }

    public NetworkItem(String ssid, String password, String ips){
        this.ssid = ssid;
        this.password = password;
        this.ips = ips;
        this.auth = -1;
    }

    public NetworkItem(String ssid, String bssid, int auth, int rssi){
        this.ssid = ssid;
        this.auth = auth;
        this.rssi = rssi;
        this.bssid = bssid;
    }



}
