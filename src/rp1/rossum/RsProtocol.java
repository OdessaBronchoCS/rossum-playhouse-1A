/*  -------------------------------------------------------------

    Rossum's Playhouse  --  a client/server based robot simulator
    Rossum's Playhouse is also known under the name "RP1".
    Copyright (C) 1999  G.W. Lucas

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

----------------------------------------------------------------- */


/*

 A NOTE ON DESIGN

 RsProtocol is the superclass for all Rs-related communications.  It has two
 primary derived classes:

     RsClient     --  client-side implementations
     RsConnection --  implemented within server implementations to
                      support client "connections."


 Because the Rs system is intended to support applications written in languages
 other than Java, all communications use primitive data types.   Socket communications
 are conducted using the DataInput and DataOutput interfaces.  If Rs were
 intended for purely Java implementations, it would be far simpler, and more
 elegant, to take advantage of Java's object serialization and use the object-based
 interfaces.




THE TRANSACTION HEADER

Each transaction begins with a header defined as follows:

   int      version        protocol version specification

   int      client ID      client identification zero or assigned by server
   int      client key     random key zero or assigned by server



   int      sequence       a counter which is increased each time the writeHeader
                           method is invoked.  Mainly used for debugging.


   int      transactionCode  identifies what kind of transaction  will follow


Strictly speaking, tacking some of these on to the beginning of each transaction
is redundant.  It would be reasonable to expect that version, client ID, and key
could all be resolved at initial connection and then ommitted.  We use these
mainly as a diagnostic tool which may help track down errant clients.
If a client incorrectly implements the RsProtocol, there is a good chance
that the byte sequence will be upset and the protocol header will not come
through properly.  In such a case, the software can detect the mismatch and
alert the implementor.

The overhead it adds is small.  Recall that the tcp/ip protocol defines a
minimum "packet" size for each "message" transmitted between client and server,
so that even a one-byte message is accompanies by nearly 1K bytes of packet
overhead.   In view of such considerations, adding a few header elements
to each message is seldom an issue.

Version

   Currently the software supports only rev 0.  All other version codes
   generate an exception.

Client ID, Client Key

   When a new client connects, it should pass the values (0, 0) for these
   fields.  Upon connection, the server will assign a unique client ID and
   random key.   These parameters should be used in all subsequent operations.

   The purpose of these two elements is to allow a client to make multiple connections
   to the server.  This is useful for clients that implement multi-threaded or multi-process
   architectures.  Although only one client connection (the first) is enabled to issue
   controlling instructions, subsequent connections are allowed to register as event
   listeners.

   The key serves as a weak security feature, anticipating a time when the simulator will
   be able to handle multiple robots.  At this time, it is useful largely for diagnostic
   purposes.


Sequence

   The sequence number is used strictly for logging and diagnostic purposes.
   If the server is in verbose mode it logs each transaction header received,
   recording the sequence number received from the client.  Such entries may be
   useful when reconstructing the events of a simulation, particularly if things
   do not work as expected.



TransactionCode

   Only those transactions described below are supported.



AFTER THE HEADER: EVENTS AND REQUESRS.

The most common kinds of transactions are EVENTS and REQUESTS.  For symmetry,
each of these are followed by two values:

   code        (eventCode,  requestCode)
   index       (eventIndex, requestIndex);


The eventCode might be something like "RsEvent.RsEvent.EVT_RANGE_SENSOR" event.  The eventIndex
would then be used to tell which range-sensor generated the event.
Some events, such as sensor events, are always coupled to a particular RsBodyPart
and so require an eventIndex.   Others, such as timeouts, are not coupled
to body parts.  In these cases, the eventIndex is always given as zero.

It is important to note that when an eventIndex is not needed, it is expected
to contain a zero value.   The way the code in RsConnection and RsClient is
implemented, it cannot be treated merely as a "don't care" field (a data field
to which you can assign any value because the system "doesn't care").


HOW TO ADD A NEW REQUEST OR EVENT

Request and/or Event

1. Implement the proper transaction classes and handlers.  For example:

      class     RsWheelMotionEvent          extends RsEvent
      interface RsWheelMotionEventHandler   extends RsEventHandler

      class     RsWheelMotionRequest        extends RsRequest
      interface RsWheelMotionRequestHandler extends RsRequestHandler


2. In RsProtocol, add the proper static final values to the Event Identification
   Codes (prefix RsEvent.RsEvent.EVT_) and/or Request Identification Codes (prefix RsRequest.REQ_) below.
   Make sure you also increase the size specifications for the
   corresponding MAX_CODE and ARRAY_SIZE.   Also add the appropriate
   strings to the eventName and requestName arrays (these are intended
   for diagnostics and logging).


3. Requests

      RsClient.java -- add proper send method for use by client application
                        for example RsClient.sendTimeoutRequest()

      RsConnection.java -- in the main switch/case statement, add logic for
                           reading the request from the DataInputStream and
                           invoking the handler.

                           also implement the add-request-handler API, for
                           example  RsConnection.addPlacementRequestHandler(...)

      additionally -- implement and RequestHandlers, SimTasks, or related logic in
                      the simulator package

4. Events

      RsConnection.java  -- implement send method for use by the simulator,
                            for example RsConnection.sendPositionEvent()


      RsClient.java -- in the main switch/case statement, add logic for
                        reading the request from the DataInputStream and
                        invoking the handler.

      additionally -- you may need to write utilities to manipulate or generate
                      the events.   Such logic may be integrated into the
                      simulator package, but will often also require changes
                      to the rossum package.



*/



