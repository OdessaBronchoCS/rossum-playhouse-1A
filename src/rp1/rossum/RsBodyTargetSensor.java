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
import java.awt.Color;
import java.lang.Math;



/*
   No testing for parameter range restrictions and valid inputs
   has been implemented.

     width is total angular range of detection (in radians of course)
     width must be <  2 PI
     use width of zero for omni-directional sensor
     for an omni-directional sensor, nWidthBins is meaningless


   Some Names and Meanings of Variables
      (xDetector, yDetector)
         coordinates of standard reference (unmapped) position
         of sensor.  changes as body moves.

      mappedPos
         an RsPoint giving the mapped position of the sensor the last time
         the computeAndSetState() method was invoked

      (vx, vy)
         unit vector giving central axis of the sensor the last time the
         computeAndSetState() method was invoked

       hot
         boolean indicating if there was a detection the last time the
         computeAndSetState() method was invoked.

      (xTarget, yTarget)
         if a target was detected when the computerAndSetState method was
         invoked, these values will indicate its position RELATIVE to the
         central axis vector (vx, vy).  These values are defined if
         and only if there is a detection (hot==true).

      (bearing, range)
         polar coordinates, relative to central axis, giving position of
         the target point.   These values are defined if and only if there
         is a detection (hot==true)


      stateChange
         indicates that the last call to computeAndSetState resulted in
         a change of sensor status.


      Note:  xTarget, yTarget, bearing, and range are computed WHENEVER
             we have a detection.   Even though an omni-directional sensor
             cannot determine bearing, these values are provided for the
             benefit of the calling application (perhaps to provide data
             for a user interface).   Applications should use these fields
             as appropriate to the real-world sensor they are modeling.
*/



/**
 * A sensor for detecting "target" objects in the simulated floor plan.
 *
 */

public class RsBodyTargetSensor extends RsBodySensor {
	
   private static final long serialVersionUID = 1L;
   
   // elements which describe the sensor

   protected double    xDetector;
   protected double    yDetector;
   protected double    sightAngle;
   protected double    width;
   protected double    maxRange;
   protected int       nWidthBin;
   protected int       nRangeBin;

   // values computed in the constructor
   protected double    halfWidth;
   protected double    cosHalfWidth;

   // objects used as "scratch space" for performing various
   // calculations (established in the constructor)
   private   RsSegSect segSect;
   private   RsSegment segment;

   // elements which describe the current state or detection
   // note that boolean "hot" is defined in a super class
   // set by computeAndSetState() method.
   protected double    vx, vy;
   protected double    xTarget, yTarget;
   protected double    range,   bearing;
   protected RsPoint   mappedPos;
   protected int       rBin, wBin;
   protected RsTarget  targetDetected;


   public RsBodyTargetSensor(
      double []point,
      int nPoint,
      double _xDetector, double _yDetector,
      double _sightAngle,
      double _width,
      double _maxRange,
      int    _nWidthBin,
      int    _nRangeBin
   ){
      super(point, nPoint);
      name = "Unnamed Target Sensor";

      hot          = false;
      hotFillColor = Color.orange;
      hotLineColor = Color.red;

      width     = _width;
      maxRange  = _maxRange;
      nWidthBin = _nWidthBin;
      nRangeBin = _nRangeBin;

      xDetector    = _xDetector;
      yDetector    = _yDetector;
      sightAngle   = _sightAngle;
      halfWidth    = _width/2.0;
      cosHalfWidth = Math.cos(halfWidth);

      segSect      = new RsSegSect();
      segment      = new RsSegment();
   }

