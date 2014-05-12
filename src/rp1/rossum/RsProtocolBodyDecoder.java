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

class RsProtocolBodyDecoder {

   public static RsBody receive(RsProtocol protocol) throws IOException {

      DataInputStream   input = protocol.input;   // merely to save me some typing
      RsBodyPart    []  part;
      RsBody            body;

      int               nPart;
      int               index;
      int               partID;

      String name = protocol.readString();
      if(name==null)
         name="Anonymous";
      body=new RsBody(name);

      nPart = input.readInt();
      part = new RsBodyPart[nPart];
      for(int i=0; i<nPart; i++){
         // receive part-specific elements
         partID = input.readInt();
         if(partID<0){
            part[i]=null;
            continue;
         }

         RsBodyPart.setNextPartSerialNumber(partID);

         index = input.readInt();
         if(index==RsProtocol.BODY_CIRCLE){
            double x = input.readDouble();
            double y = input.readDouble();
            double radius = input.readDouble();
            part[i]=new RsBodyCircle(x, y, radius);

         }else if(index==RsProtocol.BODY_PAINTER){
            int nCoordinate = input.readInt()*2;
            double [] c = new double[nCoordinate];
            for(int j=0; j<nCoordinate; j++)
               c[j]=input.readDouble();
            part[i] = new RsBodyPainter(c, nCoordinate/2);

         }else if(index==RsProtocol.BODY_TARGET_SENSOR){
            int nCoordinate = input.readInt()*2;
            double [] c = new double[nCoordinate];
            for(int j=0; j<nCoordinate; j++)
               c[j]=input.readDouble();
            double xDetector  = input.readDouble();
            double yDetector  = input.readDouble();
            double sightAngle = input.readDouble();
            double width      = input.readDouble();
            double maxRange   = input.readDouble();
            int    nWidthBin  = input.readInt();
            int    nRangeBin  = input.readInt();
            part[i] = new RsBodyTargetSensor(
                              c, nCoordinate/2,
                              xDetector, yDetector, sightAngle,
                              width, maxRange,
                              nWidthBin, nRangeBin);

         }else if(index==RsProtocol.BODY_CONTACT_SENSOR){
            int nCoordinate = input.readInt()*2;
            double [] c = new double[nCoordinate];
            for(int j=0; j<nCoordinate; j++)
               c[j]=input.readDouble();
            part[i] = new RsBodyContactSensor(c, nCoordinate/2);

         }else if(index==RsProtocol.BODY_RANGE_SENSOR){
            int nCoordinate = input.readInt()*2;
            double [] c = new double[nCoordinate];
            for(int j=0; j<nCoordinate; j++)
               c[j]=input.readDouble();
            double xDetector  = input.readDouble();
            double yDetector  = input.readDouble();
            double sightAngle = input.readDouble();
            double maxRange   = input.readDouble();
            int    nRangeBin  = input.readInt();
            part[i] = new RsBodyRangeSensor(
                              c, nCoordinate/2,
                              xDetector, yDetector, sightAngle,
                              maxRange, nRangeBin);

         }else if(index==RsProtocol.BODY_PAINT_SENSOR){
            RsBodyPaintSensor pSensor;
            int nCoordinate = input.readInt()*2;
            double [] c;
            if(nCoordinate==0){
               c = null;
            } else{
               c = new double[nCoordinate];
               for(int j=0; j<nCoordinate; j++)
                  c[j]=input.readDouble();
            }
            double xDetector  = input.readDouble();
            double yDetector  = input.readDouble();
            pSensor = new RsBodyPaintSensor(
                                 c, nCoordinate/2,
                                 xDetector, yDetector);
            int nSensitivity = input.readInt();
            if(nSensitivity>0){
                pSensor.regionSensitivity = new int[nSensitivity];
                for(int iSense=0; iSense<nSensitivity; iSense++)
                   pSensor.regionSensitivity[iSense] = input.readInt();
            }
            part[i]=pSensor;
         }else if(index==RsProtocol.WHEEL_SYSTEM){
            RsWheelSystem wheelSystem = null;
            int wheelSystemType = input.readInt();
            if(wheelSystemType == 0){
               double trackWidth       = input.readDouble();
               double driveWheelRadius = input.readDouble();
               wheelSystem = new RsDifferentialSteering(trackWidth, driveWheelRadius);
            }else{
               double  trackWidth          = input.readDouble();
               double  wheelBase           = input.readDouble();
               double  driveWheelRadius    = input.readDouble();
               double  maxAbsSteeringAngle = input.readDouble();
               boolean dualSteering        = input.readBoolean();
               wheelSystem = new RsAckermanSteering(
                                    trackWidth,
                                    wheelBase,
                                    driveWheelRadius,
                                    maxAbsSteeringAngle,
                                    dualSteering);
            }

            int nWheel = input.readInt();
            for(int iWheel=0; iWheel<nWheel; iWheel++){
               int wheelType = input.readInt();
               int nCoordinate = input.readInt()*2;
               double [] c = new double[nCoordinate];
               for(int j=0; j<nCoordinate; j++)
                  c[j]=input.readDouble();
               double  wX        = input.readDouble();
               double  wY        = input.readDouble();
               double  wRadius   = input.readDouble();
               double  wMaxSteer = input.readDouble();
               RsWheel wheel;
               if(wheelType==0){
                  wheel = new RsWheel(      c, nCoordinate/2, wX, wY, wRadius, wMaxSteer);
               }else
                  wheel = new RsWheelCaster(c, nCoordinate/2, wX, wY);
               wheelSystem.addWheel(wheel);
              int      wEncoderIntervalCount = input.readInt();
              if(wEncoderIntervalCount>0)
                   wheel.addEncoder(wEncoderIntervalCount);
            }

            part[i] = wheelSystem;

         }else if(index==RsProtocol.BODY_SHAPE){
            int nCoordinate = input.readInt()*2;
            double [] c = new double[nCoordinate];
            for(int j=0; j<nCoordinate; j++)
               c[j]=input.readDouble();
            part[i] = new RsBodyShape(c, nCoordinate/2);
         }else {
            System.err.println("Protocol exception");
            System.exit(-1);
         }


         // receive elements common to all parts
         part[i].name = protocol.readString();
         part[i].setFillColor(receiveColor(input));
         part[i].setLineColor(receiveColor(input));
         part[i].hotFillColor=receiveColor(input);
         part[i].hotLineColor=receiveColor(input);

         body.addPart(part[i]);

         // Still needed, write out the name of the body part
      }



      return body;
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

