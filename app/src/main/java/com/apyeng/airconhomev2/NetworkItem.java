package com.apyeng.airconhomev2;

public class NetworkItem {

    public String ssid, bssid, password, ips;
    public int rssi;

    public NetworkItem(){

    }

    public NetworkItem(String ssid, String bssid, int rssi){
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
    }

    public NetworkItem(String ssid, String password, String ips){
        this.ssid = ssid;
        this.password = password;
        this.ips = ips;
    }




}
