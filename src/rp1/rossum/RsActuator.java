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


package rp1.rossum;


/*
  Note: This class is in transition. Eventually, it will be the
  base class for all actators include

  RsWheelSystem
      RsDifferentialSteering
      RsAckerman
      RsOmniDrive
      RsSkidSteer

  RsPivot
  RsSlide  (a linear translation stage or similar actuator)

*/

import java.awt.Graphics;
import java.util.ArrayList;



/**
 * The abstract, base class for all actuator implementations.
 *
 */

public class RsActuator extends RsBodyPart {

	private static final long serialVersionUID = 7927693651821514155L;
	
	protected RsBodyPart           [] partsArray;
	protected ArrayList  <RsBodyPart> partsList;

   public RsActuator(){
       super();
       name      = "Actuator";
       partsList = new ArrayList<RsBodyPart>();
   }

   public RsBodyPart [] getPartsArray(){
      if(partsArray==null){
        if(partsList.size()>0){
            partsArray = (RsBodyPart [])partsList.toArray(new RsBodyPart[partsList.size()]);
        }
      }
      return partsArray;
   }

   @Override
public void paint(Graphics g, RsTransform gt){
     RsBodyPart [] p = getPartsArray();
     if(p!=null){
        for(int i=0; i<p.length; i++)
            p[i].paint(g, gt);
     }
   }
}

