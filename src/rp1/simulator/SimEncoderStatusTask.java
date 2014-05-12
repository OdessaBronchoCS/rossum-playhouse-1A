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
import rp1.rossum.event.RsEncoderStatusEvent;
import rp1.rossum.request.RsEncoderStatusRequest;






/**
 * A task queued by a SimClient in response to a RsEncoderStatusRequest.
 */

public class SimEncoderStatusTask extends SimTask {


   private   SimClient              client;
   private   RsEncoderStatusRequest request;


   public SimEncoderStatusTask(SimClient client, RsEncoderStatusRequest request){
      super();
      this.client  = client;
      this.request = request;
      originator   = client;
   }


   @Override
public void process() {
     for(int i=0; i<request.encoderID.length; i++){
         RsWheel w = client.body.wheelSystem.getWheelForID(request.encoderID[i]);
         RsEncoderStatusEvent event =
              w.getEncoderStatusEvent(
                    startTime,
                    request.requestIndex,
                    request.clearOnReport);
         client.sendEncoderStatusEvent(event);
     }
   }
}
