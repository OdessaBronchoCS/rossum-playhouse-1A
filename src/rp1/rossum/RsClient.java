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


package rp1.rossum;

import rp1.rossum.event.*;
import rp1.rossum.request.*;


/*

  RsClient.java

  If everything is properly implemented, Clients should seldom receive an event
  for which no handler is registered (added).   This can happen due to timing
  considerations between the client and the server (the client could remove
  a handler shortly after an event has been propagated by the server, but
  before it's received).  All events for which there is no handler will
  be read and then ignored.


  Note that all the "send" methods are synchronized.

  BUGS:

     although you can add multiple event handlers, I have not implemented
     code to let you remove individual handlers.  the only thing you
     can do is to remove ALL of the handlers for a particular event type
     or event type/ID.


*/

import java.io.*;
import java.net.*;


/**
 * The main class for connecting a client-side application to the
 * simulator.
 *
 * Note that even though this class implements Runnable (through RsRunnable),
 * an application does not necessarily have to launch it as an independent thread.
 * For example, an object of type
 * RsClient might just be an element in another class which does, itself,
 * extend Thread.   The run method and Runnable interface is provided both as
 * documentation and as a convenience.
 *
 * In general, the methods in this class are organized as follows:
 * <ul>
 * <li>Send methods -- send requests (control sequences) to simulator
 * <li>Add methods -- allow an application to add event handlers
 * <li>Initialization and run methods
 * <li>Miscelaneous utilities
 * </ul>
 *
 * <p>In implementing an RP1 client, it is often useful to write a client class that
 * extends RsClient. This is not, of course, the only approach to building an RP1
 * client, but is a good way of taking advantage of Java's object-oriented capabilities.
 * The sequence for establishing an RP1 client is discussed extensively
 * in the RP1 Users Guide.
 * <p>
 * This class will not prevent you from sending a request for an event when you have
 * no event handler registered.   When the requested, but non-handled, event comes
 * in, it will be ignored.
 *
 *
 */

public class RsClient extends RsProtocol implements RsRunnable, RsLogInterface
{

   private double                simTime;
   private int                   timeoutRequestIndex;
   private int                   encoderStatusRequestIndex;
   private RsHandlerRegistry     handlerRegistry;
   public  RsProperties          rsProperties;

   public RsClient() {
      super();
      handlerRegistry = new RsHandlerRegistry(RsEvent.EVT_ARRAY_SIZE);
      simTime         = 0;
      timeoutRequestIndex       = 0;
      encoderStatusRequestIndex = 0;
   }


   public synchronized void initiateLogger(RsProperties properties) throws IOException {

      // set up the logging for the client...   some poor API design
      // results in needlessly complicated logic here...  clean this up
      // in the future

      if(!properties.logToFile && !properties.logToSystemOut){
         // no logging, do nothing
         setLogger(null);
      }else{
         if(properties.logToFile){
            setLogger(new RsLogger(properties.logFileName, properties.logToSystemOut));
         }else{ // logToSystemOut must be set
            setLogger(new RsLogger(null, properties.logToSystemOut));
         }
         logger.setVerbosity(properties.logVerbose);
         verbosity=true;
      }
   }

   public synchronized void initiateConnection(RsProperties properties) throws IOException {
      // create a "socket" connection to the server
      // if the attempt fails, Java will throw an exception and
      // we will allow the calling application to terminate.
      log("Connecting to RP1 server using: "+properties.port+"@"+properties.hostName);
      Socket socket   = new Socket(properties.hostName, properties.port);
      setSocket(socket);
      exchangeIntroductions();
   }

   public RsProperties getProperties(){
      return rsProperties;
   }

   protected void exchangeIntroductions() throws IOException {

      // until have established all the protocol elements, we cannot use
      // the convenience methods in the RsProtocol super class.  The first set of
      // communications should establish the clientID and clientKey, after which
      // we can use RsProtocol.readHeader() and RsProtocol.writeHeader()

      output.writeInt(VERSION);
      output.writeInt(clientID);
      output.writeInt(clientKey);
      output.writeInt(sequence++);
      output.writeInt(CONNECT);
      output.flush();

      int   item;
      item = input.readInt();
      if(item!=VERSION)
         throw new IOException("RsProtocol violation, version mismatch:  reader="+item+", writer="+item);

      clientID  = input.readInt();
      clientKey = input.readInt();

      item = input.readInt();   // sequence from server, we don't care about it.

      item=input.readInt();
      if(item!=ACCEPT)
         throw new IOException("RsProtocol violation, invalid transaction code "+item+" when expecting ACCEPT");
   }



