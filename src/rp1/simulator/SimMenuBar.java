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

/* Things To Do:  There are lot's of them...  In the short term, I want to
   add a menu button under "View" to activate/deactive the depiction
   of the nav-net (already coded in SimCanvas) and to draw a track
   behind the robot simulacrum as it moves (no coded).
*/

package rp1.simulator;

import java.awt.*;
import java.awt.event.*;




/**
 * The main menu bar for the RP1 GUI.
 */

public class SimMenuBar extends MenuBar
{
   /**
	 * 
	 */
	private static final long serialVersionUID = -8653547113119516544L;
public SimMenuBar(SimFrame frame){

      super();

      this.frame = frame;
      add(createFileMenu());
      add(createViewMenu());
      add(createActionsMenu());

      setUnits(frame.plan.getUnits().getName());
   }

   private Menu createFileMenu(){
      Menu     fileMenu = new Menu("File");
      MenuItem exitItem = new MenuItem("Exit");
      fileMenu.add(exitItem);
      exitItem.addActionListener(
         new ActionListener(){
            public void actionPerformed(ActionEvent event){
               frame.session.terminate();
            }
         }
      );
      return fileMenu;
   }

   private Menu createViewMenu(){
      Menu viewMenu  = new Menu("View");
      Menu unitsMenu = new Menu("Units");
      CheckboxItemListener c = new CheckboxItemListener();

      unitsMenu.add(inchesItem = createUnitsItem("Inches", c));
      unitsMenu.add(feetItem   = createUnitsItem("Feet",   c));
      unitsMenu.add(metersItem = createUnitsItem("Meters", c));
      viewMenu.add(unitsMenu);
      return viewMenu;
   }

   private CheckboxMenuItem createUnitsItem(String s, CheckboxItemListener c){
      CheckboxMenuItem item = new CheckboxMenuItem(s);
      item.setState(false);
      item.addItemListener(c);
      return item;
   }

   private void radioMenuSelectItem(MenuItem item){
      CheckboxMenuItem testItem;
      Menu             parent=(Menu)(item.getParent());
      int              nItem=parent.getItemCount();

      for(int i=0; i<nItem; i++){
         testItem=(CheckboxMenuItem)(parent.getItem(i));
         testItem.setState(item==testItem);
      }
   }

   protected void setUnits(String s){
      if(s.equals("Inches")){
         inchesItem.setState(true);
         feetItem.setState(  false);
         metersItem.setState(false);
      }else if(s.equals("Feet")){
         inchesItem.setState(false);
         feetItem.setState(  true);
         metersItem.setState(false);
      }else if(s.equals("Meters")){
         inchesItem.setState(false);
         feetItem.setState(  false);
         metersItem.setState(true);
      }
   }

   class CheckboxItemListener implements ItemListener {
      public void itemStateChanged(ItemEvent event){
         radioMenuSelectItem((MenuItem)(event.getItemSelectable()));
         String s = (String)(event.getItem());
         frame.setUnits(s);
      }
   }


   private Menu createActionsMenu(){
      Menu     actionsMenu = new Menu("Actions");
      MenuItem removePaintItem = new MenuItem("Remove Paint Trails");
      actionsMenu.add(removePaintItem);
      removePaintItem.addActionListener(
         new ActionListener(){
            public void actionPerformed(ActionEvent event){
               frame.session.scheduler.add(new SimClearAllPaintBoxesTask(frame.session));
            }
         }
      );
      return actionsMenu;
   }

   protected SimFrame         frame;
   protected CheckboxMenuItem inchesItem;
   protected CheckboxMenuItem feetItem;
   protected CheckboxMenuItem metersItem;

}


