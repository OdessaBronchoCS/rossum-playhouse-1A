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






/*


   DEVELOPMENT NOTE:  Implementation is NOT COMPLETE.


   RsPolygon.java    provides general data for a line segment

   This class is still under development.   It is used in
   the RsPaintSensor class, but could stand some rigorous
   testing.  Also, I am still working out the details of the API.

   method checkContainment(double x, double y)
   method checkContainment(double x, double y, double proximity)

   This method determines whether a point is inside the polygon,
   returning the following values:

       -1  inside
        0  on the boundary
        1  outside

   Note that the method is called "checkContainment" rather than something
   like "isPointInside" because having an "is" in the name usually
   suggests that the method returns a boolean...  Certainly, the name
   "isPointInside" would have been easier to remember, but it would also
   a bit misleading.


   The proximity value is used to specify that points very close
   to the polygon (less than a specified distance from an edge)
   are to be treated as lying on its boundary.   This feature is
   useful in RP1 to avoid certain coding complexities, I don't
   how much value it will have for other applications.


   The algorithm used here is pretty straight-forward.  In this
   implementation, it does depend on having the magnitude (length) of each
   edge segment pre-computed.   In the rossum system, this is a reasonable
   assumption (because we reprocess the same polygons over and over
   again, so there is value in pre-computing certain quantities).
   In other applications, the code can be adjusted so that the
   magnitude is only computed as necessary when resolving ambiguity
   (though you'd have to drop the proximity feature -- which needs
   the magnitude -- from your implementation).


   PTO DO AND VERY MUCH PROBLEMATIC

   The current constructor assumes all the segments you give
   it define a valid polygon (with complete closure).   I intend
   to implement a constructor that performs sanity checking.
   If the polygon fails sanity checks, it throws an exception.
   These checks will be added to the "checkConsistency" method.

   Potential alternate constructors

      input arrays of x/y coordinates
      input arrays of RsPoints

   Potential sanity checks

        segments do not connect
        polygon is not closed
        polygon is self-intersecting
        fewer than 3 segments
        spike condition (angle between two subsequent polygons
        is close to zero)

        Failed sanity checks will cause the constructor
        to throw an exception



   Potentially useful additional methods

         compute the union/intersection of two
         polygons

         clip a polygon

         create a convex bounding polygon (a planar equivalent
         of a convex hull)


    TO DO:  The order of the arguments in the two constructors is
            opposite (the array comes before the number of elements in
            one and after in the other).  The double points[] method was
            implemented for consistency with older method in RsBodyShape.
            We should really clean this up.

*/




package rp1.rossum;

import java.io.Serializable;



/**
 * A representation of a polygon; this class is used by the simulator
 *for collision analysis, placement determination, paint sensing, etc.
 *
 */

public class RsPolygon implements Serializable{

   /**
	 * 
	 */
	private static final long serialVersionUID = 2166101501614042125L;
public int          nSegment;
   public RsSegment [] segment;
   public double       area;
   public int          orientation;

   public RsPolygon(int nSegment, RsSegment [] segment) throws RsPolygonException {
      int n;

      if(nSegment<3)
         throw new RsPolygonException("Insufficient number of points/segments for polygon");

      RsSegment s = segment[nSegment-1];
      if(s.x+s.v.x==segment[0].x && s.y+s.v.y==segment[0].y){
         n = nSegment;  // the input segments close a loop
      }else{
         n = nSegment+1; // add a synthetic segment to close the loop
      }

      this.segment  = new RsSegment[n];
      this.nSegment = n;
      for(int i=0; i<nSegment; i++)
         this.segment[i] = new RsSegment(segment[i]);
      if(n>nSegment){
         this.segment[nSegment] =
            new RsSegment(
               segment[nSegment-1].x, segment[nSegment-1].y,
               segment[0].x,          segment[0].y);
      }

      checkConsistency();
   }

