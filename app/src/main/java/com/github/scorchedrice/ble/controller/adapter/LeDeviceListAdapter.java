package com.github.scorchedrice.ble.controller.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.github.scorchedrice.ble.controller.R;
import com.github.scorchedrice.ble.controller.viewholder.LeDeviceViewHolder;

import java.util.ArrayList;

public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> leDevices;
    private LayoutInflater inflater;

    public LeDeviceListAdapter(LayoutInflater inflater) {
        leDevices = new ArrayList<>();
        this.inflater = inflater;
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

        LeDeviceViewHolder viewHolder;

        if (view == null) {
            view = inflater.inflate(R.layout.listitem_device, null);
            viewHolder = new LeDeviceViewHolder();
            viewHolder.deviceName = view.findViewById(R.id.device_name);
            viewHolder.deviceAddress = view.findViewById(R.id.device_address);
            view.setTag(viewHolder);
        } else {
            viewHolder = (LeDeviceViewHolder) view.getTag();
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
