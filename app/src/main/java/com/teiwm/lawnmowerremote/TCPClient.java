package com.teiwm.lawnmowerremote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class TCPClient {
    private static String LOG_TAG = "RPI TCP client";
    private boolean receiveThreadRunning = false;
    private Socket mConnectionSocket;

    //Runnables for sending and receiving data
    private SendRunnable mSendRunnable;
    //Threads to execute the Runnables above
    private Thread mSendThread;
    private Thread mReceiveThread;

    private String mSeverIp =   "0.0.0.0";
    private int mServerPort = 0;

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

    private void RiseEvent( int type, String params ) {
        Event event = new Event( type, params );
        EventDispatcher.getInstance().dispatchEvent( event );
    }

    public void Connect( String ip, int port ){
        mSeverIp = ip;
        mServerPort = port;

        try {
            InetAddress serverAddr = InetAddress.getByName( mSeverIp );
            //Create a new instance of Socket
            mConnectionSocket = new Socket();

            //Start connecting to the server with 2000ms timeout
            //This will block the thread until a connection is established
            if ( !isConnected() )
                mConnectionSocket.connect( new InetSocketAddress(serverAddr, mServerPort ),
                        10000);

            EventDispatcher.getInstance().addEventListener(
                    Local_event_ids.tcp_event_ids.send.getID(),
                    new IEventHandler() {
                        @Override
                        public void callback(Event event) {
                            if ( isConnected() ) {
                                startSending();
                                String data = event.getParams().toString();
                                mSendRunnable.Send( data.getBytes() );
                            }
                        }
                    });

            RiseEvent( Local_event_ids.tcp_event_ids.connected.getID(), null );

            startReceiving();
        } catch ( IOException e ) {
            Log.e( LOG_TAG, e.getMessage() );
            RiseEvent( Local_event_ids.tcp_event_ids.disconnected.getID(),
                    e.getMessage() );
        }
    }

    /**
     * Returns true if TCPClient is connected, else false
     * @return Boolean
     */
    public boolean isConnected() {
        return mConnectionSocket != null && mConnectionSocket.isConnected() && !mConnectionSocket.isClosed();
    }

    /**
     * Close connection to server
     */
    public void Disconnect() {
        stopThreads();

        try {
            if ( !mConnectionSocket.isClosed() ) {
                mConnectionSocket.close();

                RiseEvent( Local_event_ids.tcp_event_ids.disconnected.getID(), null );
            }
        } catch (IOException e) {
            Log.e ( LOG_TAG, e.getMessage() );
            RiseEvent( Local_event_ids.tcp_event_ids.disconnected.getID(),
                    e.getMessage() );
        }

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
                    byte[] packet_size = new byte[1];
                    if ( input.read( packet_size, 0, 1 ) > 0 ) {

                        ByteArrayOutputStream recieved_data = new ByteArrayOutputStream();
                        //Read the first integer, it defines the length of the data to expect
                        byte[] msg = new byte[packet_size[0]];
                        if ( input.read(msg, 0, packet_size[0]) > 0 ) {
                            recieved_data.write(msg, 0, msg.length);
                            recieved_data.flush();
                            RiseEvent( Local_event_ids.tcp_event_ids.response.getID(),
                                     recieved_data.toString() );
                        }
                    }

                } catch ( IOException e ) {
                    Log.e ( LOG_TAG, e.getMessage() );
                    if ( isConnected() )
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
                if( isConnected() )
                    Disconnect();
            }
        }

        /**
         * Send data as bytes to the server
         * @param data data to send.
         */
        public void Send( byte[] data ) {
            this.data = data;
            this.hasMessage = true;
        }

        @Override
        public void run() {
            while ( !Thread.currentThread().isInterrupted() && isConnected() ) {
                if ( this.hasMessage ) {
                    if( receiveThreadRunning )
                        mReceiveThread.interrupt();
                    try {
                        this.out.write( data, 0, data.length );
                        //Flush the stream to be sure all bytes has been written out
                        this.out.flush();
                    } catch ( IOException e ) {
                        Log.e ( LOG_TAG, e.getMessage() );
                        if( isConnected() )
                            Disconnect();
                    }
                    this.hasMessage = false;
                    this.data =  null;

                    if ( !receiveThreadRunning )
                        startReceiving(); //Start the receiving thread if it's not already running
                }
            }
        }
    }
}
