package com.apyeng.airconhomev2;

import android.os.Parcel;
import android.os.Parcelable;

public class Indoor implements Parcelable{

    //======================== Constant ======================//
    //TCP TAG
    public static final String MB_INFO = "1";
    public static final String MB_WRITE = "2";
    public static final String SET_CONFIG = "3";
    public static final String CONFIG_DATA = "4";
    public static final String FULL_DATA = "5";
    public static final String SWITCH_MODE = "6";
    public static final String PING_TEST = "7";
    public static final String SET_TIMER = "8";
    public static final String ABOUT = "9";
    public static final String CONFIG_CONNECT_ROUTER = "10";
    //Result & Modbus error
    public static final String ON_SUCCESS = "success";
    public static final String ON_TEST = "Frecon WiFi Module 2018";
    //Buffer
    public static final int BUFF_MAX = 50;
    //Modbus Address
    public static final String COMMAND_ADDR = "1000";
    public static final String SET_POINT_ADDR = "1001";
    public static final String TIMER_ON_H_ADDR = "1002";
    public static final String TIMER_OH_M_ADDR = "1003";
    public static final String TIMER_OFF_H_ADDR = "1004";
    public static final String TIMER_OFF_M_ADDR = "1005";
    public static final String STATUS_ADDR = "1008";
    public static final String TRIP_TYPE_ADDR = "1010";
    public static final String ROOM_TEMP_ADDR = "1015";
    //Connection
    public static final int CONNECT_TIMEOUT = 5000;
    public static final int RESPONSE_TIMEOUT = 5000;
    public static String SETUP_IP = "192.168.4.1";
    public static final int PORT = 502;
    public int ip; //TCP IP address

    //====================== Variable, Object ===================//
    //Command & Status (MODBUS value)
    private int command;
    public int setPointTemp, roomTemp;
    public int mode, fan, louver, tripCode;
    public boolean onoff, sleep, timerOn, timerOff, eco, turbo, quiet, service;
    public Time onTime, offTime;

    private static final String TAG = "Indoor";

    public Indoor(){

    }
    //Base on MODBUS value
    public Indoor(int command, int setPointTemp, int tripCode, int roomTemp){
        setCommand(command);
        this.setPointTemp = setPointTemp;
        this.tripCode = tripCode;
        this.roomTemp = roomTemp;
    }

    public Indoor(int command, int setPointTemp, Time onTime, Time offTime, int tripCode, int roomTemp){
        setCommand(command);
        this.setPointTemp = setPointTemp;
        this.onTime = onTime;
        this.offTime = offTime;
        this.tripCode = tripCode;
        this.roomTemp = roomTemp;
    }


    protected Indoor(Parcel in) {
        ip = in.readInt();
        command = in.readInt();
        setPointTemp = in.readInt();
        roomTemp = in.readInt();
        mode = in.readInt();
        fan = in.readInt();
        louver = in.readInt();
        tripCode = in.readInt();
        onoff = in.readByte() != 0;
        sleep = in.readByte() != 0;
        timerOn = in.readByte() != 0;
        timerOff = in.readByte() != 0;
        eco = in.readByte() != 0;
        turbo = in.readByte() != 0;
        quiet = in.readByte() != 0;
        service = in.readByte() != 0;
    }

    public static final Creator<Indoor> CREATOR = new Creator<Indoor>() {
        @Override
        public Indoor createFromParcel(Parcel in) {
            return new Indoor(in);
        }

        @Override
        public Indoor[] newArray(int size) {
            return new Indoor[size];
        }
    };

    public void setCommand(int command){
        //Set command Modbus value
        this.command = command;
        //Convert bit value to each command
        onoff = BitOperation.readBit(0, 1, command)>0;
        mode = BitOperation.readBit(1, 2, command);
        fan = BitOperation.readBit(3, 2, command);
        louver = BitOperation.readBit(5, 3, command);
        sleep = BitOperation.readBit(8, 1, command)>0;
        timerOn = BitOperation.readBit(9, 1, command)>0;
        timerOff = BitOperation.readBit(10, 1, command)>0;
        eco = BitOperation.readBit(11, 1, command)>0;
        turbo = BitOperation.readBit(12, 1, command)>0;
        quiet = BitOperation.readBit(13, 1, command)>0;
        service = BitOperation.readBit(14, 1, command)>0;
    }

    public int getCommand() {
        return command;
    }

    //============ Start Changing bit value in Command Value ============//
    //@return Command value that changed
    public  int setOnOff(boolean onoff){
        return BitOperation.setBit(0, 1, onoff? 1:0, command);
    }
    public int setMode(int mode){
        return BitOperation.setBit(1, 2, mode, command);
    }
    public int setFan(int fan){
        return BitOperation.setBit(3, 2, fan, command);
    }
    public int setLouver(int louver){
        return BitOperation.setBit(5, 3, louver, command);
    }
    public int setSleep(boolean enable){
        return BitOperation.setBit(8, 1, enable? 1:0, command);
    }
    public int setTimerOn(boolean enable){
        return BitOperation.setBit(9, 1, enable? 1:0, command);
    }
    public int setTimerOff(boolean enable){
        return BitOperation.setBit(10, 1, enable? 1:0, command);
    }
    public int setDisableTimer(){
        return BitOperation.setBit(9, 2, 0, command);
    }
    public int setEco(boolean enable){
        return BitOperation.setBit(11, 1, enable? 1:0, command);
    }
    public int setTurbo(boolean enable){
        return BitOperation.setBit(12, 1, enable? 1:0, command);
    }
    public int setQuiet(boolean enable){
        return BitOperation.setBit(13, 1, enable? 1:0, command);
    }
    public int setService(boolean enable){
        return BitOperation.setBit(14, 1, enable? 1:0, command);
    }
    //=============== End Changing bit value in Command Value ===============//

    //=========== Other ========//
    public static int DATA_SIZE(String tag){
        switch (tag){
            case MB_INFO: return 8;
            case MB_WRITE:
            case SET_TIMER: return 1;
            case ABOUT: return 2;
        }
        return -1;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(ip);
        parcel.writeInt(command);
        parcel.writeInt(setPointTemp);
        parcel.writeInt(roomTemp);
        parcel.writeInt(mode);
        parcel.writeInt(fan);
        parcel.writeInt(louver);
        parcel.writeInt(tripCode);
        parcel.writeByte((byte) (onoff ? 1 : 0));
        parcel.writeByte((byte) (sleep ? 1 : 0));
        parcel.writeByte((byte) (timerOn ? 1 : 0));
        parcel.writeByte((byte) (timerOff ? 1 : 0));
        parcel.writeByte((byte) (eco ? 1 : 0));
        parcel.writeByte((byte) (turbo ? 1 : 0));
        parcel.writeByte((byte) (quiet ? 1 : 0));
        parcel.writeByte((byte) (service ? 1 : 0));
    }
}
