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

RsConnection is the main class for handling client connections from within
a simulation server.  RsConnection provides the implementation of all server-side
methods for dealing with the RsProtocol.


TO BE DONE:   I have made a change but have not yet updated the following
notes.   The send methods are no longer synchronized...   Experiementation
showed that, especially on Windows, synchronizing overhead had a
substantial performance penalty (at least for the RP1 design).  As the project
evolved, it turned out that almost all send methods were being invoked
from the task-queue thread.   With small changes, I was able to make
it so that the task thead was the ONLY thread invoking the sends, thus
they no longer needed to be synchronized.   This approach should
help avoid synchronization overhead.

The recommend sequence here is

   ServerSocket thread accepts new socket connection and instantiates an
   RsConnection.

   The accept method for the RsConnection reads the introductory data from
   the client, most notably the clientID and clientKey.  It then registers
   (adds) all request handlers.

   The ServerSocket thread communicates with the rest of the simulator to determine
   if a valid key has been passed.    If clientID of zero has been supplied, the
   application should assume that the connect is supposed to represent a new client.
   The application should then assign the client an ID.   If the ID is greater than
   zero, it means that the client is attempting to establish an auxilliary connection
   for some existing client.  Check to ensure that the key is correct.
   If everything is okay, the run() method will send the ACCEPT transaction to the client.
   If something is not okay, invoke the shutdown() method to shutdown the socket.

   RsConnection does not extend the Thread class, it implements the Runnable interface.
   The run() method can be invoked as part of a new thread creation or within some kind of
   container class.   It is, however, a dead end.  Control never returns from run().
   It will however, call the Shutdown Handler if one has been set.


About "Helper" Threads  ------------------------------------------------

The design of java.io practically REQUIRES that an application use two threads to
talk to a client: an input thread and an output thread.   This need suggests
two general architectures for a simulator:

   1.   One thread for each client to read input;
        A single thread takes care of all output.

   2.   One thread for each client to read input.
        One thread for each client to write output.

In the case of the second approach, we say that the client input thread
has a "helper" thread to handle client output.

The disadvantage of the first approach is that if a rouge client
does not process its input, the server's socket output buffer will
eventally fill up until any attempts to write to it block.   When
a write blocks for one client, it blocks communications to ALL clients.

The "helper-thread" approach avoids this disadvantage, but has performance issues
of its own.   The amount of communications between threads is increased with
the attendent increase in calls to synchronized methods and so forth.
In a situation where high transaction rates are required, this may prove
to be a problem.  It also has the disadvantage of multiplying the number
of threads (by adding two threads per client rather than just one).
On systems running the green-thread model (such as Solaris
implementations), this is not a problem.  On systems running pre-emptive
thread models (such as Windows), this overhead could add up.


In the initial implementation of Rossum's Playhouse (RP1), I began with
the two-thread per client approach but abandoned it due to suspected
performance issues.   I say "suspected" because I was never really certain
that I HAD a performance problem at the time I made the design decision.
Even if there were issues, they might have yielded to careful coding and
optimization.  But a second factor was even more important than performance
considerations.  I wanted to simplify (and expedite) the overall coding
of the simulator.




About the Shutdown Handler  --------------------------------------------

   In the event of an IO error on the socket, it is assumed that the
   client is dead and the socket is shutdown.

   The shutdown handler should make sure that both the client thread and
   any "helper" threads are stoped.  In cases where clients make multiple
   connections to a server, you can shutdown all the related client connections,
   but this is probably overkill (they will probably generate IOExceptions
   and go away on their own).


