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
import rp1.rossum.event.RsMouseClickEvent;

import java.awt.event.*;



/**
 * An AWT listener that monitors mouse clicks.
 */

public class SimMouseListener extends MouseAdapter implements MouseMotionListener
{
   public SimMouseListener(SimFrame frameReference){
      frame = frameReference;
   }

   public void mouseDragged(MouseEvent e){}
   public void mouseMoved(MouseEvent e){
      RsPoint p = frame.canvas.inverseMap(e.getX(), e.getY());
      frame.panel.updatePositionText(p.x, p.y);
   }

   @Override
public void mouseExited(MouseEvent e){
      frame.panel.xText.setText("");
      frame.panel.yText.setText("");
   }

   @Override
public void mousePressed(MouseEvent e){
      SimSession session = frame.session;
      int mask = e.getModifiers();
      int button;
      // note that all buttons are mapped as if we were dealing with
      // a two button mouse.   On systems with 3 buttons (X-Windows)
      // the right two buttons are mapped to the same value.
      // On the one-button Macintosh, using the ALT key causes
      // the application to interpret the key press as if it were
      // the right button.

      if((mask&InputEvent.BUTTON1_MASK)==InputEvent.BUTTON1_MASK){
         button=0;
         if((mask & InputEvent.ALT_MASK) == InputEvent.ALT_MASK)
            button=1;
         else if ((mask & InputEvent.META_MASK) == InputEvent.META_MASK)
            button=1;
         else
            button = 0;
      }else if((mask&InputEvent.BUTTON2_MASK)==InputEvent.BUTTON2_MASK)
         button=1;
      else if((mask&InputEvent.BUTTON3_MASK)==InputEvent.BUTTON3_MASK)
         button=1;
      else
         button=0;

      RsPoint p = frame.canvas.inverseMap(e.getX(), e.getY());
      // e.consume();

      // send an RsMouseClickEvent to any interested clients.

      RsMouseClickEvent mce = new RsMouseClickEvent(
         session.scheduler.getUpdatedSimTime(),
         button,
         e.getClickCount(),
         p.x, p.y);
      SimMouseClickTask smt = new SimMouseClickTask(session, mce);
      session.scheduler.add(smt);
   }

   protected SimFrame frame;
}



