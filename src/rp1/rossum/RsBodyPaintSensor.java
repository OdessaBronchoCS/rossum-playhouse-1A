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


      hot
         boolean indicating if there was a detection the last time the
         computeAndSetState() method was invoked.


      stateChange
         indicates that the last call to computeAndSetState resulted in
         a change of sensor status.


*/


package rp1.rossum;

import rp1.rossum.event.*;

import java.awt.Color;


/**
 * A sensor for detecting paint features in the simulated floor plan.
 *
 */

public class RsBodyPaintSensor extends RsBodySensor {
   private static final long serialVersionUID = 1L;
   
   public RsBodyPaintSensor(
      double []point,
      int      nPoint,
      double  _xDetector,
      double  _yDetector
   ){
      super(point, nPoint);
      name = "Unnamed Paint Sensor";

      hot          = false;
      hotFillColor = Color.orange;
      hotLineColor = Color.red;

      xDetector    = _xDetector;
      yDetector    = _yDetector;

   }

   @Override
public boolean computeAndSetState(double simTime, RsPlan plan, RsTransform transform){

      RsObject  [] objectArray;
      RsObject     rsObject;
      RsPaint      rsPaint;

      int          iObject, iRegion;

      boolean      oldState;
      int          oldRegion;

      oldState = hot;
      oldRegion    = region;

      timeStateComputed = simTime;
      stateChange       = false;



      mappedPos = transform.map(xDetector, yDetector);

      objectArray = plan.getObjectArray();
      if(objectArray==null){
         hot  = false;

         stateChange=oldState;  // if oldState was true there was a change
         return stateChange;
      }



      // we loop through all the RsPaint objects to determine
      // whether the sensor lies above one of them.   Note that
      // since it is possible to paint one object on top of another,
      // we could get a detection for either of two overlapping objects.
      // Since the last one painted is the one that is visible, it
      // gets priority...  which is why we step through the object
      // array backwards.
      hot    = false;
      region = 0;
      for(iObject=objectArray.length-1; iObject>=0; iObject--){
         rsObject = objectArray[iObject];
         if(rsObject instanceof RsPaint){
            rsPaint = (RsPaint)rsObject;
            if(rsPaint.polygon.checkContainment(mappedPos.x, mappedPos.y, 0.0)<0){
               region = rsPaint.region;
               if(regionSensitivity==null){
                  // since no particular sensitivity was specified,
                  // it is essentially "omni-sensitive"
                  hot = true;
               }else{
                  hot=false;
                  for(iRegion=0; iRegion<regionSensitivity.length; iRegion++){
                     if(regionSensitivity[iRegion]==region){
                        hot=true;
                        break;
                     }
                  }
               }
               break;
            }
         }
      }

      if(hot){
         stateChange = (!oldState || region!=oldRegion);
      }else{
         stateChange = (oldState  || region!=oldRegion);
      }

      return stateChange;
   }


   @Override
public RsSensorEvent getSensorEvent(double simTime){
      return new RsPaintSensorEvent(
       simTime,
       getID(),
       mappedPos.x,
       mappedPos.y,
       hot,
       region);
    }

    public void setRegionSensitivity(int region){
       regionSensitivity = new int[1];
       regionSensitivity[0]=region;
    }

    public void setRegionSensitivity(int []region){
       regionSensitivity = new int[region.length];
       for(int i=0; i<region.length; i++)
          regionSensitivity[i]=region[i];
    }


   // The following elements describe the sensor and should not
   // change once it's constructed (of course, I am not quite
   // so confident of that claim as to be willing to declare
   // them as "final")


   protected double    xDetector;
   protected double    yDetector;


   // The following elements describe the dynamic state of the
   // sensor and change each time computeAndSetState is called.
   // Note that booleans "hot" and "stateChange" are defined
   // in the super class.

   protected RsPoint   mappedPos;
   protected int       region;
   protected int     [] regionSensitivity;
}
