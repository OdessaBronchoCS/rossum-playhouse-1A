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

import  rp1.rossum.event.RsSensorEvent;


/*

 TO DO:  Right now, I have a method getSensorEvent which always
         returns null.   This should become an abstract method.
         and all the sensor classes should implement methods which
         return their own sensor events.  Right now, not all the sensor
         classes are completed, and so we use the null return as
         a bit of scaffolding until we get the missing pieces completed.

*/


import java.awt.Color;



/**
 * The abstract base class for all sensor classes.
 *
 */

public abstract class RsBodySensor extends RsBodyShape {

   private static final long serialVersionUID = 1L;
   
   protected boolean stateChange;
   protected double  timeStateComputed;
	   
   public RsBodySensor(double []point, int nPoint){
      super(point, nPoint);
      hotFillColor = Color.orange;
      hotLineColor = Color.red;
   }

   @Override
public boolean isASensor(){
      return true;
   }

   public boolean  didStateChange(){
      return stateChange;
   }

   public double  getTimeOfLastStateComputation(){
      return timeStateComputed;
   }

   public abstract boolean computeAndSetState(double simTime, RsPlan plan, RsTransform transform);

   public RsSensorEvent  getSensorEvent(double simTime){
      return null;
   }



}

