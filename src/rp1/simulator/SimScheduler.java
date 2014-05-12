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



package rp1.simulator;


/*

GARY -- TO DO:

  in other classes, find places where getUpdatedSimTime() is called
  to set up a simTime for a task and replace it with the new mthods:

     addTaskAtUpdatedSimTime
     addTaskForImmediateProcessing

  This is more elegant, should make it more clear what's going on
  and should help avoid bugs.


waitForNextTask() overrides the SimTaskQueue method, adding the
stipulation that we must wait until the next task is DUE FOR PROCESSING...

simSpeed is the ratio of simulated time to real time (simulated_second/real_second)

The updateSimTime() method updates the simulation time to reflect the passage of real-time.
Normally, the simulation clock is updated by tasks in the task queue.   Since the performance of
tasks is driven by a scheduler, there may be lengthy periods of time during which the
simTime value would not be changed if the updateSimTime() method were not available.  The
updateSimTime() value checks the system clock against the time at which the most recently
processed task was executed and adjusts the simTime value to reflect the passage of time.

getSimTime() returns the current value of the simulation time, but for most
applications, the getUpdatedSimTime() is a better method.   All time values for events
are obtained using the getUpdatedSimTime() method.


The goal of the Scheduler is to provide as stable and consistent a time base
as possible. Now a computer with a conventional OS makes a lousey clock and
running Java time-slicing on top of that makes things worse.  So what we do is
to try to keep a time basis and adjust the simulation time accordingly.
Typically, we will add tasks under two conditions:

         immediate mode:  the task has no relation to the simulation
                          time basis and we want it to happen immediately
                          Typically, this might be when we remove a client of
                          queue a maintenance event.

         time mode:       the task needs to have a time basis.  even if it
                          is to happen immediately, we want it to be assigned
                          a time reflecting the current state of the simulation.

The simTime element tracks the current simulation and is updated as tasks
are taken off the task queue. But sometimes it needs to be updated using
other methods. Imagine a scenario where the simulator has been in a quiescent state
for a relatively long time.  If we want to queue a time-related task, we don't
want to use the current simTime, because it will be out-of-date.  We need to update
the time before adding it.


The elements

   double clockTime0    the time at which the clock was started
                        it will be equivalent to simTime0

   double simTime0      the simTime at which the clock was started.
                        during an ordinary start up, this value will
                        be zero. but if the clock is stopped and restarted,
                        it will be non zero.



*/





/**
 * A time-driven task queue that provides the backbone of the RP1
 * simulator; this class extends SimTaskQueue by adding clock-driven
 * delays between sunsequent tasks so that they are executed according
 * to a time sequence.  It does this by overloading the waitForNextTask
 * from SimTaskQueue. In SimTaskQueue, waitForNextTask returns a task
 * as soon as one is available. In SimScheduler, it waits until the
 * next task in the queue is scheduled for processing.  Note that
 * sometimes tasks with a startTime value of zero meaning
 * "for immediate execution."  Such tasks will cause the 
 * waitForNextTask method to return control to the calling
 * application without delay.
 */

public class SimScheduler extends SimTaskQueue{


   boolean simIsRunning;
   double  clockTime0;
   double  simTime0;
   double  simTime;            // in seconds
   double  simSpeed;

   public SimScheduler(){
      super();
      simIsRunning=false;
      simTime=0;
      simSpeed=1.0;
   }


   public synchronized void setSimSpeed(double newSpeed){
      if(simIsRunning){
         stopClock();
         simSpeed=newSpeed;
         startClock();
      }else{
         simSpeed = newSpeed;
      }
      notifyAll();
   }

   public synchronized void startClock(){
      if(simIsRunning)
         return;
      simIsRunning      = true;

      clockTime0   = System.currentTimeMillis()/1000.0;
      simTime0     = simTime;
      notifyAll();
   }

   public synchronized void stopClock(){
      if(!simIsRunning)
         return;

      updateSimTime();
      simIsRunning = false;
   }

   public synchronized double getSimSpeed(){
      return simSpeed;
   }


   public double getSimTime(){
      return simTime;
   }

   public synchronized double getUpdatedSimTime(){
      updateSimTime();
      return simTime;
   }


