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


/* it is possible that the motion could involve a circular
   arc of MORE than 360 degrees.  The collision test doesn't
   care about that, it only handles cases up to 360 degrees.
   But if there's going to be a collision, it will happen within the
   first 360 degrees and truncate the motion.

   note that unlike case of linear motion, displacement is
   in radians.   each point on robot may have a unique speed
   and displacement (length of arc for trajectory).


   COLLISIONS

   Note that upon creation of the motion, collisionTime is always
   set to be totalTime...  this value is used to support algorithms,
   but is not meaningful unless the flag "collision" is set.

   The algorithm for handling collisions is a bit lacking and we do
   a rather sub-optimal work-around to deal with it. See the discussion
   for the method RsMotion.collisionDeltaT for more detail.

*/



/**
 * The class used for modeling trajectories that follow a circular arc.
 *
 */

public class RsMotionCircle extends RsMotion {

   private static final long serialVersionUID = 1L;
	
   RsPoint      pivot;
   double       angle;          // current angle
   double       deltaAngle;     // in radians, of course, but |deltaAngle| may be > 2*PI
   double       turnRadius;

   RsSegment    testSegment;
   RsTransform  testTransform;

   double       rotationalVelocity;
   double       linearVelocity;
   double       pivotSide;


   public RsMotionCircle(RsMotion start, double startTime, double duration, RsPoint refPivot, double displacement){
      super(start, startTime, duration);
      pivot = refPivot;

      RsPoint center = transform.getOffset();  // center pos of robot body
      double  ax     = center.x-pivot.x;
      double  ay     = center.y-pivot.y;
      turnRadius     = Math.sqrt(ax*ax+ay*ay);
      if(turnRadius<1.0e-10)
         turnRadius = 0;
      deltaAngle         = displacement;
      rotationalVelocity = deltaAngle/duration;
      linearVelocity     = turnRadius*rotationalVelocity;

      // linearVelocity will have the same sign as the rotationalVelocity
      // but if the pivot is to the right side, then the velocities must be
      // of different sign, so we will multiply it by pivotSide

      if(turnRadius==0){
          pivotSide = 0;
      }else{
         RsPoint left    = transform.map(0.0, 1.0);
         double       bx = center.x - left.x;
         double       by = center.y - left.y;
         if( ax*bx+ay*by>=0.0 ){
             pivotSide = 1;   // it's to the left
         }else{
             pivotSide = -1;  // it's to the right
         }
      }
      linearVelocity *= pivotSide;

      testSegment        = new RsSegment();
      testTransform      = new RsTransform(transform0);
      testTransform.m13 -= pivot.x;
      testTransform.m23 -= pivot.y;
   }

   @Override
protected void applyTime(){
      double t = (time-time0)/(time1-time0);
      angle = t*deltaAngle;
      rotateAboutPivot(transform, transform0, pivot.x, pivot.y, angle);
 }

   @Override
protected RsTransform getStateAtTime(double _time){
      if(_time<time0)
         return null;
      if(_time>time1)
          _time=time1;
      double t     = (_time-time0)/(time1-time0);
      double angle = t*deltaAngle;

      RsTransform transformAtTime = new RsTransform();
      rotateAboutPivot(transformAtTime, transform0, pivot.x, pivot.y, angle);
      return transformAtTime;
   }


   // given a transform indicating the robot's initial position rotate it
   // by angle theta about the pivot point.  We do this by concatenating
   // two transformations:
   //    transformation for initial placement (t0, from start of motion)
   //    transformation for rotation about pivot by angle theta
   //

   private void rotateAboutPivot(RsTransform t, RsTransform t0, double px, double py, double theta){
      double cosTheta = Math.cos(theta);
      double sinTheta = Math.sin(theta);

      t.m11 =  cosTheta*t0.m11-sinTheta*t0.m21;
      t.m12 =  cosTheta*t0.m12-sinTheta*t0.m22;
      t.m13 =  cosTheta*t0.m13-sinTheta*t0.m23-cosTheta*px+sinTheta*py+px;

      t.m21 =  sinTheta*t0.m11+cosTheta*t0.m21;
      t.m22 =  sinTheta*t0.m12+cosTheta*t0.m22;
      t.m23 =  sinTheta*t0.m13+cosTheta*t0.m23-sinTheta*px-cosTheta*py+py;
   }



   @Override
public double getTurnRate(){
      return deltaAngle/(time1-time0);  // radians per second
   }

   @Override
public double getVelocity(){
      return turnRadius*getTurnRate();
   }


   @Override
public RsMotionDepiction getMotionDepictionAtBodyPoint(double xBodyPoint, double yBodyPoint){

      RsPoint aPoint = transform.map(xBodyPoint, yBodyPoint);
      if(time>=time1){
         // the motion has completed, so the vector is zero
         return new RsMotionDepiction(time, aPoint, new RsVector(0.0, 0.0), 0.0, false);
      }

      double aX = aPoint.x-pivot.x;
      double aY = aPoint.y-pivot.y;
      // (vX,vY) is the perpendicular to (aX,aY) scaled for the
      // rotational velocity.  Note that if the rotational velocity
      // is negative, the direction of the motion vector will be inverted
      double   vX = -aY*rotationalVelocity;
      double   vY =  aX*rotationalVelocity;
      RsVector v  = new RsVector(vX, vY);

      RsVector forward  = transform.mapVector(1.0, 0.0);
      double   d        = forward.x*v.x + forward.y*v.y;
      boolean  reverse  = (d<0.0);

      return new RsMotionDepiction(
                           time,
                           aPoint,
                           v,
                           rotationalVelocity, reverse);
   }




