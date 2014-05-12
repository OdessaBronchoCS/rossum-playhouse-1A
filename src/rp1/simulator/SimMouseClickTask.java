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
 * A task queued by SimCanvas in response to a mouse click, ultimately sends
 * a mouse-click event to clients.
 */

public class SimMouseClickTask extends SimTask {

   public SimMouseClickTask(SimSession _session, RsMouseClickEvent _event){
      super();
      session = _session;
      event   = _event;
   }

   @Override
public void process(){
      session.sendMouseClickEvent(event);
   }


   SimSession             session;
   RsMouseClickEvent      event;
}