package rp1.rossum;

import java.io.*;
import java.net.*;
import java.awt.Color;



/**
 * The base class for both RsClient and RsConnection, defines
 *most of the protocol elements for RP1 communications.
 *
 */

public class RsProtocol implements RsLogInterface {

   protected Socket              socket;
   protected DataInputStream     input;
   protected DataOutputStream    output;
   protected int                 clientID;
   protected int                 clientKey;
   protected int                 sequence;
   protected int                 serverSequence;   // don't really know what to do here yet.
   private   RsProtocolShutdownHandler shutdownHandler;

   protected RsLogInterface            logger;
   protected boolean                   verbosity;
   private   boolean                   loggerIsSet;

   protected static final int VERSION = 0;

   /* general transactions */
   protected static final int MAX_TRANSACTION_CODE   = 6;
   protected static final int TRANSACTION_ARRAY_SIZE = 7;
   protected static final int NULL_TRANSACTION       = 0;
   protected static final int CONNECT                = 1;
   protected static final int ACCEPT                 = 2;
   protected static final int EVENT                  = 3;
   protected static final int REQUEST                = 4;
   protected static final int BODY_SPECIFICATION     = 5;
   protected static final int INTERLOCK              = 6;


    // body-part identification codes
   protected static final int UNDEFINED_BODY_PART = 0;
   protected static final int WHEEL_SYSTEM        = 1;
   protected static final int BODY_CIRCLE         = 2;
   protected static final int BODY_SHAPE          = 3;
   protected static final int BODY_SENSOR         = 4;
   protected static final int BODY_TARGET_SENSOR  = 5;
   protected static final int BODY_CONTACT_SENSOR = 6;
   protected static final int BODY_RANGE_SENSOR   = 7;
   protected static final int BODY_PAINT_SENSOR   = 8;
   protected static final int BODY_PAINTER        = 9;

   // floor-plan identification codes
   protected static final int UNDEFINED_PLAN_OBJECT = 0;
   protected static final int PLAN_WALL             = 1;
   protected static final int PLAN_TARGET           = 2;
   protected static final int PLAN_PLACEMENT        = 3;
   protected static final int PLAN_NAV_NODE         = 4;
   protected static final int PLAN_NAV_LINK         = 5;
   protected static final int PLAN_PAINT            = 6;

   protected static final String transactionName[] = {
      "Null Transaction",
      "Connection Request",
      "Server Acceptance Response",
      "Request",
      "Event",
      "Body Specification",
   };



   // TO DO:  This must go into rp1.rossum.request
   protected static final String requestName[] = {
      "Client Timeout",
      "Subscription",
      "Sensor Status",
      "Start Wheel Motion",
      "Halt",
      "Position",
      "Set Position",
      "Obtain Placement",
      "Target Selection",
      "Floor Plan",
      "Heartbeat",
      "Painter Change",
      "Actuator Control",
      "Encoder Status"
   };


   // TO DO: This must go into rp1.rossum.event
   protected static final String eventName[] = {
      "Contact Sensor",
      "Mouse Click",
      "Position",
      "Range Sensor",
      "Target Sensor",
      "Timeout",
      "Motion Started",
      "Motion Halted",
      "Placement",
      "Target Selection",
      "Floor Plan",
      "Paint Sensor",
      "Heartbeat",
      "Encoder Status"
   };



   public RsProtocol(){
      socket=null;
      input=null;
      output=null;
      clientID=0;
      clientKey=0;
      sequence=0;
      serverSequence=0;
      shutdownHandler=null;
      logger=null;
      verbosity=false;
   }

   public void setSocket(Socket socketRef) throws IOException  {
      socket = socketRef;
      input =  new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   }




