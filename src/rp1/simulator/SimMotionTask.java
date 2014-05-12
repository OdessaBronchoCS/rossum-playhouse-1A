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



/**
 * A task for scheduling and managing <i>all</i> client motion
 * computations; this is a self-recycling task and only one is
 * allowed on the task queue at any time.
 */

public class SimMotionTask extends SimTask {

   private SimSession session;
   private boolean    enableAnimation;
   private double     timeOfLastAnimationFrame;
   private double     simSpeed;

   public SimMotionTask(SimSession session){
      super();
      this.session = session;
      originator   = session;
      enableAnimation          = false;
      timeOfLastAnimationFrame = 0;
      simSpeed = 1.0;
   }

   public void setSimSpeed(double simSpeed){
      this.simSpeed=simSpeed;
   }

   @Override
public void process() {

      double  minNextTime, nextTime;

      //  session.verbose("SimMotionTask at time "+startTime);
      if(session.clients == null){
         return;  // should never happen
      }

      // to do... we need to process new placements first, because
      // they get priority...  if multiple robots are in motion,
      // a pre-existing robot could move into a requested placement
      // we have to ensure that the newly placed robot is there
      // already so that the collision is detected.

      minNextTime=0;
      for(int i=0; i<session.clients.length; i++){
         if(session.clients[i].isMotionProcessingRequired()){
            enableAnimation = true;
            nextTime = session.clients[i].processMotion(startTime);
            if(nextTime>0){
               if(minNextTime==0 || nextTime<minNextTime)
                  minNextTime = nextTime;
            }
         }
      }

      if(enableAnimation && session.simFrame!=null){
         double realTime         = System.currentTimeMillis()/1000.0;
         double deltaFrameInterval = realTime - timeOfLastAnimationFrame;
         if(deltaFrameInterval >= session.animationFrameInterval){
            timeOfLastAnimationFrame = realTime;
            enableAnimation = false;
            session.queueAnimationEvent();
         }else{
            // this.startTime should be very close to the updated simulation time
            nextTime = this.startTime+(session.animationFrameInterval-deltaFrameInterval)*simSpeed;
            if(nextTime<1)
               nextTime = 1;
            if(minNextTime==0 || nextTime<minNextTime)
                minNextTime = nextTime;
         }
      }

      if(minNextTime>0){
         // recycle this task back onto the queue.
         this.startTime= minNextTime;
         session.scheduler.add(this);
      }
   }
}
