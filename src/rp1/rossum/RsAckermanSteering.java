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



// TO DO:  Need to resolve the whole matter
// of the "default wheels" which didn't work out
// too well.  how do we make the program know
// how to add steering orientation to wheels?




package rp1.rossum;

import rp1.rossum.request.RsActuatorControlRequest;
import rp1.rossum.request.RsMotionRequest;


/**
 * An actuator class representing the Ackerman steering locomotion systems.
 * Ackerman steering is the systen used on virtually all automobile implementations.
 * This class presents a very high level of such a system, ignoring the mechanical
 * details of the steering apparatus and focussing on its general effects.
 *
 * <h3>Steering Geomtry</h3>
 * When the constructor for this class is called, it accepts a description
 * of the steering geometry (length of wheel base and width of wheel track)
 * as well as a specification telling the absolute maximum steering angle
 * of the inner wheel in a turn. These specifications are used to compute
 * the performance limits of the steering: the minimum turn radius and
 * the maximum steering angle for the system. When a turn is executed by an
 * Ackerman steering system, the inner wheel is turned slightly more into
 * the turn than the outer wheel. Thus the limits of the inner wheel define
 * the overall limits of the steering. This class computes a value called
 * the maximum absolute steering angle which describes the maximum positive
 * value for a "virtual steering wheel" located in the center of the
 * two steering wheels. This value will be slightly smaller in magnitude than
 * the associated steering angle of the inner wheel.
 *
 * When you specify a steering angle for the various methods provided by this
 * class, it will be treated as the "central steering angle", that is the
 * steering angle for a conceptual steering wheel located in the center of the
 * two actual wheels.
 *
 * <h3>Dual Steering</h3>
 * In general, the longer the wheel base relative to the track width, the
 * larger the minimum turn radius that can be performed by a simulated vehicle.
 * The specification dualSteering for the constructor allows an application
 * to specify a vehich that has a matched, dual set of steering systems.
 * This feature is sometimes found on upscale, long wheel-base trucks and SUV's
 * and on certain RC truck chassis that are suitable for use in small scale
 * robotics. In effect, setting dual steering to true divides the length of
 * the wheel base in half, permitting tighter turns.
 *
 * <h3>Steering and Coordinate Systems</h3>
 * If dualSteering is not specified, the origin of the robot coordinate
 * system is placed at the center of the rear axle. If dualSteering is
 * specified, the origin is placed at the midpoint between the
 * front and back sets of wheels.
 *
 *
 *  @since 0.60
 */

public class RsAckermanSteering extends RsWheelSystem {


   // inherited elements from RsWheelSystem
   // protected double trackWidth;   // width from wheel center to center
   // protected double wheelBase;    // length from wheel center to center
   // protected double driveWheelRadius;

   /**
	 * 
	 */
	private static final long serialVersionUID = 5098267698788746447L;
protected double  maxAbsSteeringAngle;
   protected double  minTurnRadius;
   protected boolean dualSteering;


   /* state data added by class */
   protected double  depictionSteeringAngle;
   protected boolean defaultWheelsAdded;

   /** Creates and initializes an object representing an Ackerman steering system..
    *  @param trackWidth the width of the wheel system, measured wheel center-to-center.
    *  @param wheelBase  the wheel base (length of wheel system), measured from wheel center-to-center.
    *  @param driveWheelRadius the radius of the drive wheels (used for converting rotational velocities).
    *  @param maxAbsInnerWheelSteeringAngle is the absolute value of the maximum angle the inner wheel may be turned.
    *  @param dualSteering indicates that the system has steering on both the front and rear axles.
    *
    *
    * TO DO: fix the bounds computation to include wheel definitions
    * and account for the swing of the steering wheels.
   */
   public RsAckermanSteering(
           double  trackWidth,
           double  wheelBase,
           double  driveWheelRadius,
           double  maxAbsInnerWheelSteeringAngle,
           boolean dualSteering){
      super();
      name = "Ackerman Steering";
      this.trackWidth       = trackWidth;
      this.wheelBase        = wheelBase;
      this.driveWheelRadius = driveWheelRadius;
      this.dualSteering     = dualSteering;


      // given the maxAbsSteeringAngle for the inner tire,
      // compute the corresponding minTurningRadius and
      // also the maxAbsCentralSteeringAngle
      maxAbsSteeringAngle=Math.abs(maxAbsInnerWheelSteeringAngle);
      if(maxAbsSteeringAngle > Math.toRadians(85.0))
         maxAbsSteeringAngle = Math.toRadians(85.0);

      double a, b;
      if(dualSteering)
         b = wheelBase/2.0;
      else
         b = wheelBase;
      a = b/Math.tan(maxAbsSteeringAngle);
      minTurnRadius = a+trackWidth/2.0;
      this.maxAbsSteeringAngle = Math.atan(b/minTurnRadius);


      defaultWheelsAdded = false;
      refBounds = new RsRectangle(
                          -wheelBase/2.0, -trackWidth/2.0,
                           wheelBase,      trackWidth);
   }


