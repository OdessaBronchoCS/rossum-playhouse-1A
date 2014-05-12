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

import rp1.rossum.request.RsMotionRequest;


/*

  RsWheelSystem extends RsBodyPart. I was thinking that it might eventually
  extend something like a "RsMovement" class (movement in the sense that
  a watch has a movement, not to be confused with RsMotion).
  There might be other kinds of movements than wheel actuators...
  such as a rotating turret or other potentially complicated devices.


  The value MAX_ALLOWABLE_TURN_RADIUS limits how large the turn radius
  is allowed to become (for example, if the calling application specifies
  an "almost straight line path").   At a certain point, the algorithms
  used for motion would suffer numeric failure.   We set the limit at
  1 kilometer turn radius.  Numerically, this is much smaller that what
  we COULD use if we wanted.   In terms of real-world modeling, it's
  quite large.


  trackWidth and wheelBase---  are zero when undefined. both trackWidth and
  wheelBase are defined in an Ackerman steering system, wheelBase will be zero
  in a differential steering system.  I'm not yet sure about an omni drive

*/

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;



/**
 * This is the abstract base class for all locomotor classes.  Eventually,
 * implementations are planned for the following classes
 *
 * <ul>
 * <li>RsDifferentialSteering - Wheelchair-type steering (implemented)
 * <li>RsAckermanSteering - Automobile-type steering (in progress)
 * <li>RsOmniDrive - Omni-directional drive (planned)
 * <li>RsSkidSteer - Tank-style drive (planned)
 * </ul>
 *
 *  A certain group of utility methods will be included with this group.
 * <ul>
 * <li>getMotionRequest - get motion request for a specific (x,y) goal position
 * <li>getMotionRequestForPivot - get motion request for a stop-and-pivot to face goal
 * </ul>
 *
 * Given the robot's current motion and a goal position (x,y) given in
 * the robot's frame of reference coordinate system, these utilities
 * provide a motion request to either get the robot to the x,y (getMotionRequest)
 * or pivot it to face the goal point (getMotionRequestForPivot).  Note
 * that while RsWheelSystem implements these methods, some of the derived
 * classes will override them.  For instance, an Ackerman (automobile)
 * drive can't perform a pivot. So it overrides getMotionRequestForPivot to
 * always return a null.
 * <p>
 * Note that in the current version of RP1, a wheel system is non-interactive.
 * This limitation is due to implementation problems to be resolved later. If the
 * wheels are contained within the general chassis of the robot, this is not
 * a problem.   On some designs, where the wheels are large compared
 * to the robot, it is common to place them in an "outboard" position.
 * In such a case, collision issues are relevant...  model these by
 * creating a body shape with the same "footprint" as the wheels.
 *
 */

public abstract class RsWheelSystem extends RsActuator {

   protected double trackWidth;   // width from wheel center to center
   protected double wheelBase;    // length from wheel center to center
   protected double driveWheelRadius;

   protected RsWheel          [] wheels;
   protected ArrayList <RsWheel> wheelList;

   protected RsPoint    center;

   static final double MAX_ALLOWABLE_TURN_RADIUS = 1000.0;

   public RsWheelSystem(){
       super();
       name = "Wheel System";

       wheelList = new ArrayList<RsWheel>();
       refBounds = null;
       bounds    = null;

       center    = new RsPoint();
   }

   public void addWheel(RsWheel w){
      wheelList.add(w);
      wheels = (RsWheel [])wheelList.toArray(new RsWheel[wheelList.size()]);
      computeBounds();
      bounds = new RsRectangle(refBounds);
   }

   public void addDefaultWheels(double wheelRadius, double wheelWidth){
      // this is a do-nothing to be overridden by derived classes
   }


   public double getTrackWidth(){
      return trackWidth;
   }

   public double getWheelBase(){
      return wheelBase;
   }

   public double getDriveWheelRadius(){
       return driveWheelRadius;
   }

   protected void computeBounds(){
      if(wheels==null)
         return;

      // for computing a wheel bounds, we implement quick fix that
      // assumes that all orientations are possible. eventually refine this.
      // for our 2-D top view, the wheel is a rectangle. we use the diagonal
      // of that rectangle assuming it's the maximum we need to deal with
      // no matter how the wheel turns.

      refBounds = new RsRectangle(0, 0, trackWidth, wheelBase);
      for(int i=0; i<wheels.length; i++){
         refBounds.union(wheels[i].refBounds);
      }
      bounds = new RsRectangle(refBounds);
   }


