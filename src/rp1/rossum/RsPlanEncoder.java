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

class RsPlanEncoder {

   public static void send(RsProtocol protocol, RsPlan plan) throws IOException {

      DataOutputStream  output = protocol.output;   // merely to save some typing

      RsObject  [] object;
      RsWall       wall;
      RsTarget     target;
      RsPlacement  placement;
      RsNavNode    navNode;
      RsNavLink    navLink;
      RsPaint      paint;

      if(plan==null){
         output.writeInt(0);
         output.flush();
         return;
      }else{
         output.writeInt(1);
      }

      protocol.writeString(plan.getCaption());

      // send plan object specifications

      object = plan.getObjectArray();
      if(object == null)
         throw new IOException("Attempt to send an empty plan definition");

      output.writeInt(object.length);
      for(int i=0; i<object.length; i++){
         // send object-specific elements

         if(object[i] instanceof RsWall){
            wall = (RsWall)(object[i]);
            output.writeInt(RsProtocol.PLAN_WALL);
            protocol.writeString(object[i].getName());
            sendGeometry(output, wall.getGeometry());

         }else if(object[i] instanceof RsTarget){
            target = (RsTarget)(object[i]);
            output.writeInt(RsProtocol.PLAN_TARGET);
            protocol.writeString(object[i].getName());
            sendGeometry(output, target.getGeometry());

         }else if(object[i] instanceof RsPlacement){
            placement = (RsPlacement)(object[i]);
            output.writeInt(RsProtocol.PLAN_PLACEMENT);
            protocol.writeString(object[i].getName());
            sendGeometry(output, placement.getGeometry());

         }else if(object[i] instanceof RsNavNode){
            navNode = (RsNavNode)(object[i]);
            output.writeInt(RsProtocol.PLAN_NAV_NODE);
            protocol.writeString(navNode.getName());
            output.writeDouble(navNode.x);
            output.writeDouble(navNode.y);
            protocol.writeString(navNode.label);
            sendColor(output, navNode.lineColor);

         }else if(object[i] instanceof RsNavLink){
            navLink = (RsNavLink)(object[i]);
            output.writeInt(RsProtocol.PLAN_NAV_LINK);
            protocol.writeString(navLink.getName());
            protocol.writeString(navLink.n0.getName());
            protocol.writeString(navLink.n1.getName());
            protocol.writeString(navLink.label);
            sendColor(output, navLink.lineColor);

         }else if(object[i] instanceof RsPaint){
            paint = (RsPaint)(object[i]);
            output.writeInt(RsProtocol.PLAN_PAINT);
            protocol.writeString(object[i].getName());
            sendGeometry(output, paint.getGeometry());
            sendColor(output, paint.fillColor);
         }
      }


      output.flush();
   }


   private static void sendGeometry(DataOutputStream output, double [] geometry) throws IOException{
       output.writeInt(geometry.length);
       for(int i=0; i<geometry.length; i++)
          output.writeDouble(geometry[i]);
   }

   private static void sendColor(DataOutputStream  output, Color color) throws IOException{
      if(color==null){
         output.writeInt(0);
      }else{
         output.writeInt(1);
         output.writeInt(color.getRed());
         output.writeInt(color.getGreen());
         output.writeInt(color.getBlue());
      }
   }

}
