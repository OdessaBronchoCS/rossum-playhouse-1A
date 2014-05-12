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
import rp1.rossum.request.*;


/*

   RsBody.java

   TO DO:

      applyMotion is called in SimClient processMotion in a
      section of code that
         a.  gets motion from RsBody
         b.  applies task time to motion
         c.  calls applyMotion so any changes to the motion
             get propagated into body parts.

      This should all be combined within a single method call. This
      will be especially important when the Actuators get the ability
      to handle nested motions and so we will have multiple motions
      that need to be updated all at once...  perhaps
      the time of the call might be

             applySimTimeToBodyMotions()



       need to add logic to detect when an inappropriate call to
           copyStateData() is called... this should never happen in a
           properly implemented system, but we should make the method
           throw an error to facilitate debugging.   The conditions
           required for a successful copyStateData() are:

              Bodies should be clones of each other (implement some kind
              of serial number in the constructor, clones have the same
              serial number, created objects do not)

              Each component should be a clone of its counter part.  It is
              possible to clone two bodies and then add more objects to
              the second.   If we do this, then the transfer of state data
              is undefined.

*/


import java.awt.*;
import java.util.ArrayList;



/**
 * The container class for defining a simulated robot.
 *
 */

public class RsBody extends RsComponent {


		private static final long serialVersionUID = 9221545608884628326L;
	
		private  static int     bodySerialNumber;
	
	   public String           name;
	   public RsWheelSystem    wheelSystem;
	   public RsMotion         motion;
	   public RsRectangle      refBounds;
	   public RsSegment []     refEnclosure;
	   public boolean          placement;
	   public boolean          collision;
	
	   private  int            bodyID;
	
	   private ArrayList <RsBodyPart>    bodyPartList;
	   private ArrayList <RsBodyPainter> bodyPainterList;
	   private ArrayList <RsBodyShape>   bodyShapeList;
	   private RsBodyPart    []          bodyPartArray;
	   private RsBodyPainter []          bodyPainterArray;
	   private RsBodyShape   []          bodyShapeArray;


   public RsBody(String nameReference){
      name        = nameReference;
      wheelSystem = null;
      motion      = new RsMotionNull(0, 0.0, 0.0, 0.0);
      refBounds   = new RsRectangle(0.0, 0.0, 0.0, 0.0);
      bodyID      = bodySerialNumber++;
   }


   public void addPart(RsBodyPart part){

      bodyPartArray    = null;
      bodyPainterArray = null;
      bodyShapeArray   = null;

      if(bodyPartList == null)
         bodyPartList = new ArrayList<RsBodyPart>();
      bodyPartList.add(part);

      if(part instanceof RsBodyShape && ((RsBodyShape)part).refSegment!=null){
         if(bodyShapeList==null)
            bodyShapeList = new ArrayList<RsBodyShape>();
         bodyShapeList.add((RsBodyShape)part);
      }

      if(part instanceof RsBodyPainter){
         if(bodyPainterList==null)
            bodyPainterList = new ArrayList<RsBodyPainter>();
         bodyPainterList.add((RsBodyPainter)part);
      }

      refBounds.union(part.refBounds);

      if(part instanceof RsWheelSystem)
         wheelSystem=(RsWheelSystem)part;
   }

   public RsBodyPart [] getBodyPartArray(){
      if(bodyPartArray==null && bodyPartList != null)
         bodyPartArray=(RsBodyPart [])bodyPartList.toArray(new RsBodyPart[bodyPartList.size()]);

      return bodyPartArray;
   }


   public RsBodyPainter[] getBodyPainterArray(){
      if(bodyPainterArray==null && bodyPainterList!=null)
         bodyPainterArray=(RsBodyPainter[])bodyPainterList.toArray(new RsBodyPainter[bodyPainterList.size()]);

     return bodyPainterArray;
   }

   public RsBodyShape[] getInteractiveBodyShapeArray(){
      if(bodyShapeArray==null && bodyShapeList!=null)
         bodyShapeArray=(RsBodyShape[])bodyShapeList.toArray(new RsBodyShape[bodyShapeList.size()]);

     return bodyShapeArray;
   }



   public String getName(){
      return name;
   }

   public RsBodyPart getPartByName(String _name){
      RsBodyPart [] bodyPart = getBodyPartArray();
      if(bodyPart != null){
         for(int i=0; i< bodyPart.length; i++)
            if(bodyPart[i].name.equals(_name))
               return bodyPart[i];
      }
      return null;
   }

   public RsBodyPart getPartByID(int _ID){
      RsBodyPart [] bodyPart = getBodyPartArray();
      if(bodyPart!=null){
         for(int i=0; i< bodyPart.length; i++)
            if(bodyPart[i].getID()==_ID)
               return bodyPart[i];
      }
      return null;
   }


   public void copyStateData(RsBody dataSource){
       dataSource.transferStateDataToBody(this);
   }

   public void applyMotion(){
     if(wheelSystem!=null)
        wheelSystem.applyMotion(motion);
   }

