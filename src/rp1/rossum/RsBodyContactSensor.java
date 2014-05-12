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

import rp1.rossum.event.*;


/**
 * The sensor for detecting contact with other objects (a touch sensor).
 *
 */

public class RsBodyContactSensor extends RsBodySensor {

	private static final long serialVersionUID = 1L;
	
   // note that boolean "hot" is defined in a super class
   // collision and collisionTime are scratch variables used
   // in motion computations
   protected boolean collision;
   protected double  collisionTime;
   protected String  collisionObjectName;


   public RsBodyContactSensor(
      double []point,
      int nPoint
   ){
      super(point, nPoint);
      name = "Unnamed Contact Sensor";
   }

   @Override
public boolean computeAndSetState(double simTime, RsPlan plan, RsTransform transform){
      // this method is a dummy right now.
      return false;  //  stateChange;
   }



   // TO DO: deprecate this method
   public void setCollision(boolean status){
      collision           = status;
      collisionObjectName = null;
   }

   public void setCollision(boolean status, String collisionObjectName){
      this.collision = status;
      if(status)
         this.collisionObjectName = collisionObjectName;
      else
         this.collisionObjectName = null;
   }

   public void setCollisionTime(double time){
      collisionTime = time;
   }

   public boolean getCollision(){
      return collision;
   }

   public double getCollisionTime(){
      return collisionTime;
   }

   @Override
public RsSensorEvent getSensorEvent(double simTime){
      return new RsContactSensorEvent( simTime, getID(), hot);
   }

   public void sendSensorEvent(RsConnection connection, double simTime){
      connection.sendContactSensorEvent(simTime, getID(), hot, collisionObjectName);
   }

}