   /**
    * This method, which overrides that of the parent class, sets the fill color
    * for all wheel objects that were added to the wheel system prior to its invocation.
    */

   @Override
public void setFillColor(Color fillColor){
      if(wheels==null)
         return;
      this.fillColor = fillColor;
      for(int i=0; i<wheels.length; i++)
         wheels[i].setFillColor(fillColor);
   }

   /**
    * This method, which overrides that of the parent class, sets the line color
    * for all wheel objects that were added to the wheel system prior to its invocation.
    */
   @Override
public void setLineColor(Color lineColor){
      if(wheels==null)
         return;
      this.lineColor = lineColor;
      for(int i=0; i<wheels.length; i++)
         wheels[i].setLineColor(fillColor);
   }


   @Override
public void paint(Graphics g, RsTransform gt){

      if(wheels==null)
          return;
      for(int iWheel=0; iWheel<wheels.length; iWheel++)
         wheels[iWheel].paint(g, gt);
   }


   public void applyMotion(RsMotion motion){
     if(wheels==null)
        return;
     for(int i=0; i<wheels.length; i++)
        wheels[i].applyMotion(motion);
   }





   public RsMotion computeMotion(RsMotion start, double startTime, RsMotionRequest request){

      double durationSec = request.duration;


      if(request.duration<0.0001 ||
          (Math.abs(request.linearVelocity)<0.0001 &&
           Math.abs(request.rotationalVelocity)<0.0001)
      ){
         return new RsMotionNull(start, startTime);
      }

      double theta = start.transform.getTheta();  // current rotation


      if(Math.abs(request.rotationalVelocity)<0.1*Math.PI/180.0){
         // linear motion, straight forward or backward
         // recall that we define forward in the direction of the robot's x-axis.
         if(request.linearVelocity<0)
            theta+=Math.PI;
         RsVector vector = new RsVector(Math.cos(theta), Math.sin(theta));
         double displacement= Math.abs(durationSec*request.linearVelocity);
         displacement=Math.abs(displacement);
         return new RsMotionLine(start, startTime, request.duration, vector, displacement);
      }else{
         // circular motion
         double turnRadius = Math.abs(request.linearVelocity/request.rotationalVelocity);
         if(request.rotationalVelocity<0)
            turnRadius = -turnRadius;
         double delta      = durationSec*request.rotationalVelocity;
         if(request.linearVelocity<0)
             delta = -delta;
         RsPoint pivot = start.transform.getOffset();
         pivot.translate(-turnRadius*Math.sin(theta), turnRadius*Math.cos(theta));
         return new RsMotionCircle(start, startTime, request.duration, pivot, delta);
      }
   }






   /** Given a goal position defined in x/y coordinates in the robots frame of
    *  reference, getMotionRequest computes a motion request that will cause
    *  the robot to move from its current position to the goal. If the goal
    *  is not directly in front or behind the robot, the path will follow the
    *  arc of a circle tangent to the robot's current orientation and intersecting
    *  both its current position and the goal position. Note that if a particular
    *  locomotion cannot satisfy the requirements of the request (for example,
    *  the application requests too tight a turn for an automobile-style steering
    *  system), this method returns a null.
    *
    *  @param x x coordiante of goal in robot's frame of reference (meters)
    *  @param y y coordinate of goal in robot's frame of reference (meters)
    *  @param speed speed at which robot is requested to travel (meters/sec)
    */

