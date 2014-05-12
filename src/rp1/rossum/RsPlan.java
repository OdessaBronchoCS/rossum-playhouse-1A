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

import  rp1.rossum.event.RsTargetSelectionEvent;
import  rp1.rossum.request.RsTargetSelectionRequest;

/*

RsPlan.java

RsPlan is the main object used in the representation of floor plans.
It it used both in the simulator and is applicable to graphics displays
and data entry applications.

About the Serial Number

Rossum's Playhouse applications require that all modeling objects (walls,
targets, doorways, etc.) be tagged with identifiers.   Of course, there are many
applications where it is not reasonable to ask a user to stop what he's doing and
invent a name for an object, so we provide a serial-number for generating new
names.  The synthesizeName() methods are useful for this purpose.  Synthesized
names are generated as hexidecimal-coded values (i.e. "0x0000f234").
As a matter of practice, it is recommended that applications reserve numeric
identifiers for synthesized names only...  users should not be allowed to
enter numerals as object names (strings such as "A001", or even "_001" are
okay, but numeric values should not be treated as valid user entries).


About the Object ArrayList

While the Java collection classes are a big improvement over the original
Java Vector class that was used in early implementations of this function,
it still has a certain amount overhead. Because the floor plan, once established,
is really quite static, we prefer to operate on arrays of floor-plan objects
rather than using an Iterator for accessing the collection.

Note that there is very little protection against misuse...  The access
method is NOT synchronized and the same reference is shared by all objects
that obtain the array. If a faulty method modifies the object array,
it will damage it everywhere in the application.

*/



import java.util.*;



/**
 * The main container class for floor-plans.
 *
 */

public class RsPlan
{
   private String fileName;
   
   private Hashtable <String, RsObject> identifierTable;
   private ArrayList <RsObject>         objectList;
   private RsObject    []               objectArray;
   private RsNavLink   []               linkArray;
   private RsNavNode   []               nodeArray;
   
   private int            serialNumber;

   private RsUnits        units;
   private String         caption;


   public RsPlan(){
      fileName=null;
      initPlan();
   }
   public RsPlan(String fileNameRef){
      fileName = fileNameRef;
      initPlan();
   }

   private void initPlan(){
      identifierTable = new Hashtable<String, RsObject>();
      objectList      = new ArrayList<RsObject>() ;
      serialNumber    = 0;
      units=new RsMeters();
   }

   public RsObject getObjectByName(String identifier){
       RsObject  object;
       object =  (RsObject)identifierTable.get(identifier);
       return object;
   }

   public String synthesizeName(){
       serialNumber++;
       return synthesizeName(serialNumber);
   }

   public String synthesizeName(int value){
       StringBuffer s = new StringBuffer(9);
       // hex code the value....
       // there are times when I really miss the sprintf() function in C
       int test;
       s.append("0x");
       for(int i=0; i<8; i++){
          test= (value>>>(7-i)*4)&0xff;
          if(test<10)
             s.append((char)(test+'0'));
          else
             s.append((char)(test-10+'a'));
       }
       return s.toString();
   }

   public String getFileName(){
      return fileName;
   }

   public void setFileName(String fileNameRef){
       if(fileNameRef==null)
          fileName=null;
       else
          fileName=fileNameRef;
   }

   public void setUnits(RsUnits unitsReference){
      if(unitsReference==null)
         units = new RsMeters();
      else
         units=unitsReference;
   }

   public RsUnits getUnits(){
       return units;
   }

   public void addObject(RsObject object){
         objectList.add(object);
         identifierTable.put(object.getName(), object);
         // the old arrays are no longer correct reflections
         // of what's in the object Vector...  nulling them out
         // will force their recalculation when the various
         // "get" methods are called.
         objectArray = null;
         linkArray=null;
         nodeArray=null;
   }


   public RsObject [] getObjectArray(){
      makeArraysReady();
      return objectArray;
   }

   public RsNavLink [] getNavLinkArray(){
      makeArraysReady();
      return linkArray;
   }

   public RsNavNode [] getNavNodeArray(){
      makeArraysReady();
      return nodeArray;
   }


