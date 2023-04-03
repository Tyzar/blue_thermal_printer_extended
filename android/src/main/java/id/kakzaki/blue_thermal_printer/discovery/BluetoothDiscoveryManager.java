package id.kakzaki.blue_thermal_printer.discovery;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDiscoveryManager {

    public static final int REQ_BL_PERMISSIONS = 13;
    private final Activity context;
    private final BlDiscoveryCallback callback;
    private final BluetoothAdapter blAdapter;

    private boolean isDisposed = false;

    public BluetoothDiscoveryManager(@NonNull Activity context
            , @NonNull BluetoothAdapter blAdapter
            , @NonNull BlDiscoveryCallback callback) {
        this.context = context;
        this.callback = callback;
        this.blAdapter = blAdapter;

        setupReceiver();
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public void dispose() {
        isDisposed = true;

        context.unregisterReceiver(blDscvReceiver);
        if (blAdapter.isDiscovering()) {
            blAdapter.cancelDiscovery();
        }
    }

    private void setupReceiver() {
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(blDscvReceiver, intentFilter);
    }

    private boolean isPermissionGranted(String[] permissions) {
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, perm)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.requestPermissions(permissions, REQ_BL_PERMISSIONS);
                }
                return false;
            }
        }

        return true;
    }

    public void startDiscovery() {
        if (isDisposed) {
            callback.onDiscoveryFinish(
                    BlDiscoveryResult.Failed("Bluetooth discovery manager has been disposed")
            );
            return;
        }

        //check perms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final String[] permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            if (!isPermissionGranted(permissions)) {
                callback.onDiscoveryFinish(BlDiscoveryResult.PermissionError("Permission error"));
                return;
            }
        } else {
            final String[] permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            if (!isPermissionGranted(permissions)) {
                callback.onDiscoveryFinish(BlDiscoveryResult.PermissionError("Permission error"));
                return;
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnabled()) {
            callback.onDiscoveryFinish(BlDiscoveryResult
                    .LocationDisabled("Location need to be enabled to scan nearby devices"));
            return;
        }

        if (blAdapter.isDiscovering()) {
            return;
        }

        //start discovery
        boolean startStatus = blAdapter.startDiscovery();
        if (!startStatus) {
            callback.onDiscoveryFinish(BlDiscoveryResult.Failed("Failed to start discovery"));
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }

    public void stopDiscovery() {
        if (blAdapter.isDiscovering())
            blAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver blDscvReceiver = new BroadcastReceiver() {
        private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                final List<BluetoothDevice> resultCopy = new ArrayList<>(discoveredDevices);
                callback.onDiscoveryFinish(BlDiscoveryResult.Success(resultCopy));
                discoveredDevices.clear();//clear discovered devices
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String address = device.getAddress();
                    for (int i = 0; i < discoveredDevices.size(); i++) {
                        if (address.equals(discoveredDevices
                                .get(i).getAddress())) {
                            return;
                        }
                    }

                    discoveredDevices.add(device);
                }
            }
        }
    };

}
