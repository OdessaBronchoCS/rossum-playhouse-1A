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



package rp1.simulator;
import rp1.rossum.*;


/*


  SimCanvas.java

      The image are in the footprint logic was padded out by
      2 pixels to cover any small errors introduced by round-off.

      paintBodies() may be invoked even when there are no bodies
      in the list...  in such a case, it is used to clear the
      display and remove any left-over robot images from view.


*/


import java.awt.*;



/**
 * The canvas on which the simulation picture is rendered; contains
 * the high-level rendering methods.
 */

public class SimCanvas extends Canvas implements SimStateDataInterface
{

   /**
	 * 
	 */
	private static final long serialVersionUID = -6461208230953041167L;
// elements used for converting coordinate systems
   private double   x0,  y0,  x1,  y1;
   private double  px0, py0, px1, py1;
   private int     ix0, iy0, ix1, iy1;

   private SimSession    session;
   private RsPlan        plan;
   private Font   font;

   protected RsBody      [] bodyArray;
   protected SimPaintBox [] paintBoxArray;


   private EventQueue eventQueue;


   private Image        bgBuffer;
   private Image        fgBuffer;
   private RsRectangle  footPrint;
   private boolean      navNetPaintingOption;
   private RsTransform  graphicsTransform;
   private RsTransform  scratchTransform;


   public SimCanvas(SimSession session){
      this.session = session;
      plan      = session.getPlan();
      font      = null;
      footPrint = null;
      navNetPaintingOption = true;
      setBackground(Color.white);

      graphicsTransform = null;
      scratchTransform = new RsTransform();

      eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
      enableEvents(0);

      bodyArray = null;
      bgBuffer  = null;
      fgBuffer  = null;

   }



   public void queueAnimationEvent(){
      SimAnimationEvent m = new SimAnimationEvent(this);
      eventQueue.postEvent(m);
   }

   @Override
public void processEvent(AWTEvent event){
      if(event.getID()==SimAnimationEvent.SIM_ANIMATION_EVENT){
         SimAnimationEvent sme = (SimAnimationEvent)event;
         processSimAnimationEvent(sme);
      }else{
         super.processEvent(event);
      }
   }

   private void processSimAnimationEvent(SimAnimationEvent sme){

      if(graphicsTransform==null)
         return;

      session.stateDataExchange.retrieveStateData(this);

      Graphics  g  = getGraphics();
      paintBodies(g, bodyArray);
      g.dispose();
  }

   public void setNavNetPaintingOption(boolean value){
      navNetPaintingOption = value;
   }

   public boolean getNavNetPaintingOption(){
      return navNetPaintingOption;
   }

   @Override
public void update(Graphics g){
      paint(g);
   }

