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

The RangeSensorEvent is generated when the status of the sensor changes.
This may include going from a hot-to-cold (detection to no-detection) or
visa versa.   It may also include a change in the rangeBin value.

The rangeBin value reflects the digital treatment of the real-valued "range"
value.  It is computed based on the resolution of the sensor in the
sensor specification.

The RangeSensorEvent includes a couple of fields which would not
be available from a real sensor:  absolute position (x, y) of the sensor
and a unit vector (ux, uy) giving the line-of-sight direction.  These are
for diagnostic or human-interface purposes.  Using these in a client's
navigation logic is cheating.

*/



/**
 * An event generated by the simulator when a range sensor undergoes
 *a change in state.
 *
 */

public class RsRangeSensorEvent extends RsSensorEvent{

   /**
	 * 
	 */
	private static final long serialVersionUID = 3496114482827806382L;
public final double  x, y;
   public final double  ux, uy;
   public final boolean status;
   public final double  range;   // valid only in the case of a detection, undefined otherwise


   // TO DO: deprecate this constructor
   public RsRangeSensorEvent(
      double  simTime,
      int     sensorID,
      double  x,
      double  y,
      double  ux,
      double  uy,
      boolean status,
      double  range)
  {
         super(EVT_RANGE_SENSOR, simTime, sensorID);
         this.x         = x;
         this.y         = y;
         this.ux        = ux;
         this.uy        = uy;
         this.status    = status;
         this.range     = range;
  }


   public RsRangeSensorEvent(
      double  simTime,
      int     sensorID,
      double  x,
      double  y,
      double  ux,
      double  uy,
      boolean status,
      double  range,
      String  nameOfObjectDetected)
  {
         super(EVT_RANGE_SENSOR, simTime, sensorID);
         this.x                    = x;
         this.y                    = y;
         this.ux                   = ux;
         this.uy                   = uy;
         this.status               = status;
         this.range                = range;
         this.nameOfObjectDetected = nameOfObjectDetected;
  }

}

