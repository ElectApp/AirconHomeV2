package com.apyeng.airconhomev2;

public class ServerTask {

    private byte[] bytes;
    private int statusCode;
    private String statusDetail;


    public ServerTask(int statusCode, String statusDetail, byte[] bytes){
        this.statusCode = statusCode;
        this.statusDetail = statusDetail;
        this.bytes = bytes;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