   private void sendSubscription(int eventCode, int eventIndex, boolean status){
      if(output==null)
         return;

      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_SUBSCRIPTION);
         output.writeInt(0);
         output.writeInt(eventCode);
         output.writeInt(eventIndex);
         output.writeBoolean(status);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   /* --------------------------------------------------------------------------------

      The "add" functions allow the client to register event handlers.   Note that
      there are two broad classes of events:

         those which are generated by the activities of the simulation

               mouse clicks         collisions
               motions              sensor reports

         those which are generated by the server in response to a client request

               timeouts             position reports


      For the first class, simulation events, the client needs to send in a
      "subscription request" (or cancellation) to let the server know it is
      interested in receiving them.  For the second class of events, those
      which come only at request, we do not send in subscriptions.

      TO DO:  I've written method for removing ALL handlers, but haven't
              written methods for removing specific handlers.   Note that
              when such methods are implemented, it is important to
              ensure that there are no remaining handlers before sending
              off an "unsubscribe" request (if handlers remain, we stay subscribed).
   */

   //  simulation-generated events ----------------------------------------

   private void addSubscribedHandler(int row, int column, RsEventHandler reference){
      if(!handlerRegistry.isThereAHandler(row, column))
         sendSubscription(row, column, true);
      handlerRegistry.add(row, column, reference);
   }

   private void removeSubscribedHandler(int row, int column, RsEventHandler reference){
      if(reference==null)
          handlerRegistry.removeAll(row, column);
      else
          handlerRegistry.remove(row, column, reference);
      sendSubscription(row, column, false);
   }


   public synchronized void addMouseClickEventHandler(RsMouseClickEventHandler reference){
      addSubscribedHandler(RsEvent.EVT_MOUSE_CLICK, 0, reference);
   }

   public synchronized void removeMouseClickEventHandler(){
      removeSubscribedHandler(RsEvent.EVT_MOUSE_CLICK, 0, null);
   }

   public synchronized void removeMouseClickEventHandler(RsMouseClickEventHandler reference){
      removeSubscribedHandler(RsEvent.EVT_MOUSE_CLICK, 0, reference);
   }




   public synchronized void addMotionStartedEventHandler(RsMotionStartedEventHandler reference){
      addSubscribedHandler(RsEvent.EVT_MOTION_STARTED, 0, reference);
   }

   public synchronized void removeMotionStartedEventHandler(){
      removeSubscribedHandler(RsEvent.EVT_MOTION_STARTED, 0, null);
   }

   public synchronized void removeMotionStartedEventHandler(RsMotionStartedEventHandler reference){
      removeSubscribedHandler(RsEvent.EVT_MOTION_STARTED, 0, reference);
   }




   public synchronized void addMotionHaltedEventHandler(RsMotionHaltedEventHandler reference){
      addSubscribedHandler(RsEvent.EVT_MOTION_HALTED, 0, reference);
   }

   public synchronized void removeMotionHaltedEventHandler(){
      removeSubscribedHandler(RsEvent.EVT_MOTION_HALTED, 0, null);
   }
   public synchronized void removeMotionHaltedEventHandler(RsMotionStartedEventHandler reference){
      removeSubscribedHandler(RsEvent.EVT_MOTION_HALTED, 0, reference);
   }





   public synchronized void addTargetSensorEventHandler(
      RsBodyTargetSensor sensor,
      RsTargetSensorEventHandler reference)
   {
      addSubscribedHandler(RsEvent.EVT_TARGET_SENSOR, sensor.getID(), reference);
   }

   public synchronized void removeTargetSensorEventHandler(RsBodyTargetSensor sensor){
      removeSubscribedHandler(RsEvent.EVT_TARGET_SENSOR, sensor.getID(), null);
   }

   public synchronized void removeTargetSensorEventHandler(
      RsBodyTargetSensor sensor,
      RsTargetSensorEventHandler reference)
   {
      removeSubscribedHandler(RsEvent.EVT_TARGET_SENSOR, sensor.getID(), reference);
   }





   public synchronized void addContactSensorEventHandler(
      RsBodyContactSensor sensor,
      RsContactSensorEventHandler reference)
   {
      addSubscribedHandler(RsEvent.EVT_CONTACT_SENSOR, sensor.getID(), reference);
   }

   public synchronized void removeContactSensorEventHandler(RsBodyContactSensor sensor){
      removeSubscribedHandler(RsEvent.EVT_CONTACT_SENSOR, sensor.getID(), null);
   }

   public synchronized void removeContactSensorEventHandler(
      RsBodyContactSensor sensor,
      RsContactSensorEventHandler reference)
   {
      removeSubscribedHandler(RsEvent.EVT_CONTACT_SENSOR, sensor.getID(), reference);
   }





