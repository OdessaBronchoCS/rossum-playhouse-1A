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

   RsRectangle.java

   This class bears an obvious similarity to java.awt.Rectangle.
   One important difference is that it is based on a Cartesian coordinate
   system, with (x,y) being the lower-left corner of the rectangular
   region (not the upper-left as in the awt).  Also, because this class is
   intended for modeling virtual physical entities rather than for graphics
   applications, all values are in a floating-point representation.

   In Rossum, a rectangle is not so much a physical entity as an identification
   of a region of space.  And in the current implementation, a zero-sized region
   is perfectly reasonable.Rectangles with zero width and/or zero height can exist.
   For example, consider the case where we create a bounding rectangle for a horizontal
   line.  Either the height or, respectively, the width will be zero.  Or consider
   the bounding rectangle for a single point. Care must be taken to verify these
   values before using them in calculations (particularly divisions).

   TO DO: Upon further consideration, this class sorely
   needs to have an element defining whether the rectangle is
   "empty" or not.  There is no way to distinquish from a rectangle that
   has never been specified versus one that is truly zero height/width.

   TO DO:  It might be interesting to extend this as a container class
   of sorts.  It could hold references to multiple objects and tell you
   the overall extent for it.  This feature might be useful when we start adding
   a lot more complexity for floor plans.

Constructors
   public RsRectangle(RsRectangle r);
   public RsRectangle(double _x, double _y, double _width, double _height);
   public RsRectangle(RsPoint [] points, int nPoints);

Public Instance Variables

   public double x;
   public double y;
   public double width;
   public double height;

Methods

   public void setBounds(double _x, double _y, double _width, double _height);
   public void setSize(double _width, double _height);
   public void union(RsRectangle r);
   public void insert(RsPoint p);
   public void insert(double _x, double _y);
   public void move(double _x, double _y);
   public void translate(double _x, double _y);
   public void translate(RsVector v);
   public void copy(RsRectangle r);
   public void clip(double x0, double y0, double x1, double y1);

   public boolean contains(RsPoint p);
   public boolean contains(double _x, double _y);
   public boolean intersects(RsRectangle r);

   public void transform(RsTransform t);

Development Notes:

   Eventually, I might like to include more methods to make this class
   symetrical with java.awt.Rectangle.  One the other hand, I want
   to avoid including too much unnecessary code in this package.

*/


import java.io.Serializable;



/**
 * A class used to represent rectangular regions using floating-point
 *values, RsRectangle is used for analysis and graphics.
 *
 */

public class RsRectangle implements Serializable {

   /**
	 * 
	 */
	private static final long serialVersionUID = -7111894810986643706L;
public double x;
   public double y;
   public double width;
   public double height;
   private boolean populated;

   public RsRectangle(){populated=false;}

   public RsRectangle(RsRectangle r){
      populated = true;
      x        = r.x;
      y        = r.y;
      width    = r.width;
      height   = r.height;
   }

   public RsRectangle(double x, double y, double width, double height){
      populated   = true;
      this.x      = x;
      this.y      = y;
      this.width  = width;
      this.height = height;

      // the following handles a case where we're handed an inverted
      // rectangle...  TO DO: figure out why the original author felt
      // such a thing was necessary.
      if(width<0){
         this.x     += width;
         this.width = -width;
      }
      if(height<0){
         this.y      += height;
         this.height = -height;
      }
   }

   public RsRectangle(RsPoint [] point, int nPoint){
      populated = true;
      width=0;
      height=0;
      if(nPoint<=0){
         x=0;
         y=0;
      }else{
         x=point[0].x;
         y=point[0].y;
         for(int i=1; i<nPoint; i++){
            insert(point[i]);
         }
      }
   }

   public void insert(RsPoint point){
      insert(point.x, point.y);
   }

   public void insert(double px, double py){
      if(!populated){
         setBounds(px, py, 0, 0);
         return;
      }
      double delta;
      delta=px-x;
      if(delta>width){
         width=delta;
      }else if(delta<0){
         width-=delta;
         x=px;
      }
      delta=py-y;
      if(delta>height){
         height=delta;
      }else if(delta<0){
         height-=delta;
         y=py;
      }
  }


