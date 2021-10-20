package com.gyuzero.smart.ventilation.controller.utils;

public class Checksum {

    private static final byte CHECKSUM_LENGTH = 3;
    private static final int CHECKSUM_0 = 27;
    private static final int CHECKSUM_1 = 28;

    public static boolean checksum(byte[] payload) {
        int checksum = 0;
        for (int i = 0; i < payload.length - CHECKSUM_LENGTH; i++) checksum += payload[i];
        int checksum1 = ((payload[CHECKSUM_0] & 0x0F) << 4) + (payload[CHECKSUM_1] & 0x0F);
        if ((checksum & 0xFF) == checksum1) return true;
        return false;
    }

    public static int createChecksum(byte[] payload) {
        int sum = 0;
        for (int i = 0; i < payload.length - CHECKSUM_LENGTH; i++) sum += payload[i];
        return sum;
    }
}
