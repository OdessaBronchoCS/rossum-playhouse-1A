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

import java.awt.*;



/**
 * A floor-plan object used for describing special regions within
 *the floor plan or, simply, just for putting "paint" on the floor.
 *
 */

public  class RsPaint extends RsObject {

   public RsPaint(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet = false;
      fillColor     = Color.lightGray;
      lineColor     = Color.lightGray;
   }

   @Override
public void setGeometry(double [] a){
      isGeometrySet=true;

      int i, i0, i1;
      int nSegment;

      if(a[0] == a[a.length-2] && a[1] == a[a.length-1]){
         // user already closed the polygon for us
         nSegment = (a.length-2)/2;
      }else{
         nSegment = a.length/2;
      }

      segmentArray = new RsSegment[nSegment];
      for(i=0; i<nSegment; i++){
         i0 = i*2;
         i1 = (i*2+2)%a.length;
         segmentArray[i] = new RsSegment(a[i0], a[i0+1], a[i1], a[i1+1]);
      }

      try{
         polygon = new RsPolygon(nSegment, segmentArray);
      }catch(RsPolygonException rspe){
         polygon = null;
      }

      RsRectangle r=new RsRectangle(
                  segmentArray[0].x, segmentArray[0].y,
                  segmentArray[0].v.x, segmentArray[0].v.y);

      for(i=2; i<segmentArray.length;i++)
         r.insert(segmentArray[i].x, segmentArray[i].y);

      setBounds(r);
   }



   @Override
public double [] getGeometry(){
      if(isGeometrySet){
         int i, k, n;
         n=segmentArray.length*2;
         double [] a = new double[n+2];
         k=0;
         for(i=0; i<segmentArray.length; i++){
            a[k++] = segmentArray[i].x;
            a[k++] = segmentArray[i].y;
         }
         a[k++] =  segmentArray[0].x;
         a[k]   =  segmentArray[0].y;
         return a;
      }else{
         return null;
      }
   }

   @Override
public String getClassName(){
      return className;
   }


   @Override
public void paint(Graphics g, RsTransform gt){

      if(segmentArray==null && segmentArray.length<3)
         return;

      int    n     = segmentArray.length+1;
      int [] xWall = new int[n];
      int [] yWall = new int[n];

      gt.iMapArray(n-1, segmentArray, xWall, yWall);
      xWall[n-1]=xWall[0];
      yWall[n-1]=yWall[0];

      g.setColor(fillColor);
      g.fillPolygon(xWall, yWall, n);
      g.setColor(lineColor);
      g.drawPolygon(xWall, yWall, n);
   }


   public int getRegion(){
      return region;
   }

   public void setRegion(int _region){
      region = _region;
   }

   public RsSegment [] getSegmentArray(){
      return segmentArray;
   }

   private static final String className = "paint";

   protected RsSegment [] segmentArray;
   protected RsPolygon    polygon;
   protected int          region;
}
