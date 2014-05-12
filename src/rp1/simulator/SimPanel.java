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



import java.awt.*;



/**
 * A panel in the main RP1 GUI.
 */

public class SimPanel extends Panel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = -2883248359757121025L;
public SimPanel(RsPlan planReference){
      plan = planReference;

      xText = new TextField(14);
      yText = new TextField(14);
      xText.setEditable(false);
      yText.setEditable(false);

      GridBagLayout      gbl      = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      setLayout(gbl);

      gbc.anchor=GridBagConstraints.NORTHWEST;
      gbc.fill=GridBagConstraints.NONE;
      gbc.gridx=0;
      gbc.gridy=0;
      gbc.weightx=0.0;
      gbc.weighty=0.0;
      gbl.setConstraints(xText, gbc);
      add(xText);

      gbc.fill=GridBagConstraints.REMAINDER;
      gbc.gridx=1;
      gbc.gridy=0;
      gbc.weightx=10.0;
      gbc.weighty=0.0;
      gbl.setConstraints(yText, gbc);
      add(yText);

   }

   protected void updatePositionText(double internalX, double internalY){
      RsUnits units = plan.getUnits();
      double x,y;
      x=units.internalToUser(internalX);
      y=units.internalToUser(internalY);
      xText.setText("x: "+units.format(x));
      yText.setText("y: "+units.format(y));
   }


   protected RsPlan    plan;
   protected TextField xText, yText;
}



