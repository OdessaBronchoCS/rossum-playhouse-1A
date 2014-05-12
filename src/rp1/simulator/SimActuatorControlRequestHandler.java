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


/* this is a transitional implementation.  right now it just assumes
   that the only source of an actuator request is a diff steering system
   and it translates the actuator request into a motion request by assuming
   an arbitrary period of time for the request.  We will replace this real soon now.
*/



package rp1.simulator;
import rp1.rossum.*;
import rp1.rossum.request.*;




/**
 * Handles specific client requests.
 */

public class SimActuatorControlRequestHandler implements RsActuatorControlRequestHandler {

   public SimActuatorControlRequestHandler(SimClient client){
      this.client = client;
   }

   public void processRequest(RsRequest t){
      process((RsActuatorControlRequest)t);
   }

   public void process(RsActuatorControlRequest request){

      if(!client.body.getPlacement())
         return;

      SimSession         session   = client.session;
      SimScheduler       scheduler = session.scheduler;
      SimStartMotionTask task;

      String [] names;
      double [] values;

      names  = request.getParameterNames();
      values = request.getParameterValues();
      if(names==null)
         return;

      RsBodyPart part = client.body.getPartByID(request.getBodyPartID());

      if(part instanceof RsDifferentialSteering){
         RsDifferentialSteering ds = (RsDifferentialSteering)part;
         RsMotionRequest mr = ds.getMotionRequestUsingWheelRotationalVelocities(values[0], values[1], 300000);

         // if any start-motion tasks are already queued, remove them
         client.removeStartMotionTask();

         double simTime = scheduler.getUpdatedSimTime();
         task           = new SimStartMotionTask(client, mr);
         task.setStartTime(simTime);
         client.setStartMotionTask(task);
         scheduler.add(task);
      }else if(part instanceof RsAckermanSteering){
         RsAckermanSteering steering = (RsAckermanSteering)part;
         steering.setSteeringAngle(values[1]);
         RsMotionRequest mr =
            steering.getMotionRequestUsingWheelRotationalVelocityAndSteeringAngle(
                        values[0], values[1], 300000);

         double simTime = scheduler.getUpdatedSimTime();
         task           = new SimStartMotionTask(client, mr);
         task.setStartTime(simTime);
         client.setStartMotionTask(task);
         scheduler.add(task);

         //session.queueAnimationEvent();

      }

   }

   private SimClient  client;
}