About Client's Registering Event Handlers ---------------------------------------

   Ideally, an Rs client should not receive events unless it has actually
   registered an event handler and sent a "Subscription Request" to receive
   those events.

   One approach to event handler registration would be to provide
   methods to let the simulator check this class to see if the client had any
   event handlers and, if so, send it events.   Since the simulator and the
   RsConnection instantiation involve multiple threads, any methods facilitating
   such communication would have to be synchronized.    Also this approach
   would be easy to implement, it would run into trouble in cases where we
   had frequent events or multiple clients.   Each event (say a sensor event or
   a mouse click) would cause the simulator to perform many synchronized method
   invocations and the synchronization overhead might actually degrade performance.
   Any in many cases (as when there are no interested event listeners), the overhead
   would be completely unnecessary.

   The approach used here is for the simulator to register a request handler for
   event-registration.  When an event-registration request comes through, the
   RsConnection instantiation informs the simulator of the registration status
   by invoking the request handler's process() method.  Through these callbacks,
   the simulator can keep a table of what clients are interested and only send
   events to the appropriate places.


   About Send Methods   ----------------------------------------------

   Send methods are all synchronized.   Obviously, we don't want to allow
   two threads to simultaneously invoke send methods and have them interfere
   with each other.    But note that the first thing each send method does
   is to check to see if the socket element is null.   The shutdown method
   which is defined in RsProtocol, and which is ALSO synchronized, sets the
   socket element null as a way of signaling that the connection has been
   broken.   It is critical for the send methods to check for the state
   of socket because a shutdown can occur at any time and the send methods
   can be invoked by objects that have no knowledge that it happened.



*/


import java.io.*;
import java.awt.Color;


/**
 * A class used by the simulator to communicate with clients.
 *
 */

public class RsConnection extends RsProtocol implements Runnable
{


   private boolean [][] subscription;

   private RsConnectionBodyHandler  bodyHandler;

   RsTimeoutRequestHandler          timeoutRequestHandler;
   RsSubscriptionRequestHandler     subscriptionRequestHandler;
   RsMotionRequestHandler           motionRequestHandler;
   RsPositionRequestHandler         positionRequestHandler;
   RsPlacementRequestHandler        placementRequestHandler;
   RsHaltRequestHandler             haltRequestHandler;
   RsSensorRequestHandler           sensorRequestHandler;
   RsTargetSelectionRequestHandler  targetSelectionRequestHandler;
   RsPlanRequestHandler             planRequestHandler;
   RsHeartbeatRequestHandler        heartbeatRequestHandler;
   RsPainterChangeRequestHandler    painterChangeRequestHandler;
   RsActuatorControlRequestHandler  actuatorControlRequestHandler;
   RsEncoderStatusRequestHandler    encoderStatusRequestHandler;

   private RsInterlock              interlock;
   private int                      maxInterlockSent;




   public RsConnection(){
      bodyHandler  = null;
      interlock    = null;
      maxInterlockSent=0;
      subscription = new boolean[RsEvent.EVT_ARRAY_SIZE][];
   }

   public void addInterlock(RsInterlock interlock){
      this.interlock = interlock;
   }

   public int getMaxInterlockSent(){
      return maxInterlockSent;
   }

   protected void writeEventHeader(RsEvent event) throws IOException {
        writeHeader(EVENT);
        output.writeInt(event.getEventID());
        output.writeInt(event.getIndex());
        output.writeDouble(event.simTime);
        if(interlock==null)
           output.writeInt(0);
        else{
           maxInterlockSent = interlock.openInterlock();
           output.writeInt(maxInterlockSent);
        }
   }

   protected void writeEventHeader(int eventID, int eventIndex, double simTime)  throws IOException {
        writeHeader(EVENT);
        output.writeInt(eventID);
        output.writeInt(eventIndex);
        output.writeDouble(simTime);
        if(interlock==null)
           output.writeInt(0);
        else{
           maxInterlockSent = interlock.openInterlock();
           output.writeInt(maxInterlockSent);
        }
   }

