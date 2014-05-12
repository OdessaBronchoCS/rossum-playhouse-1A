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

  TO DO:  include a method to check to see if all the
          body specifications are complete.  For example,
          we cannot send a body if it does not have a
          wheel system.    Make the send() method detect
          such errors and throw an exception (maybe RsBodyEncodingException)
*/


import java.io.*;
import java.awt.Color;

class RsProtocolBodyEncoder {

   public static void writeToFile(String fileName, RsBody body) throws IOException {

        FileOutputStream  fos = new FileOutputStream(fileName);
        DataOutputStream  dos = new DataOutputStream(new BufferedOutputStream(fos));
        RsProtocol        p   = new RsProtocol();

        p.setInputOutputStreams(null, dos);
        send(p, body);
   }

   public static void send(RsProtocol protocol, RsBody body) throws IOException {

      DataOutputStream  output = protocol.output;   // merely to save some typing
      RsBodyPart       [] parts;
      RsBodyCircle        circle;
      RsBodyShape         shape;
      RsBodyTargetSensor  tSensor;
      RsBodyContactSensor cSensor;
      RsBodyRangeSensor   rSensor;
      RsBodyPaintSensor   pSensor;
      protocol.writeString(body.name);

      // send body part specifications

      parts = body.getBodyPartArray();
      if(parts == null)
         throw new IOException("Attempt to send an empty body definition");

      output.writeInt(parts.length);
      for(int i=0; i<parts.length; i++){
         // send part-specific elements
         if(parts[i] == null){
            output.writeInt(-1);
            continue;
         }

         output.writeInt(parts[i].getID());

         if(parts[i] instanceof RsBodyCircle){
            circle = (RsBodyCircle)parts[i];
            output.writeInt(RsProtocol.BODY_CIRCLE);
            output.writeDouble(circle.refXCenter);
            output.writeDouble(circle.refYCenter);
            output.writeDouble(circle.refRadius);

         }else if(parts[i] instanceof RsBodyTargetSensor){
            tSensor = (RsBodyTargetSensor)parts[i];
            output.writeInt(RsProtocol.BODY_TARGET_SENSOR);
            sendSegment(output, tSensor.refSegment);
            output.writeDouble(tSensor.xDetector);
            output.writeDouble(tSensor.yDetector);
            output.writeDouble(tSensor.sightAngle);
            output.writeDouble(tSensor.width);
            output.writeDouble(tSensor.maxRange);
            output.writeInt(tSensor.nWidthBin);
            output.writeInt(tSensor.nRangeBin);

         }else if(parts[i] instanceof RsBodyContactSensor){
            cSensor = (RsBodyContactSensor)parts[i];
            output.writeInt(RsProtocol.BODY_CONTACT_SENSOR);
            sendSegment(output, cSensor.refSegment);

         }else if(parts[i] instanceof RsBodyRangeSensor){
            rSensor = (RsBodyRangeSensor)parts[i];
            output.writeInt(RsProtocol.BODY_RANGE_SENSOR);
            sendSegment(output, rSensor.refSegment);
            output.writeDouble(rSensor.xDetector);
            output.writeDouble(rSensor.yDetector);
            output.writeDouble(rSensor.sightAngle);
            output.writeDouble(rSensor.maxRange);
            output.writeInt(rSensor.nRangeBin);

         }else if(parts[i] instanceof RsBodyPaintSensor){
            pSensor = (RsBodyPaintSensor)parts[i];
            output.writeInt(RsProtocol.BODY_PAINT_SENSOR);
            sendSegment(output, pSensor.refSegment);
            output.writeDouble(pSensor.xDetector);
            output.writeDouble(pSensor.yDetector);
            if(pSensor.regionSensitivity==null)
               output.writeInt(0);
            else{
               output.writeInt(pSensor.regionSensitivity.length);
               for(int iRegion=0; iRegion<pSensor.regionSensitivity.length; iRegion++)
                  output.writeInt(pSensor.regionSensitivity[iRegion]);
            }
         }else if(parts[i] instanceof RsBodyPainter){
            // note that all painting-specific elements are CURRENTLY sent
            // over as part of the painter activation request. we do this
            // because they are changeable in nature.
            RsBodyPainter pPainter = (RsBodyPainter)parts[i];
            output.writeInt(RsProtocol.BODY_PAINTER);
            sendSegment(output, pPainter.refSegment);

         }else if(parts[i] instanceof RsWheelSystem){
            // to do:  obviously: other wheel systems
            output.writeInt(RsProtocol.WHEEL_SYSTEM);
            RsWheelSystem          ws = (RsWheelSystem)parts[i];
            if(parts[i] instanceof RsDifferentialSteering){
               RsDifferentialSteering ds = (RsDifferentialSteering)parts[i];
               output.writeInt(0);
               output.writeDouble(ds.trackWidth);
               output.writeDouble(ds.driveWheelRadius);
            }else if(parts[i] instanceof RsAckermanSteering){
               RsAckermanSteering acks = (RsAckermanSteering)parts[i];
               output.writeInt(1);
               output.writeDouble(acks.trackWidth);
               output.writeDouble(acks.wheelBase);
               output.writeDouble(acks.driveWheelRadius);
               output.writeDouble(acks.maxAbsSteeringAngle);
               output.writeBoolean(acks.dualSteering);
            }
            if(ws.wheels==null){
               output.writeInt(0);
            }else{
               output.writeInt(ws.wheels.length);
               for(int iWheel=0; iWheel<ws.wheels.length; iWheel++){
                  if(ws.wheels[iWheel] instanceof RsWheelCaster){
                      output.writeInt(1);
                  }else{
                      output.writeInt(0);
                  }
                  sendSegment(output,ws.wheels[iWheel].refSegment);
                  output.writeDouble(ws.wheels[iWheel].x);
                  output.writeDouble(ws.wheels[iWheel].y);
                  output.writeDouble(ws.wheels[iWheel].wheelRadius);
                  output.writeDouble(ws.wheels[iWheel].getMaxAbsSteeringAngle());
                  output.writeInt(   ws.wheels[iWheel].encoderIntervalCount);
               }
            }

         }else if(parts[i] instanceof RsBodyShape){
            // note that most body parts are derived from RsBodyShape,
            // and would pass the instanceof test for this block.
            // so we need to put this AFTER the others.
            shape = (RsBodyShape)parts[i];
            output.writeInt(RsProtocol.BODY_SHAPE);
            sendSegment(output, shape.refSegment);

         }else{
            output.writeInt(RsProtocol.UNDEFINED_BODY_PART);
            continue;  //  we haven't yet written protocol for sending this part
         }


         // send elements common to all parts
         protocol.writeString(parts[i].getName());
         sendColor(output, parts[i].fillColor);
         sendColor(output, parts[i].lineColor);
         sendColor(output, parts[i].hotFillColor);
         sendColor(output, parts[i].hotLineColor);
      }

      output.flush();
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

   private static void sendSegment(DataOutputStream output, RsSegment [] segment) throws IOException {
      if(segment==null){
         output.writeInt(0);
      }else{
         output.writeInt(segment.length);
         for(int j=0;j<segment.length;j++){
             output.writeDouble(segment[j].x);
             output.writeDouble(segment[j].y);
         }
      }
   }

}