   /**
    * Returns minimum allowable turn radius based on steering geometry and
    * the maximum inner wheel steering angle specified in the constructor.
    */
   public double getMinTurnRadius(){
      return minTurnRadius;
   }

   /**
    * Returns the maximum absolute value of the central steering angle
    * for the actuator based on the steering geometry and maximum inner wheel
    * steering angle specified in the constructor.
    *
   */
   public double getMaxAbsSteeringAngle(){
     return maxAbsSteeringAngle;
   }


   /**
    * Adds default wheels of requested size with stub axle (supply values of zero
    * for stub axle to suppress its creation and just draw a simple wheel).
    * Note that the wheel will pivot at the center of its stub axle, so
    * you should keep it relatively short (the stub axle is meant to depict
    * the swing arm for the wheel, not the entire axle).
    * @param wheelRadius the tire radius of all four wheels (for depiction purposes)
    * @param wheelWidth  the tire width for all four wheels
    * @param stubLength length of a stub axle
    * @param stubWidth  width of a stub axle
    */
   public void addDefaultWheels(double wheelRadius, double wheelWidth, double stubLength, double stubWidth){
          // set up p as the template for all wheels
          double [] p  = new double[19];
          double    r  = wheelRadius;
          double    w  = wheelWidth;
          double    sl = stubLength;
          double    sw = stubWidth;
          int       n;  // number of template points

          defaultWheelsAdded = true;
          p[0] =  -r;
          p[1] =   w/2.0;

          p[2] =  -r;
          p[3] =  -w/2.;

          p[4] =   r;
          p[5] =  -w/2;

          p[6] =   r;
          p[7] =   w/2;

          if(sl<=0.0 || sw<=0.0){
             n = 4;
             p[8] = p[0];
             p[9] = p[1];
          }else{
             n = 8;
             p[8] =   sw/2;
             p[9] =   w/2;

             p[10] =  sw/2;
             p[11] =  w/2+sl;

             p[12] = -sw/2;
             p[13] =  w/2+sl;

             p[14] = -sw/2;
             p[15] =  w/2;
          }
          p[2*n]   = p[0];
          p[2*n+1] = p[1];
          n++;




          double xJoint, yJoint;
          double xAxle,  yWheel;
          double []d = new double[n*2];

          if(dualSteering)
              xAxle = -wheelBase/2.0;
          else
              xAxle = 0.0;

          for(int iAxle=0; iAxle<=1; iAxle++){
            yWheel = trackWidth/2;
            for(int iSide=-1; iSide<=1; iSide+=2){
              for(int i=0; i<n; i++){
                d[2*i]   = xAxle +p[2*i];
                d[2*i+1] = yWheel+iSide*p[2*i+1];
              }
              xJoint = xAxle;
              yJoint = yWheel+iSide*(w/2+sl/2);
              addWheel(new RsWheel(d, n, xJoint, yJoint, wheelRadius, maxAbsSteeringAngle));
              yWheel-=trackWidth;
            }
            xAxle+=wheelBase;
          }
   }


   private double limitSteeringAngle(double steeringAngle){

      if(steeringAngle<-maxAbsSteeringAngle)
         return -maxAbsSteeringAngle;
      else if(steeringAngle>maxAbsSteeringAngle)
         return maxAbsSteeringAngle;
      else
         return steeringAngle;
   }



