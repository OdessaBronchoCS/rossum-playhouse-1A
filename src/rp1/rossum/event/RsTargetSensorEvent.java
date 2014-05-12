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


package rp1.rossum.event;

/*

RsTargetSensorEvent.java

The target sensor is used to detect the presense of a target point
in the Rs simulator.  It does not correspond to any real-world sensor
but provides data that can be used to model the behavior of an actual
device.

Typically, an RsTargetSensor has a footprint, a detection occurs when
the center point of an RsTarget falls into the footprint.
The TargetSensorEvent is generated when the detection status of the sensor
changes. This may include going from a hot-to-cold (detection to no-detection) or
visa versa.   The coordinate pair (xDetection, yDetection) give the position,
relative to the sensor, of the target point.


The TargetSensorEvent includes a couple of fields which would not
be available from a real sensor:  absolute position (x, y) of the sensor and a
unit vector (ux, uy) giving the line-of-sight direction.  These are
for diagnostic or human-interface purposes.  Using these in a client's navigation
logic is cheating.

*/



/**
 * An event issued when a target sensor undergoes a state change.
 *
 */

public class RsTargetSensorEvent extends RsSensorEvent{


   /**
	 * 
	 */
	private static final long serialVersionUID = -2717659539694960337L;
public final double  x, y;
   public final double  ux, uy;
   public final boolean status;
   public final double  xDetection, yDetection;
   public final double  range, bearing;

   // TO DO: retire this constructor
   public RsTargetSensorEvent(
      double   simTime,
      int     sensorID,
      double  x,
      double  y,
      double  ux,
      double  uy,
      boolean status,
      double  xDetection,
      double  yDetection,
      double  range,
      double  bearing)
  {
         super(EVT_TARGET_SENSOR, simTime, sensorID);
         this.x          = x;
         this.y          = y;
         this.ux         = ux;
         this.uy         = uy;
         this.status     = status;
         this.xDetection = xDetection;
         this.yDetection = yDetection;
         this.range      = range;
         this. bearing   = bearing;
         this.nameOfObjectDetected = null;
   }

   public RsTargetSensorEvent(
      double   simTime,
      int     sensorID,
      double  x,
      double  y,
      double  ux,
      double  uy,
      boolean status,
      double  xDetection,
      double  yDetection,
      double  range,
      double  bearing,
      String  nameOfObjectDetected)
  {
         super(EVT_TARGET_SENSOR, simTime, sensorID);
         this.x          = x;
         this.y          = y;
         this.ux         = ux;
         this.uy         = uy;
         this.status     = status;
         this.xDetection = xDetection;
         this.yDetection = yDetection;
         this.range      = range;
         this. bearing   = bearing;
         this.nameOfObjectDetected = nameOfObjectDetected;
   }



}

