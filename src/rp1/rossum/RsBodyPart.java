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

/*  RsBodyPart


  Naming conventions

    Elements which are named "ref" or "reference" provide coordinates and
    specifications for the robot prior to being transformed (moved and rotatated).
    An element with a related name gives the position of the body at its most
    recent transform.  Thus refBounds is the bounding bounds for a body
    part and bounds is the equivalent for the body part at its current position
    and orientation.

    Note that once specified, the coordinates stored in "reference" elements
    should never change.

    Hot and Cold




   About copyStateData()

   copyStateData() allows us to copy the state of one body part to another.
   you should always copy identical body parts (preferably, clones of another).
   there is little intrinsic protection in the way these methods are written,
   so be careful.  Note that this operation is NOT synchronized.   Typically,
   when we're copying the state of one body to another, we do so across dozens
   of pairs of objects.   The overhead of synchronization would clobber us.


*/

import java.awt.Color;
import java.awt.Graphics;



/**
 * The abstract base class for all body parts.
 *
 * The RsBodyPart class defines two general states for body parts: hot and cold.
 * These states are mainly used for rendering purposes. An application can
 * define different sets of color specifications depending on what state
 * a body part is set.  These color settings include
 * <ul>
 * <li> lineColor for edges
 * <li> fillColor for area fills
 * <li> hotLineColor for edges when body part is in hot state
 * <li> hotFillColor for area fills when body part is hot state
 * </ul>
 *
 * The interpretation of these states differs depending on the derived classes.
 * For a sensor class, the hot state occurs when the sensors register a detection,
 * the cold state when they register no detection. For example, when a contact
 * sensor in pressed against an object, it switches to its hot state.
 * For the body-painter class, the body part goes to its hot state when the
 * painter is activated.  Actuators (motors and wheels) would be "hot" when they
 * are activated. For some body parts, the hot state is not currently assigned an
 * interpretation for the hot state.
 *
 * As mentioned above, in either hot or cold state,
 * RsBodyPart objects may be assigned two color values for rendering: the
 * fill color (applies to polygonal forms only) and the line color.
 * If these parameters are assigned null values, then the related
 * feature, if any, will not be rendered. When a fill is performed, the line color
 * may be used to add a contrasting edge to polygonal
 * features.  Even if you prefer not to add a contrasting edge, you may want to
 * specify a line color with the same value as the fill color.
 * One many graphics systems, the area-fill algorithm used for polygons
 * sometimes cover slightly less extent than the lines that define their boundaries.
 * So if you "fill" two adjacent polygons, you may find slight
 * gaps between them unless you also "draw" their edge lines.
 *
 * By default both the line and fill color values are defined to be
 * Color.lightGray.  If you prefer not to render a particular feature
 * you may use the RsBodyPart.setFillColor() and RsBodyPart.setLineColor
 * methods to assign null values (which will suppress drawing).  Even if
 * a feature is not assigned a color value (and is not visible) it will
 * be used in modeling.
 *
 */

public abstract class RsBodyPart extends RsComponent {

   private static int    nextPartSerialNumber=1;
   protected String      name;

   protected RsRectangle refBounds;
   protected RsRectangle bounds;

   protected boolean     hot;

   protected Color       fillColor;
   protected Color       lineColor;
   protected Color       hotFillColor;
   protected Color       hotLineColor;

   private   int         partID;

   public RsBodyPart(){
      refBounds = null;
      bounds    = null;
      fillColor    = Color.lightGray;
      lineColor    = Color.lightGray;
      hotFillColor = Color.orange;
      hotLineColor = Color.orange;

      name         = "Unnamed";
      partID       = nextPartSerialNumber++;
      hot          = false;
   }


   /** Strictly for rossum internals, allows the body decoder to set part
    *  the part id of the next body part to be transmitted
    */
   static protected void setNextPartSerialNumber(int partID){
       nextPartSerialNumber = partID;
   }

   public void  setFillColor(Color colorRef){
      fillColor=colorRef;
   }

   public void  setLineColor(Color colorRef){
      lineColor=colorRef;
   }

   public void setHotFillColor(Color c){
      hotFillColor = c;
   }

   public void setHotLineColor(Color c){
      hotLineColor = c;
   }

   public Color getLineColor(){
      if(hot)
         return hotLineColor;
      else
         return lineColor;
   }

   public Color getFillColor(){
      if(hot)
         return hotFillColor;
      else
         return fillColor;
   }

   public boolean isASensor(){
      return false;
   }

   public boolean searchForCollisions(RsPlan plan, RsMotion motion){
      return false;
   }

   public void paint(Graphics g, RsTransform gt){
     // the RsTransform gt (graphics transform) supplies the
     // data necessary to map double-valued points to pixel coordinates
     // it is expected that this method will be overridden.
   }

   public void setName(String nameReference){
      name=nameReference;
   }

   public String getName(){
      return name;
   }


   public int getID(){
      return partID;
   }


   public void copyStateData(RsBodyPart dataSource){
      hot       = dataSource.hot;
   }

   /**
    * resetStateData is usually called when a placement is established.
    * it is implemented as a do-nothing and overridden by derived
    * classes according to their specific behaviors
    */
   public void resetStateData(){
      // here this is a do-nothing, to be overridden by derived classes.
   }

   public void setHot(boolean status){
      hot=status;
   }

   public boolean getHot(){
      return hot;
   }
}





