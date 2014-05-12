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
import  rp1.rossum.event.RsEncoderStatusEvent;

import java.awt.Graphics;
import java.lang.Math;



/**
 * A class used to represent wheels, generally stored in
 * the collection elements of the RsWheelSystem.
 *
 * TO DO: The encoder implementation here is a stop-gap
 * until we get a full implementation. Eventually, the
 * encoder-related fields need to be moved to RsEncoder.
 *
 */

public class RsWheel extends RsBodyShape implements RsEncoder {
   /**
	 * 
	 */
	private static final long serialVersionUID = 864057705383374484L;
final double  x;
   final double  y;
   final double  wheelRadius;
   final double  maxAbsSteeringAngle;

   protected double motionOrientationOffset;

   // descriptive parameters for accumulator
   protected int    encoderIntervalCount;
   protected double encoderIntervalWidth;

   // state data for accumulator sensor and memory
   protected int    encoderAccumulator;
   protected int    encoderAbsoluteAccumulator;
   protected int    encoderQuadrature;
   protected double encoderAngle;     // current angle
   protected int    encoderInterval;  // current interval

   // state data for computational operations (from previous motion)
   protected double encoderMotionVelocity;
   protected double encoderMotionTime;


   public RsWheel(double point[], int nPoint, double x, double y, double radius)
   {
      super(point, nPoint);
      this.x           = x;
      this.y           = y;
      this.wheelRadius = radius;
      this.motionOrientationOffset = 0;
      this.maxAbsSteeringAngle     = 0;
   }

   public RsWheel(double point[], int nPoint, double x, double y, double radius, double maxAbsSteeringAngle)
   {
      super(point, nPoint);
      this.x           = x;
      this.y           = y;
      this.wheelRadius = radius;
      this.motionOrientationOffset = 0;
      this.maxAbsSteeringAngle     = maxAbsSteeringAngle;
      refBounds = computeRefBounds();
      bounds    = new RsRectangle(refBounds);
   }

   /**
    * Indicates whether a wheel is a candidate for steering or in a fixed position
    *
    */
   public boolean isWheelSteerable(){
      return maxAbsSteeringAngle>0.0;
   }

   public double getWheelRadius(){
      return wheelRadius;
   }

   public double getMaxAbsSteeringAngle(){
      return maxAbsSteeringAngle;
   }

   private boolean isAngleInSteeringRange(double theta){
      // put angle into range -PI/2 to PI/2 (it's usually there already)
      // here were use IEEEremainder because java.lang.math doesn't implement
      // an equivalent to the C/C++ modulus (grrr....)
      theta = Math.IEEEremainder(theta+4*Math.PI, 2*Math.PI)*(2.0*Math.PI);
      if(theta>Math.PI)
         theta = 2*Math.PI-theta;
      if(Math.abs(theta)>maxAbsSteeringAngle+1.0e-6)
         return false;
      return true;
   }

   protected RsRectangle computeRefBounds(){
      RsRectangle bnd = new RsRectangle(x, y, 0.0, 0.0);
      for(int i=0; i<refSegment.length; i++){
         bnd.insert(refSegment[i].x, refSegment[i].y);
         if(maxAbsSteeringAngle==0)
            continue;
         double dx, dy, r, phi;
         dx = refSegment[i].x - x;
         dy = refSegment[i].y - y;
         r = Math.sqrt(dx*dx+dy*dy);
         if(r<1.0e-6)
            continue;
         phi=Math.atan2(dx, dy);
         // x is based on cos, critial points at 0 and PI
         // y is based on sin, critical points at PI/2 and 3*PI/2
         if(isAngleInSteeringRange(0.0))
             bnd.insert(x+r, y);
         if(isAngleInSteeringRange(Math.PI))
             bnd.insert(x-r, y);
         if(isAngleInSteeringRange(Math.PI/2.0))
             bnd.insert(x, y+r);
         if(isAngleInSteeringRange(3.0*Math.PI/2.0))
             bnd.insert(x, y-r);
         bnd.insert(x+r*Math.cos(phi+maxAbsSteeringAngle), y+r*Math.sin(phi+maxAbsSteeringAngle));
         bnd.insert(x+r*Math.cos(phi-maxAbsSteeringAngle), y+r*Math.sin(phi-maxAbsSteeringAngle));
      }
      return bnd;
   }


