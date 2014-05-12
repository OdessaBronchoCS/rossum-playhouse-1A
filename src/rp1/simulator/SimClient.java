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



package rp1.simulator;
import rp1.rossum.*;
import rp1.rossum.event.*;
import rp1.rossum.request.*;


public class SimClient extends RsConnection {

      protected SimSession         session;
      protected RsBody             body;
      protected SimPaintBox []     paintBox;

      private   SimHeartbeatTask   heartbeatTask;

      private   SimStartMotionTask startMotionTask;
      private   boolean            motionEngaged;
      private   boolean            placementRequested;
      private   RsMotionRequest    motionRequest;
      private   int                motionWaypoint;
      private   double             motionTime0;
      private   double             motionTime1;

      private   boolean            collision;  // the motion ends in a collision;
      private   RsWall             collisionWall;
      private   RsBodyPart         collisionPart;


   public SimClient(SimSession session){
      super();
      this.session  = session;
      body          = null;
      if(session.properties.interlockEnabled){
         addInterlock(session);
      }
   }

   public void setBody(RsBody body){

      this.body = body;

      RsBodyPainter [] painter = body.getBodyPainterArray();
      if(painter==null){
         paintBox = null;
      }else{
         paintBox = new SimPaintBox[painter.length];
         for(int i=0; i<painter.length; i++){
            paintBox[i] = new SimPaintBox(painter[i].getID());
            session.addPaintBox(paintBox[i]);
         }
      }
   }

   public SimPaintBox [] getPaintBoxArray(){
      return paintBox;
   }



   public synchronized void queueStopMotionTask(){
      // we queue up a StopMotionTask with a startTime of zero
      // which should cause it to be called before any other
      // note that a motion-halted request will be sent only
      // if motion is engaged.   If a motion-start request/task
      // is queued up, it will be removed without sending a
      // motion halted event.
      SimStopMotionTask s = new SimStopMotionTask(this);
      session.scheduler.add(s);
   }


   @Override
public void  sendMouseClickEvent(RsMouseClickEvent mce){
      if(isClientSubscribedToEvent(RsEvent.EVT_MOUSE_CLICK, 0))
         super.sendMouseClickEvent(mce);
   }

   @Override
public void  sendMotionHaltedEvent(RsMotionHaltedEvent event){
      if(isClientSubscribedToEvent(RsEvent.EVT_MOTION_HALTED, 0))
         super.sendMotionHaltedEvent(event);
   }

   @Override
public void  sendMotionStartedEvent(RsMotionStartedEvent event){
      if(isClientSubscribedToEvent(RsEvent.EVT_MOTION_STARTED, 0))
         super.sendMotionStartedEvent(event);
   }

   public void startHeartbeat(double interval){
       if(interval==0){
          // an interval of zero stops the heartbeat
          return;
       }
       heartbeatTask           = new SimHeartbeatTask(this, interval);
       heartbeatTask.startTime = session.scheduler.getUpdatedSimTime()+interval;
       heartbeatTask.enable    = true;
       session.scheduler.add(heartbeatTask);
   }

   public void stopHeartbeat(){
     if(heartbeatTask !=null){
         heartbeatTask.enable=false;
         session.scheduler.remove(heartbeatTask);
         heartbeatTask=null;
      }
   }

   public void removeStartMotionTask(){
      if(startMotionTask!=null){
         session.scheduler.remove(startMotionTask);
         startMotionTask = null;
      }
   }


   private void applyMotionToPaintBoxes(RsMotion motion){
      if(paintBox!=null){
         for(int i=0; i<paintBox.length; i++)
            paintBox[i].bufferTrailerPosition(motion);
      }
   }

