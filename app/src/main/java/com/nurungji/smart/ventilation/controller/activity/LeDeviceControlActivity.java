package com.nurungji.smart.ventilation.controller.activity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.scorchedrice.ble.controller.R;
import com.nurungji.smart.ventilation.controller.service.BluetoothLeService;
import com.nurungji.smart.ventilation.controller.util.Checksum;
import com.nurungji.smart.ventilation.controller.util.HexStringToByteArrayConverter;

import java.util.List;

public class LeDeviceControlActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LeDeviceControlActivity.class.getSimpleName();


    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private boolean mConnected = false;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private String mDeviceName;
    private String mDeviceAddress;

    private TextView fanVal;
    private TextView inTempVal;
    private TextView outTempVal;
    private TextView rhVal;
    private TextView dustVal;
    private TextView co2Val;
    private TextView vocVal;
    private TextView modeVal;
    private TextView damperVal;

    private Button btnPowerOn;
    private Button btnPowerOff;
    private Button btnDamperOpen;
    private Button btnDamperClose;
    private Button btnAuto;
    private Button btnManual;
    private Button btnFanUp;
    private Button btnFanDown;

    private static final byte START_CODE = 0x5C;
    private static final byte TYPE = 0x57;
    private static final byte DEFAULT_BYTE = 0x30;
    private static final byte END_CODE = 0x50;
    private static final int CHECKSUM_1 = 7;
    private static final int CHECKSUM_2 = 8;
    private static final byte POWER = 0x31;
    private static final byte DAMPER = 0x32;
    private static final byte MODE = 0x33;
    private static final byte FANSPEED = 0x34;

    private Toast toastMaxFanSpeed;
    private Toast toastMinFanSpeed;
    private Toast toastDisconnected;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // 서비스에서 발생하는 다양한 이벤트를 처리
    // ACTION_GATT_CONNECTED: GATT 서버에 연결됨
    // ACTION_GATT_DISCONNECTED: GATT 서버와 연결이 끊어짐
    // ACTION_GATT_SERVICES_DISCOVERED: GATT 서비스 발견
    // ACTION_DATA_AVAILABLE: 장치에서 데이터를 수신
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                selectGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ledevice_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        init();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void init() {
        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fanVal = findViewById(R.id.fanVal);
        inTempVal = findViewById(R.id.inTempVal);
        outTempVal = findViewById(R.id.outTempVal);
        rhVal = findViewById(R.id.rhVal);
        dustVal = findViewById(R.id.dustVal);
        co2Val = findViewById(R.id.co2Val);
        vocVal = findViewById(R.id.vocVal);
        modeVal = findViewById(R.id.modeVal);
        damperVal = findViewById(R.id.damperVal);

        btnPowerOn = findViewById(R.id.btnPowerOn);
        btnPowerOn.setOnClickListener(this);
        btnPowerOff = findViewById(R.id.btnPowerOff);
        btnPowerOff.setOnClickListener(this);
        btnDamperOpen = findViewById(R.id.btnDamperOpen);
        btnDamperOpen.setOnClickListener(this);
        btnDamperClose = findViewById(R.id.btnDamperClose);
        btnDamperClose.setOnClickListener(this);
        btnAuto = findViewById(R.id.btnAuto);
        btnAuto.setOnClickListener(this);
        btnManual = findViewById(R.id.btnManual);
        btnManual.setOnClickListener(this);
        btnFanUp = findViewById(R.id.btnFanUp);
        btnFanUp.setOnClickListener(this);
        btnFanDown = findViewById(R.id.btnFanDown);
        btnFanDown.setOnClickListener(this);

        toastMaxFanSpeed = Toast.makeText(this, R.string.maxFanSpeed, Toast.LENGTH_SHORT);
        toastMinFanSpeed = Toast.makeText(this, R.string.minFanSpeed, Toast.LENGTH_SHORT);
        toastDisconnected = Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        BluetoothGattCharacteristic characteristic = null;

        // 사용 가능한 GATT 서비스
        for (BluetoothGattService gattService : gattServices) {
            characteristic = gattService.getCharacteristic(BluetoothLeService.HM_RX_TX);
        }

        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }
    }

    // 듣고자 하는 이벤트(액션)
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onClick(View view) {
        if (mConnected) {
            String fanvaltext = fanVal.getText().toString();
            int speed = 0;
            if (!fanvaltext.equals("")) {
                speed = Integer.parseInt(fanVal.getText().toString());
            }

            switch (view.getId()) {
                case R.id.btnPowerOn:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(POWER, (byte) 0x31));
                    break;

                case R.id.btnPowerOff:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(POWER, (byte) 0x30));
                    break;

                case R.id.btnDamperOpen:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(DAMPER, (byte) 0x31));
                    break;

                case R.id.btnDamperClose:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(DAMPER, (byte) 0x30));
                    break;

                case R.id.btnAuto:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(MODE, (byte) 0x31));
                    break;

                case R.id.btnManual:
                    mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(MODE, (byte) 0x30));
                    break;

                case R.id.btnFanUp:
                    if (speed < 5) {
                        speed += 0x30;
                        mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(FANSPEED, (byte) ++speed));
                    } else
                        toastMaxFanSpeed.show();
                    break;

                case R.id.btnFanDown:
                    if (speed > 1) {
                        speed += 0x30;
                        mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic, send(FANSPEED, (byte) --speed));
                    } else
                        toastMinFanSpeed.show();
                    break;
            }
        } else toastDisconnected.show();
    }

    private void displayData(String data) {
        if (data != null) {
            final byte[] payload = HexStringToByteArrayConverter.hexStringToByteArray(data);

            if (payload[0] == 0x5C && payload[29] == 0x50 && Checksum.checksum(payload)) {
                int fanspeed = payload[3] & 0x0F;
                double temp1 = (double) (((payload[4] & 0x0F) << 8) + ((payload[5] & 0x0F) << 4) + (payload[6] & 0x0F)) / 10;
                double rh = (double) (((payload[7] & 0x0F) << 8) + ((payload[8] & 0x0F) << 4) + (payload[9] & 0x0F)) / 10;
                double temp2 = (double) (((payload[10] & 0x0F) << 8) + ((payload[11] & 0x0F) << 4) + (payload[12] & 0x0F)) / 10;
                int dust = ((payload[13] & 0x0F) << 12) + ((payload[14] & 0x0F) << 8) + ((payload[15] & 0x0F) << 4) + (payload[16] & 0x0F);
                int co2 = ((payload[17] & 0x0F) << 12) + ((payload[18] & 0x0F) << 8) + ((payload[19] & 0x0F) << 4) + (payload[20] & 0x0F);
                int voc = ((payload[21] & 0x0F) << 8) + ((payload[22] & 0x0F) << 4) + (payload[23] & 0x0F);
                int mode = payload[24] & 0x0F;
                int damper = payload[25] & 0x0F;
                int fan = payload[26] & 0x0F;

                Log.d(TAG, String.format("fanspeed=%d, temp1=%.1f, rh=%.1f, temp2=%.1f, dust=%d, co2=%d, voc=%d, mode=%d, damper=%d, fan=%d",
                        fanspeed, temp1, rh, temp2, dust, co2, voc, mode, damper, fan));

                inTempVal.setText(String.valueOf(temp1));
                outTempVal.setText(String.valueOf(temp2));
                rhVal.setText(String.valueOf(rh));
                dustVal.setText(String.valueOf(dust));
                co2Val.setText(String.valueOf(co2));
                vocVal.setText(String.valueOf(voc));

                if (fan == 1) fanVal.setText(R.string.label_error);
                else fanVal.setText(String.valueOf(fanspeed));

                if (mode == 0) modeVal.setText(R.string.label_manual);
                else if (mode == 1) modeVal.setText(R.string.label_auto);

                if (damper == 0) damperVal.setText(R.string.label_damperClose);
                else if (damper == 1) damperVal.setText(R.string.label_damperOpen);
                else if (damper == 2) damperVal.setText(R.string.label_error);
            }
        }
    }

    public static byte[] send(byte id, byte value) {
        byte[] data = {START_CODE, TYPE, id, DEFAULT_BYTE, DEFAULT_BYTE, DEFAULT_BYTE, value, 0, 0, END_CODE};
        int checksum = Checksum.createChecksum(data);
        data[CHECKSUM_1] = (byte) (((checksum & 0xF0) >> 4) + DEFAULT_BYTE);
        data[CHECKSUM_2] = (byte) ((checksum & 0x0F) + DEFAULT_BYTE);
        return data;
    }
}