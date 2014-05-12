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



/***********************************************************

TO DO:  The basis for a body shape REALLY ought to be an
  RsPolygon, not an array of RsSegment objects. This makes
  sense and probably would have been the way this class had been
  implemented except that the RsPolygon class was started much
  later in the history of the rossum package.

*/


package rp1.rossum;



import java.awt.Color;
import java.awt.Graphics;



/**
 * A class for defining physical, interactive body parts for robot bodies.
 *
 */

public class RsBodyShape extends RsBodyPart
{
   private static final long serialVersionUID = 1L;

   // elements used for modeling
   RsSegment  [] refSegment;

   // elements used for rendering
   int        [] ix;
   int        [] iy;


   public RsBodyShape(){
      super();
      refSegment = null;
      refBounds  = null;
   }


   public RsBodyShape(double []point, int nPoint){
      super();
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
      if(d2>=1.0e-6){
         // the user did not supplied a closed loop, add a closure point */
         s[n++] = new RsSegment(x1, y1, point[0], point[1]);
      }

      //  there should really be exceptions thrown if
      //  n<3 or the polygon is non-simple
      //  but such tests are not implemented at this time

      refSegment = new RsSegment[n];
      refBounds  = new RsRectangle(point[0], point[1], 0.0, 0.0);
      for(i=0; i<n; i++){
         refSegment[i] = s[i];
         refBounds.insert(refSegment[i]);
      }

      ix = new int[n+1];
      iy = new int[n+1];
   }

   public RsSegment [] getSegmentArray(){
      return refSegment;
   }



   @Override
public void paint(Graphics g, RsTransform gt){

      Color fc, lc;

      fc = getFillColor();
      lc = getLineColor();

      if(fc==null && lc==null)
         return;

      if(refSegment==null){
        // the shape is not depicted and not interactive because
        // no polygon was ever supplied
      }

      int      nSegment = refSegment.length;

      double   x, y;

      for(int i=0; i<nSegment; i++){
         x=refSegment[i].x;
         y=refSegment[i].y;
         ix[i] = (int)Math.floor(gt.m11*x + gt.m12*y + gt.m13 + 0.5);
         iy[i] = (int)Math.floor(gt.m21*x + gt.m22*y + gt.m23 + 0.5);
      }

      ix[nSegment]=ix[0];
      iy[nSegment]=iy[0];

      if(fc!=null){
         g.setColor(fc);
         g.fillPolygon(ix, iy, nSegment+1);
      }
      if(lc!=null){
         g.setColor(lc);
         g.drawPolygon(ix, iy, nSegment+1);
      }
   }

   public boolean checkForOverlap(RsPlan plan, RsTransform transform){

      if(refSegment==null || refSegment.length==0)
         return false;

      int            nSegment = refSegment.length;
      RsSegSect      segSect = new RsSegSect();
      RsObject    [] objArray;
      RsWall         wall;

      RsSegment []s = new RsSegment[nSegment];
      for(int i=0; i<nSegment; i++){
         s[i] = new RsSegment();
         transform.map2(refSegment[i], s[i]);
      }

      objArray = plan.getObjectArray();
      for(int iObject=0; iObject<objArray.length; iObject++){
         if(!(objArray[iObject] instanceof RsWall))
            continue;
         wall = (RsWall)objArray[iObject];
         for(int i=0; i<nSegment; i++){
           for(int j=0; j<wall.segmentArray.length; j++){
             if(segSect.process(s[i], wall.segmentArray[j]))
               return true;
           }
         }

         if(wall.polygon!=null){
            for(int i=0; i<nSegment; i++){
               if(wall.polygon.checkContainment(
                 s[i].x, s[i].y, 1.0e-4)<=0)
                 return true;
            }
         }

         if(nSegment<2)
           continue;

         try{
           RsPolygon polygon = new RsPolygon(nSegment, s);
           for(int i=0; i<wall.segmentArray.length; i++){
              if(polygon.checkContainment(
                 wall.segmentArray[i].x, wall.segmentArray[i].y, 1.0e-4)<=0)
                 return true;
           }
         }catch(RsPolygonException rspe){ }
      }
      return false;
   }


}
