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
import rp1.rossum.request.RsPainterChangeRequest;






/**
 * A task queued by a SimClient in response to a RsPainterChangeRequest.
 */

public class SimPainterChangeTask extends SimTask {


   private   SimClient              client;
   private   RsPainterChangeRequest request;


   public SimPainterChangeTask(SimClient client, RsPainterChangeRequest request){
      super();
      this.client  = client;
      this.request = request;
      originator   = client;
   }


   @Override
public void process() {
      if(client.paintBox==null)
         return;

      SimPaintBox paintBox = null;
      if(client.paintBox!=null){
         for(int i=0; i<client.paintBox.length; i++){
            if(client.paintBox[i].getBodyPartID() == request.painterID){
               paintBox = client.paintBox[i];
               break;
            }
         }
      }

      if(paintBox==null)
         return; // this should NEVER have happend...
                      // if it does, something is probably wrong with the client

      if(request.erasure){
          paintBox.clear();
          client.session.queueRepaintEvent();
          return;
      }

      RsBodyPart part       = client.body.getPartByID(request.painterID);
      RsBodyPainter painter = (RsBodyPainter)part;
      painter.setPainterActivationStatus(request.activation);
      painter.setHot(request.activation);
      paintBox.setActivationProperty(request.activation);
      if(request.activation){
         paintBox.setTrailerProperties(
                             request.xTrailer,
                             request.yTrailer,
                             request.wTrailer);
         paintBox.setColorProperties(request.color);
      }
   }
}
