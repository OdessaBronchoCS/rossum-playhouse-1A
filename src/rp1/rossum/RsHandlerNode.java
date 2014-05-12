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

import rp1.rossum.event.RsEventHandler;

import java.util.Arrays;
import java.lang.Comparable;


/**
 * A class used in the handler registry to store event handlers.
 *
 */

class RsHandlerNode implements Comparable {



   public static void main(String[] args) throws Exception
   {
      RsHandlerNode [] node = new RsHandlerNode[3];
      node[0] = new RsHandlerNode(0, 2, null);
      node[1] = new RsHandlerNode(0, 0, null);
      node[2] = new RsHandlerNode(0, 1, null);
      Arrays.sort(node);
      for(int i=0; i<node.length; i++)
         System.err.println(" "+i+", "+node[i].partID);

      RsHandlerNode key = new RsHandlerNode(0, 3, null);
      int index = Arrays.binarySearch(node, key);
      System.err.println("index: "+index);
   }

   public int compareTo(Object other){
      int test;
      test = this.eventID-((RsHandlerNode)other).eventID;
      if(test==0)
          test = this.partID - ((RsHandlerNode)other).partID;
      return test;
   }


   public RsHandlerNode(int eventID, int partID, RsEventHandler handler){
      this.handler = handler;
      this.eventID = eventID;
      this.partID  = partID;
      this.next    = null;
      this.prior   = null;
   }

   public RsHandlerNode getNext(){
      return next;
   }

   public RsHandlerNode getPrior(){
      // recall that the first node in the list (the root node)
      // is just a place holder and has a null handler.
      // all other nodes have a non-null handler.

      if(prior!=null && prior.handler!=null)
         return prior;
      return null;
   }

   public final RsEventHandler handler;
   public final int            eventID;
   public final int            partID;
   protected    RsHandlerNode  next;
   protected    RsHandlerNode  prior;
}


