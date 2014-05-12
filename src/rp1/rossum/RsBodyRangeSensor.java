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




/*
   The present implementation does not test to see whether the contructor
   parameters are valid specifications.


   Some Names and Meanings of Variables
      (xDetector, yDetector)
         Coordinates of standard reference (unmapped) position
         of sensor relative to the robot's coordinate system.
         A mapped position (see mappedPos below) will be computed
         to reflect robot position and orientation.

      mappedPos
         an RsPoint giving the mapped position of the sensor the last time
         the computeAndSetState() method was invoked

      (vx, vy)
         unit vector giving central axis of the sensor the last time the
         computeAndSetState() method was invoked

       hot
         boolean indicating if there was a detection the last time the
         computeAndSetState() method was invoked.


       range
         range to the detection point, measured along the central axis.
         This value is defined if and only if there is a detection (hot==true)


      stateChange
         indicates that the last call to computeAndSetState resulted in
         a change of sensor status.


*/


package rp1.rossum;

import rp1.rossum.event.*;

import java.awt.Color;
import java.lang.Math;



/**
 * The sensor for measuring distance to objects.
 *
 */

public class RsBodyRangeSensor extends RsBodySensor {

   private static final long serialVersionUID = 1L;

   // elements which describe the sensor

   protected double    xDetector;
   protected double    yDetector;
   protected double    sightAngle;
   protected double    maxRange;
   protected int       nRangeBin;

   // objects used as "scratch space" for performing various
   // calculations (established in the constructor)
   private   RsSegSect segSect;
   private   RsSegment segment;

   // elements which describe the current state or detection
   // note that boolean "hot" is defined in a super class
   // set by computeAndSetState() method.
   protected double    vx, vy;
   protected double    range;
   protected RsPoint   mappedPos;
   protected int       rBin;
   protected RsObject  objectDetected;


   public RsBodyRangeSensor(
      double []point,
      int nPoint,
      double _xDetector, double _yDetector,
      double _sightAngle,
      double _maxRange,
      int    _nRangeBin
   ){
      super(point, nPoint);
      name = "Unnamed Range Sensor";

      hot          = false;
      hotFillColor = Color.orange;
      hotLineColor = Color.red;

      if(_maxRange>1.0e+6)
         maxRange=1.0e+6;
      else
         maxRange  = _maxRange;

      if(_nRangeBin<1)
         nRangeBin=1;
      else
         nRangeBin = _nRangeBin;

      xDetector    = _xDetector;
      yDetector    = _yDetector;
      sightAngle   = _sightAngle;

      segSect      = new RsSegSect();
      segment      = new RsSegment();

      rBin=-1;  // an impossible value
   }

   @Override
public boolean computeAndSetState(double simTime, RsPlan plan, RsTransform transform){

      RsObject     [] objectArray;
      RsObject     rsObject;
      RsWall       wall;

      int          iObject, iSegment;

      boolean      oldState;
      int          oldrBin;
      double       vx, vy, minT;
      RsObject     minObject;

      oldState = hot;
      oldrBin  = rBin;

      timeStateComputed = simTime;
      stateChange       = false;

      hot            = false;
      range          = 0;
      rBin           = 0;
      objectDetected = null;

      double       tAngle = transform.getTheta()+sightAngle;
      mappedPos = transform.map(xDetector, yDetector);
      vx = Math.cos(tAngle);
      vy = Math.sin(tAngle);

      objectArray = plan.getObjectArray();
      if(objectArray==null){
         hot = false;
         stateChange=oldState;  // if oldState was true there was a change
         return stateChange;
      }

      segment.x = mappedPos.x;
      segment.y = mappedPos.y;
      segment.v.x=vx*maxRange;
      segment.v.y=vy*maxRange;
      segment.m=maxRange;

      minT      = 2.0;   // max possible value should be one.
      minObject = null;
      for(iObject=0; iObject<objectArray.length; iObject++){
         rsObject = objectArray[iObject];
         if(rsObject instanceof RsWall){
            wall = (RsWall)rsObject;
            for(iSegment=0; iSegment<wall.segmentArray.length; iSegment++){
               if(segSect.process(segment, wall.segmentArray[iSegment])){
                  if(segSect.t1<minT && segSect.t1<=1.0){
                     minT      = segSect.t1;
                     minObject = rsObject;
                  }
               }
            }
         }
      }

      if(minT < 1.0){
         // a detection is within range
         objectDetected = minObject;
         hot = true;
         range=minT*maxRange;
         rBin=(int)Math.floor(nRangeBin*range/maxRange);
         if(rBin>=nRangeBin)
            rBin=nRangeBin-1;
      }

      if(hot){
         stateChange = (!oldState || oldrBin!=rBin);
      }else{
         stateChange = oldState;
      }

      return stateChange;
   }


   @Override
public RsSensorEvent getSensorEvent(double simTime){

      String nameOfObjectDetected = null;
      if(objectDetected != null)
         nameOfObjectDetected = objectDetected.getName();

      return new RsRangeSensorEvent(
       simTime,
       getID(),
       mappedPos.x,
       mappedPos.y,
       vx,
       vy,
       hot,
       range,
       nameOfObjectDetected);
    }

}

