package com.gyuzero.smart.ventilation.controller.client;

import lombok.Data;

@Data
public class VentClient {

    private double inTemp; // 실내온도
    private double rh; // 습도
    private double outTemp; // 실외온도
    private int dust; // 미세먼지
    private int co2; // CO2
    private int voc; // VOC
    private int mode; // 모드
    private int damper; // 댐퍼
    private int fanStatus; // 팬 상태
    private int fanSpeed; // 풍량

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

