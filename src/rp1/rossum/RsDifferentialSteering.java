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

import rp1.rossum.request.RsActuatorControlRequest;
import rp1.rossum.request.RsMotionRequest;



/**
 * An actuator class representing differential steering locomotion systems.
 * The differential steering system is similar to that used for wheel chairs.
 * This class presents a very high level of such a system, ignoring the mechanical
 * details of the steering apparatus and focussing on its general effects.
 *
 * <h3>Steering and Coordinate Systems</h3>
 * Differential steering treats a pair of the robot drive wheels spaced
 * equal distances from the center of the robot. Thus the origin is placed
 * at the center of the drive wheel axle.  If desired, representations of
 * caster wheels (used in the real world to provide support for a robot
 * platform) may be added at arbitrary locations.
 *
 * @since 0.60
 */


public class RsDifferentialSteering extends RsWheelSystem {
	 
	private static final long serialVersionUID = 1L;

     /** Creates and initializes an object representing an differential steering system.
      *  @param trackWidth the width of the wheel system, measured wheel center-to-center
      *  @param driveWheelRadius the radius of the drive wheels (used for converting rotational velocities)
      *
      */

   public RsDifferentialSteering(double trackWidth, double driveWheelRadius){
      super();
      name = "Differential Steering";
      this.trackWidth       = trackWidth;
      this.driveWheelRadius = driveWheelRadius;
      refBounds = new RsRectangle(
                           -driveWheelRadius, -trackWidth/2.0,
                          2*driveWheelRadius,  trackWidth);
   }

   /** For depiction purposes, add a pair of wheels located on the ends of
    *  an axle at the center of the robot body. It is recommended, but not
    *  required, that the depicted wheel radius agree with the driveWheelRadius
    *  supplied in the constructor.  Note that at this time the wheels supplied
    *  in this implementation are non-interactive. That will change in future
    *  versions of this class.
    *
    *  @param wheelRadius radius of the depicted wheels
    *  @param wheelWidth width of the tire to be depicted
    */

   @Override
public void addDefaultWheels(double wheelRadius, double wheelWidth){

      double d[] = new double[10];
      double xCenter, yCenter;

      for(int iWheel=0; iWheel<2; iWheel++){
         RsWheel w;
         xCenter = 0;
         yCenter = (0.5-iWheel)*trackWidth;   // will be (0.5 and -0.5)*trackWidth

         d[0] = -wheelRadius  + xCenter;
         d[1] =  wheelWidth/2 + yCenter;

         d[2] = -wheelRadius  + xCenter;
         d[3] = -wheelWidth/2 + yCenter;

         d[4] =  wheelRadius  + xCenter;
         d[5] = -wheelWidth/2 + yCenter;

         d[6] =  wheelRadius  + xCenter;
         d[7] =  wheelWidth/2 + yCenter;

         d[8] = d[0];
         d[9] = d[1];

         w = new RsWheel(d, 5, xCenter, yCenter, wheelRadius);
         if(yCenter>0)
            w.setName("left wheel");
         else
            w.setName("right wheel");
         addWheel(w);  // sets wheel ID
      }
   }


   /**
    * Adds encoders to the drive wheels in the differential steering
    * apparatus. If successful, an array of two encoders is returned,
    * index 0 for the left wheel, index 1 for the right.
    *
    * Note that the encoders are installed so that they always
    * return a positive value for forward motion and a negative
    * value for reverse motion.
    *
    * @param nIntervals The number of intervals for the encoder
    */
   public RsEncoder [] addEncodersToDefaultWheels(int nIntervals){
      if(wheels==null)
         return null;

     RsEncoder [] temp = new RsEncoder[wheels.length];
     int n                 = 0;

      for(int i=0; i<wheels.length; i++){
         if(wheels[i].getName().equalsIgnoreCase("left wheel")){
            temp[n] = wheels[i].addEncoder(nIntervals);
            n++;
         }
         if(wheels[i].getName().equalsIgnoreCase("right wheel")){
            temp[n] = wheels[i].addEncoder(nIntervals);
            n++;
         }
      }

      if(n!=2)
         return null;

      RsEncoder [] e = new RsEncoder[2];
      e[0] = temp[0];
      e[1] = temp[1];
      return e;
   }



   public RsMotionRequest getMotionRequestUsingWheelVelocities(
                  double velocityLeft, double velocityRight, double duration){

      double linearVelocity = (velocityLeft+velocityRight)/2;
      double rotationalVelocity = (velocityRight - velocityLeft)/trackWidth;

      return new RsMotionRequest(linearVelocity, rotationalVelocity, duration);
   }


   public RsMotionRequest getMotionRequestUsingWheelRotationalVelocities(
                  double rotLeft, double rotRight, double duration){

      return getMotionRequestUsingWheelVelocities(
                      rotLeft*driveWheelRadius,
                      rotRight*driveWheelRadius,
                      duration);
   }

   public RsActuatorControlRequest getControlRequestForWheelRotation(
                double leftRotationalVelocity, double rightRotationalVelocity)
   {

      if(Math.abs(leftRotationalVelocity)==0 && Math.abs(rightRotationalVelocity)==0){
         return new RsActuatorControlRequest(this.getID(), RsActuatorControlRequest.HALT);
      }


      RsActuatorControlRequest req =
         new RsActuatorControlRequest(
               this.getID(),
               RsActuatorControlRequest.ACTIVATE );

      req.addParameter("leftRotationalVelocity",  leftRotationalVelocity);
      req.addParameter("rightRotationalVelocity", rightRotationalVelocity);

      return req;
   }




}