   /** This method is used to set input and output by the simulator when launching
       a dynamically loaded client (see RP1 Users Guide).
   */
   public void setInputOutputStreams(InputStream input, OutputStream output){
      socket = null;

      if(input==null)
         this.input = null;
      else
         this.input = new DataInputStream(new BufferedInputStream(input));

      if(output==null)
         this.output = null;
      else
         this.output = new DataOutputStream(new BufferedOutputStream(output));
   }

   public boolean isInputSet(){
      return input!=null;
   }

   public boolean isOutputSet(){
      return output!=null;
   }

   public boolean isLoggerSet(){
      return loggerIsSet;
   }

   public void setProtocolShutdownHandler(RsProtocolShutdownHandler reference){
      shutdownHandler=reference;
   }

   public void setLogger(RsLogInterface loggerReference){
      loggerIsSet = true;
      logger=loggerReference;
      if(logger==null)
         verbosity=false;
      else
         verbosity = logger.getVerbosity();
   }

   public void log(String message){
      if(logger!=null)
         logger.log(message);
   }

   public void verbose(String message){
      if(verbosity && logger!=null)
         logger.verbose(message);
   }

   public boolean getVerbosity(){
      if(logger==null)
         return false;
      else
         return logger.getVerbosity();
   }

   public void setVerbosity(boolean level){
      if(logger!=null)
         logger.setVerbosity(level);
   }

   public void logIt(String level, String message){
      if(logger!=null)
         logger.logIt(level, message);
   }



   protected int readHeader() throws IOException {
      int item;
      item = input.readInt();
      if(item!=VERSION)
         throw new IOException("RsProtocol violation, version mismatch:  reader="+VERSION+", writer="+item);

      item = input.readInt();

      if(item!=clientID){
         throw new IOException("RsProtocol violation, Client ID mismatch: reader="+clientID+", writer="+item);
      }

      item = input.readInt();
      if(item!=clientKey){
         throw new IOException("RsProtocol violation, Client Key mismatch: reader="+clientKey+", writer="+item);
      }

      serverSequence = input.readInt();

      item=input.readInt();
      if(item<1 || item>MAX_TRANSACTION_CODE)
         throw new IOException("RsProtocol violation, Unidentified transaction code "+item);
      return item;
   }

   protected int readHeader(int expectation) throws IOException {
      int  transaction=readHeader();
      if(transaction!=expectation)
         throw new IOException("Protocol violation, response "+transaction+" does match expectated "+expectation);
      return transaction;
   }

   protected  void writeHeader(int transaction) throws IOException {
      output.writeInt(VERSION);
      output.writeInt(clientID);
      output.writeInt(clientKey);
      output.writeInt(++sequence);
      output.writeInt(transaction);
   }

   protected void writeString(String s) throws IOException {
      if(s==null || s.length()==0)
         output.writeShort(0);
      else
         output.writeUTF(s);
   }

   protected String readString() throws IOException {
      String s = input.readUTF();
      return s;
   }


   protected void writeColor(Color color) throws IOException{
      if(color==null){
         output.writeInt(0);
      }else{
         output.writeInt(1);
         output.writeInt(color.getRed());
         output.writeInt(color.getGreen());
         output.writeInt(color.getBlue());
      }
   }

   protected void writeColorArray(Color [] color) throws IOException {
      if(color==null){
        output.writeInt(0);
      }else{
        output.writeInt(color.length);
        for(int i=0; i<color.length; i++)
           writeColor(color[i]);
      }
   }

   protected Color readColor() throws IOException {
      int i = input.readInt();
      if(i==0)
         return null;
      int r, g, b;
      r = input.readInt();
      g = input.readInt();
      b = input.readInt();
      return new Color(r, g, b);
   }

   protected Color [] readColorArray() throws IOException {
      int nColor = input.readInt();
      if(nColor==0)
         return null;
      Color [] color = new Color[nColor];
      for(int i=0; i<nColor; i++)
        color[i] = readColor();
      return color;
   }

   protected synchronized void shutdown(){

      if(input!=null)
         try {input.close();}catch(IOException eio1){}
      if(output!=null)
         try {output.close();}catch(IOException eio2){}
      if(socket!=null)
         try {socket.close();}catch(IOException eio3){}

      input=null;
      output=null;
      socket=null;

      if(shutdownHandler!=null)
         shutdownHandler.process();
   }


   /** @deprecated */
   @Deprecated
protected void setClientID(int ID){
      clientID=ID;
   }
   /** @deprecated */
   @Deprecated
protected void setClientKey(int key){
      clientKey=key;
   }

   /** @deprecated */
   @Deprecated
protected int getClientID(){
      return clientID;
   }

   /** @deprecated */
   @Deprecated
protected int getClientKey(){
      return clientKey;
   }


}