   @Override
public boolean computeAndSetState(double simTime, RsPlan plan, RsTransform transform){

      RsObject     [] objectArray;
      RsObject     o;
      RsObject     jo;
      RsTarget     test;
      RsTarget     target=null;
      RsWall       wall;
      double       minTestRange=1.0e+32;
      double       r, dx, dy, tX, tY, c;
      int          iObject, jObject, iSegment;

      boolean      oldState;
      int          oldrBin, oldwBin;
      RsTarget     oldTarget;



      oldState  = hot;
      oldrBin   = rBin;
      oldwBin   = wBin;
      oldTarget = targetDetected;

      timeStateComputed = simTime;
      stateChange = false;
      xTarget     = 0;
      yTarget     = 0;
      range       = 0;
      bearing     = 0;
      rBin        = 0;
      wBin        = 0;
      targetDetected = null;

      double       tAngle = transform.getTheta()+sightAngle;
      mappedPos = transform.map(xDetector, yDetector);
      vx = Math.cos(tAngle+sightAngle);
      vy = Math.sin(tAngle+sightAngle);


      objectArray = plan.getObjectArray();
      if(objectArray==null){
         hot = false;
         stateChange=(oldState!=hot);
         return stateChange;
      }

      // for each floor-plan object that's a target, determine if it falls
      // within the detection range and width of the sensor.  if
      // so, then perform logic to ensure that the line-of-sight
      // to the target is not blocked.   code is written to provide
      // an early exit where possible (to save processing)
      iObjectLoop:
      for(iObject = 0; iObject<objectArray.length; iObject++){
         o = objectArray[iObject];
         if(o instanceof RsTarget && o.getSelected()){
            test = (RsTarget)o;
            dx = test.x - mappedPos.x;
            dy = test.y - mappedPos.y;
            r  = Math.sqrt(dx*dx+dy*dy);
            if(r>maxRange || r>minTestRange)
               continue;
            if(r<1.0e-6){
               // at very close range, the sensor is essentially omni-directional
               // and width doesn't matter.   we also have no futher need
               // to test for occlusions with walls or to process
               // other target points.
               target=test;
               wBin=0;
               rBin=0;
               xTarget=0;
               yTarget=0;
               range=0;
               bearing=0;
               break;
            }else{
               // compute (tX, tY) coordinates with respect to sensor's central axis
               tX =  dx*vx + dy*vy;
               tY = -dx*vy + dy*vx;

               // object is only in sight if it is within the half-width restriction
               // recall that width==0 signifies an omni-directional sensor.
               c = tX/r;
               if(width>0){
                 if(c<cosHalfWidth)
                     continue;  // out of angle of view
               }

               segment.x = mappedPos.x;
               segment.y = mappedPos.y;
               segment.v.x=dx;
               segment.v.y=dy;
               segment.m=r;
               for(jObject=0; jObject<objectArray.length; jObject++){
                  jo = objectArray[jObject];
                  if(jo instanceof RsWall){
                     wall = (RsWall)jo;
                     for(iSegment=0; iSegment<wall.segmentArray.length; iSegment++){
                        if(segSect.process(segment, wall.segmentArray[iSegment]))
                           continue iObjectLoop;
                     }
                  }
               }
               xTarget = tX;
               yTarget = tY;
               range   = r;
               bearing = Math.acos(c);
               if(tY<0)
                  bearing = -bearing;
               if(width==0){
                  // omni-directional sensor
                  wBin=0;
               }else{
                  wBin=(int)Math.floor(nWidthBin*((bearing+halfWidth)/width));
                  if(wBin>=nWidthBin)
                     wBin=nWidthBin-1;
                  else if(wBin<0)
                     wBin=0;
               }

               target=test;
               minTestRange=r;
               rBin=(int)Math.floor(nRangeBin*r/maxRange);
               if(rBin>=nRangeBin)
                  rBin=nRangeBin-1;
            }
         }
      }


      hot = (target!=null);
      if(hot){
         stateChange = (!oldState || oldrBin!=rBin || oldwBin!=wBin || target!=oldTarget);
         targetDetected = target;
      }else{
         stateChange = oldState;
      }

      return stateChange;
   }


   @Override
public RsSensorEvent getSensorEvent(double simTime){

      String nameOfTargetDetected = null;
      if(targetDetected!=null)
         nameOfTargetDetected = targetDetected.getName();

      return new RsTargetSensorEvent(
       simTime,
       getID(),
       mappedPos.x,
       mappedPos.y,
       vx,
       vy,
       hot,
       xTarget,
       yTarget,
       range,
       bearing,
       nameOfTargetDetected);
   }


}

