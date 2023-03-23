package id.kakzaki.blue_thermal_printer.socket_thread.msg;

import androidx.annotation.Nullable;

public class SocketMsgWrite extends SocketThreadMsg {
    public final byte[] data;

    public SocketMsgWrite(byte[] data, SocketActionStatus status, @Nullable Exception error) {
        super(SocketThreadMsg.WRITE_ACTION, status, error);
        this.data = data;
    }
}