   public double processMotion(double taskTime) {

      RsMotion            motion;
      RsBodyPart       [] bodyPart;

      RsBodyContactSensor contact;
      RsSensorEvent       sEvent;
      double              duration;

      motion = body.getMotion();

      motion.setTimeForward(taskTime);
      body.applyMotion(); // see "TO DO" in RsBody

      applyMotionToPaintBoxes(motion);


      bodyPart = body.getBodyPartArray();


      // if there's a request, set up the motion.  remember that
      // one motion flows into another.

      if(motionRequest!=null){
         motionWaypoint=0;
         body.computeMotion(taskTime, motionRequest);
         body.setCollision(false);
         motion = body.getMotion();
         session.verbose("got motion "+motion);
         processCollision(session.getPlan(), body, motion);
         session.verbose("processed collision ");
         if(motion.collision){
            body.setCollision(true);
            session.verbose("Crash in "+motion.collisionTime+" sec.\n\t\tPart :"
                 + collisionPart.getName()+ ",  Wall: "+collisionWall.getName());
            if(motion.collisionTime==0){
               body.setMotion(new RsMotionNull(motion, taskTime));
               motion = body.getMotion();
            }else
               motion.truncateForCollision();
         }else{
            session.verbose("Initiating motion (v,omega,time): "
                                  +motion.getVelocity()+", "
                                  +motion.getTurnRate()+", "
                                  +(motion.time1-motion.time0));
         }

         duration = motion.time1-motion.time0;
         if(duration>0){
            sendMotionStartedEvent(
               new RsMotionStartedEvent(
                  motion.time0,
                  motionRequest.linearVelocity,
                  motionRequest.rotationalVelocity,
                  duration,
                  motion.transform.m13,
                  motion.transform.m23,
                  motion.getOrientation()
               )
            );
         }

         motionRequest=null;   // we're done with it, null the reference to enable collection.
         motionEngaged=true;
         motionTime0 = System.currentTimeMillis()/1000.0;


         // go through the RsBodyContact sensors.  if we have
         // any that are hot, and should go to cold due to the movement,
         // we need to issue new sensor events

         for(int j=0; j<bodyPart.length; j++){
            if(bodyPart[j] instanceof RsBodyContactSensor){
               contact = (RsBodyContactSensor)bodyPart[j];
               if(contact.getHot()){
                 if(!contact.getCollision() || contact.getCollisionTime()>0){
                     contact.setHot(false);
                     // this is a state change, issue the event
                     if(isClientSubscribedToEvent(RsEvent.EVT_CONTACT_SENSOR, contact.getID())){
                        contact.sendSensorEvent(this, motion.time0);
                     }
                 }
               }
            }
         }
      }



      /* perform en-route processing of sensors.   note that we don't
         process the contact sensors because they are a special case...
         they get set after the motion is completed (because this is
         the only time that they SHOULD change).   */

      boolean  sChange;
      sChange = body.processSensors(taskTime, session.getPlan(), motion.transform);
      if(sChange){
         RsBodyPart  [] part = body.getBodyPartArray();
         RsBodySensor   sensor;
         for(int i=0; i<part.length; i++){
            if(part[i] instanceof RsBodySensor && !(part[i] instanceof RsBodyContactSensor)){
               sensor = (RsBodySensor)part[i];
               if(!sensor.didStateChange())
                   continue;
               if(sensor instanceof RsBodyTargetSensor){
                  if(isClientSubscribedToEvent(
                     RsEvent.EVT_TARGET_SENSOR, sensor.getID()))
                  {
                     sEvent = sensor.getSensorEvent(taskTime);
                     sendTargetSensorEvent((RsTargetSensorEvent)sEvent);
                  }
               }else if(sensor instanceof RsBodyRangeSensor){
                  if(isClientSubscribedToEvent(
                     RsEvent.EVT_RANGE_SENSOR, sensor.getID()))
                  {
                     sEvent = sensor.getSensorEvent(taskTime);
                     sendRangeSensorEvent((RsRangeSensorEvent)sEvent);
                  }
               }else if(sensor instanceof RsBodyPaintSensor){
                  if(isClientSubscribedToEvent(
                     RsEvent.EVT_PAINT_SENSOR, sensor.getID()))
                  {
                     sEvent = sensor.getSensorEvent(taskTime);
                     sendPaintSensorEvent((RsPaintSensorEvent)sEvent);
                  }
               }
            }
         }
      }


      motionWaypoint++;

      /* the robot has been moved and, if the GUI is enabled, it's time to plot...
         but we might not be completely ready to do so because we haven't
         processed the contact sensors.   In theory, the contact sensors are
         activated only when we collide, so we don't normally process them
         until the final end-of-motion logic below.   So we have to make
         calls to postMotionEvent in two places to ensure that we
         have the sensors in the correct state when we post. */


      if(motion.time<motion.time1){
         //  schedule next time we evaluate the motion.
         //  TO DO:
         //  right now, we always use the modelingFrameInterval
         //  except when the motion is nearly complete, in which
         //  case we use a shorter time.
         //  eventually, replace this hard-wired time gap
         //  with a distance-traveled value...  for example, if
         //  we have very fast moving bodies, then we wish to evaluate
         //  motion more often.

         double nextTime=motion.time+session.modelingFrameInterval;
         if(nextTime >motion.time1)
            nextTime=motion.time1;
         return nextTime;
      }

      // if we get here, we've finished the movement.

      motionTime1 = System.currentTimeMillis()/1000.0;

      if(motion.collision){
         for(int j=0; j<bodyPart.length; j++){
            if(bodyPart[j] instanceof RsBodyContactSensor){
               contact = (RsBodyContactSensor)bodyPart[j];;
               if(contact.getCollision() && !contact.getHot()){
                  // this is a state change, issue the event
                  contact.setHot(true);
                  if(isClientSubscribedToEvent(
                       RsEvent.EVT_CONTACT_SENSOR,
                       contact.getID()))
                  {
                     contact.sendSensorEvent(this, motion.time1);
                  }
               }
            }
         }
      }

      if(!placementRequested){
        // a placement would have resulted in a null motion
         duration = motion.time1-motion.time0;
         if(motion.collision){
            session.verbose("Motion truncated by collision in "
                  +motionWaypoint+" segments, real sec: "+(motionTime1-motionTime0)+
                  ", sim sec.  "+duration);
         }else{
            session.verbose("Motion completed successfully in "
                  +motionWaypoint+" segments, real sec: "+(motionTime1-motionTime0)+
                  ", sim sec.  "+duration);
         }


         int terminationStatus;
         if(motion.collision){
            terminationStatus=RsMotionHaltedEvent.HALTED_ON_COLLISION;
            body.setCollision(true);
         }else{
            terminationStatus = RsMotionHaltedEvent.HALTED_ON_COMPLETION;
         }

         RsMotionHaltedEvent event = new RsMotionHaltedEvent(
            taskTime,
            terminationStatus,
            motion.transform.m13,
            motion.transform.m23,
            motion.getOrientation(),
            duration
         );

         sendMotionHaltedEvent(event);
      }

      motionEngaged=false;
      placementRequested=false;

      return 0;
   }




