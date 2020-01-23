package com.teiwm.lawnmowerremote;

import android.util.SparseArray;

import java.util.ArrayList;

public class EventDispatcher implements IEventDispatcher {
    private static EventDispatcher ourInstance = new EventDispatcher();

    public static EventDispatcher getInstance() {
        return ourInstance;
    }

    private SparseArray< ArrayList< Listener > > listeners = new SparseArray<>();

    public void addEventListener( int type, IEventHandler handler ) {
        ArrayList< Listener > listenersList = listeners.get( type );
        if ( listenersList == null ) {
            listenersList = new ArrayList<>( );
            listenersList.add( new Listener(handler) );
            listeners.put( type, listenersList );
        }
        else {
            listenersList.add( new Listener(handler) );
        }
    }

    public void removeEventListener( IEventHandler handler ) {
        for( int i = 0; i < listeners.size(); i++ ) {
            ArrayList< Listener > list = listeners.get( listeners.keyAt( i ) );
            for ( Listener listener : list ) {
                if (listener.getHandler() == handler) {
                    list.remove( listener );
                }
            }
        }
    }

    public void dispatchEvent( Event event ) {
        ArrayList< Listener > listenersList = listeners.get( event.getEventId() );

        for( Listener listener : listenersList ) {
            IEventHandler eventHandler = listener.getHandler();
            eventHandler.callback( event );
        }
    }

    public void removeAllListeners(){
        if ( listeners.size() != 0 )
            listeners.clear();
    }
}
