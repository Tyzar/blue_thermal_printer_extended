package id.kakzaki.blue_thermal_printer.discovery;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class BlDiscoveryResult {
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
