package com.teiwm.lawn_mower_remote;

interface IEventDispatcher {

    void addEventListener( int type, IEventHandler cbInterface );
    void removeEventListener( IEventHandler handler );
    void riseEvent( Event event );
    void removeAllListeners();
}
