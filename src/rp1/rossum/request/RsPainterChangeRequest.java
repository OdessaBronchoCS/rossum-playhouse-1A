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


// TO DO:  expand this to handle the missing data fields


package rp1.rossum.request;

import java.awt.Color;



/**
 * A request for a change in the status of a RsBodyPainter.
 *
 */

public class RsPainterChangeRequest extends RsRequest {

   /**
	 * 
	 */
	private static final long serialVersionUID = -7179520364805801817L;
public final int      painterID;
   public final boolean  erasure;
   public final boolean  activation;
   public final boolean  trailerDefined;
   public final double   xTrailer;
   public final double   yTrailer;
   public final double   wTrailer;
   public final Color [] color;

   public RsPainterChangeRequest(
      int        painterID,
      boolean    erasure,
      boolean    activation,
      boolean    trailerDefined,
      double     xTrailer,
      double     yTrailer,
      double     wTrailer,
      Color []   color)
   {
      super(REQ_PAINTER_CHANGE);
      this.painterID  = painterID;
      this.erasure    = erasure;
      this.activation = activation;
      if(activation){
         this.trailerDefined          = trailerDefined;
         this.xTrailer                = xTrailer;
         this.yTrailer                = yTrailer;
         this.wTrailer                = wTrailer;
         this.color                   = color;
      }else{
         this.trailerDefined          = false;
         this.xTrailer = 0;
         this.yTrailer = 0;
         this.wTrailer = 0;
         this.color    = null;
      }
   }
}