   public RsPolygon(double []point, int nPoint) throws RsPolygonException {
      if(nPoint<3)
         throw new RsPolygonException("Insufficient number of points/segments for polygon");


      RsSegment []s = new RsSegment[nPoint+1];

      int i, n, k;
      double x0, y0;
      double x1, y1;
      double d2, dx, dy;

      x0=point[0];
      y0=point[1];
      x1=x0;
      y1=y0;
      k=2;
      n=0;
      for(i=1; i<nPoint; i++){
         x1=point[k++];
         y1=point[k++];
         dx = x1-x0;
         dy = y1-y0;
         d2 = dx*dx+dy*dy;
         if(d2<1.0e-6)
            continue;  // disregard edges of less than 1 mm length
         s[n++]=new RsSegment(x0, y0, x1, y1);
         x0=x1;
         y0=y1;
      }

      dx = x1-point[0];
      dy = y1-point[1];
      d2 = dx*dx+dy*dy;
      if(d2>0){
         if(d2>=1.0e-6){
            // the user did not supply a closed loop, add a closure point */
            s[n++] = new RsSegment(x1, y1, point[0], point[1]);
         }else{
            // the last point is close but not perfectly on top of the
            // initial point. Rather than create a segment of length<1mm,
            // we simply reassign the last segment to close the loop.
            s[n-1] = new RsSegment(x0, y0, point[0], point[1]);
         }
      }



      segment  = new RsSegment[n];
      nSegment = n;
      for(i=0; i<n; i++)
         segment[i] = s[i];

      checkConsistency();
   }

   private void checkConsistency() throws RsPolygonException {
      // TO DO:  check for self-crossing and throw an exception
      area = 0;
      RsSegment s;
      for(int i=0; i<nSegment; i++){
         s=segment[i];
         area += s.x*s.v.y - s.y*s.v.x;
      }
      area/=2.0;

      if(area<0){
        // reverse the orientation
        orientation = -1;  // it's clockwise
        area = -area;
      }else{
        orientation = 1;   // it's counter-clockwise
      }
   }



   public int checkContainment(double xTest, double yTest){
      return checkContainment(xTest, yTest, 0.0);
   }

   public int checkContainment(double xTest, double yTest, double proximity){

      int       i;
      double    x, y;
      double    s, c, a;
      double    sCritical, cCritical, aCritical;
      boolean   ambiguity;
      RsSegment e;   // e for edge
      RsVector  v;
      double    scanFlag;

      ambiguity = false;
      sCritical = 0;
      cCritical = 0;
      aCritical = 0;
      for(i=0; i<nSegment; i++){
         e = segment[i];
         v = e.v;
         x = xTest - e.x;
         y = yTest - e.y;

         c = (x*v.y - y*v.x)/e.m;

         // see if it is proximite to the line (proximity value
         // will often be input as zero).
         if(proximity>=c && c>=-proximity){
            // is it on the line?  find the coordinate of (x,y) in
            // the direction of vector v (recall e.m is |v|)
            a = (x*v.x + y*v.y)/e.m;
            if(-proximity<=a && a<=e.m+proximity)
               return 0;
         }

         // see if the edge overlaps the scan line
         scanFlag = y*(v.y-y);
         if(scanFlag<0)
            continue;  // there is no overlap


         if(scanFlag>0){
           // the edge overlaps the scan line
           // compute displacement (note that scanFlag>0 implies v.y != 0)
           s = x - y*v.x/v.y;
           if(s<0)
             continue;  // only consider segments to left of point
           if(sCritical>0 && s>sCritical)
             continue;
           sCritical = s;
           cCritical = c;
           ambiguity = false;
         }else{
           // scanFlag == 0 which implies that the scanline
           // passes through an end-point of the edge, we may
           // have to dis-ambiguate the situation.
           if(v.y==0)
             continue;
           a = v.x/e.m;   // cosine of angle between edge and scan line
           if(y==0){
             s = x;
           }else {
             a = -a;  // reverse direction of vector
             s = x - v.x;
           }
           if(s<0)
              continue;  // only consider segments to left of point
           if(sCritical==0){
              sCritical = s;
              cCritical = c;
              aCritical = a;
              ambiguity = true;
           }else{
              if(s>sCritical)
                continue;
              if(ambiguity){
                // we can now resolve the ambiguity
                if(a>aCritical)
                  cCritical = c;
                ambiguity = false;
              }else{
                // we need to establish ambiguity, resolve it later
                ambiguity=true;
                aCritical = a;
              }
           }
         }
       }

       if(sCritical==0){
          // the scan line never intersected an edge, so we know
          // that the entire polygon was outside the range of the (x,y) point.
          return 1;
       }

       if(cCritical<0){
          // point is to left of near-by edge
          if(orientation>0)
             return -1;  // it's inside a counter-clockwise polygon
          else
             return  1;  // it's outside a clockwise polygon
       }else{
          if(orientation>0)
             return 1;
          else
             return -1;
      }
   }

   public RsRectangle getBounds(){
      RsRectangle r = new RsRectangle(segment[0].x, segment[0].y, 0.0, 0.0);
      for(int i=1; i<nSegment; i++)
         r.insert(segment[i].x, segment[i].y);
      return r;
   }
}
