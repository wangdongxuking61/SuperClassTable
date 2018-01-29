package com.BRMicro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;

public class PARA_TABLE {
    public int dwProductSN;
    public int dwFingerNum;
    public int dwDeviceAddress;
    public int dwCommPassword;
    public int dwComBaudRate;
    public int wCmosExposeTimer;
    public int cDetectSensitive;
    public int cSecurLevel;
    public String cManuFacture;
    public String cproductModel;
    public String cSWVersion;
    public String cReserve;
}