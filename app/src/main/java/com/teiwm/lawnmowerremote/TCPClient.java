package com.teiwm.lawnmowerremote;


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.ContentValues.TAG;

public class TCPClient {

    private boolean receiveThreadRunning = false;
    private String m_LastError = "";

    private Socket mConnectionSocket;

    //Runnables for sending and receiving data
    private SendRunnable mSendRunnable;
    //Threads to execute the Runnables above
    private Thread mSendThread;
    private Thread mReceiveThread;

    private String mSeverIp =   "0.0.0.0";
    private int mServerPort = 0;

    private byte[] mRecievemsg = new byte[32];



    /**
     * Returns true if TCPClient is connected, else false
     * @return Boolean
     */
    public boolean isConnected() {
        return mConnectionSocket != null && mConnectionSocket.isConnected() && !mConnectionSocket.isClosed();
    }

    /**
     * Open connection to server
     */
    public void Connect(String ip, int port) {
        mSeverIp = ip;
        mServerPort = port;
        new Thread(new ConnectRunnable()).start();
    }

    /**
     * Close connection to server
     */
    public void Disconnect() {
        stopThreads();

        try {
            mConnectionSocket.close();
        } catch (IOException e) { }

    }

    public byte[] GetResponce(){
        return mRecievemsg;
    }
    /**
     * Send data to server
     * @param data byte array to send
     */
    public void WriteData(byte[] data) {
        if (isConnected()) {
            startSending();
            mSendRunnable.Send(data);
        }
    }

    private void stopThreads() {
        if (mReceiveThread != null)
            mReceiveThread.interrupt();

        if (mSendThread != null)
            mSendThread.interrupt();
    }

    private void startSending() {
        mSendRunnable = new SendRunnable(mConnectionSocket);
        mSendThread = new Thread(mSendRunnable);
        mSendThread.start();
    }

    private void startReceiving() {
        ReceiveRunnable mReceiveRunnable = new ReceiveRunnable(mConnectionSocket);
        mReceiveThread = new Thread(mReceiveRunnable);
        mReceiveThread.start();
    }

    public class ReceiveRunnable implements Runnable {
        private Socket sock;
        private InputStream input;

        public ReceiveRunnable(Socket server) {
            sock = server;
            try {
                input = sock.getInputStream();
            } catch (Exception e) { }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if (!receiveThreadRunning)
                    receiveThreadRunning = true;

                try {
                    //Read the first integer, it defines the length of the data to expect
                    input.read(mRecievemsg,0,mRecievemsg.length);

                    //Stop listening so we don't have e thread using up CPU-cycles when we're not expecting data
                    stopThreads();
                } catch (Exception e) {
                    Disconnect(); //Gets stuck in a loop if we don't call this on error!
                }
            }
            receiveThreadRunning = false;
        }

    }

    public class SendRunnable implements Runnable {

        byte[] data;
        private OutputStream out;
        private boolean hasMessage = false;
        int dataType = 1;

        public SendRunnable(Socket server) {
            try {
                this.out = server.getOutputStream();
            } catch (IOException e) {
            }
        }

        /**
         * Send data as bytes to the server
         * @param bytes
         */
        public void Send(byte[] bytes) {
            this.data = bytes;
            this.hasMessage = true;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if (this.hasMessage) {
                    try {
                        this.out.write(data, 0, data.length);
                        //Flush the stream to be sure all bytes has been written out
                        this.out.flush();
                    } catch (IOException e) { }
                    this.hasMessage = false;
                    this.data =  null;

                    if (!receiveThreadRunning)
                        startReceiving(); //Start the receiving thread if it's not already running
                }
            }
        }
    }

    public String GetLastError() {
        return m_LastError;
    }
    public class ConnectRunnable implements Runnable {

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(mSeverIp);
                //Create a new instance of Socket
                mConnectionSocket = new Socket();

                //Start connecting to the server with 5000ms timeout
                //This will block the thread until a connection is established
                mConnectionSocket.connect(new InetSocketAddress(serverAddr, mServerPort), 5000);


            } catch (Exception e) {
                m_LastError = e.getMessage();
            }
        }
    }
}