   /**
    * Sets steering angle to be used for depiction
    *
    */
    public void setSteeringAngle(double steeringAngle){
      depictionSteeringAngle = limitSteeringAngle(steeringAngle);

      if(wheels==null)
         return;

      // pre-set all wheels to straight before making computation
      for(int i=0; i<wheels.length; i++)
         wheels[i].setOrientation(0.0);

      double a, b, tanSteering, radius, theta;
      tanSteering = Math.tan(steeringAngle);
      if(Math.abs(tanSteering)<1.0e-5){
        // steer straight
        return;
      }


      if(dualSteering)
         radius = (wheelBase/2)/tanSteering;
      else
         radius = wheelBase/tanSteering;

      for(int iWheel=0; iWheel<wheels.length; iWheel++){
         if(wheels[iWheel].isWheelSteerable()){
           b = wheels[iWheel].x;
           a = radius-wheels[iWheel].y;
           theta = Math.atan2(Math.abs(b), Math.abs(a));  // a should never == zero
           if(a<0)
             theta = -theta; // the pivot is to the right
           if(b<0)
             theta = -theta; // the rear set of a dual-steering system
           wheels[iWheel].setOrientation(theta);
         }
      }
   }



   public double getTurnRadiusForSteeringAngle(double steeringAngle){
      if(steeringAngle==0)
         return Double.MAX_VALUE;

      steeringAngle = limitSteeringAngle(steeringAngle);

      double b, tanSteering, radius;
      tanSteering = Math.tan(steeringAngle);
      if(dualSteering)
         b = wheelBase/2.0;
      else
         b = wheelBase;
      radius = b/tanSteering;
      return radius;
   }


   public double mapVelocityToWheelRotationalVelocity(double velocity){
      return velocity/driveWheelRadius;
   }

   public double mapVelocityToWheelRPM(double velocity){
      return 60.0*velocity/(2.0*Math.PI*driveWheelRadius);
   }

   public double mapWheelRPMToVelocity(double rpm){
       return 2.0*Math.PI*driveWheelRadius*rpm/60.0;
   }

   public RsMotionRequest getMotionRequestUsingVelocityAndSteeringAngle(
                              double velocity,
                              double steeringAngle,
                              double duration)
   {

      double linearVelocity, rotationalVelocity;

      steeringAngle = limitSteeringAngle(steeringAngle);

      linearVelocity     = velocity;
      rotationalVelocity = 0;

      if(Math.abs(steeringAngle)>1.0e-5){
        double radius      = getTurnRadiusForSteeringAngle(steeringAngle);
        rotationalVelocity = linearVelocity/radius;
      }

      return new RsMotionRequest(linearVelocity, rotationalVelocity, duration);
   }



   public RsMotionRequest getMotionRequestUsingWheelRotationalVelocityAndSteeringAngle(
                              double rotationalVelocity,
                              double steeringAngle,
                              double duration)
   {
      return getMotionRequestUsingVelocityAndSteeringAngle(
                            rotationalVelocity*driveWheelRadius,
                            steeringAngle,
                            duration);
   }



   public RsActuatorControlRequest getControlRequestForWheelRotationAndSteeringAngle(
                double driveRotationalVelocity, double steeringAngle)
   {

      // note that even if the driveRotationalVelocity is zero (a halt)
      // the request may still change the steering angle
      RsActuatorControlRequest req =
         new RsActuatorControlRequest(
               this.getID(),
               RsActuatorControlRequest.ACTIVATE );

      req.addParameter("driveRotationalVelocity",  driveRotationalVelocity);
      req.addParameter("steeringAngle",            steeringAngle);

      return req;
   }





   /** TO DO: Not implemented at this time. The Ackerman steering system
    *          cannot support all the range of movements that some wheel systems can.
    *          so we need to override the method defined in RsWheelSystem.
    */
   @Override
public RsMotionRequest getMotionRequest(double x, double y, double speed){
      return null;
   }



   /** The request for a pivot method always returns a null because the
    *  real-world Ackerman steering system does not support a pivot
    */

   @Override
public RsMotionRequest getMotionRequestForPivot(double x, double y, double speed){

      return null;
      // TO DO:  The Ackerman steering system cannot support a pivot.  Perhaps we could
      //         think about taking this method out of RsWheelSystem and moving
      //         making it to RsDifferentialSteering and making it undefined in
      //         RsAckermanSteering.
   }
}
