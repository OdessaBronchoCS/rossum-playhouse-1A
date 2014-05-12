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
import java.util.*;



/**
 * An extension of the Java Properties class with some
 *convenience functions for reading datatypes.
 *
 */

public class RsProperties extends Properties {



   /**
	 * 
	 */
	private static final long serialVersionUID = 4873099719014646691L;
public int     port;
   public String  hostName;

   public String  logFileName;
   public boolean logVerbose;
   public boolean logToFile;
   public boolean logToSystemOut;


   public RsProperties(){

      // set up standard defaults
      port           = 7758;
      hostName       = "127.0.0.1";
      logFileName    = "rossum.log";
      logToFile      = false;
      logToSystemOut = true;
      logVerbose     = false;

      // load the defaults specified in the rossum package
      try{
         InputStream ins = this.getClass().getResourceAsStream("rossum.ini");
         super.load(ins);
         ins.close();
      }catch(IOException eio){
         System.err.println("Serious Error -- unable to load default rossum properties");
         System.err.println("Probable RP1 installation error. Exception: "+eio.toString());
      }

      try{
         extractProperties();
      }catch(RsPropertiesException ers){
         System.err.println("Serious Error -- unable to load default rossum properties");
         System.err.println("Probable RP1 installation error. Exception: "+ers.toString());
      }

   }



   public RsProperties(String args[]) throws RsPropertiesException {
      processArgs(args);
   }

   public void processArgs(String args[]) throws RsPropertiesException {

      // normally, we would read the properties out of rossum.ini.
      // but first, see if the command-line arguments specify an alternate
      // properties file.   This is not a "100 percent Java" way of
      // doing things, and won't work on a Macintosh (which doesn't
      // support command-line arguments), but is awfully convenient
      // if you're working Unix or DOS.

      boolean alternate = false;
      if(args!=null){
         for(int i=0; i<args.length; i++){
            if(args[i].equals("-p")){
               if(i==args.length-1)
                  throw new RsPropertiesException("Missing specification for -p option");
               loadFromFile(args[i]);
               alternate = true;
            }
         }
      }


      // if no alternate was specified, try to read properties out of rossum.ini
      // failure to do so is considered an exception.
      if(!alternate){
         System.err.println("Loading properties from rossum.ini");
         load("rossum.ini");
      }


      // next copy in any other values from command line
      if(args!=null){
         commandeer(args, "port");
         commandeer(args, "hostName");
         commandeer(args, "logFileName");
         commandeer(args, "logToFile");
         commandeer(args, "logVerbose");
         commandeer(args, "logToSystemOut");
      }

      extractProperties();
   }

   private void commandeer(String args[], String name) throws RsPropertiesException {
      String test = "-"+name;
      for(int i=0; i<args.length; i++){
         if(test.equals(args[i])){
            if(i==args.length-1 || args[i+1].charAt(0)=='-')
               throw new RsPropertiesException("Missing specification for "+test+" option");
            put(name, args[i+1]);
            i++;
         }
      }
   }

   // the load(String fileName) method is deprecated.
   // Use loadFromFile instead

   public void load(String fileName) throws RsPropertiesException{
      loadFromFile(fileName);
   }

   public void loadFromFile(String fileName) throws RsPropertiesException {
      try{
         FileInputStream sf = new FileInputStream(fileName);
         super.load(sf);
         sf.close();
      }catch(IOException eio){
         throw new RsPropertiesException(eio.toString());
      }
      extractProperties();
   }

   public void loadFromResource(Object object, String resource) throws RsPropertiesException
   {

      Class       tempClass   = object.getClass();
      InputStream tempInput   = tempClass.getResourceAsStream(resource);
      if(tempInput==null)
         throw new RsPropertiesException(
             "Unable to find resource \""+resource+
                 "\" based on CLASSPATH for \""+tempClass.getName()+"\"");
      try{
         load(tempInput);
         tempInput.close();
      }catch(IOException eio){
          throw new RsPropertiesException(eio.toString());
      }
   }

   @Override
public void load(InputStream in) throws RsPropertiesException {
      try{
         super.load(in);
      }catch(IOException eio){
         throw new RsPropertiesException(eio.toString());
      }
      extractProperties();
   }

   private String extractString(String name){

      StringTokenizer st;
      String          string;

      string=getProperty(name);
      if(string!=null){
         st = new StringTokenizer(string);
         string = st.nextToken();
      }
      return string;
   }

   private String extractMandatoryString(String name) throws RsPropertiesException {
      String s = extractString(name);
      if(s==null)
         throw new RsPropertiesException("Missing mandatory specification for "+name);
      return s;
   }



   private boolean extractBoolean(String name) throws RsPropertiesException {
      String s = extractString(name);
      if(s==null)
         return false;
      if(s.equalsIgnoreCase("true"))
         return true;
      if(s.equalsIgnoreCase("false"))
         return false;
      throw new RsPropertiesException("Invalid specification where true/false expected for "+name);
   }

   public double extractDouble(String name) throws RsPropertiesException {
      String s = extractString(name);
      if(s==null)
         throw new RsPropertiesException(name+" property is unavailable ");
      double d=0.0;
      try{
         d = (new Double(s)).doubleValue();
      }catch(NumberFormatException nfe){
         throw new RsPropertiesException("Invalid number format \""+s+"\"");
      }
      return d;
   }

   public int extractInt(String name) throws RsPropertiesException {
      String s = extractString(name);
      if(s==null)
         throw new RsPropertiesException(name+" property is unavailable ");
      int i=0;
      try{
         i = (new Integer(s)).intValue();
      }catch(NumberFormatException nfe){
         throw new RsPropertiesException("Invalid number format \""+s+"\"");
      }
      return i;
   }


   private void extractProperties() throws RsPropertiesException {


      // get the hostName
      hostName = extractMandatoryString("hostName");

      // get the port number
      String string = extractMandatoryString("port");
      try {
         port = new Integer(string).intValue();
      } catch (NumberFormatException e){
         throw new RsPropertiesException("Bad number format for integer port specification: \""+string+"\"");
      }
      if(port<1025 || port>32767){
         throw new RsPropertiesException("Integer port number "+port+" not in valid range [1025,32767]");
      }

      logFileName    = extractString("logFileName");
      logToFile      = extractBoolean("logToFile");
      logToSystemOut = extractBoolean("logToSystemOut");
      logVerbose     = extractBoolean("logVerbose");

      if(logToFile && logFileName==null)
         throw new RsPropertiesException("The logToFile option was requested, but logFileName is missing");

   }


}
