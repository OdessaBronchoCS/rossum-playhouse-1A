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

import java.io.*;
import java.text.*;
import java.util.*;



/**
 * A deprecated class for logging.
 *
 */

public class RsLogger extends Thread implements RsLogInterface {

   public RsLogger(String fileName, boolean systemOut){

      logFileName = fileName;
      logToSystemOut = systemOut;

      if(logFileName!=null){
         try {
            logWriter = new PrintWriter(
                        new BufferedWriter(
                        new FileWriter(logFileName)));
         }catch(IOException e){
            System.err.println("Fatal Error -- unable to open log file \""+logFileName+"\"");
            System.exit(-1);
         }
         System.err.println("Now Logging to file "+logFileName);
      }else{
         logWriter=null;
      }
      logDateFormat = new SimpleDateFormat("h:mm:ss.SSS");
   }

   public synchronized void logIt(String level, String message){

      String s=level + " " + logDateFormat.format(new Date())+ " "+message;

      if(logWriter!=null){
         logWriter.println(s);
         logWriter.flush();
      }
      if(logToSystemOut)
         System.out.println(s);
   }


   public synchronized void log(String message){
      logIt("s", message);
   }

   public void verbose(String message){
     // DEVELOPMENT NOTE...   I don't know if this is significant.   The reason
     // that verbose is not synchronized is that I did not want to incurr the
     // overhead associated with synchronization if the verbose logging was OFF.
     // If verbose logging is on, nobody would care.  But if verbose is off,
     // and performance becomes an issue, I don't want the vestigial
     // "verbose" method invocations to be significant

     if(logVerbose)
        logIt("v", message);
   }

   public synchronized boolean getVerbosity(){
      return logVerbose;
   }

   public synchronized void setVerbosity(boolean flag){
      logVerbose=flag;
   }


   private   boolean       logVerbose;
   private   boolean       logToSystemOut;
   private   PrintWriter   logWriter;
   private   DateFormat    logDateFormat;
   private   String        logFileName;
}

