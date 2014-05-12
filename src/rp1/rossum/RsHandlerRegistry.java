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

/* The Handler Registry Organization

The main element of the registry is RsHandlerNode [][]table which
contains an 2-dimensional array of handler nodes.  The handler nodes
themselves are linked-lists containing a series of event handlers.

In rossum, events are associated with integer ID's in a known
range of values, starting with zero.  Thus, the first array index
into the table is easily established table[eventID][...].

The second table array index is a bit more complicated.
Some event handlers are associated with RsBodyParts, others are
not. RsBodyParts are indexed by a partID. By design, the minimum
body part ID is 1.  So if the application passes in a partID of zero,
it means it is dealing with an event that is not associated with
a body part.  However, it might also pass in a non-zero part ID.
Since this value is completely arbitrary and can be quite large
we cannot simply use partID as the second array index. Instead,
we we treat each row of the table as a list sorted by partID.

To obtain the link-list chain for a particular combination
of eventID and partID, we find the appropriate table row based
on eventID, then do a binary search based on partID to find
a RsHandlerNode.  This node is the first node in a linked list
that contains event handlers for the target eventID/partID.

One other complication...  the first node in the linked list
does not contain a handler. This feature was part of an
earlier implementation in which it simplified things.  Now
it is largely vestigial.  Perhaps in a future release we
can remove it.

The Iterator

More than one handler can be registered for a particular
event. To allow an application to loop through these handlers,
the handler registry acts as a kind of iterator (though
it does not implement the Java Iterator interface).
The iterationNode value always points to the NEXT handler
that is to be executed.  Note that if we remove an
event handler, its value needs to be adjusted.



*/






/**
 * A collections class used to store event handlers.  Note that none
 * of the methods in this class are synchronized, they depend on
 * wrapper methods, such as those in RsClient, to provide synchronization
 * where necessary.
 *
 */

class RsHandlerRegistry {

   private int                  nRow;
   private RsHandlerNode    [][]table;
   private RsHandlerNode        iterationNode;


   public RsHandlerRegistry(int maxNumberOfRows){
      nRow=maxNumberOfRows;

      // to simplify logic in the utility methods, ensure that for each eventID, there is
      // storage for at least one handler... set the contents of that handler to null
      table = new RsHandlerNode[nRow][];
      for(int i=0; i<nRow; i++){
         table[i]=new RsHandlerNode[1];
         table[i][0]=new RsHandlerNode(i, 0, null);
      }

      iterationNode = null; // just a diagnostic.
   }



   private RsHandlerNode searchForNode(int eventID, int partID){
      if(partID==0){
         // many events are not associated with a body part
         // and this case should happen often enough to make it
         // worth the special handling.
         return table[eventID][0];
      }
      RsHandlerNode key = new RsHandlerNode(eventID, partID, null);
      int index         = Arrays.binarySearch(table[eventID], key);
      if(index<0)
         return null;
      return table[eventID][index];
   }


   private RsHandlerNode addChainForPartID(int eventID, int partID){
      RsHandlerNode [] n = new RsHandlerNode[table[eventID].length+1];

      int i=0, k = 0;
      while (i<table[eventID].length){
         if(table[eventID][i].partID>partID)
           break;
         n[i] = table[eventID][i];
         i++;
      }

      RsHandlerNode rootNode = new RsHandlerNode(eventID, partID, null);

      n[i] = rootNode;
      k = i+1;
      while(i<table[eventID].length){
         n[k] = table[eventID][i];
         i++;
         k++;
      }

      table[eventID] = n;
      return rootNode;

   }


   public void add(int eventID, int partID, RsEventHandler handler){

      if(handler==null)
         throw new Error("Attempt to add a null Event handler");
      if(eventID<0 || eventID>=nRow)
         throw new Error ("Attempt to add handler with a eventID value "+eventID+" out of bounds [0,"+nRow+"]");

      RsHandlerNode rootNode = searchForNode(eventID, partID);
      if(rootNode==null){
          // there was no matching set of handlers
          rootNode = addChainForPartID(eventID,  partID);
      }

      // traverse to end of list
      RsHandlerNode node = rootNode;
      while(node.next!=null)
         node=node.next;

      RsHandlerNode newNode = new RsHandlerNode(eventID, partID, handler);
      node.next     = newNode;
      newNode.prior = node;
   }