   private void
   processCollision(RsPlan plan, RsBody body, RsMotion motion){


      int            i, j;
      RsWall         wall;
      RsBodyPart  [] bodyPart;
      RsBodyShape [] bodyShape;
      RsBodyContactSensor contact;
      RsSegment[] sa;
      RsSegment[] wa;

      bodyPart  = body.getBodyPartArray();
      bodyShape = body.getInteractiveBodyShapeArray();
     /** An unresolved issue

        we have the bounding box of the robot and we know that
        if the box doesn't collide with anything, then the
        robot won't either and we can complete the entire motion
        without futher testing.  The box consists of only 4 segments, so we
        can process it faster than the robot, which contains many
        segments.  Simple enough.

        Except that it doesn't seem to work.  Testing the
        idea with the random walk (drunk) application, the
        robot eventually ends up "in the wall."   I haven't
        yet figured out why.  */

      collision=false;

      RsObject   [] objectArray = plan.getObjectArray();
      if(objectArray==null)
         return;

      for(j=0; j<bodyPart.length; j++){
         if(bodyPart[j] instanceof RsBodyContactSensor){
            contact = (RsBodyContactSensor)bodyPart[j];
            contact.setCollision(false);
            sa = contact.getSegmentArray();
            for(i=0; i<objectArray.length; i++){
               if(objectArray[i] instanceof RsWall){
                  wall = (RsWall)objectArray[i];
                  wa = wall.getSegmentArray();
                  if(motion.processCollision(sa, sa.length, wa, wa.length)){
                       collision=true;
                       collisionWall=wall;
                       collisionPart=bodyPart[j];
                       contact.setCollision(true, wall.getName());
                       contact.setCollisionTime(motion.collisionTime);
                       // no early quit here...   even if collisionTime==0,
                       // we still have to do other sensors
                  }
               }
            }
         }
      }

      for(j=0; j<bodyPart.length; j++){
         if(bodyPart[j] instanceof RsBodyContactSensor){
            contact = (RsBodyContactSensor)bodyPart[j];
            if(contact.getCollision() && contact.getCollisionTime()>motion.collisionTime)
                contact.setCollision(false);
         }
      }

      if(collision && motion.collisionTime==0)
         return;  // quit early


      // TO DO:  I suspect that this loop may result in redundant
      //         processing with the logic above for contact sensors.
      //         when there's time,  see if we can skip
      //         any body shapes that are contact sensors.

      for(i=0; i<objectArray.length; i++){
         if(objectArray[i] instanceof RsWall){
            wall = (RsWall)objectArray[i];
            wa = wall.getSegmentArray();
            for(j=0; j<bodyShape.length; j++){
               sa = bodyShape[j].getSegmentArray();
               if(motion.processCollision(sa, sa.length, wa, wa.length)){
                    collision=true;
                    collisionWall=wall;
                    collisionPart=bodyPart[j];
                    if(motion.collisionTime==0)
                        return;  // quit early
               }
            }
         }
      }
   }

   public boolean isMotionProcessingRequired(){
      return motionEngaged || (motionRequest!=null) || placementRequested;
   }

   public void setMotionRequest(RsMotionRequest motionRequest){
      this.motionRequest = motionRequest;
   }


   public void setPlacementRequested(boolean status){
      placementRequested=status;  // probably always true?
   }

   public boolean isMotionEngaged(){
      return motionEngaged;
   }

   public void setStartMotionTask(SimStartMotionTask task){
      startMotionTask = task;
   }

   public SimStartMotionTask getStartMotionTask(){
      return startMotionTask;
   }

   public void stopMotionFromTask(){

      if(startMotionTask!=null){
         session.scheduler.remove(startMotionTask);
         startMotionTask=null;
      }
      motionRequest=null;

      if(motionEngaged){
         motionEngaged=false;
         double startTime = session.scheduler.getUpdatedSimTime();
         RsMotion motion = body.getMotion();
         motion.stopMotionAtTime(startTime);

         RsPoint p = motion.getPosition();
         RsMotionHaltedEvent event = new RsMotionHaltedEvent(
            startTime,
            RsMotionHaltedEvent.HALTED_ON_REQUEST,
               p.x,
               p.y,
               motion.getOrientation(),
               startTime-motion.time0
            );

         sendMotionHaltedEvent(event);
      }
   }
}
