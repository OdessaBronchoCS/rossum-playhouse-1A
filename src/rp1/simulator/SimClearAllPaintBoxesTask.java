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



// TO DO:  remove paint boxes not assocated with clients


package rp1.simulator;






/**
 * A task queued when the user selects the clear paint boxes menu item.
 */

public class SimClearAllPaintBoxesTask extends SimTask {

   SimSession session;


   public SimClearAllPaintBoxesTask(SimSession session){
      super();
      this.session = session;
      originator   = null;
   }


   @Override
public void process() {
      SimPaintBox [] paintBoxArray = session.getPaintBoxArray();
      if(paintBoxArray==null)
         return;

      for(int i=0; i<paintBoxArray.length; i++)
          paintBoxArray[i].clear();

      session.queueRepaintEvent();
   }

}
