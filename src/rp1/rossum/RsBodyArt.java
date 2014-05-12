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

/*   RsBodyArt

An object of class RsBodyArt is simply used for depiction purposes,
but does not interact with its environment.   It is coupled to the body
(or assembly) and undergoes the same transform() operation.  It is defined
as a series of points and can act as either a line or as polygon depending
on whether the "fillColor" is set to non-null.

*/

import java.awt.Color;



/**
 * A class for composing non-interactive, render-only body parts.
 *
 */

public class RsBodyArt extends RsBodyPart {

   /**
	 * 
	 */
	private static final long serialVersionUID = 8771377407496656622L;

public RsBodyArt(double [] pointList, int nPointList){
       super();
       fillColor=null;
       lineColor=null;
       refPoint = new RsPoint[nPointList];
       for(int i=0; i<nPointList; i++){
           refPoint[i] = new RsPoint(pointList[i*2], pointList[i*2+1]);
           point[i]    = new RsPoint(pointList[i*2], pointList[i*2+1]);
       }

       refBounds = new RsRectangle(refPoint, refPoint.length);
       bounds    = new RsRectangle(refBounds);
   }

   public void transform(double sinTheta, double cosTheta, double xOffset, double yOffset){}

   RsPoint [] refPoint;
   RsPoint [] point;

   Color      fillColor;
}