   public synchronized void addRangeSensorEventHandler(
      RsBodyRangeSensor sensor,
      RsRangeSensorEventHandler reference)
   {
      addSubscribedHandler(RsEvent.EVT_RANGE_SENSOR, sensor.getID(), reference);
   }

   public synchronized void removeRangeSensorEventHandler(RsBodyRangeSensor sensor){
      removeSubscribedHandler(RsEvent.EVT_RANGE_SENSOR, sensor.getID(), null);
   }

   public synchronized void removeRangeSensorEventHandler(
      RsBodyRangeSensor sensor,
      RsRangeSensorEventHandler reference)
   {
      removeSubscribedHandler(RsEvent.EVT_RANGE_SENSOR, sensor.getID(), reference);
   }








   public synchronized void addPaintSensorEventHandler(
      RsBodyPaintSensor sensor,
      RsPaintSensorEventHandler reference)
   {
      addSubscribedHandler(RsEvent.EVT_PAINT_SENSOR, sensor.getID(), reference);
   }

   public synchronized void removePaintSensorEventHandler(RsBodyPaintSensor sensor){
      removeSubscribedHandler(RsEvent.EVT_PAINT_SENSOR, sensor.getID(), null);
   }

   public synchronized void removePaintSensorEventHandler(
      RsBodyPaintSensor sensor,
      RsPaintSensorEventHandler reference)
   {
      removeSubscribedHandler(RsEvent.EVT_PAINT_SENSOR, sensor.getID(), reference);
   }







   //  client-requested events ---- no subscription involved  -------------------------

