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
 * A floor-plan object representing targets.
 *
 */

public  class RsTarget extends RsObject {


   public RsTarget(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet=false;
      label=null;
      lineColor=Color.lightGray;
      fillColor=Color.lightGray;
      lineWidth=0;
   }

   @Override
public void setGeometry(double [] a){
      isGeometrySet=true;
      x=a[0];
      y=a[1];
      radius=a[2];
      setBounds(new RsRectangle(x-radius, y-radius, 2*radius, 2*radius));
   }

   @Override
public double [] getGeometry(){
      if(isGeometrySet){
         double [] a = new double[3];
         a[0]=x;
         a[1]=y;
         a[2]=radius;
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

      int    ix, iy, iRadius;
      int    w, h;

      RsPoint p0, p1;

      p0 = gt.map(x, y);
      p1 = gt.map(x+radius, y);
      iRadius=(int)(Math.abs(p1.x-p0.x)+0.5);
      ix=(int)(p0.x+0.5);
      iy=(int)(p0.y+0.5);

      g.setColor(lineColor);

      if(iRadius>0){
         if(lineWidth>=2 && iRadius>4*lineWidth){
             w=iRadius;
             g.fillOval(ix-w, iy-w, 2*w, 2*w);
             g.setColor(Color.white);
             w=iRadius-lineWidth;
             g.fillOval(ix-w, iy-w, 2*w, 2*w);
             g.setColor(fillColor);
         }else{
            g.drawOval(ix-iRadius, iy-iRadius, iRadius*2, iRadius*2);
         }
      }

      if(label==null){
         g.drawLine(ix, iy-4, ix, iy+4);
         g.drawLine(ix-4, iy, ix+4, iy);
      }else{
         FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
         w=fontMetrics.stringWidth(label);
         h=fontMetrics.getAscent();
         g.drawString(label, ix-w/2, iy+h/2);
      }
   }




   private static final String  className = "target";

   public double   x, y, radius;   // the geometry
   public int      lineWidth;
}

