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

import java.io.*;
import rp1.rossum.*;




/**
 * A task used to launch dynamically loaded clients.
 */

public class SimClientLauncherTask extends SimTask implements Runnable{

   private SimSession   session;
   private String       clientClassName;
   private boolean      supplyClientIO;
   private boolean      supplyClientLogging;
   private RsRunnable   clientObject;

   public SimClientLauncherTask(
      SimSession session,
      String     clientClassName,
      boolean    supplyClientIO,
      boolean    supplyClientLogging)
   {
      super();
      this.session          = session;
      this.clientClassName  = clientClassName;
      this.supplyClientIO   = supplyClientIO;
      this.supplyClientLogging = supplyClientLogging;
      startTime    = 0;   // causes immediate response
   }


   @Override
public void process() {
      session.log("Loading class for client: \""+clientClassName+"\"");

      Class clnMain;
      try {
        clnMain = Class.forName(clientClassName);
      }catch(ClassNotFoundException clnfe){
        session.log("Unable to load class\n"+clnfe.toString());
        return;
      }

      Object o=null;
      try {
          o = clnMain.newInstance();
      }catch(InstantiationException iex){
         session.log("Unable to instantiate class\n"+iex.toString());
         return;
      }catch(IllegalAccessException iax){
         session.log("Unable to instantiate class\n"+iax.toString());
      }

      clientObject = (RsRunnable)o;

      if(supplyClientIO){
         SimClient simClient = new SimClient(session);

         try{
            PipedOutputStream clnOut = new PipedOutputStream();
            PipedInputStream  srvIn  = new PipedInputStream(clnOut);

            PipedOutputStream srvOut = new PipedOutputStream();
            PipedInputStream  clnIn  = new PipedInputStream(srvOut);

            simClient.setInputOutputStreams(srvIn, srvOut);
            clientObject.setInputOutputStreams(clnIn, clnOut);

         }catch (IOException eio){
            session.log("Serious error, unable to create client/server IO");
            return;
         }


         session.log("Adding SimClient and starting its connection thread");
         session.addSessionElementsToClient(simClient);
         (new Thread(simClient)).start();
      }

      if(supplyClientLogging){
         clientObject.setLogger(session);
      }

      session.log("Launching thread for client: \""+clientClassName+"\"");
      (new Thread(this)).start();
   }


   public void run() {
      try{
          clientObject.initialize();
      }catch(IOException ioex){
          session.log("IO Error caused termination of client "+clientClassName+".initialize()\n"+ioex.toString());
          return;
      }
      clientObject.run();
   }

}