   public RsMotionRequest getMotionRequest(double x, double y, double speed){

      double durationSeconds = 0;
      double linearVelocity  = 0;
      double rotationalVelocity = 0;
      double linearDistance  = 0;

      // by definition speed is always positive. But well take a precaution
      // and FORCE it to be positive in case someone mistakingly gave a neg value.
      speed = Math.abs(speed);
      if(speed<1.0e-6)
         return null;

      if(y==0){
         // either straight forward or straight back
         linearDistance  = Math.abs(x);
         linearVelocity  = speed;
         if(x<0)
            linearVelocity = -linearVelocity;
         rotationalVelocity = 0;
         durationSeconds = linearDistance/speed;
      }else{
         // curved path. we want an arc of circle that passes through
         // our current point (0, 0) and (x,y).  To determine the arc of
         // interest, we require that it be tangent to the robot's path
         // at both the initial point (0,0) and the terminal point (x,y).
         // We assume that distance |(0,0), (x,y)| is
         // the base of an equilateral triangle with the apex on the pivot point.
         // the interior angles at the base are just the ARCTAN(x/y) (note the x/y
         // reversal) we could have picked other constructions, but this one works fine.
         double s2, ax, ay, interiorAngle, alpha, turnRadius;
         s2 = x*x+y*y;
         if(s2<0.001)
            return null;
         ax = Math.abs(x);
         ay = Math.abs(y);
         turnRadius = s2/(2.0*ay);
         if(turnRadius>MAX_ALLOWABLE_TURN_RADIUS){
            return getMotionRequest(x, 0.0, speed);
         }
         interiorAngle  = Math.atan2(ax, ay);
         alpha          = Math.PI-2*interiorAngle; // total angle we will turn

         linearVelocity  = speed;
         linearDistance  = alpha*turnRadius;
         durationSeconds = linearDistance/speed;
         rotationalVelocity = alpha/durationSeconds;

         if(y>0){
            // we're turning left
            rotationalVelocity = alpha/durationSeconds;
         }else{
            // we're turning right
            rotationalVelocity = -alpha/durationSeconds;
         }

         if(x<0){
            // we're backing up
            linearVelocity = -speed;
         }
      }

      int durationMillis = (int)Math.floor(durationSeconds*1000.0);  // no round off
      if(durationMillis<1.0)
         return null;

      if(linearDistance<0.0001)
         return null;
      return new RsMotionRequest(linearVelocity, rotationalVelocity, durationSeconds);
   }


      /** Given a goal position defined in x/y coordinates in the robot's frame of
       *  reference, getMotionRequestForPivot computes a pivot maneuver that will cause
       *  the robot to face the goal. Note that not all locomotion systems support
       *  pivot maneuvers. For those that do not, this method always returns a null.
       *
       *  @param x x coordiante of goal in robot's frame of reference (meters)
       *  @param y y coordinate of goal in robot's frame of reference (meters)
       *  @param speed rotational speed at which robot is requested to pivot (radians/sec)
       */


   public RsMotionRequest getMotionRequestForPivot(double x, double y, double speed){
      double theta = Math.atan2(y, x);
      double rotationalVelocity = Math.PI/2;   // 90 degrees per second
      if(theta<0)
         rotationalVelocity = -rotationalVelocity;

      double durationSec    = theta/rotationalVelocity;
      int    durationMillis = (int)Math.floor(1000*durationSec);
      if(durationMillis<1.0)
         return null;


      return new RsMotionRequest(0.0, rotationalVelocity, durationSec);
   }


   @Override
public void resetStateData(){
      if(wheels!=null){
         for(int i=0; i<wheels.length; i++)
           wheels[i].resetStateData();
      }
   }

   @Override
public void copyStateData(RsBodyPart dataSource){
      super.copyStateData(dataSource);
      RsWheelSystem wSource = (RsWheelSystem)dataSource;
      if(wheels!=null){
         for(int i=0; i<wheels.length; i++)
           wheels[i].copyStateData(wSource.wheels[i]);
      }
   }


   /**
    * Provides a convenience function for adding a default caster wheel to
    * the system. There is nothing special about the design of this caster wheel,
    * and this method is included simply as an example.
    */
   public void addDefaultCasterWheel(double x, double y, double wheelRadius, double wheelWidth){
      double d[] = new double[18];

      double   r = wheelRadius;
      double   t = wheelWidth;

      d[0] =  x-2*r;
      d[1] =  y+t/2.0;

      d[2] =  x-2*r;
      d[3] =  y-t/2.0;

      d[4] =  x-t/2.0;
      d[5] =  y-t/2.0;

      d[6] =  x-t/2.0;
      d[7] =  y-t;

      d[8] =  x+t/2.0;
      d[9] =  y-t;

      d[10] = x+t/2.0;
      d[11] = y+t;

      d[12] = x-t/2.0;
      d[13] = y+t;

      d[14] = x-t/2.0;
      d[15] = y+t/2;

      d[16] = d[0];
      d[17] = d[1];

      addWheel(new RsWheelCaster(d, 9, x, y));

   }


   public RsWheel getWheelForID(int wheelID){
     if(wheels!=null){
        for(int i=0; i<wheels.length; i++)
          if(wheels[i].getID()==wheelID)
             return wheels[i];
     }
     return null;
   }


   public RsWheel [] getWheels(){
     return wheels;
   }

}

