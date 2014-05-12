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

/*

Note that this task will put itself back on the queue as
part of its process.  Because it is the heartbeat operation is
repetitive, we wish to preserve and recycle the object rather
than creating a new one each time a new heartbeat is scheduled.

*/


package rp1.simulator;
import rp1.rossum.event.*;





/**
 * A task for generating heartbeats in response to client requests
 * (one per client).
 */

public class SimHeartbeatTask extends SimTask {

   public SimHeartbeatTask(SimClient clientRef, double interval){
      super();
      this.client     = clientRef;
      this.originator = client;
      this.sequence   = 0;
      this.priority   = 0;
      this.interval   = interval;
   }

   @Override
public void process() {
      sequence++;
      client.sendHeartbeatEvent(new RsHeartbeatEvent(startTime, sequence));
      if(enable){
         startTime+=interval;
         client.session.scheduler.add(this);
      }
   }


   protected SimClient  client;
   private   double     interval;
   private   int        sequence;
   protected boolean    enable;
}