   public void removeAll(int eventID, int partID){
      if(eventID<0 || eventID>=nRow)
         throw new Error ("Attempt to remove handler with a eventID value "+eventID+" out of bounds [0,"+nRow+"]");



      RsHandlerNode node, next;
      node = searchForNode(eventID, partID);  // this is the root node, it's handler is empty
      if(node==null || node.handler==null)
         return;  // we're done

      next      = node.next;
      node.next = null;  // this puts the chain out of scope... next, take care of the iterator

      while(next!=null){
         node = next;
         if(node==iterationNode){
            iterationNode = null;
         }
         next       = node.next;
         node.prior = null;
         node.next  = null;
      }
   }

   public void removeAll(){
      iterationNode = null;
      table = new RsHandlerNode[nRow][];
      for(int i=0; i<nRow; i++){
         table[i]=new RsHandlerNode[1];
         table[i][0]=new RsHandlerNode(i, 0, null);
      }
   }

   public void remove(int eventID, int partID, RsEventHandler handler){
      if(eventID<0 || eventID>=nRow)
         throw new Error ("Attempt to remove handler with a eventID value "+eventID+" out of bounds [0,"+nRow+"]");


      RsHandlerNode node, next, prior;

      node=searchForNode(eventID, partID);  // this is the root node, it's handler is empty
      if(node==null)
         return;  // we're done. no handler was added here.

      // this loop is set up in case the handler was added
      // to the chain more than once (why anyone would actually
      // do that is unknown at this time).
      next  = node.next;
      while(next!=null){
         prior = node;
         node  = next;
         next  = node.next;
         if(node.handler==handler){
            if(node==iterationNode)
               iterationNode = node.next;
            node.next    = null;    // pro forma, remove it from list
            node.prior   = null;
            node         = prior;   // this will always exist because we keep the empty node at beginning
            prior.next   = next;
            if(next!=null)
               next.prior=prior;
         }
      }
   }

   public RsHandlerNode getHandlerNode(int eventID, int partID){
      if(eventID<0 || eventID>=nRow)
         return null;

      RsHandlerNode node=searchForNode(eventID, partID);
      if(node!=null)
         return node.next;
      return null;
   }

   public boolean isThereAHandler(int eventID, int partID){
      return (getHandlerNode(eventID, partID)!=null);
   }

   public void setIterator(int eventID, int partID){
      iterationNode = getHandlerNode(eventID, partID);
   }

   public RsEventHandler getNextHandler(){
      RsEventHandler handler = null;
      if(iterationNode!=null){
          handler      = iterationNode.handler;
          iterationNode = iterationNode.next;
      }
      return handler;
   }
}



   /* -----  TEST STUFF -----
      To run test, cut and past into the above class


   private class TestHandler implements RsEventHandler {
      public String name;
      public TestHandler(String name){
         this.name = name;
      }
   }


   public static void main(String[] args) throws Exception
   {
      RsHandlerRegistry r = new RsHandlerRegistry(4);
      r.testSeries();
   }

   private void testSeries(){
      TestHandler       h1 = new TestHandler("one");
      TestHandler       h2 = new TestHandler("two");
      TestHandler       h3 = new TestHandler("three");
      this.add(1, 1, h1);
      this.add(1, 1, h2);
      this.add(1, 1, h3);

      this.setIterator(1, 1);
      TestHandler h;
      while((h=(TestHandler)this.getNextHandler())!=null)
         System.err.println("test add: "+h.name);


      this.setIterator(1, 1);
      while((h=(TestHandler)this.getNextHandler())!=null){
         System.err.println("test remove: "+h.name);
         this.remove(1, 1, h2);
      }
   }
*/
