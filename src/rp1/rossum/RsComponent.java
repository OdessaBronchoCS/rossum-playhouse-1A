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

/*

RsComponent.java

RsComponent is the "root" class of all RsBody and related classes
including the RsMotion hierarchy.

Cloning

It implements a clone method which makes it possible to clone
the RsBody class and any class instances that it may contain.

The elegant clone method used here is taken almost directly from

    "Core Java, Volume II -- Advanced Features"
    by Cay S. Horstmann and Gary Cornell
    Sun Microsystems Press (Prentice Hall)

One important variation I've made is to have my clone() method throw
a CloneNotSupportExceptioned if anything goes wrong.   This is valuable
for debugging and essential to guaranteeing that the clone produced
by the method is both correct and complete.



*/

import java.io.*;



/**
 * The abstract base class for all RP1 simulation objects, including 
 *body parts, motions, and walls and other floorplan features.
 *
 */

public abstract class RsComponent implements Serializable,  Cloneable {

   @Override
public Object clone() throws CloneNotSupportedException {
      try{
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         ObjectOutputStream    out  = new ObjectOutputStream(bout);
         out.writeObject(this);
         out.close();

         ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
         ObjectInputStream    in  = new ObjectInputStream(bin);
         Object               ret = in.readObject();
         in.close();
         return ret;
      }catch(IOException eio){
         throw new CloneNotSupportedException(eio.toString());
      }catch(ClassNotFoundException enf){
         throw new CloneNotSupportedException(enf.toString());
      }
   }
}

