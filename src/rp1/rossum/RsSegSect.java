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
   RsSegSect.java    finds interection of two segments

   intersection is found by solving the linear system

      a1*x + b1*y = c1
      a2*x + b2*y = c2

   in matrix form

      | a1   b1 |   |x|     |c1|
      |         | * | |  =  |  |
      | a2   b2 |   |y|     |c2|


   Once we've solved for (x,y), we also need to test to see that the
   point lies within both segments...   if there is an intersection,
   parameters t1 and t2 are computed for the intersection on both segments.


   Critical Assumption:  We assume that the line-segments are non-trivial
                         (of length greater than something close to zero, 1.0e-9)


   This is a VERY heavily used function and would benefit
   from some effort to make it more efficient.  Of course,
   it's hard to say what's a worthy improvement and what just
   gets lost in the noise.


*/

import java.io.Serializable;



/**
 * A class used to treat line intersections.
 *
 */

public class RsSegSect implements Serializable{

   /**
	 * 
	 */
	private static final long serialVersionUID = 5940348661315250325L;
public RsSegSect(){
      intersection = false;
   }

   public boolean process(RsSegment s1, RsSegment s2){


     /*  check overlap of segments -----------------
         
        before solving the linear system as described above,
        we perform a check to see if the segments have
        any change of overlapping.  For example, if the maximum
        X-coordinate of one segment is smaller than the minimum
        X-coordinate of the other, then there is no need to
        solve the linear system.   While this pre-test is not
        strictly necessary in terms of getting the right answer,
        it can speed the calculations quite a bit.

        Of course, there are plenty of cases where the
        pre-test will be inconclusive and we will have
        to solve the linear system anyway.  Furthermore,
        in those cases where the segments DO intersect,
        then the pre-test will turn out to be just an 
        unnecessary extra step.  So it is reasonable to
        ask whether it really helps.

        To find out, I did an experiment.  It turns out that
        in a typical RP1 simulation environment (and in other
        applications like it, I suppose), that it helps a
        LOT.  In over 90 percent of the cases where this
        logic was invoked, there was no need to bother
        computing the determinant.  

        So it turns out it was worth doing...  though you
        will note that I took pains to ensure that it was
        implemented efficiently (if the pre-test cost more
        than solving the linear system, it STILL wouldn't
        be worth doing).

        DEBUGGING HINT:  The pre-test is conclusive in
        so large a percentage of the cases, that if you
        ever need to debug the linear-system part of this
        logic, I suggest you comment out the pre-test...
        or you will feel like you never get into the code
        you're trying to debug.

    */
        


     // copy the nessary variables out of the segments.
     // although not strictly necessary, and actually extra
     // operations for the computer...  this approach does
     // make things easier to follow.  

     double ax0, ay0, ax1, ay1, bx0, by0, bx1, by1, adx, ady, bdx, bdy;

     ax0 = s1.x;
     ay0 = s1.y;
     adx = s1.v.x;
     ady = s1.v.y;
     ax1 = ax0+adx;
     ay1 = ay0+ady;

     bx0 = s2.x;
     by0 = s2.y;
     bdx = s2.v.x;
     bdy = s2.v.y;
     bx1 = bx0+bdx;
     by1 = by0+bdy;


     if(ady<0){
       // ay0 is the max, ay1 is the min 
       if(bdy<0){
         // by0 is the max, by1 is the min 
         if(by0<ay1)
           return false;
         if(by1>ay0)
           return false;
       }else{
         // by0 is the min, by1 is the max 
         if(by0>ay0)
           return false;
         if(by1<ay1)
           return false;
       }
     }else{
       // ay0 is the min, ay1 is the max 
       if(bdy<0){
         // by0 is the max, by1 is the min 
         if(by0<ay0)
           return false;
         if(by1>ay1)
           return false;
       }else{
         // by0 is the min, by1 is the max 
         if(by0>ay1)
           return false;
         if(by1<ay0)
           return false;
       }
     }


     if(adx<0){
       // ax0 is the max, ax1 is the min 
       if(bdx<0){
         // bx0 is the max, bx1 is the min 
         if(bx0<ax1)
           return false;
         if(bx1>ax0)
           return false;
       }else{
         // bx0 is the min, bx1 is the max 
         if(bx0>ax0)
           return false;
         if(bx1<ax1)
           return false;
       }
     }else{
       // ax0 is the min, ax1 is the max 
       if(bdx<0){
         // bx0 is the max, bx1 is the min 
         if(bx0<ax0)
           return false;
         if(bx1>ax1)
           return false;
       }else{
         // bx0 is the min, bx1 is the max 
         if(bx0>ax1)
           return false;
         if(bx1<ax0)
           return false;
       }
     }




     // The pre-test was inconclusive, solve the linear system

      double a1, b1, c1, a2, b2, c2;

      a1 =  s1.v.y;
      b1 = -s1.v.x;
      a2 =  s2.v.y;
      b2 = -s2.v.x;

      double det = a1*b2-a2*b1;
      if(-1.0e-9<det && det<1.0e-9){
         // close to zero, no intersection
         intersection = false;
         return false;
      }

      c1 = a1*s1.x + b1*s1.y;
      c2 = a2*s2.x + b2*s2.y;

      x = (b2*c1 - b1*c2)/det;
      y = (a1*c2 - a2*c1)/det;

      // now we must see if (x,y) is on both line segments

      if(-1.0e-9<a1 && a1<1.0e-9)
         t1 = -(x-s1.x)/b1;
      else
         t1 = (y-s1.y)/a1;

      if(t1<0 || t1>1){
         intersection = false;
         return false;
      }

      if(-1.0e-9<a2 && a2<1.0e-9)
         t2 = -(x-s2.x)/b2;
      else
         t2 = (y-s2.y)/a2;

      if(t2<0 || t2>1){
         intersection = false;
         return false;
      }


      intersection = true;
      return true;
   }

   public boolean intersection;
   public double  t1, t2;
   public double  x, y;
}