   private double getEstimatedSimTime(){
      double clockTime         = System.currentTimeMillis()/1000.0;
      double elapsedClockTime  = clockTime-clockTime0;
      double estElapsedSimTime = elapsedClockTime*simSpeed;
      double estSimTime        = simTime0+estElapsedSimTime;
      return estSimTime;
   }

   private void updateSimTime(){

      if(simIsRunning){

         // if we are between scheduled tasks, we want to update the simulation
         // time to reflect how much real time has passed since we performed the
         // most recent task (adjusted for simulation speed).    However, if we
         // are not keeping up, then the simTime cannot be advanced to the future
         // of the next scheduled task in the queue.

         double estSimTime = getEstimatedSimTime();

         if(estSimTime<=simTime){
            // there's nothing to do... the simulator has actually gotten
            // ahead of the clock.  if everything is working, the distance will
            // be small.
            return;
         }


         // the estimated sim time exceeds the actual sim time. so we need
         // to advance the sim time.  However, we cannot advance it beyond that
         // of any tasks on the queue.
         // find next scheduled task, skip those with "startTime zero"
         SimTask task = firstTask;
         while(task!=null && task.startTime<=0)
            task=task.next;

         if(task==null){
            simTime = estSimTime;
         }else if(task.startTime>estSimTime){
            // the task on the queue is not yet due.
            simTime = estSimTime;
         }else{
            // the task on the queue is either due or overdue. we cannot advance
            // the simTime past that of the task, but we do set it to the task.startTime
            // to force it to be processed without further delay the next time
            // waitForNextTask is called.
            simTime = task.startTime;
         }
      }
   }

   @Override
public synchronized SimTask waitForNextTask(){

      // we may need to wait until the next task in queue is due to be processed.
      // note that the next task may have a zero-time stamp (meaning "do it immediately").
      // or it may already be due for processing, or it may be due in the future.
      // after the wait, we also have to check to ensure that the next task was
      // not removed from the queue or the simulation was halted while we were
      // waiting.

      while(true){
         while(!simIsRunning || firstTask==null){
            try{wait(5000);}catch(InterruptedException e){}
         }

         // the tasks in the queue could have startTimes==0, startTime==simTime,
         // or startTime>simTime.   In the first two cases, we simply execute them
         // without changing any of the internal time-tracking elements.
         // if the task is in the future of the simTime, we need to calculate
         // the simulated and real-time delay before the task is to be
         // executed.

         if(firstTask.startTime>simTime){
            // The startTime is in the future of the simTime.   Of course,
            // some real time may have passed since the last time the
            // simTime was updated... so it may be appropriate to increase
            // its value.
            double estSimTime   = getEstimatedSimTime();
            double simTimeDelay = firstTask.startTime - estSimTime;
            double realTimeDelay;

            if(simTimeDelay>0){
               // even with the adjusted simTime, there is still some delay
               // time before the next task is due to be run.  we update
               // the sim time and compute the delay.  Of course, since the
               // minimum wait period we can specify is 1 millisecond and
               // our calculation may be somewhat less than that.
               // TO DO: is there an algorithmic problem with skipping the wait?
               // what if we have a bunch of closely spaced tasks queued up.
               // could we end up running the clock too far ahead?

               simTime         = estSimTime;
               realTimeDelay   = simTimeDelay/simSpeed;
               long waitPeriod = (long)Math.floor(realTimeDelay*1000.0+0.5);
               if(waitPeriod>0){
                  try{wait(waitPeriod);}catch(InterruptedException e){}
                  // note that the thing that broke us out of the wait may have
                  // been the notify when some other thread invoked add() or remove().
                  // so we can't assume there's still a task ready to be processed.
                  // we have to jump back to top of loop to check.
                  continue;
               }
            }
         }



         SimTask task=firstTask;
         firstTask=firstTask.next;
         task.next=null;
         if(firstTask!=null)
            firstTask.prior=null;

         if(task.startTime==0)
            updateSimTime();
         else
            simTime = task.startTime;
         return task;
      }
   }

   public synchronized void addTaskAtUpdatedSimTime(SimTask task){
      updateSimTime();
      task.startTime = simTime;
      add(task);
   }

   public synchronized void addTaskAtUpdatedSimTime(double offset, SimTask task){
      updateSimTime();
      task.startTime = simTime+offset;
      add(task);
   }


   public synchronized void addTaskForImmediateProcessing(SimTask task){
      task.startTime = 0;
      add(task);
   }
}
