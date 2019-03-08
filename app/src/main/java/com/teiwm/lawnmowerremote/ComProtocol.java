package com.teiwm.lawnmowerremote;

public final class ComProtocol {

    /* Begin of COMMAND declaration */

    /* Communication protocol COMMAND constants
     * First byte is general command, second byte is property to set or get and
     * third byte to 32 byte used for value/s.
     */

    // General commands
    public static final byte COMMAND_READY            =	0x0A;
    public static final byte COMMAND_COMM_TERMINATE   =	0x55;
    public static final byte COMMAND_IS_ALIVE 		  =	0x1F;
    public static final byte COMMAND_UNKNOW 		  =	0x0F;
    public static final byte COMMAND_PROPERTY_SET	  =	0x70;
    public static final byte COMMAND_PROPERTY_GET	  =	0x40;
    public static final byte COMMAND_PROPERTY_INFO	  =	0x10;
    public static final byte COMMAND_PROPERTY_ERROR	  =	0x20;

    // Begin of DO commands

    // Begin of moving commands
    public static final byte COMMAND_MOVE_FORWARD	  =	0x71;
    public static final byte COMMAND_MOVE_BACKWARD	  =	0x72;
    public static final byte COMMAND_MOVE_LEFT		  =	0x73;
    public static final byte COMMAND_MOVE_RIGHT		  =	0x74;
    public static final byte COMMAND_MOVE_FR		  =	0x75;
    public static final byte COMMAND_MOVE_FL		  =	0x76;
    public static final byte COMMAND_MOVE_BR		  =	0x77;
    public static final byte COMMAND_MOVE_BL		  =	0x78;
    // End of moving commands

    // Beginning of enable function commands
    public static final byte COMMAND_ENABLE_blade	  =	0x79;
    public static final byte COMMAND_ENABLE_camera	  =	0x7A;
    public static final byte COMMAND_ENABLE_prox_sens =	0x7B;
    // End of enable function commands

    // End of DO commands

    // End of commands declaration
    
    private ComProtocol(){
        throw new AssertionError();
    }
}
