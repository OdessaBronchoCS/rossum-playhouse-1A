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


/* The selection of placements is as follows:

    Recall that all placements must position the robot
    so that it does not touch or overlap any walls.
    the isPlacementClear() method is intended to decide
    if a placement if valid.  It is not implemented
    at this time.

    Selecting a placement:

      If the user supplies the special name "$random",
      select a random X/Y coordinate and create a placement.
      Test the placement to see if it is valid.  If not
      repeat the randomization up to 50 times to
      attempt to find a good placement.

      If the client provides a null placement name, select
      randomly from the available placements.  Test the
      placement to see if it is valid.  If not, randomly
      select from the other placements.  Repeat until we
      have found a good placement or exhausted all
      possibilities.

      If the client provides a name, search for a matching
      placement.  If no match is found, return an "invalid"
      placement to the client application.  If one is
      found, test it to see if it is valid.

      In all cases, if no valid placement is found,
      the client is sent a "invalid placement" message.
*/




package rp1.simulator;
import rp1.rossum.*;
import rp1.rossum.event.RsPlacementEvent;
import rp1.rossum.request.RsPlacementRequest;
import java.lang.Math;




public class SimPlacementTask extends SimTask {


   private   SimClient          client;
   private   RsPlacementRequest request;


   public SimPlacementTask(SimClient client, RsPlacementRequest request){
      super();
      this.client  = client;
      this.request = request;
      originator   = client;
   }


   @Override
public void process() {

      RsObject      [] objectArray;

      RsPlacementEvent pm=null;

      String targetName;
      if(request.name==null || request.name.length()==0)
         targetName = "";
      else
         targetName = request.name;

      objectArray = client.session.getPlan().getObjectArray();

      if(objectArray==null || objectArray.length==0){
         // should never happen
         pm = new RsPlacementEvent(startTime, false, null,  0.0, 0.0, 0.0);
         client.sendPlacementEvent(pm);
         return;
      }

      // now apply the various placement methods

      pm        = null;
      if(request.name!=null && request.name.equals("$random")){
         // the user requested a random x/y/orientation
         // we will try a maximum of 50 times to find one
         // that works.
         RsRectangle rect = client.session.getPlan().getBounds();
         double      x, y, theta;
         for(int i=0; i<50; i++){
            x     = rect.x+Math.random()*rect.width;
            y     = rect.y+Math.random()*rect.height;
            theta = Math.random()*2*Math.PI;
            if(isPlacementClear(x, y, theta)){
               pm = new RsPlacementEvent(
                     startTime, true,
                     "$random", x, y, theta);
               break;
            }
         }
      }else{
         // the remaining methods all work on the basis of
         // placements specified in the floor plan,
         // and will need a list of those placements

         RsObject       obj;
         RsPlacement [] pArray;
         RsPlacement    placement=null;
         int            nPlacement;


         nPlacement = 0;
         pArray = new RsPlacement[objectArray.length];

         for(int iObject = 0; iObject<objectArray.length; iObject++){
            obj = objectArray[iObject];
            if(obj instanceof RsPlacement)
               pArray[nPlacement++] = (RsPlacement)obj;
         }

         // ----------------------------------------------
         if(request.name==null || request.name.length()==0){
            // the user gave us a null name.  randomly from
            // our list of placements (note that this algorithm
            // consumes the list as it goes).
            int iPlacement;
            while(nPlacement>0){
               iPlacement = (int)(Math.random()*nPlacement);
               placement  = pArray[iPlacement];
               if(isPlacementClear(placement.x, placement.y, placement.orientation)){
                  pm = new RsPlacementEvent(
                              startTime, true,
                              placement.getName(),
                              placement.x,
                              placement.y,
                              placement.orientation);
                  break;
               } else {
                  // the placement didn't work.  remove it
                  // from the list and try again.
                  pArray[iPlacement] = pArray[nPlacement-1];
                  nPlacement--;
               }
            }
        }else {
          // the client supplied a desired placement name
          for(int i=0; i<nPlacement; i++){
            placement = pArray[i];
            if(request.name.equals(placement.getName())){
              if(isPlacementClear(placement.x, placement.y, placement.orientation)){
                  pm = new RsPlacementEvent(
                              startTime, true,
                              placement.getName(),
                              placement.x,
                              placement.y,
                              placement.orientation);
                  break;
              }
            }
          }
        }
      }

      if(pm==null){
        // we failed to locate a placement
        client.session.queueAnimationEvent();
        pm = new RsPlacementEvent(startTime, false, targetName,  0.0, 0.0, 0.0);
      }else{
        // we have a valid placement
         client.body.resetStateData();
         client.body.setPlacement(true);
         client.body.motion = new RsMotionNull(startTime, pm.orientation, pm.x, pm.y);

         // even though the motion is null, we need to queue a motion
         // task to ensure that all the sensors are processed correctly.
         // and that the client is depicted in its proper position
         client.setPlacementRequested(true);
         client.session.queueMotionTask();
      }

      client.sendPlacementEvent(pm);
   }

   private boolean isPlacementClear(double x, double y, double theta){

      RsTransform transform = new RsTransform(theta, x, y);
      RsPlan plan = client.session.getPlan();
      return !client.body.checkForOverlap(plan, transform);
   }
}
