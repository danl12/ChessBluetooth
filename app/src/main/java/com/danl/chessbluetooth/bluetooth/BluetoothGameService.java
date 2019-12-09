package com.danl.chessbluetooth.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class BluetoothGameService {

    final static int MESSAGE_STATE_CHANGE = 1;
    final static int MESSAGE_DEVICE_NAME = 2;
    final static int MESSAGE_TOAST = 3;
    final static int MESSAGE_MOVE = 4;
    final static int MESSAGE_REDO = 5;
    final static int MESSAGE_UNDO = 6;
    final static int MESSAGE_RESTART = 7;
    final static int MESSAGE_REQUEST_UNDO = 8;
    final static int MESSAGE_REQUEST_RESTART = 9;
    final static int MESSAGE_POS_HISTORY = 10;
    final static int MESSAGE_SERVER = 11;
    final static int MESSAGE_SAVE_POS_HISTORY = 12;

    final static String DEVICE_NAME = "device_name";
    final static String TOAST = "toast";

    static final int STATE_NONE = 0;
    static final int STATE_LISTEN = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;

    private static final String TAG = "BluetoothGameService";

    private static final String NAME = "BluetoothGame";
    private static final UUID MY_UUID =
            UUID.fromString("8358fbde-f54a-4af6-b2de-654e61c4f429");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    BluetoothGameService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        mState = state;
    }

    int getState() {
        return mState;
    }

    void start() {
        mAcceptThread = new AcceptThread();
        setState(STATE_LISTEN);
        mAcceptThread.start();
    }

    synchronized void connect(BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        setState(STATE_CONNECTING);
        mConnectThread.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, boolean server) {
        if (server && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mConnectedThread = new ConnectedThread(socket);
        setState(STATE_CONNECTED);
        mConnectedThread.start();

        mHandler.obtainMessage(MESSAGE_SERVER, server).sendToTarget();
    }

    void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
        mHandler.obtainMessage(MESSAGE_SAVE_POS_HISTORY).sendToTarget();
    }

    void write(int message, byte... bytes) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(message, bytes);
    }

    private void sendToast(String string) {
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, string);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionFailed() {
        sendToast("Невозможно подключить устройство");
        setState(STATE_NONE);
        start();
    }

    private void connectionLost() {
        sendToast("Связь с устройством была потеряна");
        setState(STATE_NONE);
        mHandler.obtainMessage(MESSAGE_SAVE_POS_HISTORY).sendToTarget();
        start();
    }

    private class AcceptThread extends Thread {

        BluetoothServerSocket mServerSocket;

        AcceptThread() {
            try {
                mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }

        public void run() {
            while (mState != STATE_CONNECTED) {
                BluetoothSocket socket;
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                    break;
                }

                switch (mState) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice(), true);
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "", e);
                        }
                        break;
                }
            }
        }

        void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    private class ConnectThread extends Thread {

        BluetoothSocket mSocket;
        BluetoothDevice mDevice;
        boolean mCanceled = false;

        ConnectThread(BluetoothDevice device) {
            try {
                mSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
            mDevice = device;
        }

        public void run() {
            mAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException e) {
                if (!mCanceled) {
                    try {
                        mSocket.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "", e);
                    }
                    connectionFailed();
                }
                return;
            }

            connected(mSocket, mDevice, false);
        }

        void cancel() {
            mCanceled = true;
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        BluetoothSocket mSocket;
        InputStream mInputStream;
        OutputStream mOutputStream;
        boolean mCanceled = false;

        ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            try {
                mOutputStream = socket.getOutputStream();
                mInputStream = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];
            while (mState == STATE_CONNECTED) {
                try {
                    int message = mInputStream.read();
                    if (message == MESSAGE_POS_HISTORY || message == MESSAGE_MOVE) {
                        int length = mInputStream.read();

                        int bytes;
                        StringBuilder stringBuilder = new StringBuilder();
                        while (stringBuilder.length() < length) {
                            bytes = mInputStream.read(buffer);
                            stringBuilder.append(new String(buffer, 0, bytes));
                        }

                        mHandler.obtainMessage(message, stringBuilder.toString()).sendToTarget();
                    } else {
                        mHandler.obtainMessage(message).sendToTarget();
                    }
                } catch (IOException e) {
                    if (!mCanceled) {
                        Log.e(TAG, "", e);
                        connectionLost();
                    }
                    break;
                }
            }
        }

        void write(int message, byte[] bytes) {
            try {
                mOutputStream.write(message);
                if (message == MESSAGE_MOVE || message == MESSAGE_POS_HISTORY) {
                    mOutputStream.write(bytes.length);
                    mOutputStream.write(bytes);
                }
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }

        void cancel() {
            mCanceled = true;
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }
}
