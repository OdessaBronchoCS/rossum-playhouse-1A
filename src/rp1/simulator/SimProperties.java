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
SimProperties reads configuration elements for the simulator from
the file RP1.ini.

The port property might be better obtained from an NIS or by using
some of the Java classes which are related to URL's.  For right now,
it is obtained from the properties file.

Three constructors are provided

   SimProperties(String fileName)  read from alternate file name
   SimProperties(String [] args)   check for command-line arguments


When reading command-line arguments, SimProperties recognizes the
following

   -p  propertiesFileName
   -f  floorPlanFileName

The -p allows the user to specify an alternate properties file,
the -f allows the user to override any specifications that might
have been found for floor plan.   Command-line arguments are not
available under all operating systems, and so are not certified as
"100 percent Java" standard.  But they are awfully useful when you're
developing and testing code on an OS that does support them.


COMMENT

This code is all over the place...   it could really stand a good
rewrite.



*/



package rp1.simulator;

import java.io.*;
import java.net.*;
import java.util.*;




/**
 * Used to read properties for simulation. This class is overdue for a serious rewrite.
 */

public class SimProperties {


   protected Properties  properties;

   protected String  floorPlanFileName;
   protected int     port;
   protected String  logFileName;
   protected boolean logVerbose;
   protected boolean logToSystemOut;
   protected boolean logToFile;

   protected boolean enableGUI;
   protected boolean enableSocket;
   protected double  simulationSpeed;
   protected double  animationFrameRate;
   protected double  modelingFrameRate;

   protected boolean  dlcEnabled;
   protected String   dlcName;
   protected boolean  dlcSetIO;
   protected boolean  dlcSetLog;

   protected boolean  interlockEnabled;

   protected Class   mainClass;


   public SimProperties(Class mainClass){
      this.mainClass = mainClass;
      properties     = new Properties();
   }

   public SimProperties(Object mainObject, String args[]) throws SimPropertiesException {

      mainClass  = mainObject.getClass();
      properties = new Properties();
      String  propertiesFile = null;

      for(int i=0; i<args.length; i++){
         if(args[i].equals("-p")){
            if(i==args.length-1)
               throw new SimPropertiesException("Missing specification for -p option");
            propertiesFile = args[i+1];
         }
      }

      if(propertiesFile!=null){
         System.err.println(
            "Loading properties for specification -p "+propertiesFile+"\n"
           +"searching for file in current directory (folder)");
         FileInputStream sf=null;
         try{
            sf = new FileInputStream(propertiesFile);
            load(sf);
            sf.close();
         }catch(IOException eio){
            System.err.println(
               "File not found in current directory/folder, will attempt to find it in class path");
            sf=null;
         }

         if(sf==null){
            loadFromClassPath(propertiesFile);
         }
      }else{
         System.err.println("Loading properties from standard RP1.ini file");
         loadFromClassPath("RP1.ini");
      }


      // see if a floor plan was specified as a command-line argument
      for(int i=0; i<args.length; i++){
         if(args[i].equals("-f")){
            if(i==args.length-1)
               throw new SimPropertiesException("Missing specification for -f (floor plan) option");
            floorPlanFileName = args[i+1];
         }
      }
   }


   public void loadFromResource(Object object, String resource) throws SimPropertiesException
   {
      Class       tempClass   = object.getClass();
      InputStream tempInput   = tempClass.getResourceAsStream(resource);
      if(tempInput==null){
         throw new SimPropertiesException(
             "Unable to find resource \""+resource+
                 "\" based on CLASSPATH for \""+tempClass.getName()+"\"");
      }

      try{
         load(tempInput);
         tempInput.close();
      }catch(IOException eio){
          throw new SimPropertiesException(eio.toString());
      }
   }




   public int getPort(){
      return port;
   }

   public boolean isGuiEnabled(){
      return enableGUI;
   }

   public boolean isSocketEnabled(){
         return enableSocket;
   }

   public String getFloorPlanFileName(){
      return floorPlanFileName;
   }

   public double getSimulationSpeed(){
      return simulationSpeed;
   }


