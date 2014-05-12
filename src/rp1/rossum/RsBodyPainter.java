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


// TO DO:  The Paint Pattern stuff is unimplemented at this time
//         affected classes:  RsEncoder, RsDecoder
//         RsClient, RsConection, and
//         the associated modules in the simulator package


package rp1.rossum;

import java.awt.Color;


/**
 * The class that enables the robot to deposit paint trails
 *in the simulation environment; a robot may have more than one
 *of these.
 *
 */

public class RsBodyPainter extends RsBodyShape
{

	private static final long serialVersionUID = 1L;
	
   double          minTrailerSegmentLength, minTrailerSegmentLength2;

   Color           paintColor[];
   RsPolygon       paintPattern;
   boolean         trailerDefined;
   double          xTrailer, yTrailer;
   double          wTrailer;   // width of trailer

   private boolean painterActivationStatus;

   public RsBodyPainter(){
      super();
      standardInitializations();

   }

   public RsBodyPainter(double []bodyPoints, int nPoints){
      super(bodyPoints, nPoints);
      standardInitializations();
   }

   private void standardInitializations(){
      minTrailerSegmentLength  = 0.01;  // 1 centimeter
      minTrailerSegmentLength2 = minTrailerSegmentLength*minTrailerSegmentLength;
   }



   public void removePaintPattern(){
      paintPattern = null;
   }

   public void removeTrailer(){
      trailerDefined          = false;
      xTrailer                = 0;
      yTrailer                = 0;
      wTrailer                = 0;
   }

   public boolean getPainterActivationStatus(){
      return painterActivationStatus;
   }

   public void setPainterActivationStatus(boolean status){
      painterActivationStatus = status;
   }

   public void setPaintPattern(double points[], int nPoints){
      if(points==null || nPoints==0){
         removePaintPattern();
      }else{
         try {
            paintPattern = new RsPolygon(points, nPoints);
         }catch(RsPolygonException rpe){
            paintPattern = null;
         }
      }

   }

   public void setTrailerPosition(double x, double y){
         trailerDefined = true;
         xTrailer = x;
         yTrailer = y;
   }

   public void setTrailerWidth(double width){
      trailerDefined          = true;
      if(width<0){
         width = 0;
      }
      wTrailer = width;
   }


   public void setTrailerMinimumSegmentLength(double minLength){
      minTrailerSegmentLength  = minLength;
      minTrailerSegmentLength2 = minLength*minLength;
   }


   public void setPaintColor(Color color){
      if(color==null){
         paintColor = null;
      }else{
         paintColor    = new Color[1];
         paintColor[0] = new Color(color.getRed(), color.getGreen(), color.getBlue());
      }
   }

   public void setPaintColor(Color color[]){
      paintColor = RsColor.copyColorList(color);
   }


   public void copyStateData(RsBodyPainter dataSource){
      super.copyStateData(dataSource);

      painterActivationStatus = dataSource.painterActivationStatus;
      trailerDefined          = dataSource.trailerDefined;
      xTrailer                = dataSource.xTrailer;
      yTrailer                = dataSource.yTrailer;
      wTrailer                = dataSource.wTrailer;
      paintColor              = RsColor.copyColorList(dataSource.paintColor);

      minTrailerSegmentLength  = dataSource.minTrailerSegmentLength;
      minTrailerSegmentLength2 = dataSource.minTrailerSegmentLength2;
   }
}
