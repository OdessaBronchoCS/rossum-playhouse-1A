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
   This class contains three private elements

        private  RsSegSect  segSect;
        private  RsSegment  segment;
        private  RsSegment  bodySegment;

   which are used only in the processCollisions() method and, thus, only
   at the inception of a motion.   Usually, they are nothing more than
   scratch-space for some calculations. Once the motion is started, they
   are no longer relevant.  The reason that they are treated as
   class-objects instead of just local to the processCollision method
   is that processCollision is called a large number of times
   and, as usual, I didn't want to pay the cost of constructors and
   garbage collection for short-lived objects.

   COLLISIONS

   Note that upon creation of the motion, collisionTime is always
   set to be totalTime...  this value is used to support algorithms,
   but is not meaningful unless the flag "collision" is set.

   The algorithm for handling collisions is a bit lacking and we do
   a rather sub-optimal work-around to deal with it. See the discussion
   for the method RsMotion.collisionDeltaT for more detail.


*/




/**
 * The class used for modeling trajectories that follow a linear path.
 *
 */

public class RsMotionLine extends RsMotion {
	
   private static final long serialVersionUID = 1L;
   
   public RsMotionLine(RsMotion start, double startTime, double duration, RsVector vector, double displacement){
      super(start, startTime, duration);
      this.vector        = vector;
      this.displacement  = displacement;

      segSect       = new RsSegSect();
      segment       = new RsSegment();
      bodySegment   = new RsSegment();
   }

   @Override
protected void applyTime(){
      double t = displacement*(time-time0)/(time1-time0);
      transform.m13=transform0.m13+t*vector.x;
      transform.m23=transform0.m23+t*vector.y;
   }

   @Override
protected RsTransform getStateAtTime(double _time){
      if(_time<time0)
         return null;
      if(_time>time1)
         _time=time1;
      double t = displacement*(_time-time0)/(time1-time0);
      RsTransform tx = new RsTransform(transform0);
      tx.m13=transform0.m13+t*vector.x;
      tx.m23=transform0.m23+t*vector.y;
      return tx;
   }

   @Override
public double getVelocity(){
      double speed = displacement/(time1-time0);
      // to find if velocity is negative (vector is reverse of robot direction)
      // we find the forward heading vector and take the dot of that with the
      // vector for absolute direction of motion. if the dot is negative, we
      // are moving in reverse of the forward direction.
      RsVector forward = transform0.mapVector(1.0, 0.0);
      double   d       = forward.x*vector.x + forward.y*vector.y;
      if(d<0.0){
         // we are headed in reverse direction of robot
         return -speed;
      }
      return speed;
   }

   @Override
public RsMotionDepiction getMotionDepictionAtBodyPoint(double xBodyPoint, double yBodyPoint){
      RsPoint aPoint = transform.map(xBodyPoint, yBodyPoint);
      if(time>=time1){
         // the motion has completed, so the vector is zero
         return new RsMotionDepiction(time, aPoint, new RsVector(0.0, 0.0), 0.0, false);
      }
      double velocity = getVelocity();
      double speed    = Math.abs(velocity);
      double vX       = speed*vector.x;
      double vY       = speed*vector.y;
      boolean reverse = (velocity<0.0);
      return new RsMotionDepiction(
                     time,
                     aPoint,
                     new RsVector(vX, vY),
                     0.0, reverse);
   }

   @Override
public boolean processCollision(RsSegment []aList, int aCount, RsSegment []bList, int bCount){

      boolean   flag    = false;
      int       ia, ib;
      double    deltaT;
      double    tMin;

      RsSegment b;


      tMin=(collisionTime+1)/timeTotal;


      for(ia=0; ia<aCount; ia++){
         transform0.map2(aList[ia], bodySegment);  // map body part for motion
         for(ib=0; ib<bCount; ib++){
            b=bList[ib];

            segment.x=bodySegment.x;
            segment.y=bodySegment.y;
            segment.v.x=vector.x*displacement;
            segment.v.y=vector.y*displacement;
            segment.m = displacement;
            if(segSect.process(segment, b) && segSect.t1<tMin){
               tMin=segSect.t1;
               deltaT = collisionDeltaT(tMin*timeTotal);
               if(deltaT<collisionTime){
                  flag=true;
                  collision=true;
                  if(deltaT<=0){
                     collisionTime=0;
                     return true;   // we're done early
                  }
                  collisionTime=deltaT;
               }
            }


            //  now the backwards motion (stationary onto moving object)
            //  note the use of (1-t) rather than t
            segment.x=b.x;
            segment.y=b.y;
            segment.v.x = -vector.x*displacement;
            segment.v.y = -vector.y*displacement;
            if(segSect.process(segment, bodySegment) && segSect.t1<tMin){
               tMin=segSect.t1;
               deltaT = collisionDeltaT(tMin*timeTotal);
               if(deltaT<=collisionTime){
                  flag=true;
                  collision=true;
                  if(deltaT<=0){
                     collisionTime=0;
                     return true;   // we're done early
                  }
                  collisionTime=deltaT;
               }
            }
         }
      }

      return flag;
   }

   @Override
public void truncateForCollision(){

      if(!collision || timeTotal==0)
         return;

      displacement *= collisionTime/timeTotal;
      timeTotal=collisionTime;
      time1=time0+timeTotal;
   }

   @Override
public void stopMotionAtTime(double simTime){
      setTimeForward(simTime);
      // if there's any time remaining in motion, truncate it
      if(time0<= time && time<time1){
         displacement *= (time-time0)/timeTotal;
         time1=time;
         timeTotal=time1-time0;
      }
   }

   RsVector  vector;        // a unit vector giving direction
   double    displacement;  // the total displacement of the movement

   RsSegSect segSect;
   RsSegment segment;
   RsSegment bodySegment;
}


