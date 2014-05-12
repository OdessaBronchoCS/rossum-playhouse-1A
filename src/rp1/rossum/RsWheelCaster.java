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


/* The RsWheelCaster class is associated with the RsWheelSystem class.

      Note that many caster wheel arrangements are actually supported off-center
      from the wheel
                    ||
                ___ ||
               / . \|/
               \___/

      The wheel trails the pivot.   Specify this by supplying the geometry
      and the x/y coordinates of the pivot.

*/


import java.lang.Math;



/**
 * A class used to represent caster wheels, generally stored in
 * the collection elements of the RsWheelSystem.
 *
 */

public class RsWheelCaster extends RsWheel {

	private static final long serialVersionUID = 1L;

public RsWheelCaster(double point[], int nPoint, double x, double y)
   {
      // note that for wheelRadius in the constructor, we pass zero
      // up to the super method... right now it is effectively undefined.
      // this is a partial implementation and has the deficiency that it
      // will prevent us from adding an encoder to the wheel.
      super(point, nPoint, x, y, 0.0, 2*Math.PI);
      refBounds = computeRefBounds();
      bounds    = new RsRectangle(refBounds);
   }


   /**
    * Always returns false because in RP1 a caster wheel is not
    * "steerable" in the sense that its orientation can be set by
    * a steering mechanism. Instead, it just steers according to
    * the robot body motion at its position.
    */

   @Override
public boolean isWheelSteerable(){
      return false;
   }

   @Override
protected void applyMotion(RsMotion motion){
      RsMotionDepiction rmd = motion.getMotionDepictionAtBodyPoint(x,y);
      double  vMag          = rmd.vector.magnitude();
      if(vMag>1.0e-6){
          double absMotionAngle   = Math.atan2(rmd.vector.y, rmd.vector.x);
          setOrientation(absMotionAngle-motion.getOrientation());
      }
   }
}
