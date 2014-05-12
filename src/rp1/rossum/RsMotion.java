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


/*

   initial transformation
   current transformation
   initial time
   current time

   signficant time increment
   final time for motion

   A motion has duration which is indicated by the time0 and time1 values.
   During the motion, the element "time" is set forward to indicate progress.
   The element "transform" is always coupled to time.   The setTimeForward
   method is used to advance the time and compute a new transform.  It will
   not, however, allow you to advance time past time1.   After time1, it
   is assumed that the object is at rest.


   COLLISIONS  

   Note that upon creation of the motion, collisionTime is always
   set to be totalTime...  this value is used to support algorithms,
   but is not meaningful unless the flag "collision" is set.

   The algorithm for handling collisions is a bit lacking and we do
   a rather sub-optimal work-around to deal with it. See the discussion
   for the method RsMotion.collisionDeltaT for more detail.



   CONTINUITY

   Most constructors for the classes derived from RsMotion all call
   super(...), in effect invoking the constructor for RsMotion.
   The constructor for RsMotion requires that you pass a previous motion
   and a "startTime" as inputs to create a new motion.   This approach allows
   the new object to capture the position and orientation transform from the
   earlier motion.   In effect, all motions are concatenated together...
   "where I am now is a product of all the movements I have made before."

   The new motion constructor also requires that you pass in a startTime.
   It may be that the old motion terminated and that there was a substantial
   gap in time before the new motion was to begin.

   The RsMotionNull class breaks continuity. The idea is that it allows
   you to specify a new position. The first motion for a simulacrum when it is
   placed on the floor plan should be a RsMotionNull.

   CONTINUITY ID

   RsMotion and its derived classes contain an element called the "continuityID".
   In most cases, when you construct one motion from a predecessors, the
   continuityID is copied from the previous motion. If you implement new classes
   it is IMPORTANT that you continue this tradition.  The one exception is
   the RsMotionNull class which is used when continuity is broken and
   a new sequence of motions is begun.


   CAUTION:

   Before creating a new motion with a startTime, it is imperitive that you invoke
   oldMotion.stopMotionAtTime(startTime).  The reason for this is that setTimeForward
   may not have been called for the oldMotion, and the internal time value
   for the old motion may not be up to date.   You also need to truncate the
   internal record of the duration of the motion to ensure that the simulacrum
   is placed in the correct position.  The constructor for RsMotion
   does not call setTimeForward.  Many object-oriented gurus, Bertrand Meyer
   in particular, have suggested that it is good practice to allow only "set"
   methods to change the contents of objects.  A "processing" method, which
   uses an object as an input, should never change the input objects states.
   A process method should also not have internal state values which are
   changed as a byproduct of some process.   While I break this rule frequently,
   I agree it is a good idea in general.   So the motion constructors do not change the
   state of earlier motions which are supplied as inputs.

   Null Movements

   One of the RsMotion derived classes has a constructor which does
   not take a previous motion as an input.   The RsMotionNull class
   allows an application to specifically set position and orientation.
   As such, the new position of the object has nothing to do with the
   old (it is as if it has magically teleported to a new place and time).

*/





/**
 * The abstract base class for all classes that model body trajectories.
 *
 */

public abstract class RsMotion extends RsComponent {



   public double  time0;      // initial time of motion in seconds
   public double  time1;      // final time of motion
   public double  timeTotal;  // total time for motion
   public double  time;       // current time


   public double  significantDeltaT;  // significant time delta

   public RsTransform   transform0;  // initial transform;
   public RsTransform   transform;   // current transform

   public boolean collision;
   public double  collisionTime;

   protected int          continuityID;
   protected int          continuitySeriesID;
   private  static int    serialMotionContinuityID;


   protected RsMotion(){
      time0 = 0;
      time  = 0;
      time1     = 0;
      timeTotal = 0;
      significantDeltaT = 0;

      transform0 = null;
      transform  = null;

      collision  = false;
   }

   public RsMotion(RsMotion start, double startTime, double duration){
      transform0 = new RsTransform(start.transform);
      time0              = startTime;
      time               = time0;
      time1              = time0+duration;
      timeTotal          = time1-time;
      significantDeltaT  = time1-time0;     // TO DO: may get smaller later on
      transform          = new RsTransform(transform0);
      collision          = false;
      collisionTime      = timeTotal;  // an algorithmic necessity

      continuityID       = start.getContinuityID();
      continuitySeriesID = start.getContinuitySeriesID()+1;
   }


   public boolean setTime(double simTime){
      if(simTime<time0 || simTime>time1)
         return false;
      time=simTime;
      applyTime();
      return true;
   }

   public boolean setTimeForward(double simTime){
      if(simTime<=time)
         return false;
      if(simTime>time1){
         if(time==time1)
            return false; // time is already advanced as far as it can go
         time=time1;
      }else
         time=simTime;

      applyTime();
      return true;
   }

   public void stopMotionAtTime(double simTime){
      setTimeForward(simTime);
      // if there's any time remaining in motion, truncate it
      if(time0<=time && time<time1){
         time1=time;
         timeTotal=time1-time0;
      }
   }

   protected abstract void        applyTime();
   protected abstract RsTransform getStateAtTime(double time);

   // TO DO: the scope on the following may want to be protected... find this out.
   public abstract RsMotionDepiction getMotionDepictionAtBodyPoint(double xBodyPoint, double yBodyPoint);

   public boolean processCollision(RsSegment [] a, int aLen, RsSegment [] b, int bLen){
      return false;
   }


   public double getTurnRate(){
      return 0;   // overridden in some derived classes
   }

   public double getVelocity(){
      return 0;   // overridden in some derived classes
   }

   public double getTime(){
      return time;
   }

   public RsPoint getPosition(){
      return transform.getOffset();
   }

   public double getOrientation(){
      return transform.getTheta();
   }


   public void truncateForCollision(){
      if(!collision || timeTotal==0)
         return;
      timeTotal=collisionTime;
      time1=time0+timeTotal;
   }

   static synchronized public int getNewContinuityID(){
      serialMotionContinuityID++;
      return serialMotionContinuityID;
   }

   public int getContinuityID(){
      return continuityID;
   }

   public int getContinuitySeriesID(){
      return continuitySeriesID;
   }


   /**
    * The algorithm for handling collisions is a bit lacking and we do
    * a rather sub-optimal work-around to deal with it.
    * There is a not-so-small fudge factor subtracted from the computation of the
    * time-to-collision.   We need to ensure that the robot never comes in
    * contact with the wall because the model lacks the sophistication to
    * handle that case.  So, in the event of a collision, we reduce the time
    * of travel by one tenth millisecond, ensuring that the robot stops just shy
    * of the collision point. 
    */

   protected double collisionDeltaT(double deltaT){
      double a = Math.floor(deltaT*1000.0-0.1);  
      if(a<0)
        return 0;
      return a/1000.0;
   }
      
}



