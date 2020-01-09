package com.teiwm.lawnmowerremote;

public interface IEventDispatcher {

    void addEventListener( int type, IEventHandler cbInterface );
    void removeEventListener( int type );
    void dispatchEvent( Event event );
    Boolean hasEventListener( int type );
    void removeAllListeners();

}
