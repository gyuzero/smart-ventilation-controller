package com.gyuzero.smart.ventilation.controller.client;

import lombok.Data;

@Data
public class VentClient {

    private double inTemp;
    private double rh;
    private double outTemp;
    private int dust;
    private int co2;
    private int voc;
    private int mode;
    private int damper;
    private int fanStatus;
    private int fanSpeed;

    public void setInTemp(double inTemp) {
        this.inTemp = calculateTemp(inTemp);
    }

    public void setOutTemp(double outTemp) {
        this.outTemp = calculateTemp(outTemp);
    }

    public double calculateTemp(double temp) {
        if (temp > 128.0) temp = (temp - 4096.0) / 10.0;
        return temp;
    }
}
