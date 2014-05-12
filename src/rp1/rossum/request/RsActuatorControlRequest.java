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


package rp1.rossum.request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
The RsActuatorControlRequest class is in transition. It is intended
to provide a more realistic way of controlling actuators than currently
supported by RP1.
*/





/**
 * The request used to specify all actuator control sequences;
 *unlike other request classes, this one is often
 *instantiated directly in client applications.
 *
 */

public class RsActuatorControlRequest extends RsRequest {



   /**
	 * 
	 */
	private static final long serialVersionUID = -3810358339455847435L;

private class ParameterPair {
      protected String name;
      protected double value;

      protected ParameterPair(String name, double value){
         this.name  = name;
         this.value = value;
      }
   }




   public final int        actuatorID;
   public final int        commandID;

   protected ArrayList <ParameterPair> parameterList;
   private   String                 [] parameterNames;
   private   double                 [] parameterValues;


   public static final int HALT=1;
   public static final int RESET=2;
   public static final int ACTIVATE=3;


   public  RsActuatorControlRequest(int actuatorID, int commandID){
      super(REQ_ACTUATOR_CONTROL);
      this.actuatorID = actuatorID;
      this.commandID  = commandID;
   }


   public void addParameter(String name, double value){
      if(parameterList==null)
         this.parameterList = new ArrayList<ParameterPair>();
      parameterList.add(new ParameterPair(name, value));
      parameterNames  = null;
      parameterValues = null;
   }

   private void makeArraysReady(){
      if(parameterList.size()>0 && parameterNames==null){
         int n = parameterList.size();
         parameterNames  = new String[n];
         parameterValues = new double[n];
         Iterator i = parameterList.iterator();
         int k = 0;
         while(i.hasNext()){
           try{
              ParameterPair p = (ParameterPair)i.next();
              parameterNames[k]  = p.name;
              parameterValues[k] = p.value;
              k++;
           }catch(NoSuchElementException nse){
              // since we used hasNext, we should never get here
              System.err.println("Iterator failure, something is seriously wrong");
           }
         }
      }
   }


   private int getParameterIndexByName(String name){
      makeArraysReady();
      if(parameterNames!=null){
         for(int i=0; i<parameterNames.length; i++){
            if(parameterNames[i].equalsIgnoreCase(name))
                return i;
         }
      }
      return -1;
   }

   public boolean isParameterDefined(String name){
      return (getParameterIndexByName(name)>-1);
   }


   public double getParameterByName(String name){
      int i = getParameterIndexByName(name);
      if(i==-1){
         // parameter is actually undefined... return a default zero
         return 0.0;
      }
      return parameterValues[i];
   }

   public String [] getParameterNames(){
      makeArraysReady();
      return parameterNames;
   }

   public double [] getParameterValues(){
      makeArraysReady();
      return parameterValues;
   }

   public int getBodyPartID(){
      return actuatorID;
   }

}



