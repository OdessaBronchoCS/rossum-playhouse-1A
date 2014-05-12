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
   RsVector.java    provides general methods for 2-dimensional vectors
                    in a Euclidean space.



   Notes on Usage:

   A word of caution is in order regarding the use of this class.  Vector
   operations are often used in tight loops and complicated mathemetical
   expressions.   Under these circumstances, the programmer has to be
   careful about creating a lot of objects with short lifespans...  the
   overhead generated through creation and collection may seriously
   affect performance.

   Except for the constructors, NONE of the methods in this class create
   new objects.   As a convenience, most of them finish with the statement
   "return this;"


   Constructors

      public RsVector();
      public RsVector(RsVector vector);
      public RsVector(double x, double y);

   Constants

      public static final NEARLY_ZERO;


   Methods

      public RsVector unit() throws ArithmeticException;            // unitizes self
      public RsVector unit(RsVector a) throws ArithmeticException;  // stores |a|, returns self

         The unit() methods return a unit vector.  If the input vector
         is of too small a magnitude, no unit vector equivalent can be
         reasonable computed.   So, unit() throws an exception.   This design
         decision is "good object-oriented practice" but may prove more
         trouble than it's worth when it comes time to write code using it.


      public RsVector normal();             // normalizes self, returns self
      public RsVector normal(RsVector a);   // stores normal of a, returns self

      public RsVector add(RsVector a);             // adds input to self, returns self
      public RsVector add(RsVector a, RsVector b)  // stores a+b, returns self reference
      public RsVector subtract(RsVector a);        // subtracts input from self, returns self
      public RsVector subtract(RsVector a, RsVector b);

      public RsVector scale(double s);              // scales self
      public RsVector scale(double s, RsVector a);  // stores s*a, returns self

      public double magnitude();
      public double dot(RsVector v);   //dots vector with v

      public static double turn(RsVector a, RsVector b) throws ArithmeticException;
      public static double unitTurn(RsVector a, RsVector b);

         given three points A, B, C, describing a path, compute the
         turn for vectors  a = B-A and b = C-B.  return values are
         in radians in range (-PI, PI] with negative angles referring
         to a right-hand turn.

         the unitTurn method assumes that the vectors a and b are unit
         vectors and, thus, is not required to throw an exception.

   Public Instance Variables

      public double x;
      public double y;


*/

import java.lang.Math;
import java.io.Serializable;



/**
 * A representation of a 2-element vector with a limited set of
 *vector algebra operations; used for analysis.
 *
 */

public class RsVector implements Serializable {

   /**
	 * 
	 */
	private static final long serialVersionUID = 145027435513888213L;
public RsVector(){
      x=0;
      y=0;
   }
   public RsVector(RsVector v){
      x=v.x;
      y=v.y;
   }
   public RsVector(double _x, double _y){
      x=_x;
      y=_y;
   }


   public RsVector unit() throws ArithmeticException {
      return unit(this);
   }

   public RsVector unit(RsVector input) throws ArithmeticException{
      double d = input.magnitude();
      if(d<=NEARLY_ZERO)
         throw new ArithmeticException("Attempt to unitize a vector of NEARLY ZERO magnitude");
      x=input.x/d;
      y=input.y/d;
      return this;
   }


   public RsVector normal(){
      double xOld;
      xOld=x;
      x=-y;
      y=xOld;
      return this;
   }

   public RsVector normal(RsVector v){
      x=-v.y;
      y=v.x;
      return this;
   }


   public double magnitude(){
      return Math.sqrt(x*x+y*y);
   }

   public double dot(RsVector v){
      return x*v.x+y*v.y;
   }

   public static double turn(RsVector a, RsVector b) throws ArithmeticException {
      return unitTurn(a.unit(), b.unit());
   }

   public static double unitTurn(RsVector a, RsVector b){
      // z would be the z coordinate of cross product
      // (a.x, a.y, 0) x (b.x, b.y, 0)
      double z, theta;
      z = a.x*b.y-a.y*b.x;
      theta = Math.acos(a.x*b.x+a.y*b.y);
      if(z<0)
         return -theta;
      else
         return theta;
   }

   public RsVector add(RsVector v){
      x+=v.x;
      y+=v.y;
      return this;
   }

   public RsVector add(RsVector a, RsVector b){
      x= a.x+b.x;
      y= a.y+b.y;
      return this;
   }

   public RsVector subtract(RsVector v){
      x-=v.x;
      y-=v.y;
      return this;
   }

   public RsVector subtract(RsVector a, RsVector b){
      x= a.x-b.x;
      y= a.y-b.y;
      return this;
   }

   public RsVector scale(double scalar){
      x*=scalar;
      y*=scalar;
      return this;
   }

   public RsVector scale(double scalar, RsVector v){
      x=scalar*v.x;
      y=scalar*v.y;
      return this;
   }

   public double x;
   public double y;
   public static final double NEARLY_ZERO=1.0e-9;
}


