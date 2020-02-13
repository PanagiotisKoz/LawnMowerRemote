package com.teiwm.lawn_mower_remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.util.Log;

class TCPClient {
    private final String LOG_TAG = "RPI TCP client";
    private final Socket mConnectionSocket = new Socket();

    private OnConnectListener mConnectCallback;
    private OnDisconnectListener mDisconnectCallback;
    private OnReceiveListener mReceiveCallback;

    private Thread mReceiveThread;

    private void startReceiving() {
        ReceiveRunnable receiveRunnable = new ReceiveRunnable();
        mReceiveThread = new Thread( receiveRunnable );
        mReceiveThread.start();
    }

    private class ReceiveRunnable implements Runnable {
        @Override
        public void run() {
            while ( !Thread.currentThread().isInterrupted() ) {
                try {
                    InputStream input = mConnectionSocket.getInputStream();
                    InputStreamReader input_reader = new InputStreamReader(input);
                    BufferedReader buffer = new BufferedReader(input_reader);
                    String response;

                    response = buffer.readLine();
                    if (response == null) {
                        disconnect( "Connection closed by lawn mower." );
                    }
                    else
                        if ( mReceiveCallback != null )
                            mReceiveCallback.onReceive( response );
                } catch (IOException e) {
                    disconnect( e.getMessage() );
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
    }

    private class SendRunnable implements Runnable {
        private final byte[] mData;
        SendRunnable( String data ) {
            mData = data.getBytes();
        }

        @Override
        public void run() {
            if( mReceiveThread != null )
                mReceiveThread.interrupt();
            try {
                OutputStream out = mConnectionSocket.getOutputStream();
                out.write( mData, 0, mData.length );
                //Flush the stream to be sure all bytes has been written out
                out.flush();
            } catch ( IOException e ) {
                disconnect( e.getMessage() );
                Log.e ( LOG_TAG, e.getMessage() );
            }
            startReceiving(); //Start the receiving thread if it's not already running
        }
    }

    /**
     * Interface definition for a callback to be invoked when a
     * client disconnects
     */
    public interface OnDisconnectListener {

        /**
         * Called when client disconnects.
         * @param errorMsg if error caused desconnection, this contains error description.
         */
        void onDisconnect( String errorMsg );
    }

    /**
     * Interface definition for a callback to be invoked when a
     * client disconnects
     */
    public interface OnConnectListener {

        /**
         * Called when client connects.
         */
        void onConnect( );
    }

    /**
     * Interface definition for a callback to be invoked when a
     * client receive a message.
     */
    public interface OnReceiveListener {

        /**
         * Called when client receives a message.
         * @param response received message.
         */
        void onReceive( String response );
    }

    public void SetOnConnectListener ( OnConnectListener listener ) {
        mConnectCallback = listener;
    }

    public void SetOnDisconnectListener ( OnDisconnectListener listener ) {
        mDisconnectCallback = listener;
    }

    public void SetOnReceiveListener ( OnReceiveListener listener ) {
        mReceiveCallback = listener;
    }

    /**
     * Send data as bytes to the server
     * @param data data to send.
     */
    public void send( String data ) {
        SendRunnable sendRunnable = new SendRunnable( data );
        Thread sendThread = new Thread( sendRunnable );
        sendThread.start();
    }

    /**
     * Returns true if TCPClient is connected, else false
     * @return Boolean
     */
    public boolean isConnected() {
        return mConnectionSocket != null && mConnectionSocket.isConnected() && !mConnectionSocket.isClosed();
    }

    public void connect( String ip, int port ){
        try {
            InetAddress serverAddr = InetAddress.getByName( ip );
            //Start connecting to the server with 8000ms timeout
            //This will block the thread until a connection is established
            mConnectionSocket.connect( new InetSocketAddress( serverAddr, port ),
                                    8000 );
            mConnectionSocket.setTcpNoDelay( true );

            startReceiving();
            if ( mConnectCallback != null )
                mConnectCallback.onConnect();

        } catch ( IOException e ) {
            disconnect( e.getMessage() );
        }
    }

    /**
     * Close connection to server
     */
    private void disconnect( String msg ) {
        if ( mReceiveThread != null )
            mReceiveThread.interrupt();

        try {
            if ( !mConnectionSocket.isClosed() ) {
                mConnectionSocket.close();
            }
        } catch (IOException e) {
            Log.e ( LOG_TAG, e.getMessage() );
        }

        if ( mDisconnectCallback != null )
            mDisconnectCallback.onDisconnect( msg );
    }

    /**
     * Close connection to server
     */
    public void disconnect( ) {
       disconnect( "" );
    }
}
