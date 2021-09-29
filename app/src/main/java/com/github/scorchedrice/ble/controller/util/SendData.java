package com.github.scorchedrice.ble.controller.util;

public class SendData {

    private static final byte START_CODE = 0x5C;
    private static final byte TYPE = 0x57;
    private static final byte DEFAULT_BYTE = 0x30;
    private static final byte END_CODE = 0x50;
    private static final int CHECKSUM_1 = 7;
    private static final int CHECKSUM_2 = 8;

    public static byte[] createPacket(byte id, byte value) {
        byte[] data = {START_CODE, TYPE, id, DEFAULT_BYTE, DEFAULT_BYTE, DEFAULT_BYTE, value, 0, 0, END_CODE};
        int checksum = Checksum.createChecksum(data);
        data[CHECKSUM_1] = (byte) (((checksum & 0xF0) >> 4) + DEFAULT_BYTE);
        data[CHECKSUM_2] = (byte) ((checksum & 0x0F) + DEFAULT_BYTE);
        return data;
    }
}
