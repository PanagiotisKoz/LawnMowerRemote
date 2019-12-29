package com.teiwm.lawnmowerremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import android.util.Log;

import static android.content.ContentValues.TAG;

public class TCPClient {
    /**
     * Interface definition for a callback to be invoked when
     * connection established.
     */
    public interface OnConnectListener {

        /**
         * Called when a connection established.
         * @param error in case of error description added,
         * else empty.
         */
        void onConnect( String error );
    }

    /**
     * Interface definition for a callback to be invoked when
     * connection disconnected.
     */
    public interface OnDisconnectListener {
        /**
         * Called when a connection lost or disconnected.
         * @param error in case of error description added,
         * else empty.
         */
        void onDisconnect( String error );
    }

    private static String LOG_TAG = "RPI TCP client - ";
    private boolean receiveThreadRunning = false;
    private byte[] mLastData = new byte[32];
    private Socket mConnectionSocket;

    //Runnables for sending and receiving data
    private SendRunnable mSendRunnable;
    //Threads to execute the Runnables above
    private Thread mSendThread;
    private Thread mReceiveThread;

    private String mSeverIp =   "0.0.0.0";
    private int mServerPort = 0;

    private byte[] mRecievemsg = new byte[32];
    private OnConnectListener mOnConnect;
    private OnDisconnectListener mOnDisconnect;

    /**
     * Register a callback to be invoked when connection established.
     * @param listener The callback that will run
     */
    public void setOnConnectListener(OnConnectListener listener) {
        mOnConnect = listener;
    }

    /**
     * Register a callback to be invoked when connection established.
     * @param listener The callback that will run
     */
    public void setOnDisconnectListener(OnDisconnectListener listener) {
        mOnDisconnect = listener;
    }

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
    public void Connect( String ip, int port ) {
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
            if ( !mConnectionSocket.isClosed() )
                mConnectionSocket.close();
            if( mOnDisconnect != null ){
                mOnDisconnect.onDisconnect( "" );
            }
        } catch (IOException e) {
            if( mOnDisconnect != null ){
                mOnDisconnect.onDisconnect( e.getMessage() );
            }
            Log.e ( LOG_TAG, e.getMessage() );
        }

    }

    public byte[] GetResponce(){
        return mRecievemsg;
    }
    /**
     * Send data to server
     * @param data byte array to send
     */
    public void WriteData( byte[] data ) {
        if ( Arrays.equals( data, mLastData ) )
            return;

        if ( isConnected() ) {
            startSending();
            mSendRunnable.Send(data);
            mLastData = data;
        }
        else {
            if( mOnDisconnect != null ){
                mOnDisconnect.onDisconnect( "Connection lost." );
            }
        }
    }

    private void stopThreads() {
        if ( mReceiveThread != null )
            mReceiveThread.interrupt();

        if ( mSendThread != null )
            mSendThread.interrupt();
    }

    private void startSending() {
        mSendRunnable = new SendRunnable( mConnectionSocket );
        mSendThread = new Thread( mSendRunnable );
        mSendThread.start();
    }

    private void startReceiving() {
        ReceiveRunnable mReceiveRunnable = new ReceiveRunnable( mConnectionSocket );
        mReceiveThread = new Thread( mReceiveRunnable );
        mReceiveThread.start();
    }

    public class ReceiveRunnable implements Runnable {
        private Socket sock;
        private InputStream input;

        public ReceiveRunnable( Socket server ) {
            sock = server;
            try {
                input = sock.getInputStream();
            } catch ( Exception e ) {
                Log.e ( LOG_TAG, e.getMessage() );
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if ( !receiveThreadRunning )
                    receiveThreadRunning = true;

                try {
                    //Read the first integer, it defines the length of the data to expect
                    input.read( mRecievemsg,0, mRecievemsg.length );

                    //Stop listening so we don't have e thread using up CPU-cycles when we're not expecting data
                    stopThreads();
                } catch ( Exception e ) {
                    if( mOnDisconnect != null ){
                        mOnDisconnect.onDisconnect( e.getMessage() );
                    }
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

        public SendRunnable( Socket server ) {
            try {
                this.out = server.getOutputStream();
            } catch ( IOException e ) {
                Log.e ( LOG_TAG, e.getMessage() );
            }
        }

        /**
         * Send data as bytes to the server
         * @param bytes data to send.
         */
        public void Send(byte[] bytes) {
            this.data = bytes;
            this.hasMessage = true;
        }

        @Override
        public void run() {
            while ( !Thread.currentThread().isInterrupted() && isConnected() ) {
                if ( this.hasMessage ) {
                    try {
                        this.out.write( data, 0, data.length );
                        //Flush the stream to be sure all bytes has been written out
                        this.out.flush();
                    } catch ( IOException e ) {
                        Log.e ( LOG_TAG, e.getMessage() );
                    }
                    this.hasMessage = false;
                    this.data =  null;

                    if ( !receiveThreadRunning )
                        startReceiving(); //Start the receiving thread if it's not already running
                }
            }
        }
    }

    public class ConnectRunnable implements Runnable {

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName( mSeverIp );
                //Create a new instance of Socket
                mConnectionSocket = new Socket();

                //Start connecting to the server with 2000ms timeout
                //This will block the thread until a connection is established
                mConnectionSocket.connect( new InetSocketAddress(serverAddr, mServerPort ), 2000);
                if( mOnConnect != null ){
                    mOnConnect.onConnect("" );
                }
            } catch ( Exception e ) {
                if( mOnConnect != null ){
                    mOnConnect.onConnect( e.getMessage() );
                }
                Log.e( LOG_TAG, e.getMessage() );
            }
        }
    }
}