   protected void transferStateDataToBody(RsBody receiver){
      RsBodyPart [] thisPart     = getBodyPartArray();
      RsBodyPart [] receiverPart = receiver.getBodyPartArray();

      if(thisPart==null || receiverPart==null || thisPart.length!=receiverPart.length){
         return;  // this should never happen
      }

      for(int i=0;i<thisPart.length;i++){
         receiverPart[i].copyStateData(thisPart[i]);
      }

      receiver.setPlacement(placement);
      receiver.collision = collision;


      // TO DO: rather than cloning, we may be able to just advance the time on the
      //        receiver motion if continuityID and continuitySeriesID both match.
      if(receiver.motion != null &&
         receiver.motion.getContinuityID()       == motion.getContinuityID() &&
         receiver.motion.getContinuitySeriesID() == motion.getContinuitySeriesID())
      {
         receiver.motion.setTimeForward(motion.time);
      }else{
         try{
            receiver.motion = (RsMotion)motion.clone();
         }catch(CloneNotSupportedException e){
            receiver.motion=null;
         }
      }

      if(receiver.wheelSystem!=null)
          receiver.wheelSystem.applyMotion(receiver.motion);

      RsBodyPainter tbp[] = getBodyPainterArray();
      RsBodyPainter rbp[] = receiver.getBodyPainterArray();
      if(rbp==null || tbp==null || rbp.length!=tbp.length){
         return;  // this should never happen
      }

      for(int i=0; i<tbp.length; i++)
          rbp[i].copyStateData(tbp[i]);
   }


   public void resetStateData(){
      RsBodyPart [] p = getBodyPartArray();
      if(p!=null){
         for(int i=0; i<p.length; i++)
           p[i].resetStateData();
      }
   }



   public void setCollision(boolean collision){
      this.collision = collision;
   }

   public boolean getCollision(){
      return collision;
   }

   public RsPositionEvent getPositionEvent(double simTime){
      RsTransform t = motion.getStateAtTime(simTime);
      if(t==null)
         return null;
      double x = t.m13;
      double y = t.m23;
      double orientation=t.getTheta();
      double velocity = motion.getVelocity();
      double turnRate = motion.getTurnRate();
      return new RsPositionEvent(
         simTime,
         x,
         y,
         orientation,
         velocity,
         turnRate);
   }

   public boolean processSensors(double simTime, RsPlan plan, RsTransform transform){
      boolean stateChange=false;
      RsBodyPart bodyPart[] = getBodyPartArray();
      for(int i=0; i<bodyPart.length; i++){
         if(bodyPart[i].isASensor()){
            if(((RsBodySensor)bodyPart[i]).computeAndSetState(simTime, plan, transform))
               stateChange=true;
         }
      }
      return stateChange;
   }

   public void computeMotion(double startTime, RsMotionRequest request){
      motion=wheelSystem.computeMotion(motion, startTime, request);
   }

   public void setMotion(RsMotion _motion){
      motion = _motion;
   }

   public RsMotion getMotion(){
      return motion;
   }

   public void paint(Graphics g, RsTransform paintTransform){
      RsBodyPart [] bodyPart = getBodyPartArray();
      if(bodyPart==null)
         return;

      for(int i=0; i<bodyPart.length; i++)
         bodyPart[i].paint(g, paintTransform);
   }

   public void setPlacement(boolean value){
      placement = value;
   }

   public boolean getPlacement(){
      return placement;
   }

   protected boolean searchForCollisions(RsPlan plan){
      RsBodyPart [] bodyPart = getBodyPartArray();
      if(bodyPart==null)
         return false;

      boolean flag=false;
      for(int i=0; i<bodyPart.length; i++)
         if(bodyPart[i].searchForCollisions(plan, motion))
            flag=true;
      return flag;
   }

   public RsRectangle getBounds(){
      return new RsRectangle(refBounds);
   }

   public RsSegment [] getRefEnclosure(){
      if(refBounds.width<0.001)
          return null;


      if(refEnclosure==null){
         RsSegment [] s = new RsSegment[4];
         double x0 = refBounds.x-0.001;
         double y0 = refBounds.y-0.001;
         double x1 = x0+refBounds.width+0.002;
         double y1 = y0+refBounds.width+0.002;

         s[0] = new RsSegment(x0, y0, x1, y0);
         s[1] = new RsSegment(x1, y0, x1, y1);
         s[2] = new RsSegment(x1, y1, x0, y1);
         s[3] = new RsSegment(x0, y1, x0, y0);
         refEnclosure = s;
      }
      return refEnclosure;
   }



   public int getID(){
     return bodyID;
   }



   public boolean checkForOverlap(RsPlan plan, RsTransform transform){
      RsBodyShape [] shape = getInteractiveBodyShapeArray();
      if(shape!=null){
         for(int i=0; i<shape.length; i++){
           if(shape[i].checkForOverlap(plan, transform))
              return true;
         }
      }
      return false;
   }

}