   @Override
public void paint(Graphics gp){
      double rAspect, sAspect, a;
      double width, height;

      Graphics   g;
      Dimension  size;

      if(gp==null)
         return;


      size = getSize();

      if(fgBuffer!=null){
         // we may be able to short circuit the draw operation by using
         // data already in the animation buffer.
         Rectangle clip = gp.getClipBounds();
         if(clip.x !=0 || clip.y!=0 || clip.width<size.width || clip.height<size.height){
             gp.drawImage(fgBuffer, clip.x, clip.y, clip.x+clip.width, clip.y+clip.height,
                                    clip.x, clip.y, clip.x+clip.width, clip.y+clip.height, null);
             gp.dispose();
             return;
         }
      }

      session.stateDataExchange.retrieveStateData(this);

      // clear out state values used for double buffering
      footPrint = null;
      bgBuffer  = null;
      fgBuffer  = null;


      RsRectangle r   = plan.getBounds();
      if(r.width<=0 || r.height<=0 || size.width<4 || size.height<4)
         return;   // trivial image, do nothing.

      bgBuffer=createImage(size.width, size.height);
      fgBuffer=createImage(size.width, size.height);

      rAspect=r.width/r.height;
      sAspect=(double)size.width/(double)size.height;
      a=rAspect/sAspect;
      if(a<1.0){
         /* it's height limited */
         height=size.height;
         if(height>30)
            height-=15;   // allow a little breathing room for the graphic
         width=rAspect*height;
      }else{
         width=size.width;
         if(width>30)
            width-=15;    // breathing room
         height=width/rAspect;
      }

      px0 = (size.width-width)/2;
      px1 = px0 + width;
      ix0 = (int)Math.floor(px0);
      ix1 = (int)Math.floor(px1);

      py1 = (size.height-height)/2;
      py0 = py1 + height;
      iy0 = (int)Math.floor(py0);
      iy1 = (int)Math.floor(py1);

      x0=r.x;
      y0=r.y;
      x1=r.x+r.width;
      y1=r.y+r.height;

      graphicsTransform = new RsTransform();
      double sx = (px1-px0)/(x1-x0);
      double sy = (py1-py0)/(y1-y0);
      graphicsTransform.m11 = sx;
      graphicsTransform.m12 = 0;
      graphicsTransform.m13 = -sx*x0+px0;
      graphicsTransform.m21 = 0;
      graphicsTransform.m22 = sy;
      graphicsTransform.m23 = -sy*y0+py0;


      g = bgBuffer.getGraphics();
      if(font==null){
         font=new Font("SansSerif", Font.BOLD, 14);
      }

      g.setFont(font);
      g.setColor(Color.white);
      g.fillRect(0, 0, size.width, size.height);

      // Loop through the objects, plot them

      RsObject [] object;

      scratchTransform.copy(graphicsTransform);
      scratchTransform.m13 = Math.floor(scratchTransform.m13+0.5);
      scratchTransform.m23 = Math.floor(scratchTransform.m23+0.5);
      object = plan.getObjectArray();
      if(object!=null && object.length>0){
         for(int iObject=0; iObject<object.length; iObject++){
            if(!object[iObject].getSelected())
               continue;
            if(!navNetPaintingOption){
               if(object[iObject] instanceof RsNavNode ||
                  object[iObject] instanceof RsNavLink)
                     continue;
            }
            object[iObject].paint(g, scratchTransform);
         }
      }

      repaintPainters(g, graphicsTransform);


      g.dispose();
      gp.drawImage(bgBuffer, 0, 0, null);
      g = fgBuffer.getGraphics();
      g.drawImage(bgBuffer, 0, 0, null);
      g.dispose();

      paintBodies(gp, bodyArray);
   }

   int xMap(double x){
      return (int)Math.floor((ix1-ix0)*(x-x0)/(x1-x0)+ix0+0.5);
   }

   int yMap(double y){
      return (int)Math.floor((iy1-iy0)*(y-y0)/(y1-y0)+iy0+0.5);
   }

   public RsPoint inverseMap(int ix, int iy){
      RsPoint p = new RsPoint();
      inverseMap(ix, iy, p);
      return p;
   }

   public void inverseMap(int ix, int iy, RsPoint p){
      p.x = (x1-x0)*(ix-ix0)/(ix1-ix0)+x0;
      p.y = (y1-y0)*(iy-iy0)/(iy1-iy0)+y0;
   }




   private RsRectangle paintPainters(){

      SimPaintBox [] paintBox = session.getPaintBoxArray();
      if(paintBox==null)
         return null;

      RsRectangle r = null;
      Graphics    g = bgBuffer.getGraphics();

      for(int i=0; i<paintBox.length; i++)
         r = paintBox[i].paintAtTime(g, graphicsTransform, r, 0);

      g.dispose();
      return r;
   }

   private void repaintPainters(Graphics g, RsTransform graphicsTransform){
      SimPaintBox [] paintBox = session.getPaintBoxArray();
      if(paintBox!=null){
         for(int i=0; i<paintBox.length; i++)
            paintBox[i].repaint(g, graphicsTransform);
      }
   }



