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


/*   SimTaskQueue

  Note:  whenever a task is removed from the queue (whether for
  execution or through the "remove()" method), it is imperitive
  that the next and prior nodes be nulled out.  This information
  is used by the SimTask.isTaskOnQueue() method.

*/


package rp1.simulator;






/**
 * Provides queue of objects of type SimTask; this class is 
 * extended by SimScheduler to provide the backbone of the
 * RP1 simulation.  When tasks are added to the queue, they
 * are inserted in order of the values of the time and priority
 * fields and insertion sequence. The waitForNextTask method 
 * provides a way of getting the next task for processing or
 * waiting until a new task is added. The SimScheduler extends
 * this method by adding a clock-based delay to create an 
 * execution schedule for tasks.
 */

public class SimTaskQueue extends Thread {


   SimTask firstTask;


   public SimTaskQueue(){
      firstTask=null;
   }

   public boolean isTaskOnQueue(SimTask task){
      // note: if there's only one task on queue, both its
      // prior and next links will be null.
      return (task.next != null || task.prior!=null || task==firstTask);
   }

   public synchronized void add(SimTask newTask){
      // advance through the linked list starting with the firstTask
      // until you find an insertion point...   the insertion point
      // occurs when
      //
      //    the next task in the queue has a simTime greater than
      //    that of the newTask (so the newTask must execute first)
      //
      //    or the next task has the same simTime as the newTask
      //    but is of lower priority.
      //
      // Note that if multiple tasks of the same simTime and priority
      // are inserted in the queue, they will be executed in the
      // order in which they are added.


     if(isTaskOnQueue(newTask)){
             // bad news.  the calling application is attempting
             // to queue the same task more than once.
             // I'm tempted to throw an exception here, but
             // I'll just ignore it.
             return;
      }


      SimTask next  = firstTask;
      SimTask prior = null;
      while(next!=null){

         if(next.startTime>newTask.startTime)
            break;
         else if(next.startTime==newTask.startTime && next.priority<newTask.priority)
            break;
         prior=next;
         next=next.next;
      }

      newTask.prior=prior;
      newTask.next=next;
      if(prior==null)
         firstTask=newTask;
      else
         prior.next=newTask;

      if(next!=null)
         next.prior=newTask;

      notifyAll();
   }



   public synchronized SimTask waitForNextTask(){

      while(firstTask==null){
         try{wait();}catch(InterruptedException e){}
      }

      SimTask task=firstTask;
      firstTask=firstTask.next;
      // the following assignments are not just good housekeeping,
      // but are critical to the isTaskOnQueue() method
      task.next=null;
      if(firstTask!=null)
         firstTask.prior=null;

      return task;
   }


   public synchronized boolean remove(SimTask targetTask){
      SimTask task  = firstTask;
      SimTask prior = null;

      while(task!=null){
         if(task==targetTask)
            break;
         prior=task;
         task=task.next;
      }

      if(task!=null){
         if(prior==null)
             firstTask=task.next;
         else{
             prior.next=task.next;
             task.prior=null;  // good housekeeping
         }

         if(task.next!=null){
            task.next.prior=prior;
            task.next=null;  // good housekeeping
         }
         return true;
      }

      // task was not on queue, clear its links just in case
      targetTask.next=null;
      targetTask.prior=null;
      return false;
   }

   public synchronized void removeTasksForOriginator(Object originator){
      SimTask task  = firstTask;
      SimTask prior = null;
      SimTask nextTask = null;
      while(task!=null){
         nextTask=task.next;
         if(task.originator==originator){
            if(prior==null)
               firstTask=task.next;
            else
               prior.next=task.next;
            if(task.next!=null)
               task.next.prior=prior;
            task.next=null;
            task.prior=null;
         }else{
            prior=task;
         }
         task=nextTask;
      }
   }

   @Override
public void run(){
       SimTask task;
       while(true){
         task=waitForNextTask();
         task.process();
      }
   }

}


