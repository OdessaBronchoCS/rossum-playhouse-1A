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
import rp1.rossum.event.*;





/**
 * A task queued by SimClient in response to a RsTimeoutRequest.
 */

public class SimTimeoutTask extends SimTask {

   public SimTimeoutTask(SimClient client, int timeoutIndex){
      super();
      this.client       = client;
      this.originator   = client;
      this.timeoutIndex = timeoutIndex;
   }

   @Override
public void process() {
         client.sendTimeoutEvent(new RsTimeoutEvent(startTime, timeoutIndex));
   }

   SimClient         client;
   public final int  timeoutIndex;
}