   public synchronized void paintBodies(Graphics gp, RsBody [] bodies){

      if(bodies==null && footPrint==null)
         return;

      int         fx0, fy0, fx1, fy1;
      Graphics    g;
      RsRectangle oldFootPrint=null;
      RsBody      body;
      Dimension   size = getSize();

      RsRectangle  rpb = paintPainters();


      if(rpb!=null){
         if(footPrint==null)
            footPrint=rpb;
         else
            footPrint.union(rpb);
      }


      g = fgBuffer.getGraphics();


      // the footPrint reference indicates whether we expect that there is
      // already one or more robots drawn to the fgBuffer.   If there is, we must
      // blit the background on top of it to obliterate the old robot rendering.
      // note that the paint() method sets the footPrint to null.
      // note also that if there is no placement, the footPrint is set
      // to null.

      if(footPrint!=null && footPrint.width>0){
         footPrint.x-=2;
         footPrint.y-=2;
         footPrint.width+=5;
         footPrint.height+=5;
         footPrint.clip(0, 0, size.width, size.height);
         fx0 = (int)footPrint.x;
         fy0 = (int)footPrint.y;
         fx1 = (int)(footPrint.x+footPrint.width);
         fy1 = (int)(footPrint.y+footPrint.height);
         g.drawImage(bgBuffer, fx0, fy0, fx1, fy1, fx0, fy0, fx1, fy1, null);
      }

      if(bodies!=null){
         oldFootPrint = footPrint;
         footPrint    = new RsRectangle();
         for(int iBody=0; iBody<bodies.length; iBody++){
            body = bodies[iBody];
            if(!body.getPlacement())
               continue;

            scratchTransform.concat(graphicsTransform, body.motion.transform);
            scratchTransform.m13 = Math.floor(scratchTransform.m13+0.5);
            scratchTransform.m23 = Math.floor(scratchTransform.m23+0.5);
            body.paint(g, scratchTransform);
            RsRectangle rx = new RsRectangle(body.refBounds);
            rx.transform(scratchTransform);
            footPrint.union(rx);
         }
      }

      g.dispose();

      if(oldFootPrint==null)
         oldFootPrint=footPrint;
      else
         oldFootPrint.union(footPrint);


      // add a little breathing room around footprint, and also clip
      // it to the display.   if the robot has wandered off the display,
      // then clipping may produce a zero-dimensioned rectangle.
      // note that in the AWT, drawImage does NOT like image dimensions
      // that were off the display...  strangeness might ensue if we didn't
      // prevent that from happening.

      oldFootPrint.x-=2;
      oldFootPrint.y-=2;
      oldFootPrint.width+=4;
      oldFootPrint.height+=4;
      oldFootPrint.clip(0, 0, size.width, size.height);

      if(oldFootPrint.width==0 && oldFootPrint.height==0){
         // for whatever reason, there's nothing to draw */
         footPrint=null;
      }else{
         fx0 = (int)oldFootPrint.x;
         fy0 = (int)oldFootPrint.y;
         fx1 = (int)(oldFootPrint.x+oldFootPrint.width);
         fy1 = (int)(oldFootPrint.y+oldFootPrint.height);
         gp.drawImage(fgBuffer, fx0, fy0, fx1, fy1, fx0, fy0, fx1, fy1, null);
      }
   }


   public RsTransform getGraphicsTransform(){
      // return a copy of graphicsTransform (keeps it safe
      // from manipulation by others...  not that I expect
      // that to ever happen).
      return new RsTransform(graphicsTransform);
   }


   // methods in support of SimStateDataInterface
   public RsBody [] getBodyArray(){
      return bodyArray;
   }

   public void setBodyArray(RsBody [] bodyArray){
       this.bodyArray = bodyArray;
   }

   public SimPaintBox [] getPaintBoxArray(){
       return paintBoxArray;
   }

   public void setPaintBoxArray(SimPaintBox [] paintBoxArray){
      this.paintBoxArray = paintBoxArray;
   }

}
