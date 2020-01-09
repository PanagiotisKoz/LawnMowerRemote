package com.teiwm.lawnmowerremote;

import java.util.ArrayList;
import java.util.Iterator;

public class EventDispatcher implements IEventDispatcher {
    private static EventDispatcher ourInstance = new EventDispatcher();

    public static EventDispatcher getInstance() {
        return ourInstance;
    }

    protected ArrayList<Listener> listenerList = new ArrayList<>();

    public void addEventListener( int type, IEventHandler handler ) {
        Listener listener = new Listener( type, handler );
        removeEventListener(type);
        listenerList.add(0,listener);
    }

    public void removeEventListener( int type ) {
        for( Listener value : listenerList ) {
            if( value.getType() == type ){
                listenerList.remove( value );
            }
        }
    }

    public void dispatchEvent( Event event ) {
        for( Listener value : listenerList ) {
            if( event.getEventId() == value.getType() ){
                IEventHandler eventHandler = value.getHandler();
                eventHandler.callback( event );
            }
        }
    }

    public Boolean hasEventListener( int type ) {
        for( Listener value : listenerList ) {
            if( value.getType() == type ){
               return true;
            }
        }

        return false;
    }

    public void removeAllListeners(){
        for( Listener value : listenerList ) {
            listenerList.remove( value );
        }
    }
}
