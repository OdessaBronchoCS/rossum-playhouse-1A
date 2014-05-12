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
 * A floor-plan object giving named positions and orientations
 *for the initial placement of robots.
 *
 */

public  class RsPlacement extends RsObject {


   public RsPlacement(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet=false;
      label=null;
      fillColor=Color.darkGray;
      lineColor=Color.darkGray;
      lineWidth=0;
   }

   @Override
public void setGeometry(double [] a){
      isGeometrySet=true;
      x           = a[0];
      y           = a[1];
      orientation = a[2];
      radius      = a[3];
      setBounds(new RsRectangle(x-radius, y-radius, 2*radius, 2*radius));
   }

   @Override
public double [] getGeometry(){
      if(isGeometrySet){
         double [] a = new double[4];
         a[0] = x;
         a[1] = y;
         a[2] = orientation;
         a[3] = radius;
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

      int    i, j;
      int    ix, iy, iRadius;
      int    w, h;

      double       [] xPlacement = null;
      double       [] yPlacement = null;

      int          [] xWall;
      int          [] yWall;
      xWall = new int[6];
      yWall = new int[6];

      // TO DO: once I settle on decent values for the placement
      //        I need to hard-wire these values as a static final
      //        rather than computing them this way.  incidentally,
      //        there's nothing particularly virtuous about these values,
      //        they're mainly just the result of trial and error.
      if(xPlacement==null){
         xPlacement = new double[6];
         yPlacement = new double[6];
         xPlacement[0] = 1;
         yPlacement[0] = 0;
         xPlacement[1] = Math.cos(60*Math.PI/180)*1.2;
         yPlacement[1] = Math.sin(45*Math.PI/180)*1.2;
         xPlacement[2] = Math.cos(135*Math.PI/180)*1.2;
         yPlacement[2] = Math.sin(135*Math.PI/180)*1.2;
         xPlacement[3] = Math.cos(-135*Math.PI/180)*1.2;
         yPlacement[3] = Math.sin(-135*Math.PI/180)*1.2;
         xPlacement[4] = Math.cos(-60*Math.PI/180)*1.2;
         yPlacement[4] = Math.sin(-45*Math.PI/180)*1.2;
         xPlacement[5] = xPlacement[0];
         yPlacement[5] = yPlacement[0];
      }

      RsPoint p0, p1;
      p0=gt.map(x, y);
      p1=gt.map(x+radius, y);
      ix=(int)(p0.x+0.5);
      iy=(int)(p0.y+0.5);
      iRadius=(int)(Math.abs(p1.x-p0.x)+0.5);

      g.setColor(lineColor);

      if(iRadius>0){
         double  ax, ay;  // the "x basis vector"
         double  bx, by;  // the "y basis vector", negated due to pixel-upside-down factor
         double  s;
         s = iRadius;
         ax =  s*Math.cos(orientation);
         ay = -s*Math.sin(orientation);
         bx =  s*Math.cos(orientation+Math.PI/2);
         by = -s*Math.sin(orientation+Math.PI/2);
         if(lineWidth>=2 && iRadius>4*lineWidth){
            for(j=0; j<2; j++){
               for(i=0; i<6; i++){
                  xWall[i] = ix+(int)(xPlacement[i]*ax+yPlacement[i]*bx+0.5);
                  yWall[i] = iy+(int)(xPlacement[i]*ay+yPlacement[i]*by+0.5);
               }
               g.fillPolygon(xWall, yWall, 6);
               g.setColor(Color.white);
               s  =  iRadius-lineWidth;
               ax =  s*Math.cos(orientation);
               ay = -s*Math.sin(orientation);
               bx =  s*Math.cos(orientation+Math.PI/2);
               by = -s*Math.sin(orientation+Math.PI/2);
            }
         }else{
            for(i=0; i<6; i++){
               xWall[i] = ix+(int)(xPlacement[i]*ax+yPlacement[i]*bx+0.5);
               yWall[i] = iy+(int)(xPlacement[i]*ay+yPlacement[i]*by+0.5);
            }
            g.drawPolygon(xWall, yWall, 6);
         }

         g.setColor(lineColor);
         if(label==null){
            g.drawLine(ix,   iy-4, ix,   iy+4);
            g.drawLine(ix-4, iy,   ix+4, iy);
         }else{
            FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
            w=fontMetrics.stringWidth(label);
            h=fontMetrics.getAscent();
            g.drawString(label, ix-w/2, iy+h/2);
         }
      }
   }



   private static final String  className = "placement";

   public double   x, y, orientation, radius;   // the geometry
   public int      lineWidth;
}

