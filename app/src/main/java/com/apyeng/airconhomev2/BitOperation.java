package com.apyeng.airconhomev2;

public class BitOperation {

    public static int readBit(int startBit, int len, int readValue){
        int con=0;
        for(int i=0; i<len; i++){
            con |= 1<<i;
        }
        return (readValue>>startBit)&con;
    }

    public static int setBit(int startBit, int len, int setValue, int currentValue){
        int con=0;
        for(int i=0; i<len; i++){
            con |= 1<<(startBit+i);
        }
        return (setValue<<startBit)|((~con)&currentValue);
    }

}
