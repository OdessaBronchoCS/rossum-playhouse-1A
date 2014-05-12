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

/*  RsBodyCircle

RsBodyCircle is a special case of RsBodyShape.

A circular body plan is a very common feature in small robot
designs.   One way to represent a circular body plan in simulation is
to simply implement a polygon with a lot of edges.   But a
circle has certain nice features that make it worth doing little extra work.
For example, for depiction purposes, we will usually get a nicer picture
if we let java.awt draw a circle rather than a polygon.   Also,
any orientation change induced by a motion transformation can be
ignored...  while the circle can be moved, its shape remains invarient
under a rotation transformation.

For this implementation, I do define a shape polygon for tracking
collisions and so forth.   I HAVE worked out the math for treating the
problem of a circle colliding with the floor plan elements directly,
but I haven't implemented BodyCircle that way.   Instead, I still use
a polygon just as for any other BodyShape.  Although the direct
treatement would speed up the modeling a bit, it requires more code
to implement and may complicate some of the class/method design issues.
On the other hand, reducing a circle to a 36-edge polygon means that we may
make errors in our collision modeling.

I will have to revisit this problem later.
*/


import java.lang.Math;
import java.awt.Color;
import java.awt.Graphics;



/**
 * A convenience class for creating circular body parts.
 *
 */

public class RsBodyCircle extends RsBodyShape {
	
   private static final long serialVersionUID = 1L;
   
   public RsBodyCircle(double xCenter, double yCenter, double radius){

      super();

      name = "Unnamed Circle";

      refXCenter = xCenter;
      refYCenter = yCenter;
      refRadius  = radius;
      refBounds  = new RsRectangle(xCenter-radius, yCenter-radius, 2*radius, 2*radius);

      refSegment = new RsSegment[36];

      double x0 = xCenter+radius;
      double y0 = yCenter;
      double x1;
      double y1;
      double a;
      for(int i=0; i< 36; i++){
          a = ((i+1.0)/36.0)*2.0*Math.PI;
          x1 = xCenter+radius*Math.cos(a);
          y1 = yCenter+radius*Math.sin(a);
          refSegment[i] = new RsSegment(x0, y0, x1, y1);
          x0 = x1;
          y0 = y1;
      }
   }


   @Override
public void paint(Graphics g, RsTransform gt){


      Color  fc, lc;

      fc = getFillColor();
      lc = getLineColor();

      if(fc == null && lc == null)
         return;

      RsPoint p0 = gt.map(refXCenter, refYCenter);
      RsPoint p1 = gt.map(refXCenter+refRadius, refYCenter);
      // because the gt adds a rotation, p1 will not necessarily be
      // at any particular orientation.  to get radius, compute distance |P0-P1|
      double x = p0.x-p1.x;
      double y = p0.y-p1.y;
      double r = Math.sqrt(x*x+y*y);
      int    ix = (int)(p0.x-r+0.5);
      int    iy = (int)(p0.y-r+0.5);
      int    ir = (int)(2*r+0.5);
      if(fc!=null){
         g.setColor(fc);
         g.fillOval(ix, iy, ir, ir);
      }
      if(lc!=null){
         g.setColor(lc);
         g.drawOval(ix, iy, ir, ir);
      }
   }

   // elements used for modeling
   protected double        refXCenter;
   protected double        refYCenter;
   protected double        refRadius;


}