   public RsTargetSelectionEvent []
      processTargetSelectionRequest(double simTime, RsTargetSelectionRequest req){

      int i, n, k;
      RsObject [] o = getObjectArray();

      if(o==null)
         return null;
      boolean [] modified = new boolean[o.length];
      for(i=0; i<o.length; i++)
         modified[i]=false;

      if(req.exclusive){
         for(i=0; i<o.length; i++){
            if(o[i] instanceof RsTarget){
               o[i].setSelected(false);
               modified[i]=true;
            }
         }
      }

      if(req.random){
         n=0;
         for(i=0; i<o.length; i++)
            if(o[i] instanceof RsTarget)
               n++;
         if(n>0){
            Random random = new Random();  // current time used as seed
            k = (int)Math.floor((n*random.nextDouble()));
            if(k==n){
               k=n-1;
               /* my documentation doesn't say whether Random.nextDouble()
                  is in the range [0., 1.0] or [0., 1.0)
                  I suspect the latter, which would make this test unnecessary,
                  but I check just in case....
               */
            }

            n=0;
            for(i=0; i<o.length; i++){
               if(o[i] instanceof RsTarget){
                  if(n==k){
                     o[i].setSelected(true);
                     modified[i]=true;
                     break;
                  }
                  n++;
               }
            }
         }
      }

      if(req.name.equals("*")){
         for(i=0; i<o.length; i++){
            if(o[i] instanceof RsTarget){
               o[i].setSelected(req.status);
               modified[i]=true;
            }
         }
      }else if(req.name!=null && req.name.length()>0){
         for(i=0; i<o.length; i++){
            if(o[i] instanceof RsTarget && req.name.equals(o[i].getName())){
               o[i].setSelected(req.status);
               modified[i]=true;
               break;
            }
         }
      }

      n=0;
      for(i=0; i<o.length;i++)
        if(modified[i])
           n++;
      if(n==0)
         return null;

      RsTargetSelectionEvent [] event = new RsTargetSelectionEvent[n];

      k=0;
      for(i=0; i<o.length; i++)
         if(modified[i])
            event[k++] = new RsTargetSelectionEvent(simTime, o[i].getName(), o[i].getSelected());

      return event;
   }

   public void applyTargetSelectionEvent(RsTargetSelectionEvent event){

      RsObject o = getObjectByName(event.targetName);
      if(o!=null && o instanceof RsTarget)
         o.setSelected(event.status);
   }



   private void makeArraysReady(){
      if(objectArray!=null){
         // if the objectArray is not null, then we already
         // have made a call to makeArraysReady() since the last floor-plan
         // object was added.   So we don't need to do anymore work.
         return;
      }
      if(objectList.size()==0)
         return; // nothing to be done...  the floor plan is empty

      objectArray = (RsObject []) objectList.toArray(new RsObject[objectList.size()]);

      int i;
      int nNode=0;
      int nLink=0;
      for(i=0; i<objectArray.length; i++){
         if(objectArray[i] instanceof RsNavLink)
            nLink++;
         else if(objectArray[i] instanceof RsNavNode)
            nNode++;
      }

      // note that if either nLink or nNode are non-zero, then
      // both are PROBABLY non-zero...   but not necessarily
      // (it's possible to have nodes without links, but not
      // links without nodes)
      if(nNode>0){
         nodeArray = new RsNavNode[nNode];
         if(nLink>0)
            linkArray = new RsNavLink[nLink];
         nLink=0;
         nNode=0;
         for(i=0; i<objectArray.length; i++){
            if(objectArray[i] instanceof RsNavNode)
               nodeArray[nNode++]=(RsNavNode)objectArray[i];
            if(objectArray[i] instanceof RsNavLink)
               linkArray[nLink++]=(RsNavLink)objectArray[i];
         }
      }
   }

   public void maximizeSerialNumber(int value){
      if(value>serialNumber)
         serialNumber=value;
   }

   public String getCaption(){
      return caption;
   }
   public void setCaption(String captionReference){
      caption=captionReference;
   }

   public RsRectangle getBounds(){
      RsRectangle bounds = null;

      RsObject  [] o = getObjectArray();
      RsRectangle  r;

      if(o!=null){
         for(int i = 0; i<o.length; i++){
            r=o[i].getBounds();
            if(r!=null){
               if(bounds==null)
                  bounds=new RsRectangle(r);
               else
                  bounds.union(r);
            }
         }
      }
      return bounds;
   }


}

