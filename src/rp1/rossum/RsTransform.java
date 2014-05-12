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

   RsTransform.java     position and orientation transforms

   Notes:


      In the Rs system, all objects are rigid.   Scaling is never performed.
      Since the matrix is only used for rotations and rigid movements,
      the bottom-most row will always be (0, 0, 1).   Thus we can save
      some operations when concatenating transforms since we never have
      to compute this row (we also know what multiplies are going to involve
      zero and one, and can act accordingly).

      Note that rows and columns are numbered 1..3 in difference to the mathematician's
      conventions rather than the computer programmer's 0..2 scheme.


      Note on iMapArray ----------------------------------

      iMapArray is intended specifically for mapping points to integer coordinates
      for graphics purposes, and will become unnecessary when we adopt the
      Java 2D API.


*/


import java.lang.Math;
import java.io.Serializable;



/**
 * A class for representing matrix-based transformation of object
 * position and orientation, used for both modeling and graphics.
 * The transformation array is implemented using a homegeneous matrix
 * with the expectation that coordinates will be implemented in a compatible
 * form.   Transform matrices are always prefixed to coordinates matrices:
 * <p>
 * A simple rotation and offset would be expressed as shown:
 * <code>
 *  where theta is rotation angle
 *
 *     T(theta, offsets)*X=X'
 *
 *     |  cos(theta)   -sin(theta)    xOffset  |  |x|      |x'|
 *     |  sin(theta)    cos(theta)    yOffset  |  |y|  =   |y'|
 *     |  0             0             1        |  |1|      |1 |
 *
 * </code>
 *
 */

public class RsTransform implements Serializable {

   /**
	 * 
	 */
	private static final long serialVersionUID = -3496508753660220698L;
public RsTransform(){
      m11=1;
      m12=0;
      m13=0;

      m21=0;
      m22=1;
      m23=0;
   }

   public RsTransform(RsTransform t){
      m11 = t.m11;
      m12 = t.m12;
      m13 = t.m13;

      m21 = t.m21;
      m22 = t.m22;
      m23 = t.m23;
   }

   public RsTransform(double theta, double xOffset, double yOffset){
      m11=Math.cos(theta);
      m12=-Math.sin(theta);
      m13=xOffset;

      m21=-m12;
      m22=m11;
      m23=yOffset;
   }


   public RsTransform(RsTransform a, RsTransform b){
      concat(a,b);
   }



   public void concat(RsTransform a, RsTransform b){
      m11 = a.m11*b.m11 + a.m12*b.m21;
      m12 = a.m11*b.m12 + a.m12*b.m22;
      m13 = a.m11*b.m13 + a.m12*b.m23 + a.m13;

      m21 = a.m21*b.m11 + a.m22*b.m21;
      m22 = a.m21*b.m12 + a.m22*b.m22;
      m23 = a.m21*b.m13 + a.m22*b.m23 + a.m23;
   }

   public double getTheta(){
      double x, y;
      x=(m11+m22)/2.0;   // these should be equal
      y=(m21-m12)/2.0;
      return Math.atan2(y, x);
   }

   public void setTheta(double theta){
      m11 =  Math.cos(theta);
      m12 = -Math.sin(theta);
      m21 = -m12;
      m22 =  m11;
   }

   public RsPoint getOffset(){
      return new RsPoint(m13, m23);
   }

   public void setOffset(double xOffset, double yOffset){
       m13 = xOffset;
       m23 = yOffset;
   }

   public RsVector mapVector(double vx, double vy){
      return new RsVector(m11*vx+m12*vy, m21*vx+m22*vy);
   }

   public RsPoint map(RsPoint p){
      return new RsPoint(m11*p.x+m12*p.y+m13, m21*p.x+m22*p.y+m23);
   }

   public RsPoint map(double x, double y){
      return new RsPoint( m11*x+m12*y+m13, m21*x+m22*y+m23);
   }

   public void map(double x, double y, RsPoint output){
      output.x = m11*x + m12*y + m13;
      output.y = m21*x + m22*y + m23;
   }


   public void iMapArray(int nPoint, RsPoint [] point, int [] ix, int []iy){
      double x,y;
      for(int i=0; i<nPoint; i++){
         x=point[i].x;
         y=point[i].y;
         ix[i] = (int)Math.floor(m11*x + m12*y + m13 + 0.5);
         iy[i] = (int)Math.floor(m21*x + m22*y + m23 + 0.5);
      }
   }

   public void iMapArrayIndex(double x, double y, int index, int [] ix, int []iy){
      ix[index] = (int)Math.floor(m11*x + m12*y + m13 + 0.5);
      iy[index] = (int)Math.floor(m21*x + m22*y + m23 + 0.5);
   }


   public void map2(RsSegment input, RsSegment output){
       output.x   = m11*input.x   + m12*input.y    + m13;
       output.y   = m21*input.x   + m22*input.y    + m23;
       output.v.x = m11*input.v.x + m12*input.v.y;
       output.v.y = m21*input.v.x + m22*input.v.y;
       output.m   = output.v.magnitude();
       // note:  if the mapping is just a rotate/move, then magnitude
       //        doesn't change ("is invariant") when the vector is mapped, but
       //        if it includes scaling (and when used for graphics purposes,
       //        it almost always does), the magnitude change.
   }

   public void map2(RsPoint input, RsPoint output){
       output.x   = m11*input.x + m12*input.y + m13;
       output.y   = m21*input.x + m22*input.y + m23;
   }

   public void copy(RsTransform source){
      m11 = source.m11;
      m12 = source.m12;
      m13 = source.m13;

      m21 = source.m21;
      m22 = source.m22;
      m23 = source.m23;
   }


   public double getScale(){
      // return an estimate of how the transform scales an object
      return Math.sqrt(m11*m11+m21*m21);
   }

   public double m11;
   public double m12;
   public double m13;

   public double m21;
   public double m22;
   public double m23;

}

