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
import java.awt.Color;

class RsPlanDecoder {

   public static RsPlan receive(RsProtocol protocol) throws IOException {

      DataInputStream   input = protocol.input;   // merely to save me some typing
      RsPlan            plan;
      RsWall            wall;
      RsTarget          target;
      RsPlacement       placement;
      RsNavNode         navNode;
      RsNavLink         navLink;
      RsPaint           paint;


      String            string;
      int               nObject;
      int               index;
      String            name;

      index = input.readInt();
      if(index==0)
         return null;  // this means it isn't available

      plan=new RsPlan();

      string = protocol.readString();
      plan.setCaption(string);

      nObject = input.readInt();
      for(int i=0; i<nObject; i++){
         index = input.readInt();
         name = protocol.readString();
         if(index == RsProtocol.PLAN_WALL){
            wall = new RsWall(name, plan);
            double [] g = receiveGeometry(input);
            if(g!=null)
               wall.setGeometry(g);
            plan.addObject(wall);

         }else if(index == RsProtocol.PLAN_TARGET){
            target = new RsTarget(name, plan);
            double [] g = receiveGeometry(input);
            if(g!=null)
               target.setGeometry(g);
            plan.addObject(target);

         }else if(index == RsProtocol.PLAN_PLACEMENT){
            placement = new RsPlacement(name, plan);
            double [] g = receiveGeometry(input);
            if(g!=null)
               placement.setGeometry(g);
            plan.addObject(placement);

         }else if(index == RsProtocol.PLAN_NAV_NODE){
            navNode = new RsNavNode(name, plan);
            double []nodeCoordinate = new double[2];
            nodeCoordinate[0] = input.readDouble();
            nodeCoordinate[1] = input.readDouble();
            navNode.setGeometry(nodeCoordinate);
            string = protocol.readString();
            navNode.setLabel(string);
            navNode.setColor(receiveColor(input));
            plan.addObject(navNode);

         }else if(index == RsProtocol.PLAN_NAV_LINK){
            navLink = new RsNavLink(name, plan);
            String  name0 = protocol.readString();
            String  name1 = protocol.readString();
            RsNavNode node0 = (RsNavNode)plan.getObjectByName(name0);
            RsNavNode node1 = (RsNavNode)plan.getObjectByName(name1);
            if(node0==null || node1==null)
               throw new IOException("Invalid nodes for link "+name+": "+name0+", "+name1);
            navLink.setNodes(node0, node1);
            string = protocol.readString();
            navLink.setLabel(string);
            navLink.setColor(receiveColor(input));
            plan.addObject(navLink);

         }else if(index == RsProtocol.PLAN_PAINT){
            paint = new RsPaint(name, plan);
            double [] g = receiveGeometry(input);
            if(g!=null)
               paint.setGeometry(g);
            paint.setColor(receiveColor(input));
            plan.addObject(paint);

         }else{
            // this will never happen unless somebody makes a coding mistake
            throw new IOException("Attempt to encode unimplemented plan object");
         }
      }

      return plan;
   }


   static double [] receiveGeometry(DataInputStream input) throws IOException {

      int n=input.readInt();
      if(n==0)
        return null;
      double [] g = new double[n];
      for(int i=0;i<n;i++)
        g[i] = input.readDouble();
      return g;
   }

   static Color receiveColor(DataInputStream input) throws IOException {
      int index=input.readInt();
      if(index==0){
         return null;
      }else{
         int r = input.readInt();
         int g = input.readInt();
         int b = input.readInt();
         return new Color(r,g,b);
      }
   }
}