   public void setBounds(double _x, double _y, double _width, double _height){
      populated = true;
      x         = _x;
      y         = _y;
      width     = _width;
      height    = _height;

      if(_width<0){
         x+=width;
         width = -width;
      }
      if(_height<0){
         y+=height;
         height = -height;
      }
   }

   public void setSize(double _width, double _height){
      populated = true;
      width     = _width;
      height    = _height;
   }


   public void move(double _x, double _y){
      populated = true;  // TO DO:  does move truly populate a rectangle
      x=_x;
      y=_y;
   }

   public void translate(double _x, double _y){
      populated = true;
      x+=_x;
      y+=_y;
   }

   public void translate(RsVector v){
      populated = true;
      x+=v.x;
      y+=v.y;
   }

   public void union(RsRectangle r){
      double p1, r1;

      if(!populated){
         copy(r);
         return;
      }

      p1=x+width;
      r1=r.x+r.width;
      if(r.x<x)
         x=r.x;
      if(r1>p1)
         p1=r1;
      width=p1-x;

      p1=y+height;
      r1=r.y+r.height;
      if(r.y<y)
         y=r.y;
      if(r1>p1)
         p1=r1;
      height=p1-y;
   }

   public void copy(RsRectangle r){
      populated = true;
      x         = r.x;
      y         = r.y;
      width     = r.width;
      height    = r.height;
   }

   public void clip(double x0, double y0, double x1, double y1){
      if(!populated)
          return;
      double rx1 = x+width;
      double ry1 = y+height;
      if(x>=x1 || y>=y1 || rx1<=x0 || ry1<=y0){
         // it's entirely clipped, null it out
         width     = 0;
         height    = 0;
         x         = 0;
         y         = 0;
         populated = false;
      }else{
         if(x<x0)
            x=x0;
         if(y<y0)
            y=y0;
         if(rx1>x1)
            rx1=x1;
         if(ry1>y1)
            ry1=y1;
         width=rx1-x;
         height=ry1-y;
         if(width<=0 || height<=0){
            width=0;
            height=0;
            x=0;
            y=0;
         }
      }
   }


   public boolean contains(RsPoint point){
      return contains(point.x, point.y);
   }

   public boolean contains(double px, double py){
      if(!populated)
         return false;
      return (px>=x && px<=x+width && py>=y && py<=y+height);
   }

   public boolean intersects(RsRectangle r){
      if(!populated || !r.populated)
         return false;
      return !(r.x+r.width<x || x+width<r.x || r.y+r.height<y || y+height<r.y);
   }


   public void transform(RsTransform t){
      // because the transform can rotate the rectangle, we don't
      // know which corners are going to be extrema, so we
      // need to test all four.

      if(!populated)
          return ;

      double tx, ty;
      double oldX      = x;
      double oldY      = y;
      double oldWidth  = width;
      double oldHeight = height;

      x      = oldX*t.m11 + oldY*t.m12 + t.m13;
      y      = oldX*t.m21 + oldY*t.m22 + t.m23;
      width  = 0;
      height = 0;

      tx     = (oldX+oldWidth)*t.m11 + oldY*t.m12 + t.m13;
      ty     = (oldX+oldWidth)*t.m21 + oldY*t.m22 + t.m23;
      insert(tx, ty);

      tx     = (oldX+oldWidth)*t.m11 + (oldY+oldHeight)*t.m12 + t.m13;
      ty     = (oldX+oldWidth)*t.m21 + (oldY+oldHeight)*t.m22 + t.m23;
      insert(tx, ty);

      tx     = oldX*t.m11 + (oldY+oldHeight)*t.m12 + t.m13;
      ty     = oldX*t.m21 + (oldY+oldHeight)*t.m22 + t.m23;
      insert(tx, ty);
    }

    public boolean isPopulated(){
       return populated;
    }
}


