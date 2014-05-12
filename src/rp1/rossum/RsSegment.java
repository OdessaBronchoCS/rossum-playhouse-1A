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
   RsSegment.java    provides general data for a line segment

*/

import java.io.Serializable;



/**
 * A representation of a fixed-length line segment; this class is used
 *by the server for analysis.
 *
 */

public class RsSegment extends RsPoint implements Serializable{

   /**
	 * 
	 */
	private static final long serialVersionUID = -4560850925153885632L;
public RsSegment(){
      x=0;
      y=0;
      v = new RsVector(0., 0.);
      m = 0;
   }

   public RsSegment(RsPoint a, RsPoint b){
      x=a.x;
      y=a.y;
      v = new RsVector(b.x-a.x, b.y-a.y);
      m = v.magnitude();
   }

   public RsSegment(double x0, double y0, double x1, double y1){
      x=x0;
      y=y0;
      v = new RsVector(x1-x0, y1-y0);
      m = v.magnitude();
   }

   public RsSegment(RsSegment s){
      x=s.x;
      y=s.y;
      v=s.v;
      m=s.m;
   }

   public RsVector v;
   public double m;         // magnitude of vector  m =|V|
}


