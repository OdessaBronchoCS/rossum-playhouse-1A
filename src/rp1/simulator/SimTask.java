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

/* SimTask

   for purposes of the SimTaskQueue.isTaskOnQueue() method,
   a task is considered "out-of-the-queue" as soon as it
   is executed
*/


package rp1.simulator;




/**
 * Provides the base class for all simulator tasks.
 * Essentially this class defines a node in a doubly-linked
 * list created by SimTaskQueue. The time and priority fields
 * are used to define order of insertion into the list. 
 */

public abstract class SimTask {

   public SimTask(){
      startTime  = 0;
      next       = null;
      prior      = null;
      originator = null;
      priority   = 1;  // the higher priority task gets executed first */
      taskSequencer++;
   }

   public abstract void process();

   public void setStartTime(double startTimeValue){
      // this should be expanded to handle case when task
      // is already on queue.   currently, we simply don't
      // set the time once we queue the task...  but someday
      // somebody will forget.
      startTime=startTimeValue;
   }
   protected double   startTime;
   protected SimTask  next;
   protected SimTask  prior;
   protected Object   originator;
   protected int      priority;
   private static int taskSequencer;
}