   public synchronized void addTimeoutEventHandler(RsTimeoutEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_TIMEOUT, 0, reference);
   }
   public synchronized void removeTimeoutEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_TIMEOUT, 0);
   }
   public synchronized void removeTimeoutEventHandler(RsTimeoutEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_TIMEOUT, 0, reference);
   }



   public synchronized void addPositionEventHandler(RsPositionEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_POSITION, 0, reference);
   }
   public synchronized void removePositionEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_POSITION, 0);
   }
   public synchronized void removePositionEventHandler(RsPositionEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_POSITION, 0, reference);
   }



   public synchronized void addPlacementEventHandler(RsPlacementEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_PLACEMENT, 0, reference);
   }
   public synchronized void removePlacementEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_PLACEMENT, 0);
   }
   public synchronized void removePlacementEventHandler(RsPlacementEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_PLACEMENT, 0, reference);
   }



   public synchronized void addTargetSelectionEventHandler(RsTargetSelectionEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_TARGET_SELECTION, 0, reference);
   }
   public synchronized void removeTargetSelectionEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_TARGET_SELECTION, 0);
   }
   public synchronized void removeTargetSelectionEventHandler(RsTargetSelectionEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_TARGET_SELECTION, 0, reference);
   }



   public synchronized void addPlanEventHandler(RsPlanEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_PLAN, 0, reference);
   }
   public synchronized void removePlanEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_PLAN, 0);
   }
   public synchronized void removePlanEventHandler(RsPlanEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_PLAN, 0, reference);
   }


   public synchronized void addHeartbeatEventHandler(RsHeartbeatEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_HEARTBEAT, 0, reference);
   }
   public synchronized void removeHeartbeatEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_HEARTBEAT, 0);
   }
   public synchronized void removeHeartbeatEventHandler(RsHeartbeatEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_HEARTBEAT, 0, reference);
   }


   public synchronized void addEncoderStatusEventHandler(RsEncoderStatusEventHandler reference){
      handlerRegistry.add(RsEvent.EVT_ENCODER_STATUS, 0, reference);
   }
   public synchronized void removeEncoderStatusEventHandler(){
      handlerRegistry.removeAll(RsEvent.EVT_ENCODER_STATUS, 0);
   }
   public synchronized void removeEncoderStatusEventHandler(RsEncoderStatusEventHandler reference){
      handlerRegistry.remove(RsEvent.EVT_ENCODER_STATUS, 0, reference);
   }





   //  request sending methods ----------------------------------------------

   /** Sends a request for a timeout event. After a period of time specified
    *  in seconds, the simulator wil respond with a timeout event. Note that
    *  this function returns a serial index number identified with the timeout request.
    *  Thus, when a timeout event is received, it can be matched to the
    *  request from which it was generated.
    *
    *  @param duration delay time in seconds until the timeout event is returned.
    */
   public synchronized int sendTimeoutRequest(double duration){
      timeoutRequestIndex++;
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_TIMEOUT);
         output.writeInt(0);
         output.writeDouble(duration);
         output.writeInt(timeoutRequestIndex);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }

      return timeoutRequestIndex;
   }

   public synchronized void sendSensorStatusRequest(RsBodySensor sensor){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_SENSOR_STATUS);
         output.writeInt(0);
         output.writeInt(sensor.getID());
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendMotionRequest(RsMotionRequest r){

      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_MOTION);
         output.writeInt(0);
         output.writeDouble(r.linearVelocity);
         output.writeDouble(r.rotationalVelocity);
         output.writeDouble(r.duration);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendPositionRequest(){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_POSITION);
         output.writeInt(0);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendPlacementRequest(String name){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_PLACEMENT);
         output.writeInt(0);
         if(name==null || name.length()==0){
            output.writeInt(0);
         }else{
            output.writeInt(name.length());
            output.writeUTF(name);
         }
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendHaltRequest(){
      try {
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_HALT);
         output.writeInt(0);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendTargetSelectionRequest(String targetName, boolean status){
      targetSelection(false, targetName, status, false);
   }

   public synchronized void sendExclusiveTargetSelectionRequest(String targetName){
      targetSelection(false, targetName, true, true);
   }

   public synchronized void sendRandomTargetSelectionRequest(boolean status){
      targetSelection(true, null, status, false);
   }

   public synchronized void sendExclusiveRandomTargetSelectionRequest(){
      targetSelection(true, null, true, true);
   }

   public synchronized void sendAllTargetsSelectionRequest(boolean status){
      targetSelection(false, "*", status, false);
   }

   private void targetSelection(
      boolean random,
      String targetName,
      boolean status,
      boolean exclusive){
      try {
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_TARGET_SELECTION);
         output.writeInt(0);
         output.writeBoolean(random);
         writeString(targetName);
         output.writeBoolean(status);
         output.writeBoolean(exclusive);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }


   public synchronized void sendPlanRequest(){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_PLAN);
         output.writeInt(0);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   /**
    * Activates the "heartbeat" function in RP1. When activated, the
    * simulator will send an RsHeartbeatEvent back to the client at a
    * fixed interval. The stream of events will continue until a
    * call is made to sendHeartbeatCancellationRequest.
    *
    * @param period heartbeat interval, in seconds.
    *
    */
   public synchronized void sendHeartbeatRequest(double period){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_HEARTBEAT);
         output.writeInt(0);
         output.writeDouble(period);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }

   public synchronized void sendHeartbeatCancellationRequest(){
       sendHeartbeatRequest(0);
   }


   public synchronized void sendPainterActivationRequest(RsBodyPainter painter){
      if(painter.paintColor==null){
         sendPainterDeactivationRequest(painter);
         return;
      }

      painter.setPainterActivationStatus(true);

      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_PAINTER_CHANGE);
         output.writeInt(0);
         output.writeInt(painter.getID());

         // write the activation status
         if(!painter.trailerDefined){
            output.writeBoolean(false);  // fake a de-activation status
            return;
         }
         output.writeBoolean(false);
         output.writeBoolean(true);  // indicates an activation
         output.writeBoolean(true);  // trailer defined
         output.writeDouble( painter.xTrailer);
         output.writeDouble( painter.yTrailer);
         output.writeDouble( painter.wTrailer);
         writeColorArray(    painter.paintColor);

         //TO DO:  add stuff for polygon, transition interval, min segment len, etc.
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }


   public synchronized void sendPainterDeactivationRequest(RsBodyPainter painter){
      painter.setPainterActivationStatus(false);
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_PAINTER_CHANGE);
         output.writeInt(0);
         output.writeInt(painter.getID());
         output.writeBoolean(false);
         output.writeBoolean(false);  // indicates an deactivation
      output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }


   public synchronized void sendPainterErasureRequest(RsBodyPainter painter){
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_PAINTER_CHANGE);
         output.writeInt(0);
         output.writeInt(painter.getID());
         output.writeBoolean(true);
      output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }


   public synchronized void sendActuatorControlRequest(RsActuatorControlRequest r){

      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_ACTUATOR_CONTROL);
         output.writeInt(0);
         output.writeInt(r.actuatorID);
         output.writeInt(r.commandID);
         String []names  = r.getParameterNames();
         double []values = r.getParameterValues();
         if(names==null){
            output.writeInt(0);
         }else{
            int n = names.length;
            output.writeInt(n);

            for(int i=0; i<n; i++){
               writeString(names[i]);
               output.writeDouble(values[i]);
            }
         }
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }
   }



   /**
    * sends a request for the status of a single encoder.
    * @param encoder An RsEncoder object associated with an RsWheel object.
    * @param clearOnRequest indicates that the encoder accumulators are to be clear upon query completion.
    */

   public synchronized int sendEncoderStatusRequest(RsEncoder encoder, boolean clearOnRequest){
      RsEncoder [] e = new RsEncoder[1];
      e[0]           = encoder;
      return sendEncoderStatusRequest(e, clearOnRequest);
   }


   /**
    * sends an request for the status of one or more encoders (given in array).
    * @param encoders An array of RsEncoder objects associated with RsWheel objects.
    * @param clearOnRequest indicates that the encoder accumulators are to be cleared upon query completion.
    *
    */

   public synchronized int sendEncoderStatusRequest(RsEncoder [] encoders, boolean clearOnRequest){
      if(encoders==null)
          return -1;
      for(int i=0; i<encoders.length; i++){
         if(encoders[i]==null)
            return -1;
      }

      encoderStatusRequestIndex++;
      try{
         writeHeader(REQUEST);
         output.writeInt(RsRequest.REQ_ENCODER_STATUS);
         output.writeInt(0);
         output.writeInt(encoderStatusRequestIndex);
         output.writeInt(encoders.length);
         for(int i=0; i<encoders.length; i++)
             output.writeInt(encoders[i].getID());
         output.writeBoolean(clearOnRequest);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error");
         shutdown();
      }

      return encoderStatusRequestIndex;
   }




  // --------------------------------------------------------------------------------


   /** The sendBodySpecification sends the body specification to the simulator
    *  and absolutely <i>must</i> be invoked before any requests are sent to RP1.
    */
   public synchronized void sendBodySpecification(RsBody body){
      try{
         writeHeader(BODY_SPECIFICATION);
         RsProtocolBodyEncoder.send(this, body);
         output.flush();
      }catch(IOException e){
         System.err.println("RsClient shutdown due to IO error sending body");
         shutdown();
      }
   }


   /**
    * The initialize call is used to set up the RP1 client before the run() method
    * is invoked.  The expectation for use is that when client classes extend
    * RsClient, they will usually override initialize(), but may call it using
    * a call to super.initialize(). Note that it performs three tasks
    * <ul>
    * <li>  reads properties from rossum.ini if they were not supplied by application
    * <li>  opens run-time log as indicated in config file if it's not opened by application
    *       by calling initiateLogger.
    * <li>  establishes connection to RP1 simulator by calling initiateConnection.
    * </ul>
    */

   public void initialize() throws IOException {
      if(rsProperties==null){
         rsProperties = new RsProperties();
         try{
            FileInputStream sf = new FileInputStream("rossum.ini");
            rsProperties.load(sf);
            sf.close();
            System.err.println("RsClient properties loaded from local file rossum.ini");
         }catch(IOException eio){
            System.err.println(
               "RsClient was unable to access rossum.ini file in current directory (folder)\n"
               +"will attempt to read rossum.ini from package-based resource");
            try{
               rsProperties.loadFromResource(this, "rossum.ini");
               System.err.println("RsClient loaded properties from package/resource file rossum.ini");
            }catch(RsPropertiesException e){
               System.err.println("Caught non-fatal exception, unable to load rossum.ini file\n"+e.toString());
            }
         }
      }

      if(!isLoggerSet()){
         initiateLogger(rsProperties);
      }
      if(!isInputSet()){
         initiateConnection(rsProperties);
      }
   }

   /**
    * This version of initialize allows the application to supply alternate
    * properties specifications rather than reading them from rossum.ini
    * as the standard initialize method does
    */
   public void initialize(RsProperties rsp) throws IOException {
      this.rsProperties = rsp;
      initialize();
   }


   /**
    * This is the main run method. It handles all simulator communications,
    * and invokes event handlers.
    */

   private RsEventHandler getNextEventHandler(RsEvent event){
      if(event.isConsumed())
         return null;

      return handlerRegistry.getNextHandler();
   }

   public  void run(){

      try{
         int                      transaction;
         int                      eventCode;
         int                      eventIndex;
         int                      eventInterlock;

         RsEventHandler                  handler;

         RsTimeoutEventHandler           timeoutEventHandler;
         RsMouseClickEventHandler        mouseClickEventHandler;
         RsMotionStartedEventHandler     motionStartedEventHandler;
         RsMotionHaltedEventHandler      motionHaltedEventHandler;
         RsPositionEventHandler          positionEventHandler;
         RsPlacementEventHandler         placementEventHandler;
         RsTargetSensorEventHandler      targetSensorEventHandler;
         RsContactSensorEventHandler     contactSensorEventHandler;
         RsRangeSensorEventHandler       rangeSensorEventHandler;
         RsTargetSelectionEventHandler   targetSelectionEventHandler;
         RsPlanEventHandler              planEventHandler;
         RsPaintSensorEventHandler       paintSensorEventHandler;
         RsHeartbeatEventHandler         heartbeatEventHandler;
         RsEncoderStatusEventHandler     encoderStatusEventHandler;


         RsTimeoutEvent                timeoutEvent;
         RsMouseClickEvent             mouseClickEvent;
         RsMotionStartedEvent          motionStartedEvent;
         RsMotionHaltedEvent           motionHaltedEvent;
         RsPositionEvent               positionEvent;
         RsPlacementEvent              placementEvent;
         RsTargetSensorEvent           targetSensorEvent;
         RsContactSensorEvent          contactSensorEvent;
         RsRangeSensorEvent            rangeSensorEvent;
         RsTargetSelectionEvent        targetSelectionEvent;
         RsPlanEvent                   planEvent;
         RsPaintSensorEvent            paintSensorEvent;
         RsHeartbeatEvent              heartbeatEvent;
         RsEncoderStatusEvent          encoderStatusEvent;


         int            requestIndex;
         int            button;
         int            causeOfHalt;
         int            clickCount;
         double         duration;

         double         linearVelocity;
         double         rotationalVelocity;
         double         orientation;
         double         turnRate;
         double         velocity;
         double         x;
         double         y;

         boolean        valid;
         
         String         string;

         boolean        sensorStatus;
         double         ux;
         double         uy;
         double         xDetection;
         double         yDetection;
         double         range;
         double         bearing;

         boolean        selectionStatus;

         RsPlan         floorPlan;

         int            region;
         int            heartbeatSequence;

         int            encoderID;
         int            encoderAccumulatorValue;
         int            encoderAbsoluteAccumulatorValue;
         int            encoderQuadratureValue;
         int            encoderInterval;
         double         encoderAngle;
         boolean        encoderClearOnRequest;

         while(true){

            transaction=readHeader();
            if(transaction!=EVENT){
               // this is fatal
               throw new IOException("RsClient received a transaction which was not an event "+transactionName[transaction]);
            }


            eventCode = input.readInt();
            eventIndex = input.readInt();
            if(eventCode<0 || eventCode>RsEvent.EVT_MAX_CODE)
               throw new IOException("RsClient received an invalid event code "+eventCode);


            // decode the event, and invoke the event handler
            // if we have not added an event handler, an event should not have
            // been sent to the client.   even so, we will treat the aberration as benign
            // and read the event elements, thus clearing them from the input stream.

            // all events include the simulation time in seconds.  when we
            // copy the simTime off the input stream, it updates the internal value
            // for this object (which may be obtained through a synchronized method).

            simTime = input.readDouble();
            if(verbosity){
               verbose("Received event "+eventName[eventCode]+"/"+eventIndex+"      Sim Time: "+simTime);
            }

            eventInterlock = input.readInt();
            if(verbosity && eventInterlock!=0){
               verbose("Received interlock "+eventInterlock);
            }


            // IMPLEMENTATION NOTE:
            // in the switch statement below, note that after an event is
            // created and processed, the reference to it is set to null.
            // this operation enables it for garbage collection.  if we did
            // no do so, the object could hang around for as long as this
            // method ran (which could be, practically, forever).
            // we also make sure that the specific event handlers get the
            // same treatment.

            handlerRegistry.setIterator(eventCode, eventIndex);
            switch(eventCode){
               case RsEvent.EVT_TIMEOUT:
                  requestIndex = input.readInt();
                  timeoutEvent = new RsTimeoutEvent(simTime, requestIndex);
                  while((handler=getNextEventHandler(timeoutEvent))!=null){
                     timeoutEventHandler = (RsTimeoutEventHandler)handler;
                     timeoutEventHandler.process(timeoutEvent);
                  }
                  timeoutEventHandler=null;
                  timeoutEvent=null;
                  break;



               case RsEvent.EVT_MOUSE_CLICK:
                  button     = input.readInt();
                  clickCount = input.readInt();
                  x          = input.readDouble();
                  y          = input.readDouble();
                  mouseClickEvent = new RsMouseClickEvent(simTime, button, clickCount, x, y);
                  while((handler=getNextEventHandler(mouseClickEvent))!=null){
                     mouseClickEventHandler=(RsMouseClickEventHandler)handler;
                     mouseClickEventHandler.process(mouseClickEvent);
                  }
                  mouseClickEventHandler=null;
                  mouseClickEvent=null;
                  break;

               case RsEvent.EVT_MOTION_STARTED:
                  linearVelocity     = input.readDouble();
                  rotationalVelocity = input.readDouble();
                  duration           = input.readDouble();
                  x                  = input.readDouble();
                  y                  = input.readDouble();
                  orientation        = input.readDouble();
                  motionStartedEvent = new RsMotionStartedEvent(
                           simTime,
                           linearVelocity,
                           rotationalVelocity,
                           duration,
                           x,
                           y,
                           orientation);
                  while((handler=getNextEventHandler(motionStartedEvent))!=null){
                     motionStartedEventHandler=(RsMotionStartedEventHandler)handler;
                     motionStartedEventHandler.process(motionStartedEvent);
                  }
                  motionStartedEventHandler=null;
                  motionStartedEvent=null;
                  break;

               case RsEvent.EVT_MOTION_HALTED:
                  causeOfHalt = input.readInt();
                  x           = input.readDouble();
                  y           = input.readDouble();
                  orientation = input.readDouble();
                  duration    = input.readDouble();
                  motionHaltedEvent = new RsMotionHaltedEvent(
                           simTime,
                           causeOfHalt,
                           x,
                           y,
                           orientation,
                           duration);
                  while((handler=getNextEventHandler(motionHaltedEvent))!=null){
                     motionHaltedEventHandler=(RsMotionHaltedEventHandler)handler;
                     motionHaltedEventHandler.process(motionHaltedEvent);
                  }
                  motionHaltedEventHandler=null;
                  motionHaltedEvent=null;
                  break;

               case RsEvent.EVT_POSITION:
                  x           = input.readDouble();
                  y           = input.readDouble();
                  orientation = input.readDouble();
                  velocity    = input.readDouble();
                  turnRate    = input.readDouble();
                  positionEvent = new RsPositionEvent(
                           simTime,
                           x,
                           y,
                           orientation,
                           velocity,
                           turnRate);
                  while((handler=getNextEventHandler(positionEvent))!=null){
                     positionEventHandler=(RsPositionEventHandler)handler;
                     positionEventHandler.process(positionEvent);
                  }
                  positionEventHandler=null;
                  positionEvent=null;
                  break;


               case RsEvent.EVT_PLACEMENT:
                  valid        = input.readBoolean();
                  string       =       readString();
                  x           = input.readDouble();
                  y           = input.readDouble();
                  orientation = input.readDouble();
                  placementEvent =  new RsPlacementEvent(
                           simTime,
                           valid,
                           string,
                           x,
                           y,
                           orientation);
                  while((handler=getNextEventHandler(placementEvent))!=null){
                     placementEventHandler=(RsPlacementEventHandler)handler;
                     placementEventHandler.process(placementEvent);
                  }
                  placementEventHandler=null;
                  placementEvent=null;
                  break;

               case RsEvent.EVT_TARGET_SENSOR:
                  x            = input.readDouble();
                  y            = input.readDouble();
                  sensorStatus = input.readBoolean();
                  ux           = input.readDouble();
                  uy           = input.readDouble();
                  xDetection   = input.readDouble();
                  yDetection   = input.readDouble();
                  range        = input.readDouble();
                  bearing      = input.readDouble();
                  string       =       readString();
                  targetSensorEvent = new RsTargetSensorEvent(
                     simTime,
                     eventIndex,
                     x, y,
                     ux, uy,
                     sensorStatus,
                     xDetection, yDetection,
                     range, bearing, string);
                  while((handler=getNextEventHandler(targetSensorEvent))!=null){
                     targetSensorEventHandler=(RsTargetSensorEventHandler)handler;
                     targetSensorEventHandler.process(targetSensorEvent);
                  }
                  targetSensorEventHandler=null;
                  targetSensorEvent=null;
                  break;

               case RsEvent.EVT_CONTACT_SENSOR:
                  sensorStatus = input.readBoolean();
                  string       =       readString();
                  contactSensorEvent = new RsContactSensorEvent(
                     simTime,
                     eventIndex,
                     sensorStatus,
                     string);
                  while((handler=getNextEventHandler(contactSensorEvent))!=null){
                     contactSensorEventHandler=(RsContactSensorEventHandler)handler;
                     contactSensorEventHandler.process(contactSensorEvent);
                  }
                  contactSensorEventHandler=null;
                  contactSensorEvent=null;
                  break;

               case RsEvent.EVT_RANGE_SENSOR:
                  x            = input.readDouble();
                  y            = input.readDouble();
                  sensorStatus = input.readBoolean();
                  ux           = input.readDouble();
                  uy           = input.readDouble();
                  range        = input.readDouble();
                  string       = readString();

                  rangeSensorEvent = new RsRangeSensorEvent(
                     simTime,
                     eventIndex,
                     x, y,
                     ux, uy,
                     sensorStatus,
                     range,
                     string);

                  while((handler=getNextEventHandler(rangeSensorEvent))!=null){
                     rangeSensorEventHandler=(RsRangeSensorEventHandler)handler;
                     rangeSensorEventHandler.process(rangeSensorEvent);
                  }
                  rangeSensorEventHandler=null;
                  rangeSensorEvent=null;
                  break;

               case RsEvent.EVT_TARGET_SELECTION:
                  selectionStatus  = input.readBoolean();
                  string           =       readString();
                  targetSelectionEvent = new RsTargetSelectionEvent(
                     simTime, string, selectionStatus);
                  while((handler=getNextEventHandler(targetSelectionEvent))!=null){
                     targetSelectionEventHandler=(RsTargetSelectionEventHandler)handler;
                     targetSelectionEventHandler.process(targetSelectionEvent);
                  }
                  targetSelectionEventHandler=null;
                  string=null;
                  targetSelectionEvent=null;
                  break;

               case RsEvent.EVT_PLAN:
                  floorPlan = RsPlanDecoder.receive(this);
                  planEvent = new RsPlanEvent(simTime, floorPlan);
                  while((handler=getNextEventHandler(planEvent))!=null){
                     planEventHandler=(RsPlanEventHandler)handler;
                     planEventHandler.process(planEvent);
                  }
                  planEventHandler = null;
                  floorPlan        = null;
                  planEvent        = null;
                  break;

               case RsEvent.EVT_PAINT_SENSOR:
                  x            = input.readDouble();
                  y            = input.readDouble();
                  sensorStatus = input.readBoolean();
                  region       =  input.readInt();

                  paintSensorEvent = new RsPaintSensorEvent(
                     simTime,
                     eventIndex,
                     x, y,
                     sensorStatus,
                     region);

                  while((handler=getNextEventHandler(paintSensorEvent))!=null){
                     paintSensorEventHandler=(RsPaintSensorEventHandler)handler;
                     paintSensorEventHandler.process(paintSensorEvent);
                  }
                  paintSensorEventHandler=null;
                  paintSensorEvent=null;
                  break;


               case RsEvent.EVT_HEARTBEAT:
                  heartbeatSequence = input.readInt();
                  heartbeatEvent = new RsHeartbeatEvent(simTime, heartbeatSequence);
                  while((handler=getNextEventHandler(heartbeatEvent))!=null){
                     heartbeatEventHandler=(RsHeartbeatEventHandler)handler;
                     heartbeatEventHandler.process(heartbeatEvent);
                  }
                  heartbeatEventHandler=null;
                  heartbeatEvent=null;
                  break;


               case RsEvent.EVT_ENCODER_STATUS:
                  requestIndex                    = input.readInt();
                  encoderID                       = input.readInt();
                  encoderAccumulatorValue         = input.readInt();
                  encoderAbsoluteAccumulatorValue = input.readInt();
                  encoderQuadratureValue          = input.readInt();
                  encoderInterval                 = input.readInt();
                  encoderAngle                    = input.readDouble();
                  encoderClearOnRequest           = input.readBoolean();

                  encoderStatusEvent = new RsEncoderStatusEvent(
                     simTime,
                     requestIndex,
                     encoderID,
                     encoderAccumulatorValue,
                     encoderAbsoluteAccumulatorValue,
                     encoderQuadratureValue,
                     encoderInterval,
                     encoderAngle,
                     encoderClearOnRequest);

                  while((handler=getNextEventHandler(encoderStatusEvent))!=null){
                     encoderStatusEventHandler=(RsEncoderStatusEventHandler)handler;
                     encoderStatusEventHandler.process(encoderStatusEvent);
                  }
                  encoderStatusEventHandler=null;
                  encoderStatusEvent=null;
                  break;




               default:
                  throw new Error("Fatal Error -- Unimplemented transaction");
            }

            if(eventInterlock>0){
               if(verbosity){
                  verbose("sent back interlock "+eventInterlock);
               }
               writeHeader(INTERLOCK);
               output.writeInt(eventInterlock);
               output.flush();
            }
         }
      }catch(IOException e){
         System.err.println("Terminating due to exception "+e.toString());
         shutdown();
      }
   }


   /**
    * returns the simulation time based on the most recent communications
    * from the simulator. Note that this value will not be updated during
    * a period of inactivity.  If more regular updates are required, it is
    * that a heartbeat request be sent to the simulator.
    */
   public synchronized double getSimTime(){
      return simTime;
   }


}


