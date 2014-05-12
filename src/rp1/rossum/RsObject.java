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

  This is the super class for all the basic physical objects
  handled by the Rs system, including walls, targets, and doorways.
  As you can imagine, there's a lot of stuff yet to go into the
  abstract class such as bounding-rectangles, convex hulls, etc.
  handy modeling and graphics-support methods, etc.

  Initially, I included code in RsObject and its derived classes
  to support a class called RsWritePlan which would write a floor plan
  files.    Later, I realized that was a mistake.  The purpose of
  RsObjects is to be used in modeling.  Code for formatting an object
  as text was irrelevant to its main function and required that I include
  a lot of extraneous material in the class.  Furthermore, it meant that
  the logic for performing the conversion was scattered all across the
  files in this package.   If I decided to change the output format or use a
  different file-output class, it would mean I'd have to change code in
  a lot of different places.

  On the other hand, RsObject and its derived classes DO include a
  paint method.   Depiction is an important function of the simulator
  and keeping the paint methods as part of the class definition has
  obvious advantages.

  So, finally, I ripped to extraneous stuff and put it where it belongs.
  It cost me a day's effort.   I hope I can avoid similar wastes of time
  in the future.

  Notes:

  the setGeometry() method is expected to populate the bounds (bounding rectangle)


*/


import java.awt.Graphics;
import java.awt.Color;



/**
 * The abstract base class for all floor-plan objects.
 *
 */

public abstract class RsObject extends Object {

   public RsObject(String nameReference, RsPlan planReference){
      name=nameReference;
      plan=planReference;
      selected = true;
      bounds=null;      // until it's set to non-null, this is a unusable object.
   }
   public  void setName(String nameReference){
      name=nameReference;
   }
   public String getName(){
     return name;
   }
   public RsRectangle getBounds(){
      return bounds;
   }
   public void setBounds(RsRectangle rectangle){
      if(rectangle==null)
         bounds=null;
      else
         bounds=new RsRectangle(rectangle);
   }

   public boolean getSelected(){
      return selected;
   }

   public void setSelected(boolean value){
      selected = value;
   }

   public void setColor(Color color){
      fillColor = color;
      lineColor = color;
   }

   public void setFillColor(Color color){
      fillColor = color;
   }

   public void setLineColor(Color color){
      lineColor = color;
   }

   public void setLabel(String label){
      this.label = label;
   }

   public abstract String getClassName();

   public abstract void setGeometry(double [] a);
   public abstract double [] getGeometry();

   public abstract void paint(Graphics g, RsTransform t);

   public    boolean     isGeometrySet;
   private   String      name;
   public    RsPlan      plan;
   private   RsRectangle bounds;
   protected boolean     selected;
   public    Color       fillColor;
   public    Color       lineColor;
   public    String      label;
}

