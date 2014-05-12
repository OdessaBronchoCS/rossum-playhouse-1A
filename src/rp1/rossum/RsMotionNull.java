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

/*

Note that RsMotionNull has a special constructor that allows the application
to ignore any previous movements and instantaneously set position and orientation.
As such, the new position of the object has nothing to do with the
old (it is as if it has magically teleported to a new place and time)
and, so, no previous movement is used as an input.

The continuityID is changed in that constructor.  Note, however, in the
alternate constructure (which is use when motion is simply halted),
it is not.

*/



/**
 * The class used for modeling stationary motions (stopped motions).
 *
 */

public class RsMotionNull extends RsMotion {

   private static final long serialVersionUID = 1L;
   
   public RsMotionNull(RsMotion start, double startTime){
       super(start, startTime, 0);
   }

   public RsMotionNull(double startTime, double theta, double xOffset, double yOffset){
      // we define the transform as "rotate by theta and then apply the offset"
      transform0         = new RsTransform(theta, xOffset, yOffset);
      time0              = startTime;
      time               = time0;
      time1              = time0;
      significantDeltaT  = 0;
      transform          = new RsTransform(transform0);
      continuityID       = RsMotion.getNewContinuityID();
      continuitySeriesID = 0;  // it's a new series, reset the ID counter
   }

   @Override
protected void applyTime(){};

   @Override
protected RsTransform getStateAtTime(double _time){
      if(_time<time0)
         return null;
      return transform0;
   }


   @Override
public RsMotionDepiction getMotionDepictionAtBodyPoint(double xBodyPoint, double yBodyPoint){
        return new RsMotionDepiction(
                        time,
                        transform.map(xBodyPoint, yBodyPoint),
                        new RsVector(0.0, 0.0),
                        0.0, false);
   }
}

