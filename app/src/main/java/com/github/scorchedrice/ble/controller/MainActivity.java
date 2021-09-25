package com.github.scorchedrice.ble.controller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler;
    private static final int REQUEST_ENABLE_BT = 1;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private LeDeviceListAdapter leDeviceListAdapter;
    private ListView leDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        init();
    }

    private void init() {
        getSupportActionBar().setTitle(R.string.title_devices);
        handler = new Handler();
        leDeviceList = findViewById(R.id.leDeviceList);
        leDeviceList.setOnItemClickListener(this);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leDeviceListAdapter.addDevice(device);
                            leDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                leDeviceListAdapter.clear();
                scanLeDevice(true);
                break;

            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final BluetoothDevice device = leDeviceListAdapter.getDevice(i);
        if (device == null) return;
        final Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            bluetoothAdapter.stopLeScan(leScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private class LeDeviceListAdapter extends BaseAdapter {

        private ArrayList<BluetoothDevice> leDevices;
        private LayoutInflater inflater;

        public LeDeviceListAdapter() {
            leDevices = new ArrayList<>();
            inflater = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice leDevice) {
            if (!leDevices.contains(leDevice))
                leDevices.add(leDevice);
        }

        public BluetoothDevice getDevice(int position) {
            return leDevices.get(position);
        }


        @Override
        public int getCount() {
            return leDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return leDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public void clear() {
            leDevices.clear();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            ViewHolder viewHolder;

            if (view == null) {
                view = inflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = leDevices.get(i);
            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    protected void onResume() {
        super.onResume();
        leDeviceListAdapter = new LeDeviceListAdapter();
        leDeviceList.setAdapter(leDeviceListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        leDeviceListAdapter.clear();
    }
}