package com.teiwm.lawnmowerremote;

import java.util.ArrayList;
import java.util.Iterator;

public class EventDispatcher implements IEventDispatcher {
    private static EventDispatcher ourInstance = new EventDispatcher();

    public static EventDispatcher getInstance() {
        return ourInstance;
    }

    private ArrayList<Listener> listenerList = new ArrayList<>();

    public void addEventListener( int type, IEventHandler handler ) {
        Listener listener = new Listener( type, handler );
        listenerList.add( listener );
    }

    public void removeEventListener( int type ) {
        for ( Listener listener : listenerList ) {
            if (listener.getType() == type) {
                listenerList.remove( listener );
            }
        }

    }

    public void dispatchEvent( Event event ) {
        for( Listener listener : listenerList ) {
            if( event.getEventId() == listener.getType() ){
                IEventHandler eventHandler = listener.getHandler();
                eventHandler.callback( event );
            }
        }
    }

    public Boolean hasEventListener( int type ) {
        for( Listener listener : listenerList ) {
            if( listener.getType() == type ){
               return true;
            }
        }

        return false;
    }

    public void removeAllListeners(){
        if ( !listenerList.isEmpty() )
            listenerList.clear();
    }
}
