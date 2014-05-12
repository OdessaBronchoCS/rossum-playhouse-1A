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




/**
 * Handles specific client requests.
 */

public class SimTargetSelectionRequestHandler implements RsTargetSelectionRequestHandler {

   public SimTargetSelectionRequestHandler(SimClient clientRef){
      client    = clientRef;
      verbosity = clientRef.session.getVerbosity();
   }

   public void processRequest(RsRequest t){
      process((RsTargetSelectionRequest)t);
   }

   public void process(RsTargetSelectionRequest request){

      SimSession   session = client.session;
      SimScheduler scheduler = session.scheduler;

      RsPlan  plan    = session.getPlan();
      double  simTime = scheduler.getUpdatedSimTime();


      RsTargetSelectionEvent event[] = plan.processTargetSelectionRequest(simTime, request);

      int n;
      if(event==null)
         n=0;
      else
         n=event.length;

      for(int i=0; i<n; i++)
         client.sendTargetSelectionEvent(event[i]);

      if(verbosity)
         session.verbose("Target Selection Request modified "+n+" targets");

      session.queueRepaintEvent();
   }

   private SimClient  client;
   private boolean    verbosity;
}
