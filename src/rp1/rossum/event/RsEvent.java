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


package rp1.rossum.event;
import java.io.Serializable;


/*

   A Reminder:  All RsEvent subsclasses implement Serializable, Cloneable
                (interfaces which are specificied in RsTransaction).


   about getIndex()
      in the RsEvent, all events send an associated index,
      though many of them send zeros.  Right now, only
      the sensor events use this field (to send the index
      indicating which sensor generated the event)
      but later on other events might use this field...
      an assertion which helps explain why I didn't just
      name this method "getSensorID".

*/



/**
 * The abstract base class for all simulator-event classes.
 *
 */

public abstract class RsEvent implements Serializable, Cloneable  {

   // event identification codes
   public static final int EVT_MAX_CODE             = 13;
   public static final int EVT_ARRAY_SIZE           = 14;
   public static final int EVT_CONTACT_SENSOR       = 0;
   public static final int EVT_MOUSE_CLICK          = 1;
   public static final int EVT_POSITION             = 2;
   public static final int EVT_RANGE_SENSOR         = 3;
   public static final int EVT_TARGET_SENSOR        = 4;
   public static final int EVT_TIMEOUT              = 5;
   public static final int EVT_MOTION_STARTED       = 6;
   public static final int EVT_MOTION_HALTED        = 7;
   public static final int EVT_PLACEMENT            = 8;
   public static final int EVT_TARGET_SELECTION     = 9;
   public static final int EVT_PLAN                 = 10;
   public static final int EVT_PAINT_SENSOR         = 11;
   public static final int EVT_HEARTBEAT            = 12;
   public static final int EVT_ENCODER_STATUS       = 13;



   /**
    * Identifies "kind of event"; a unique value is associated with each event class.
    */
   public final int    eventID;

   /**
    * Simulation time when the event was issued.
    */
   public final double simTime;

   protected boolean consumed;


   public RsEvent(int eventID, double simTime){
      this.eventID = eventID;
      this.simTime=simTime;
   }


   /**
    * accessor for simTime element of event
    */
   public double getSimTime(){
     return simTime;
   }

   /**
    * All RsEvent classes set a unique eventID value in their
    * constructors; the getEventID method returns that ID.
    * Its primary use is in the RP1 communications protocol and has
    * relatively limited relevance to client-side applications.
    */

   public int getEventID(){
     return eventID;   // set in constructor for all final event classes
   }


   /**
    * Used to associate event with a body part.
    */
   public int getIndex(){
     return 0;
   }


   /** Used by client-side applications to indicate that the event
    *  is not to be used by any other event handlers.
    */
   public void consume(){
      consumed=true;
   }

   /**
    * returns status of consumed flag
    */
   public boolean isConsumed(){
      return consumed;
   }

}