   public void sendTimeoutEvent(RsTimeoutEvent timeout){
      if(output==null)
         return;
      try{
         writeEventHeader(timeout);
         output.writeInt(timeout.timeoutIndex);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

    public void sendMouseClickEvent(RsMouseClickEvent mouseClick){
      if(output==null)
         return;
      try{
         writeEventHeader(mouseClick);
         output.writeInt(mouseClick.button);
         output.writeInt(mouseClick.clickCount);
         output.writeDouble(mouseClick.x);
         output.writeDouble(mouseClick.y);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendPositionEvent(RsPositionEvent position){
      if(output==null)
         return;
      try{
         writeEventHeader(position);
         output.writeDouble(position.x);
         output.writeDouble(position.y);
         output.writeDouble(position.orientation);
         output.writeDouble(position.velocity);
         output.writeDouble(position.turnRate);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendPlacementEvent(RsPlacementEvent placement){
      if(output==null)
         return;
      try{
         writeEventHeader(placement);
         output.writeBoolean(placement.valid);
         writeString(placement.name);
         output.writeDouble(placement.x);
         output.writeDouble(placement.y);
         output.writeDouble(placement.orientation);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendMotionHaltedEvent(RsMotionHaltedEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);
         output.writeInt(event.causeOfHalt);
         output.writeDouble(event.x);
         output.writeDouble(event.y);
         output.writeDouble(event.orientation);
         output.writeDouble(event.duration);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendMotionStartedEvent(RsMotionStartedEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);
         output.writeDouble(event.linearVelocity);
         output.writeDouble(event.rotationalVelocity);
         output.writeDouble(event.duration);
         output.writeDouble(event.x);
         output.writeDouble(event.y);
         output.writeDouble(event.orientation);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }



   public void sendTargetSensorEvent(RsTargetSensorEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);

         output.writeDouble(event.x);
         output.writeDouble(event.y);
         output.writeBoolean(event.status);
         output.writeDouble(event.ux);
         output.writeDouble(event.uy);
         output.writeDouble(event.xDetection);
         output.writeDouble(event.yDetection);
         output.writeDouble(event.range);
         output.writeDouble(event.bearing);
                writeString(event.getNameOfObjectDetected());
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

    public void sendContactSensorEvent(RsContactSensorEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);

         output.writeBoolean(event.status);
         writeString( event.getNameOfObjectDetected());
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }


    public void sendContactSensorEvent(double simTime, int sensorID, boolean status, String contactObjectName){
      if(output==null)
         return;
      try{
         writeEventHeader(RsEvent.EVT_CONTACT_SENSOR, sensorID, simTime);
         output.writeBoolean(status);
         writeString(contactObjectName);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }


   public void sendRangeSensorEvent(RsRangeSensorEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);
         output.writeDouble(event.x);
         output.writeDouble(event.y);
         output.writeBoolean(event.status);
         output.writeDouble(event.ux);
         output.writeDouble(event.uy);
         output.writeDouble(event.range);
                writeString(event.getNameOfObjectDetected());
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }


   public void sendTargetSelectionEvent(RsTargetSelectionEvent targetSelection){
      if(output==null)
         return;
      try{
         writeEventHeader(targetSelection);

         output.writeBoolean(targetSelection.status);
         writeString(targetSelection.targetName);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendPlanEvent(RsPlanEvent planEvent){
      if(output==null)
         return;
      try{
         writeEventHeader(planEvent);
         RsPlanEncoder.send(this, (RsPlan)(planEvent.plan));
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendPaintSensorEvent(RsPaintSensorEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);
         output.writeDouble(event.x);
         output.writeDouble(event.y);
         output.writeBoolean(event.status);
         output.writeInt(event.region);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }

   public void sendHeartbeatEvent(RsHeartbeatEvent heartbeat){
       sendHeartbeatEvent(heartbeat.simTime, heartbeat.sequence);
   }

   public void sendHeartbeatEvent(double simTime, int sequence){
      if(output==null)
         return;
      try{
         writeEventHeader(RsEvent.EVT_HEARTBEAT, 0, simTime);
         output.writeInt(sequence);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }


   public void sendEncoderStatusEvent(RsEncoderStatusEvent event){
      if(output==null)
         return;
      try{
         writeEventHeader(event);
         output.writeInt(event.requestIndex);
         output.writeInt(event.encoderID);
         output.writeInt(event.accumulatorValue);
         output.writeInt(event.absoluteAccumulatorValue);
         output.writeInt(event.quadratureValue);
         output.writeInt(event.encoderInterval);
         output.writeDouble(event.encoderAngle);
         output.writeBoolean(event.clearOnReport);
         output.flush();
      }catch(IOException eio){
         System.err.println("RsConnection IO error sending event to client\n"+eio.toString());
      }
   }




   // although not the best object-oriented practice, the following is provided
   // to support the use of an RsConnection object with the RsHandlerRegistry class.
   // not only is it poor practice, it's just plain wrong.   The if/then
   // structure is inefficient.   And any use of "downcasting" (casting a more
   // to a more general type to one of its derived types) is also expensive
   // (because Java has to check at runtime to make sure the cast is okay).
   // Of course, all of this is moot, because I haven't yet integrated the
   // RsHandlerRegistry into RsConnection, nor am I certain that I will do so.


   public void sendEvent(RsEvent t){
      if(t instanceof RsTimeoutEvent)
         sendTimeoutEvent((RsTimeoutEvent) t);
      else if(t instanceof RsMouseClickEvent)
         sendMouseClickEvent((RsMouseClickEvent)t);
      else if(t instanceof RsPositionEvent)
         sendPositionEvent((RsPositionEvent)t);
      else if(t instanceof RsPlacementEvent)
         sendPlacementEvent((RsPlacementEvent)t);
      else if(t instanceof RsMotionHaltedEvent)
         sendMotionHaltedEvent((RsMotionHaltedEvent)t);
      else if(t instanceof RsTargetSensorEvent)
         sendTargetSensorEvent((RsTargetSensorEvent)t);
      else if(t instanceof RsContactSensorEvent)
         sendContactSensorEvent((RsContactSensorEvent)t);
      else if(t instanceof RsRangeSensorEvent)
         sendRangeSensorEvent((RsRangeSensorEvent)t);
      else if(t instanceof RsTargetSelectionEvent)
         sendTargetSelectionEvent((RsTargetSelectionEvent)t);
      else if(t instanceof RsPaintSensorEvent)
         sendPaintSensorEvent((RsPaintSensorEvent)t);
      else if(t instanceof RsHeartbeatEvent)
         sendHeartbeatEvent((RsHeartbeatEvent)t);
      else{
         throw new Error("RsConnection.sendEvent called with invalid event "+t.toString());
      }
   }


   public synchronized void setTimeoutRequestHandler(RsTimeoutRequestHandler handler){
      timeoutRequestHandler = handler;
   }

   public synchronized void setSubscriptionRequestHandler(RsSubscriptionRequestHandler handler){
      subscriptionRequestHandler = handler;
   }

   public synchronized void setMotionRequestHandler(RsMotionRequestHandler handler){
      motionRequestHandler = handler;
   }

   public synchronized void setPositionRequestHandler(RsPositionRequestHandler handler){
      positionRequestHandler = handler;
   }

   public synchronized void setPlacementRequestHandler(RsPlacementRequestHandler handler){
      placementRequestHandler = handler;
   }

   public synchronized void setHaltRequestHandler(RsHaltRequestHandler handler){
      haltRequestHandler = handler;
   }

   public synchronized void setSensorRequestHandler(RsSensorRequestHandler handler){
      sensorRequestHandler = handler;
   }


   public synchronized void setTargetSelectionRequestHandler(RsTargetSelectionRequestHandler handler){
      targetSelectionRequestHandler = handler;
   }

   public synchronized void setPainterChangeRequestHandler(RsPainterChangeRequestHandler handler){
      painterChangeRequestHandler = handler;
   }

   public synchronized void setActuatorControlRequestHandler(RsActuatorControlRequestHandler handler){
      actuatorControlRequestHandler = handler;
   }


   public synchronized void setEncoderStatusRequestHandler(RsEncoderStatusRequestHandler handler){
      encoderStatusRequestHandler = handler;
   }


   public synchronized void setBodyHandler(RsConnectionBodyHandler handler){
      bodyHandler = handler;
   }


   public synchronized void setPlanRequestHandler(RsPlanRequestHandler handler){
      planRequestHandler = handler;
   }

   public synchronized void setHeartbeatRequestHandler(RsHeartbeatRequestHandler handler){
      heartbeatRequestHandler = handler;
   }


   public void exchangeIntroductions() throws IOException {

      // until have established all the protocol elements, we cannot use
      // the convenience methods in the RsProtocol super class.  The first set of
      // communications should establish the clientID and clientKey, after which
      // we can use RsProtocol.readHeader() and RsProtocol.writeHeader()

      // read initial protocol and introductions
      int   item;
      item = input.readInt();

      if(item!=VERSION){
          throw new IOException("RsProtocol violation, version mismatch:  reader="+item+", writer="+item);
      }

      clientID  = input.readInt();
      clientKey = input.readInt();

      item = input.readInt();   // sequence from server, we don't care about it.
      item = input.readInt();
      if(item != CONNECT){
         throw new IOException("RsProtocol violation, invalid transaction code "+item+" when CONNECT expected");
      }

      // send Accept sequence
      writeHeader(ACCEPT);
      output.flush();
   }


   /** note:  in the references below, we have several places
       where we check verbosity
          if(verbosity)
             verbose(someString+someOtherString);

       If we are not in verbose mode, verbose is a do-nothing method,
       but we check first anyway so as not to incur the overhead of
       creating a temporary string (in Java, someString+someOtherString
       would be created as a short-lived String object even though it
       is never used).

       In cases where I don't think the verbose method would be
       invoked often, or where no short-lived string is required,
       I often don't bother checking.  If you see a place I missed,
       let me know.

   */


   public void run(){

      log("Running connection");
      try {

         int                transaction;
         int                requestCode;
         int                requestIndex;

         int                interlockIndex;

         double             timeoutPeriod;
         int                timeoutIndex;

         int                eventCode;
         int                eventIndex;
         boolean            eventEnable;

         double             linearVelocity;
         double             rotationalVelocity;
         double             duration;

         int                stringLength;
         String             string;

         int                sensorID;

         boolean            randomTarget;
         boolean            enableTarget;
         boolean            exclusiveTarget;

         double             heartbeat;

         int                painterID = 0;
         boolean            painterErasure          = false;
         boolean            activatePainter         = false;
         boolean            trailerDefined          = false;
         double             xTrailer   = 0;
         double             yTrailer   = 0;
         double             wTrailer   = 0;
         Color []           paintColor = null;

         int                i;
         double             dValue;

         log("Accepting input from client");
         while(true){
            transaction=readHeader(); // readHead ensures we have valid transaction header

            if(transaction==BODY_SPECIFICATION){
                verbose("Decoding body specification");
                RsBody body = RsProtocolBodyDecoder.receive(this);
                log("Received body specification with name "+body.getName());
                if(bodyHandler!=null)
                  bodyHandler.process(body);
                continue;
            }

            if(transaction==INTERLOCK){
               interlockIndex = input.readInt();
               if(interlock!=null)
                 interlock.closeInterlock(interlockIndex);
               continue;
            }

            if(transaction!=REQUEST)
               throw new IOException("RsConnection received a transaction which was not an request "+transactionName[transaction]);

            requestCode  = input.readInt();
            requestIndex = input.readInt();
            if(requestCode<0 || requestCode>RsRequest.REQ_MAX_CODE)
               throw new IOException("RsClient received an invalid request code "+requestCode);

            if(verbosity)
               verbose("Received request "+requestName[requestCode]+"/"+requestIndex);

            // decode the request, and invoke the request handler
            // if we have not set an request handler, an request should not have
            // been sent to the server.   even so, we will treat the aberration as benign
            // and read the request elements, thus clearing them from the input stream.



            switch(requestCode){
               case RsRequest.REQ_TIMEOUT:
                  timeoutPeriod = input.readDouble();
                  timeoutIndex  = input.readInt();
                  if(timeoutRequestHandler!=null){
                     timeoutRequestHandler.process(timeoutPeriod, timeoutIndex);
                  }
                  break;
               case RsRequest.REQ_SUBSCRIPTION:
                  eventCode=input.readInt();
                  eventIndex=input.readInt();
                  eventEnable=input.readBoolean();
                  if(verbosity)
                    verbose("Subscription request: event "+eventCode+"/"+eventIndex+" ("+eventEnable+")");
                  //  certain kinds of events are not generated by the simulator, but are
                  //  sent only upon client request (for example, timeouts and position
                  //  reports).   A correctly implemented client should not be subscribing
                  //  to these (see RsClient.java).   To provide a bit of forgiveness
                  //  we ignore requests for subscriptions to these events.
                  if(eventCode==RsEvent.EVT_TIMEOUT || eventCode==RsEvent.EVT_POSITION || eventCode==RsEvent.EVT_PLACEMENT)
                     break;  // ignore the request
                  setSubscriptionTableEntry(eventCode, eventIndex, eventEnable);
                  if(subscriptionRequestHandler!=null){
                     subscriptionRequestHandler.process(
                         new RsSubscriptionRequest(
                              eventCode,
                              eventIndex,
                              eventEnable));
                  }
                  break;
               case RsRequest.REQ_MOTION:
                  linearVelocity     = input.readDouble();
                  rotationalVelocity = input.readDouble();
                  duration           = input.readDouble();
                  if(motionRequestHandler!=null){
                     motionRequestHandler.process(
                        new RsMotionRequest(linearVelocity, rotationalVelocity, duration));
                  }
                  break;
               case RsRequest.REQ_POSITION:
                  if(positionRequestHandler!=null){
                     positionRequestHandler.process();
                  }
                  break;

               case RsRequest.REQ_PLACEMENT:
                  stringLength = input.readInt();
                  if(stringLength==0)
                     string=null;
                  else
                     string = input.readUTF();
                  if(placementRequestHandler!=null){
                     placementRequestHandler.process(
                        new RsPlacementRequest(string)
                     );
                  }
                  if(verbosity)
                    verbose("Placement request received \""+string+"\"");
                  string=null;  // to foster garbage collection;
                  break;

               case RsRequest.REQ_HALT:
                  if(haltRequestHandler!=null){
                     haltRequestHandler.process(new RsHaltRequest());
                  }
                  break;

                case RsRequest.REQ_SENSOR_STATUS:
                  sensorID = input.readInt();
                  if(sensorRequestHandler!=null){
                     sensorRequestHandler.process(new RsSensorRequest(sensorID));
                  }
                  break;

               case RsRequest.REQ_TARGET_SELECTION:
                  randomTarget    = input.readBoolean();
                  string          = readString();
                  enableTarget    = input.readBoolean();
                  exclusiveTarget = input.readBoolean();
                  if(verbosity)
                     verbose("Target selection request received for"+string);
                  if(targetSelectionRequestHandler!=null){
                     targetSelectionRequestHandler.process(
                        new RsTargetSelectionRequest(
                            randomTarget, string, enableTarget, exclusiveTarget));
                  }
                  string=null;
                  break;

               case RsRequest.REQ_PLAN:
                  if(planRequestHandler!=null){
                     planRequestHandler.process(new RsPlanRequest());
                  }
                  break;

               case RsRequest.REQ_HEARTBEAT:
                  heartbeat = input.readDouble();
                  if(verbosity)
                     verbose("Received heartbeat request with interval "+heartbeat);
                  if(heartbeatRequestHandler!=null){
                     heartbeatRequestHandler.process(new RsHeartbeatRequest(heartbeat));
                  }
                  break;
               case RsRequest.REQ_PAINTER_CHANGE:
                  painterID       = input.readInt();
                  painterErasure  = input.readBoolean();
                  if(!painterErasure){
                     activatePainter = input.readBoolean();
                     if(activatePainter){
                        trailerDefined          = input.readBoolean();
                        xTrailer                = input.readDouble();
                        yTrailer                = input.readDouble();
                        wTrailer                = input.readDouble();
                        paintColor              = readColorArray();
                     }else{
                        trailerDefined          = false;
                        xTrailer                = 0;
                        yTrailer                = 0;
                        wTrailer                = 0;
                        paintColor              = null;
                     }
                  }

                  if(verbosity)
                     verbose("Received painter change for ID "+painterID+", activation "+activatePainter);
                  if(painterChangeRequestHandler!=null){
                      painterChangeRequestHandler.process(
                         new RsPainterChangeRequest(
                                painterID,
                                painterErasure,
                                activatePainter,
                                trailerDefined,
                                xTrailer,
                                yTrailer,
                                wTrailer,
                                paintColor));
                  }
                  break;


               case RsRequest.REQ_ACTUATOR_CONTROL:
                  int actuatorID = input.readInt();
                  int controlID  = input.readInt();
                  RsActuatorControlRequest acr = new RsActuatorControlRequest(actuatorID, controlID);
                  int nParameters = input.readInt();
                  for(i=0; i<nParameters; i++){
                     string = readString();
                     dValue = input.readDouble();
                     acr.addParameter(string, dValue);
                  }
                  if(verbosity){
                     log("Received actuator control request for ID "+actuatorID+", control: "+controlID);
                  }

                  if(actuatorControlRequestHandler!=null)
                      actuatorControlRequestHandler.process(acr);
                  acr = null;
                  break;

               case RsRequest.REQ_ENCODER_STATUS:
                  int encoderStatusRequestIndex = input.readInt();
                  int nEncoders                 = input.readInt();
                  int [] encoderID = new int[nEncoders];
                  for(i=0; i<nEncoders; i++)
                      encoderID[i] = input.readInt();
                  boolean encoderClear = input.readBoolean();

                  RsEncoderStatusRequest esr =
                      new RsEncoderStatusRequest(encoderStatusRequestIndex, encoderID, encoderClear);

                  if(verbosity){
                     log("Received encoder status request index "+encoderStatusRequestIndex+" for "+nEncoders+" encoders");
                  }

                  if(encoderStatusRequestHandler!=null)
                     encoderStatusRequestHandler.process(esr);
                  esr = null;
                  break;
            }
         }
      }catch(IOException e){
         log("RsConnection terminating due exception on input socket "+e.toString());
         shutdown();
      }
   }

   public void setSubscriptionTableEntry(int row, int column, boolean value){
      if(row<0 || row>=RsEvent.EVT_ARRAY_SIZE)
         return;
      if(column<0 || column>1000)
         return;
      if(subscription[row] == null || subscription[row].length<=column){
         boolean [] oldRow = subscription[row];
         boolean [] newRow = new boolean[column+10];
         int i = 0;
         if(oldRow!=null){
            for(i=0; i<oldRow.length; i++)
               newRow[i]=oldRow[i];
         }
         for(;i<newRow.length;i++)
            newRow[i]=false;
         subscription[row]=newRow;
      }
      subscription[row][column]=value;
   }

   public boolean isClientSubscribedToEvent(int row, int column){
      if(row<0 || row>=RsEvent.EVT_ARRAY_SIZE)
         return false;
      if(column<0)
         return false;
      if(subscription[row]==null || subscription[row].length<=column)
         return false;
      return subscription[row][column];
   }


}
