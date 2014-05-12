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
 * A floor-plan object used to represent walls.
 *
 */

public  class RsWall extends RsObject {

   public RsWall(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet=false;
      fillColor = Color.lightGray;
      lineColor = Color.darkGray;
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
         double [] a = new double[segmentArray.length*2];
         for(int i=0; i<segmentArray.length; i++){
             a[i*2]   = segmentArray[i].x;
             a[i*2+1] = segmentArray[i].y;
         }
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

      if(segmentArray==null || segmentArray.length<3)
         return;

      int          [] xWall;
      int          [] yWall;

      int          nPoint = segmentArray.length+1;

      xWall = new int[nPoint];
      yWall = new int[nPoint];

      gt.iMapArray(segmentArray.length, segmentArray, xWall, yWall);
      xWall[nPoint-1]=xWall[0];
      yWall[nPoint-1]=yWall[0];
      g.setColor(fillColor);
      g.fillPolygon(xWall, yWall, nPoint);
      g.setColor(lineColor);
      g.drawPolygon(xWall, yWall, nPoint);
   }


   public RsSegment [] getSegmentArray(){
      return segmentArray;
   }

   private static final String className = "wall";
   protected double x0, y0, x1, y1, thickness;
   protected RsSegment [] segmentArray;
   protected RsPolygon polygon;
}


