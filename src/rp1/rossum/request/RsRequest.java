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

import java.io.Serializable;



/**
 * The abstract base class for all requests.
 *
 */



public abstract class RsRequest implements Serializable, Cloneable {


   public static final int REQ_MAX_CODE         = 13;
   public static final int REQ_ARRAY_SIZE       = 14;
   public static final int REQ_TIMEOUT          = 0;
   public static final int REQ_SUBSCRIPTION     = 1;
   public static final int REQ_SENSOR_STATUS    = 2;
   public static final int REQ_MOTION           = 3;
   public static final int REQ_HALT             = 4;
   public static final int REQ_POSITION         = 5;
   public static final int REQ_SET_POSITION     = 6;
   public static final int REQ_PLACEMENT        = 7;
   public static final int REQ_TARGET_SELECTION = 8;
   public static final int REQ_PLAN             = 9;
   public static final int REQ_HEARTBEAT        = 10;
   public static final int REQ_PAINTER_CHANGE   = 11;
   public static final int REQ_ACTUATOR_CONTROL = 12;
   public static final int REQ_ENCODER_STATUS   = 13;



   public final int requestID;

   public RsRequest(int requestID){
      this.requestID = requestID;
   }

   public int getRequestID(){
      return requestID;
   }

   public int getIndex(){
      return 0;
   }

}
