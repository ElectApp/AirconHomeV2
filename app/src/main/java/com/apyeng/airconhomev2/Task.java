package com.apyeng.airconhomev2;

public class Task {

    public String ip;
    public int port;
    public String request[];
    public String response[];

    public Task(String ip, int port, String request[]){
        this.ip = ip;
        this.port = port;
        this.request = request;
    }

    public Task(String ip, int port, String request[], String response[]){
        this.ip = ip;
        this.port = port;
        this.request = request;
        this.response = response;
    }


    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setRequest(String[] request) {
        this.request = request;
    }

    public String[] getRequest() {
        return request;
    }

    public void setResponse(String[] response) {
        this.response = response;
    }

    public String[] getResponse() {
        return response;
    }
}
