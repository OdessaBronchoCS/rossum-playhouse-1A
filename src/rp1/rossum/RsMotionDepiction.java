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



*/



/**
 * A class used for obtaining instantaneous velocity and other state
 * data for bodies in motion.
 * <p>
 * This class provides data useful in depicting a motion related
 * to one point on the robot body.  It is a transitional class that
 * will probably be replaced by something more general (at the very
 * least, it may be enhanced to include the Jacobian).  Anyway, it
 * gives us data for one point (x,y) at a simulation time and is
 * obtained by a RsMotion object.
 * <p>
 * Recall that in a circular motion, not all points on the robot
 * are moving in a single direction. Furthermore, not all points are
 * moving at a single velocity (though they all have the same rotational
 * velocity. Recall also that the robot may be moving forward or backward.
 * The reverseMotion flag indicates that the <i>query point</i> is moving
 * in a reverse (or null) motion with respective the the robot's orientation.
 * However, if a robot is rotatating, it is possible for a point on
 * one side of the robot body to have a forward motion while a point on
 * the opposite side has a reverse motion.  Note that in the even that a
 * point is stationary, the reverse motion flag will be set to false
 * (even though it has no forward motion).
 *
 * <p>
 * The following elements are populated
 * <ul>
 * <li>time      the simulation time at which the computations were made
 * <li>point     the point on the robot body
 * <li>vector    a scaled vector in direction of motion (magnitude is speed in m/s)
 * <li>rotationalVelocity  is given in +/- radians/sec.
 * <li>reverseMotion   indicates that motion is in a reverse direction from robot's perspective
 *
 *</ul>
 *
 *  @since 0.60
 */

public class RsMotionDepiction{

   final double       time;
   final RsPoint      point;
   final RsVector     vector;
   final double       rotationalVelocity;
   final boolean      reverseMotion;

   public RsMotionDepiction(
             double   time,
             RsPoint  point,
             RsVector vector,
             double   rotationalVelocity,
             boolean  reverseMotion)
   {
      this.time               = time;
      this.point              = point;
      this.vector             = vector;
      this.rotationalVelocity = rotationalVelocity;
      this.reverseMotion      = reverseMotion;
   }
}
