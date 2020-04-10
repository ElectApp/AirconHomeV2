package com.apyeng.airconhomev2;

public class ModbusItem {

    private String slaveID, deviceName, deviceID;

    public ModbusItem(String slaveID, String deviceName, String deviceID){
        this.slaveID = slaveID;
        this.deviceName = deviceName;
        this.deviceID = deviceID;
    }

    public void setSlaveID(String slaveID) {
        this.slaveID = slaveID;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getSlaveID() {
        return slaveID;
    }

    public String getDeviceID() {
        return deviceID;
    }
}
