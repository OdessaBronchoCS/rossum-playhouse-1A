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


package rp1.rossum.request;

 /*
   The parameters in the RsMotionRequest are used in conjunction with those
   in the RsWheelSystem to compute a path for the simulated robot.

      linearVelocity:    velocity forward (+) or backward (-) in meters/sec
      rotationVelocity:  turn velocity left is +, right is - in meters.sec
      duration:          duration in seconds
 */




/**
 * A request for a motion; unlike many other request classes,
 *this one is often instantiated directly in client applications.
 *
 */

public class RsMotionRequest extends RsRequest {

   /**
	 * 
	 */
	private static final long serialVersionUID = 5365260271545676026L;
public RsMotionRequest(double linearVelocity, double rotationalVelocity, double duration){
      super(REQ_MOTION);
      this.linearVelocity     = linearVelocity;
      this.rotationalVelocity = rotationalVelocity;
      this.duration           = duration;

   }

   public final double  duration;
   public final double  linearVelocity;
   public final double  rotationalVelocity;
}