   // ------------  private methods -------------------



   private String extractString(String name){

      StringTokenizer st;
      String          string;

      string=properties.getProperty(name);
      if(string!=null){
         st = new StringTokenizer(string);
         string = st.nextToken();
      }
      return string;
   }


   private String extractString(String name, String _default){

      StringTokenizer st;
      String          string;

      string=properties.getProperty(name);
      if(string==null)
         return _default;

      st = new StringTokenizer(string);
      return st.nextToken();
   }

   private boolean extractBoolean(String name, boolean _default) throws SimPropertiesException {
      String s = extractString(name);
      if(s==null)
         return _default;
      if(s.equalsIgnoreCase("true"))
         return true;
      if(s.equalsIgnoreCase("false"))
         return false;
      throw new SimPropertiesException("Invalid specification where true/false expected for "+name);
   }

   private double extractDouble(String name, double _default) throws SimPropertiesException {
      String s = extractString(name);
      if(s==null)
         return _default;
      return (new Double(s)).doubleValue();
   }


   /******************************************************************
   private void loadFromFilePath(String fileName) throws SimPropertiesException {

      FileInputStream sf=null;
      try{
         sf = new FileInputStream(fileName);
         load(sf);
         sf.close();
      }catch(IOException eio){
         throw new SimPropertiesException(eio.toString());
      }
   }
   **********************************************************/
   
   private void loadFromClassPath(String fileName) throws SimPropertiesException {
      InputStream sf=null;
      URL url = mainClass.getClassLoader().getResource("Properties/"+fileName);
      if(url!=null){
          System.err.println("Reading properties from "+url.toString());
          sf = mainClass.getClassLoader().getResourceAsStream("Properties/"+fileName);
      }

      if(sf==null){
         throw new SimPropertiesException("Unable to obtain resource "+fileName);
      }
      load(sf);
      try{
         sf.close();
      }catch(IOException eieio){
         throw new SimPropertiesException(eieio.toString());
      }
   }

   private void load(InputStream sf) throws SimPropertiesException {

      try{
         properties.load(sf);
         sf.close();
      }catch(IOException eio){
         throw new SimPropertiesException(eio.toString());
      }


      // get the port number
      String string = extractString("port", "7758");
      try {
         port = new Integer(string).intValue();
      } catch (NumberFormatException e){
         throw new SimPropertiesException("Bad number format for integer port specification: \""+string+"\"");
      }
      if(port<1025 || port>32767){
         throw new SimPropertiesException("Integer port number "+port+" not in valid range [1025,32767]");
      }


      // get the floor plan...  note that this is optional
      floorPlanFileName = extractString("floorPlanFileName");


      logFileName    = extractString("logFileName");
      logToFile      = extractBoolean("logToFile",      false);
      logToSystemOut = extractBoolean("logToSystemOut", false);
      logVerbose     = extractBoolean("logVerbose",     false);

      if(logToFile && logFileName==null)
         throw new SimPropertiesException("The logToFile option was requested, but logFileName is missing");

      enableGUI          = extractBoolean("enableGUI",            true);
      enableSocket       = extractBoolean("enableNetworkClients", true);

      simulationSpeed    = extractDouble("simulationSpeed",    1.0);
      if(simulationSpeed<0.001 || simulationSpeed>1000){
          throw new SimPropertiesException("Simulation Speed "+simulationSpeed+" is out of range [0.001, 1000.0]");
      }
      animationFrameRate = extractDouble("animationFrameRate", 20.0);
      modelingFrameRate  = extractDouble("modelingFrameRate",  20.0);
      if(animationFrameRate>32)
         animationFrameRate=32;
      if(modelingFrameRate<animationFrameRate)
         modelingFrameRate=animationFrameRate;

      dlcEnabled = extractBoolean("dlcEnabled", false);
      dlcName    = extractString( "dlcName",     null);
      dlcSetIO   = extractBoolean("dlcSetIO",    true);
      dlcSetLog  = extractBoolean("dlcSetLog",   true);

      interlockEnabled = extractBoolean("interlockEnabled", false);

   }



}
