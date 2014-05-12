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

   RsPoint.java    2-dimensional coordinate points

Constructors
   public RsPoint();
   public RsPoint(RsPoint point);
   public RsPoint(double xarg, double yarg);
   public RsPoint(int xarg, int yarg);
   public RsPoint(RsVector input);


Public Instance Variables

   public double x;
   public double y;

Methods

   public RsVector subtract(RsPoint p);

   public void move(double xArg, double yArg)
   public void move(int xArg, int yArg);

   public void translate(double xArg, double yArg);
   public void translate(int xArg, int yArg);
   public void translate(RsVector v);


Development Notes:

   I would like to write a method to create an array of points from
   an input of double [n][2]. I used to have a createArray(int n)
   method that created an array populated with referneces to RsPoint(0,0),
   but that was more wasteful than helpful (since it meant initializing
   values to zero which were going to be immediately over-written).


*/


import java.io.Serializable;



/**
 * A representation of a single point.
 *
 */

public class RsPoint implements Serializable {

   /**
	 * 
	 */
	private static final long serialVersionUID = -3115912069334717682L;
public RsPoint(){
      x=0;
      y=0;
   }

   public RsPoint(RsPoint point){
      x=point.x;
      y=point.y;
   }

   public RsPoint(double xarg, double yarg){
      x=xarg;
      y=yarg;
   }

   public RsPoint(int xarg, int yarg){
      x=xarg;
      y=yarg;
   }

   public RsPoint(RsVector input){
      x=input.x;
      y=input.y;
   }


   public RsVector subtract(RsPoint p){
      return new RsVector(x-p.x, y-p.y);
   }

   public void move(double xArg, double yArg){
      x=xArg;
      y=yArg;
   }

   public void move(int xArg, int yArg){
      x=xArg;
      y=yArg;
   }

   public void translate(double xArg, double yArg){
      x+=xArg;
      y+=yArg;
   }

   public void translate(int xArg, int yArg){
      x+=xArg;
      y+=yArg;
   }


   public void translate(RsVector v){
      x+=v.x;
      y+=v.y;
   }

   public double distance(RsPoint test){
      double dx, dy;
      dx = test.x - x;
      dy = test.y - y;
      return Math.sqrt(dx*dx+dy*dy);
   }

   public void copy(RsPoint p){
      x = p.x;
      y = p.y;
   }

   public double x;
   public double y;

}