   /**
    * Adds an encoder to the wheel.
    */
   public RsEncoder addEncoder(int nIntervals){
     if(nIntervals<=0){
        encoderIntervalCount = 0;
        return null;
     }else{
        encoderIntervalCount = nIntervals;
        encoderIntervalWidth = 2.0*Math.PI/nIntervals;
        resetEncoder();
     }
     return this;
   }

   /**
    * Resets all state data for the encoder.
    */
   public void resetEncoder(){
     encoderAccumulator         = 0;
     encoderAbsoluteAccumulator = 0;
     encoderQuadrature          = 0;
     encoderInterval            = 0;
     encoderAngle               = 0;
     encoderMotionVelocity      = 0;
     encoderMotionTime          = 0;
   }

   protected int getEncoderBin(double angle){
      if(encoderIntervalCount==0)
         return 0;
      return (int)Math.floor(angle/encoderIntervalWidth+0.5);
   }


   protected void applyMotion(RsMotion motion){
      if(wheelRadius>0 && Math.abs(encoderMotionVelocity)>0){
         double deltaT       = motion.time - encoderMotionTime;
         double angle        = encoderAngle+deltaT*encoderMotionVelocity/wheelRadius;
         int    i            = getEncoderBin(angle);
         int    deltaI       = i-encoderInterval;
         encoderAccumulator += deltaI;
         if(deltaI<0)
            encoderAbsoluteAccumulator -= deltaI;
         else
            encoderAbsoluteAccumulator += deltaI;

         if(encoderMotionVelocity<0)
            encoderQuadrature = -1;
         else
            encoderQuadrature =  1;

         if(angle<0){
            encoderAngle = 2*Math.PI- ((-angle)%(2*Math.PI) );
         }else{
            encoderAngle = angle%(2*Math.PI);
         }
         encoderInterval = getEncoderBin(encoderAngle);
      }
      RsMotionDepiction rmd = motion.getMotionDepictionAtBodyPoint(x,y);
      encoderMotionVelocity = rmd.vector.magnitude();
      if(rmd.reverseMotion)
         encoderMotionVelocity=-encoderMotionVelocity;
      encoderMotionTime     = motion.time;
   }

   protected RsTransform rotateAboutPivot(RsTransform gt, double px, double py, double theta){

      RsTransform w = new RsTransform();
      double sinTheta = Math.sin(theta);
      double cosTheta = Math.cos(theta);

      w.m11 =  cosTheta;
      w.m12 = -sinTheta;
      w.m13 = -px*cosTheta+py*sinTheta+px;

      w.m21 =  sinTheta;
      w.m22 =  cosTheta;
      w.m23 = -px*sinTheta-py*cosTheta+py;

      RsTransform t = new RsTransform();
      t.concat(gt, w);
      return t;

   }

   protected void setOrientation(double orientation){
         motionOrientationOffset = orientation;
   }

   @Override
public void resetStateData(){
     motionOrientationOffset = 0;
   }

   @Override
public void copyStateData(RsBodyPart dataSource){
      super.copyStateData(dataSource);
      motionOrientationOffset = ((RsWheel)dataSource).motionOrientationOffset;
   }

   @Override
public void paint(Graphics g, RsTransform gt){
      if(fillColor==null && lineColor==null)
         return;

      if(motionOrientationOffset==0){
         super.paint(g, gt);
      }else{
         RsTransform t = rotateAboutPivot(gt, x, y, motionOrientationOffset);
         super.paint(g, t);
      }
   }


   /**
    * Returns an encoder-status event giving current state data from wheel encoder.
    * You may supply zero values for simTime and requestIndex if you're not actually
    * using them.
    */
   public RsEncoderStatusEvent getEncoderStatusEvent(double simTime, int requestIndex, boolean clearOnReport){
      RsEncoderStatusEvent event = new RsEncoderStatusEvent(
          simTime,
          requestIndex,
          getID(),
          encoderAccumulator,
          encoderAbsoluteAccumulator,
          encoderQuadrature,
          encoderInterval,
          encoderAngle,
          clearOnReport);
          if(clearOnReport){
             encoderAccumulator         = 0;
             encoderAbsoluteAccumulator = 0;
          }
      return event;
   }


   /**
    * maps an accumulator value to a linear displacement (accounting
    * for the encoder resolution and wheel size).
    */
   public double mapAccumulatorToDisplacement(int accumulatorValue){
      return wheelRadius*encoderIntervalWidth*accumulatorValue;
   }

}