   @Override
public boolean processCollision(RsSegment []aList, int aCount, RsSegment []bList, int bCount){

      boolean   flag    = false;
      int       ia, ib;
      double    deltaT;

      RsSegment s;

      double   xi, yi, xt, yt, ax, ay;
      double   a, b, c, cPrime, r2, d;
      int      n;
      double   [] t = new double[2];
      double   theta;

      for(ib=0; ib<bCount; ib++){
         s=bList[ib];
         a=s.v.x*s.v.x + s.v.y*s.v.y;
         ax=s.x-pivot.x;   // treat pivot as origin
         ay=s.y-pivot.y;
         b=2*(s.v.x*ax + s.v.y*ay);
         cPrime = ax*ax+ay*ay;
         for(ia=0; ia<aCount; ia++){
            testTransform.map2(aList[ia], testSegment);  // testTransform treats pivot as origin
            xi=testSegment.x;
            yi=testSegment.y;
            r2 = xi*xi+yi*yi;
            if(Math.abs(r2)<1.0e-6){
               // the test point is right on the pivot...  r2==0 would cause
               // calamity in the math below.  fortunately, we can assume that
               // if the body was not in collision before we started the movement,
               // then the test point is not in collision...  and since it lies on
               // the pivot, it isn't going anywhere now.
               continue;
            }

            c = cPrime-r2;
            d = b*b-4*a*c;
            if(d<0){
               // the descriminant is <0 so the descriptive equation has no solution,
               // the circular trajectory will fall inside the wall segment.
               continue;
            }

            if(d==0){
               n=1;
               t[0]=-b/(2*a);
            }else{
               n=2;
               d=Math.sqrt(d);
               t[0]=(-b+d)/(2*a);
               t[1]=(-b-d)/(2*a);
            }
            while(n>0){
               n--;
               if(t[n]<0 || t[n]>1)
                  continue;   // t is out of range [0,1]
               xt = t[n]*s.v.x+ax;
               yt = t[n]*s.v.y+ay;
               theta = Math.atan2(xi*yt-yi*xt,  xi*xt+yi*yt);
               if(deltaAngle>0){
                  if(theta<0)theta+=2*Math.PI;
                  if(theta>deltaAngle)
                     continue;
               }else{
                  if(theta>0)theta-=2*Math.PI;
                  if(theta<deltaAngle)
                     continue;
               }
               deltaT = collisionDeltaT(timeTotal*theta/deltaAngle);
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


      //  Now perform the counter-motion test
      s=testSegment;
      for(ia=0; ia<aCount; ia++){
         testTransform.map2(aList[ia], s);  // testTransform treats pivot as origin
         a=s.v.x*s.v.x + s.v.y*s.v.y;
         ax=s.x;
         ay=s.y;
         b=2*(s.v.x*ax + s.v.y*ay);
         cPrime = ax*ax+ay*ay;
         for(ib=0; ib<bCount; ib++){
            xi=bList[ib].x-pivot.x;
            yi=bList[ib].y-pivot.y;
            r2 = xi*xi+yi*yi;
            if(Math.abs(r2)<1.0e-6){
               continue;
            }
            c = cPrime-r2;
            d = b*b-4*a*c;
            if(d<0){
               continue;
            }

            if(d==0){
               n=1;
               t[0]=-b/(2*a);
            }else{
               n=2;
               d=Math.sqrt(d);
               t[0]=(-b+d)/(2*a);
               t[1]=(-b-d)/(2*a);
            }
            while(n>0){
               n--;
               if(t[n]<0 || t[n]>1)
                  continue;   // t is out of range [0,1]
               xt = t[n]*s.v.x+ax;
               yt = t[n]*s.v.y+ay;
               theta = -Math.atan2(xi*yt-yi*xt,  xi*xt+yi*yt);
               if(deltaAngle>0){
                  if(theta<0)theta+=2*Math.PI;
                  if(theta>deltaAngle)
                     continue;
               }else{
                  if(theta>0)theta-=2*Math.PI;
                  if(theta<deltaAngle)
                     continue;
               }
               deltaT = collisionDeltaT(timeTotal*theta/deltaAngle);
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

      deltaAngle *= collisionTime/timeTotal;
      timeTotal=collisionTime;
      time1=time0+timeTotal;
   }

   @Override
public void stopMotionAtTime(double simTime){
      setTimeForward(simTime);
      // if there's any time remaining in motion, truncate it
      if(time0<= time && time<time1){
         deltaAngle *= (time-time0)/timeTotal;
         time1=time;
         timeTotal=time1-time0;
      }
   }
}


