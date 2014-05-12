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

/* ---------------------------------------------------------------------

SimClientListenerTask.java

About the design of this class...

Because it handles potentially blocking I/O, the SimClientListener
needs to run in its own thread.   We implement it as a SimTask because
that gives us a way to ensure that it doesn't start processing clients
until the SimSession has completed its initializations and enterred
its task queue.   The listen socket IS established before hand
(and clients can start connecting), but incoming clients won't
be processed until the thread is established from within the task
queue.   Note that I expect this task to be one of the VERY FIRST
tasks processed.

Note that we establish the listen socket for this class in the
constructor.   A failure to establish the listen socket is fatal,
and causes the termination of the simulator.   It is possible
that establishing a listen socket can take several seconds, particularly
on OLD Windows 95 systems and on Unix systems that are being subject
to heavy abuse.   Because the establishment can take so long,
the constructor can take a long time to complete its operation.

----------------------------------------------------------------- */


package rp1.simulator;
import java.io.*;
import java.net.*;



/**
 * A task used to establish clients when they connect to the
 * simulator/server through a remote connection; also establishes
 * a separate thread for listening to new client connection attempts.
 */

public class SimClientListenerTask extends SimTask implements Runnable
{
   private SimSession   session;
   private ServerSocket listener;

   public SimClientListenerTask(SimSession session){

      super();

      this.session = session;
      startTime    = 0;

      session.log("Initializing socket for network and local clients");
      try {
         listener = new ServerSocket(session.properties.port);
      }catch(IOException e0){
         session.log("Fatal Exception -- unable to create server socket \n"+e0.toString());
         System.exit(-1);
      }

      session.log("Socket ready");
   }

   @Override
public void process(){
      session.log("Launching listener thread for network and local clients");
      (new Thread(this)).start();
   }

   public void run() {

      session.log("Now accepting connections for network and local clients");
      while(true){

         Socket          socket=null;
         SimClient       client=null;


         try{
            socket = listener.accept();
            session.log("New client accepted on address "+socket.getInetAddress().getHostAddress());
         }catch(IOException e1){
            session.log("Error accepting client, probable serious system error."+
                        "No further clients will be accepted\n"+e1.toString());
            return;  // recall that this return terminates the thread
         }

         client = new SimClient(session);

         try{
            client.setSocket(socket);
            client.exchangeIntroductions();
         }catch(IOException eio){
            session.log("Client connection dropped due to IO error "+eio.toString());
            continue;
         }

         session.addSessionElementsToClient(client);
         session.log("Starting SimClient/RsConnection thread");
         (new Thread(client)).start();
      }
   }
}
