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


/*

An object of this class serves as an intermediary for transferring
state data from the Simulator session thread to the rendering threads.
Two kinds of data are handled:  simulacrum bodies and paint boxes.

The general pattern of use is:

   A task in the simulator (task queue) thread performs operations that
   change the state or composition of its list of robot bodies or paint box
   objects. It transfers the state data to corresponding objects in the StateDataExchange
   object.

   An animation or other rendering event is fired.

   The event-listener in the graphics thread uses the StateDataExchange
   to transfer data to its own corresponding objects. It then does whatever
   rendering it needs to do.

   The key here is that the transfer methods storeStateData() and
   extractStateData() are synchronized. The StateDataExchange object
   serves as the monitor for the synchronization.  The transfer methods
   attempt to execute as quickly as possible so that there is a minimum
   period of time during which one thread holds the lock on the object.
   Thus reducing, if the not the probability, at least the duration of
   one thread being locked in a wait for the synchronized call to execute.

   Another consequence of this approach is that there are generally three
   copies of objects:

        the bodies owned by the simulator thread
        the bodies owned by the StateDataExchange object
        the bodies owned by a rendering object (the graphics thread).

   Finally, the nice thing about this approach is that during the
   copy we can be sure that only one thread is accessing the contents.
   Our copy logic can work on the assumption that the contents of the
   RsBody or SimPaintBox is not changing underneath it.



   Note: in doing the SimPaintBox copies, we need to make sure that
         the actual geometry data (which can run into the 100's of Kbytes)
         is stable and shareable between paint box objects... we don't
         want to go duplicating such a large amount of data. This is accomplished
         through the SimPaintBox.copyStateData() method which copies the
         references for the geometry, but does not create new objects.

*/



package rp1.simulator;
import  rp1.rossum.*;




/**
 * Provides a temporary repository for state data (body states and
 * paint box information) to allow different threads to transfer
 * state data; currently used to transfer information from
 * the main session thread to SimCanvas thread for rendering.
 * In terms of future development, this class provides an opportunity for
 * statistics gathering since it can be made to detect changes
 * in state data each time data is transferred.
 */

public class SimStateDataExchange implements SimStateDataInterface {

   private RsBody      []    bodyArray;
   private SimPaintBox []  paintBoxArray;



   public SimStateDataExchange(){

   }

   // methods in support of SimStateDataInterface
   public RsBody [] getBodyArray(){
      return bodyArray;
   }

   public void setBodyArray(RsBody [] bodyArray){
       this.bodyArray = bodyArray;
   }

   public SimPaintBox [] getPaintBoxArray(){
       return paintBoxArray;
   }

   public void setPaintBoxArray(SimPaintBox [] paintBoxArray){
      this.paintBoxArray = paintBoxArray;
   }



   // the following methods are called something other than "get" and "set"
   // because they act a bit differently than the conventional usage...

   public synchronized void storeStateData(SimStateDataInterface source){
      transferBodyStateData(    this, source);
      transferPaintBoxStateData(this, source);
   }

   public synchronized void retrieveStateData(SimStateDataInterface receiver){
      transferBodyStateData(    receiver, this);
      transferPaintBoxStateData(receiver, this);
   }





   private void transferBodyStateData(SimStateDataInterface rcv, SimStateDataInterface src){
      int i, j;
      RsBody [] source = src.getBodyArray();
      if(source == null){
        rcv.setBodyArray(null);
        return;
      }

      RsBody [] receiver = rcv.getBodyArray();
      if(receiver==null){
        receiver = new RsBody[source.length];
        for(i=0; i<source.length; i++){
          try{
             receiver[i] = (RsBody)source[i].clone();
          }catch (CloneNotSupportedException cse){
             // this would be a very serious error and things are failing all around
             System.err.println("Serious problem: "+cse.toString());
             return;
          }
        }
        rcv.setBodyArray(receiver);
        return;
      }

      if(source.length==receiver.length){
         for(i=0; i<source.length; i++){
            if(source[i].getID()!=receiver[i].getID()){
               break;
            }
         }

         if(i==source.length){
            // all sources and receivers matched
            for(i=0; i<source.length; i++)
               receiver[i].copyStateData(source[i]);
            return;
         }
      }


      // the two lists differ, though there might be some overlap.
      // for all elements on the source list that are matched by the
      // receiver list, we simply copy over the state data. for all
      // elements on the source list that are not on the receiver
      // list, we clone the bodies.  any items on the receiver list
      // that were not on the source list are simply dropped.

      RsBody [] newList = new RsBody[source.length];
      for(i=0; i<source.length; i++){
         for(j=0; j<receiver.length; j++){
            if(source[i].getID()==receiver[j].getID()){
               newList[i] = receiver[j];
               newList[i].copyStateData(source[i]);
               break;
            }
         }
         if(j==receiver.length){
            // we searched the whole existing list without finding a match
            try{
               newList[i] = (RsBody)source[i].clone();
            }catch (CloneNotSupportedException cse){
               // this would be a very serious error and things are failing all around
               System.err.println("Serious problem: "+cse.toString());
               return;
            }
         }
      }
      for(i=0; i<receiver.length; i++)
         receiver[i] = null;  // just for housekeeping/debugging

      rcv.setBodyArray(newList);

   }


   private void transferPaintBoxStateData(SimStateDataInterface rcv, SimStateDataInterface src){

      SimPaintBox [] source = src.getPaintBoxArray();
      SimPaintBox [] receiver = rcv.getPaintBoxArray();
      int i;

      if(source==null){
         rcv.setPaintBoxArray(null);
         return;
      }

      if(receiver!=null && source!=null && receiver.length==source.length){
         for(i=0; i<source.length; i++){
            if(source[i].getPaintBoxID() != receiver[i].getPaintBoxID())
               break;
         }

         if(i==source.length){
            // all paint box ID's matched
            // so the code makes sure to copy all state data
            // and then it's done
            for(i=0; i<source.length; i++)
                receiver[i].copyStateData(source[i]);
            return;
         }
      }

      receiver = new SimPaintBox[source.length];
      for(i=0; i<source.length; i++)
         receiver[i] = new SimPaintBox(source[i]);
      rcv.setPaintBoxArray(receiver);
   }
}
