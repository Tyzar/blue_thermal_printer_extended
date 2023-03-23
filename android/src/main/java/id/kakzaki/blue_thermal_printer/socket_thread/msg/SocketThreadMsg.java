package id.kakzaki.blue_thermal_printer.socket_thread.msg;

import androidx.annotation.Nullable;

public class SocketThreadMsg {
    public static final int WRITE_ACTION = 1;
    public static final int CLOSE_ACTION = 0;

    public final int action;

    public final SocketActionStatus status;
    @Nullable
    public final Exception error;

    public SocketThreadMsg(int action, SocketActionStatus status, @Nullable Exception error) {
        this.action = action;
        this.status = status;
        this.error = error;
    }

    public boolean isError() {
        return status == SocketActionStatus.FAILED && error != null;
    }
}
