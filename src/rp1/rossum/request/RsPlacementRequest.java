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


package rp1.rossum.request;


// note that name IS allowed to be null
// a null name tells the simulator to randomly choose a placement



/**
 * A request for placement.
 *
 */

public class RsPlacementRequest extends RsRequest {

   /**
	 * 
	 */
	private static final long serialVersionUID = -1508131319031357593L;

public RsPlacementRequest(String name){
      super(REQ_PLACEMENT);
      this.name = name;
   }

   public final String name;
}

