package id.kakzaki.blue_thermal_printer.socket_thread;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import id.kakzaki.blue_thermal_printer.socket_thread.msg.SocketActionStatus;
import id.kakzaki.blue_thermal_printer.socket_thread.msg.SocketMsgWrite;
import id.kakzaki.blue_thermal_printer.socket_thread.msg.SocketThreadMsg;

public class SocketThread extends HandlerThread {
    private final BluetoothSocket mmSocket;

    //handler this socket thread
    private Handler handler;

    public SocketThread(BluetoothSocket socket) {
        super("socket_handler_thread");
        mmSocket = socket;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        try {
            handler = new SocketThreadHandler(Looper.myLooper(), mmSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data) {
        final Message msg = Message.obtain(
                handler,
                SocketThreadMsg.WRITE_ACTION,
                new SocketMsgWrite(
                        data,
                        SocketActionStatus.NONE,
                        null
                )
        );
        handler.sendMessage(msg);
    }

    public void cancel() {
        final Message msg = Message.obtain(handler
                , SocketThreadMsg.CLOSE_ACTION
                , new SocketThreadMsg(
                        SocketThreadMsg.CLOSE_ACTION
                        , SocketActionStatus.NONE
                        , null
                ));
        handler.sendMessage(msg);
        if (isAlive()) {
            quitSafely();
        }
    }
}

class SocketThreadHandler extends Handler {
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    SocketThreadHandler(Looper looper, BluetoothSocket socket) throws Exception {
        super(looper);
        this.socket = socket;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        final int action = msg.what;
        if (action == SocketThreadMsg.WRITE_ACTION) {
            final SocketMsgWrite writeMsg = (SocketMsgWrite) msg.obj;
            writeData(writeMsg.data);
        } else {
            closeSocket();
        }
    }

    private void writeData(byte[] data) {
        try {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket() {
        try {
            outputStream.flush();
            outputStream.close();

            inputStream.close();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
