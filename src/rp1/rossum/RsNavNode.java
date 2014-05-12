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
 * A floor-plan object usef for modeling pre-programmed path intersections.
 *
 */

public  class RsNavNode extends RsObject {


   public RsNavNode(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet = false;
      label         = null;
      fillColor     = Color.cyan;
      lineColor     = Color.cyan;
      link    = null;
      visited = false;
   }

   @Override
public void setGeometry(double [] a){
      isGeometrySet=true;
      x=a[0];
      y=a[1];
      setBounds(new RsRectangle(x-0.005, y-0.005, 0.01, 0.01));
   }

   @Override
public double [] getGeometry(){
      if(isGeometrySet){
         double [] a = new double[2];
         a[0]=x;
         a[1]=y;
         return a;
      }else{
         return null;
      }
   }

   @Override
public void setLabel(String _label){
      label = _label;
   }

   public String getLabel(){
      return label;
   }

   @Override
public String getClassName(){
      return className;
   }

   @Override
public void paint(Graphics g, RsTransform gt){


      int    ix, iy;


      RsPoint p0;

      p0 = gt.map(x, y);
      ix=(int)(p0.x+0.5);
      iy=(int)(p0.y+0.5);

      g.setColor(lineColor);
      g.drawRect(ix-1, iy-1, 3, 3);

      if(label!=null){
         FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
         //  int w=fontMetrics.stringWidth(label);
         int h=fontMetrics.getAscent();
         g.drawString(label, ix+4, iy+h);
      }
   }


      public void addLink(RsNavLink newlink){
      if(link==null){
         link = new RsNavLink[1];
         link[0] = newlink;
      }else{
         RsNavLink [] l = new RsNavLink[link.length+1];
         for(int i=0; i<link.length; i++)
            l[i] = link[i];
         l[link.length] = newlink;
         link=l;
         l=null;
      }
   }


   public    RsNavLink [] link;
   public    boolean    visited;

   private static final String  className = "NavNode";
   public double   x, y;   // the geometry

}

