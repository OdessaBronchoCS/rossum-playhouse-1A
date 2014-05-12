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
 * A floor-plan object used for modeling pre-programmed paths.
 *
 */

public  class RsNavLink extends RsObject {


   public RsNavLink(String nameReference, RsPlan planReference){
      super(nameReference, planReference);
      isGeometrySet= false;
      label        = null;
      fillColor    = Color.cyan;
      lineColor    = Color.cyan;
      n0=null;
      n1=null;
      visited=false;
   }

   public void setNodes( RsNavNode _n0, RsNavNode _n1){
      n0 = _n0;
      n1 = _n1;
      segment = new RsSegment(n0.x, n0.y, n1.x, n1.y);
      n0.addLink(this);
      n1.addLink(this);
      isGeometrySet=true;
      setBounds(new RsRectangle(segment.x, segment.y, segment.v.x, segment.v.y));
   }


   @Override
public void setGeometry(double [] a){

   }

   @Override
public double [] getGeometry(){
      if(isGeometrySet){
         double [] a = new double[4];
         a[0]=segment.x;
         a[1]=segment.y;
         a[2]=segment.x+segment.v.x;
         a[3]=segment.y+segment.v.y;
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

      int    ix0, iy0, ix1, iy1;
      RsPoint p0, p1;

      p0 = gt.map(segment.x, segment.y);
      p1 = gt.map(segment.x+segment.v.x, segment.y+segment.v.y);
      ix0=(int)(p0.x+0.5);
      iy0=(int)(p0.y+0.5);
      ix1=(int)(p1.x+0.5);
      iy1=(int)(p1.y+0.5);


      g.setColor(lineColor);

      g.drawLine(ix0, iy0, ix1, iy1);


      /*  Right now, I'm not sure what to do about a label (any at all?)
          It would be a lot easier using the Java 2D API,
          maybe I should wait until I convert...

            if(label!=null){
               FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
               w=fontMetrics.stringWidth(label);
               h=fontMetrics.getAscent();
               g.drawString(label, ix+4, iy+h);
            }
      */
   }




   public RsNavNode n0, n1;
   public RsSegment  segment;
   public boolean    visited;


   private static final String  className = "NavLink";
}

