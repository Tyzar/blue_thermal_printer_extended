package id.kakzaki.blue_thermal_printer;

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
import androidx.annotation.Nullable;
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
        context.registerReceiver(blDscvReceiver, intentFilter);
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(blDscvReceiver, intentFilter);
    }

    private boolean isPermissionGranted() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted()) {
            callback.onDiscoveryFinish(BlDiscoveryResult.PermissionError("Permission error"));
            return;
        }

        //check location is enabled
        if (!isLocationEnabled()) {
            callback.onDiscoveryFinish(BlDiscoveryResult.LocationDisabled("Please enable location"));
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
        blAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver blDscvReceiver = new BroadcastReceiver() {
        private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopDiscovery();
                final List<BluetoothDevice> resultCopy = new ArrayList<>(discoveredDevices);
                callback.onDiscoveryFinish(BlDiscoveryResult.Success(resultCopy));
                discoveredDevices.clear();//clear discovered devices
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    discoveredDevices.add(device);
                }
            }
        }
    };

}

interface BlDiscoveryCallback {
    void onDiscoveryFinish(BlDiscoveryResult result);
}

class BlDiscoveryResult {
    public static final String success = "success";
    public static final String failed = "failed";
    public static final String locationDisabled = "locationDisabled";
    public static final String permissionFailed = "permissionFailed";

    @NonNull
    public final String status;
    @Nullable
    public final List<BluetoothDevice> deviceList;
    @Nullable
    public final String errMsg;

    private BlDiscoveryResult(@NonNull String status
            , @Nullable List<BluetoothDevice> deviceList
            , @Nullable String errMsg) {
        this.status = status;
        this.deviceList = deviceList;
        this.errMsg = errMsg;
    }

    public static BlDiscoveryResult Success(@NonNull List<BluetoothDevice> deviceList) {
        return new BlDiscoveryResult(BlDiscoveryResult.success, deviceList, null);
    }

    public static BlDiscoveryResult Failed(@NonNull String errMsg) {
        return new BlDiscoveryResult(BlDiscoveryResult.failed, null, errMsg);
    }

    public static BlDiscoveryResult LocationDisabled(@NonNull String errMsg) {
        return new BlDiscoveryResult(BlDiscoveryResult.locationDisabled, null, errMsg);
    }

    public static BlDiscoveryResult PermissionError(@Nullable String errMsg) {
        return new BlDiscoveryResult(BlDiscoveryResult.permissionFailed, null, errMsg);
    }
}
