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




/**
 * A request for a subscription to events such as mouse-clicks or
 *heartbeats that are triggered by something other client requests.
 *
 */

public class RsSubscriptionRequest extends RsRequest {

   /**
	 * 
	 */
	private static final long serialVersionUID = 7192907169459698829L;
public RsSubscriptionRequest(int eventCode, int eventIndex, boolean eventEnable){
      super(REQ_SUBSCRIPTION);
      this.eventCode   = eventCode;
      this.eventIndex  = eventIndex;
      this.eventEnable = eventEnable;
   }


   public final int     eventCode;
   public final int     eventIndex;
   public final boolean eventEnable;
}

