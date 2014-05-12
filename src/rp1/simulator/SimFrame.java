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
import java.awt.event.*;

/**
 * The main frame for the RP1 GUI.
 */
public class SimFrame extends Frame {

    /**
     *
     */
    private static final long serialVersionUID = 90922544456691867L;
    public SimSession session;
    public SimMenuBar menuBar;
    public SimCanvas canvas;
    public SimPanel panel;
    public RsPlan plan;

    public SimFrame(SimSession sessionReference) {

        session = sessionReference;
        plan = session.getPlan();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                session.terminate();
            }
        });


        RsRectangle r = plan.getBounds();
        int width, height;

        if (r == null) {
            return;
        }

        width = 500;
        height = 400;
        setSize(width, height);

        String s = plan.getCaption();
        if (s == null) {
            setTitle("Rossum's Playhouse -- Untitled Plan");
        } else {
            setTitle(s);
        }

        setBackground(Color.lightGray);


        // build components for inclusion -----------------------
        menuBar = new SimMenuBar(this);
        canvas = new SimCanvas(session);
        panel = new SimPanel(plan);

        // add components to frame -------------------------
        setMenuBar(menuBar);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gbl);

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 100.0;
        gbc.weighty = 100.0;
        gbl.setConstraints(canvas, gbc);
        add(canvas);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbl.setConstraints(panel, gbc);
        add(panel);

        canvas.addMouseListener(new SimMouseListener(this));
        canvas.addMouseMotionListener(new SimMouseListener(this));
    }

    protected void setUnits(String s) {
        RsUnits units = null;
        switch (s) {
            case "Inches":
                units = new RsInches();
                break;
            case "Feet":
                units = new RsFeet();
                break;
            case "Meters":
                units = new RsMeters();
                break;
        }
        plan.setUnits(units);


        if (menuBar != null) {
            menuBar.setUnits(s);
        }
        // To Do: Right now, this module is only called as a response
        // to the units-selection under the View menu in the menu bar.
        // To click on view, the user must move the mouse off the canvas,
        // thus clearing the X/Y position associated with the mouse.  Thus
        // when the setUnits() method is invoked there is no need to
        // update the text display (a good thing too, because it would
        // be a bit difficult to do so).    If this assumption becomes
        // invalid, we will have to address the problem.
    }
}
