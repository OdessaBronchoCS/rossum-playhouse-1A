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


/*

   The SimPaintBox class is deeply intertwined with the SimDataExchange
   class. This relationship is important to allow data to be moved between
   the main simulator thread (where paint-box data is created) and the
   rendering threads.  The only time methods are
   required is in the transfer.

   SimPaintBox objects originate in two ways:
      as "original" objects in the data-authoring thread (the simulator thread)
      as "copy" objects in the rendering thread.

   When an original object is created, it is assigned a unique ID based on
   the class variable paintBoxSequencer element.  When it is created through
   the SimPaintBox.copy() method, it takes on the same ID as the PaintBox
   from which it was derived. Within the object itself, a similar approach is
   used for PaintBoxNodes.  Thus we can identify associations between paint
   box objects and their components.

   The SimPaintBox and PaintBoxNode objects serve as "wrappers" around the
   geometry elements (x and y coordinates of paint). They also contain
   metadata, linked-list references etc. Although we copy the metadata,
   we do not make redundant copies of the geometry. Instead, the memory
   devoted to geometry (which can be quite large) is shared across
   a paint box and its copies...  Thus we avoid having redundant copies
   of the geomtry. However, this means that the various methods in this
   class must obey one important design contract:

      The Geometry Contract:

      No operation in one thread may write data to or otherwise damage
      the integrity of the geometry elements already exposed to other
      threads.

   The Copy Algorithm

   1.  Find SimPaintBox objects with matching ID's.  If no ID can
       be matched, assume it is a new object.

   2.  We only copy data for nodes that contain more than zero points.
       The last node in the chain will often contain zero points
       because it is created every time there is a property change.
       If there are multiple property changes (as when we change both
       paint color and line thickness), the last node may change
       multiple times, so there is no sense if copying it before we
       are sure that a) it's not going to change and b) there's data
       in it.

   3.  In the present implementation, it is possible to remove all nodes
       from an original object. However, there is never a case where
       we truncate nodes from the end of the list (we never truncate newer
       nodes, leaving older nodes in place). We do not truncate nodes from the
       end of the list.  In future implementations, we may remove nodes
       from the beginning of the list (i.e. remove the oldest nodes)
       due to time-out criteria. This feature is not handled at this time.

   4.  Special case 1:  The original is empty...  If the original
       paint box is empty (has no PaintBoxNode), simply call .clear()
       to remove all data in the copy paint box (which, itself, will
       usually be empty).  We're done.

   5.  Special case 2: The copy is empty.  Goto step


   6.  Unless the entire list in the original was cleared out and
       restarted, the PaintBoxNode ID's at the beginning of the
       list should match.  If they don't we clear out the copy
       list and go to step 8.  Note: in future implementation
       when we implement the ability to drop the oldest nodes,
       there may be a mis-match at the beginning of the lists.
       In that case, we try to match the oldest node on the
       original list with a node somewhere in the copy list.
       If we find one, we just drop any copy nodes that are
       older than the first node in the original list.


   7.  We loop through the nodes while the ID's match...  unless
       something is broken, all the ID's in the copy list should
       match those in the beginning of the original list and we
       shouldn't come up with a mismatch until we reach the
       end of the copy list (the original list may contain more
       nodes than the copy list).  For each node, transfer
       the metadata including the references to the geometry elements
       (these will usually be the same, except when compaction
       has been performmed, but it is easier and costs little to just copy them
       anyway).

    8. Copy all new nodes from the original list to the end of the
       copy list.



*/


/* ABOUT THE FAT LINE

   When the application sets the trailer width to be more than 1 pixel wide,
   the paint methods draw a fat line.  We do this the hard way, by computing the
   polygons ourselves.  The reason I wrote it this way rather than using the
   stroke settings in java.awt.Graphics2 is not because I enjoy flexing my linear
   algebra muscle (if only...), but rather because RP1 was begun before Graphics2
   was available and the effort to retrofit the Graphics outweighed the effort
   to write this thing...  of course, if we do enough of this kind of thing,
   the accumulated effort would make us wish that we went to Graphics2 in the
   first place...
*/


package rp1.simulator;

import rp1.rossum.*;
import java.awt.Color;
import java.awt.Graphics;
import java.lang.Math;



/**
 * The main class for managing paint data generated by clients.
 */

public class SimPaintBox extends RsComponent {


   /**
	 * 
	 */
	private static final long serialVersionUID = 1093947314264586795L;


private class PaintBoxNode {
      PaintBoxNode nextNode;
      PaintBoxNode priorNode;

      int  nodeID;

      // buffer-related elements for single node
      int    nPointsInBuffer;
      int    nPointsAllocated;
      int    firstUnpaintedPoint;
      float  x[], y[];
      double time[];

      // scratch resources
      RsPoint     workPoint;


      // settings related to properties
      double  xTrailer, yTrailer;
      double  wTrailer;
      Color   color[];
      boolean activationStatus;
      int     transitionInterval;
      double  minTrailerSegmentLength, minTrailerSegmentLength2;
      int     continuityID;



      public PaintBoxNode(int nodeID){
        this.nodeID = nodeID;
        nextNode    = null;
        priorNode   = null;
        workPoint   = new RsPoint(0.0, 0.0);
        minTrailerSegmentLength = 1.0e-2;
        minTrailerSegmentLength2 = minTrailerSegmentLength*minTrailerSegmentLength;
      }

      public void allocateGeometry(){
        nPointsAllocated    = 1024; // nPointsInitiallyAllocated;
        nPointsInBuffer     = 0;
        firstUnpaintedPoint = 0;

        x    = new float[ nPointsAllocated];
        y    = new float[ nPointsAllocated];
        time = new double[nPointsAllocated];
      }

      public void copyGeometry(PaintBoxNode source){
         this.nPointsAllocated    = source.nPointsAllocated;
         this.nPointsInBuffer     = source.nPointsInBuffer;
         this.firstUnpaintedPoint = source.firstUnpaintedPoint;

         this.x    = source.x;
         this.y    = source.y;
         this.time = source.time;
      }

      public void copyProperties(PaintBoxNode source){
        // copy properties from old node to new
        this.xTrailer                 = source.xTrailer;
        this.yTrailer                 = source.yTrailer;
        this.wTrailer                 = source.wTrailer;
        this.color                    = source.color;
        this.activationStatus         = source.activationStatus;
        this.transitionInterval       = source.transitionInterval;
        this.minTrailerSegmentLength  = source.minTrailerSegmentLength;
        this.minTrailerSegmentLength2 = source.minTrailerSegmentLength2;
        this.continuityID             = source.continuityID;
     }

      public void addPoint(double x, double y, double time){
         if(nPointsInBuffer>0){
            double dx, dy;
            dx = x-this.x[nPointsInBuffer-1];
            dy = y-this.y[nPointsInBuffer-1];
            if(dx*dx+dy*dy<minTrailerSegmentLength2)
               return;
         }

         if(nPointsInBuffer==nPointsAllocated){
            PaintBoxNode nextNode = addNode();
            // copy last two points from current node to next node;
            int k = nPointsInBuffer-3;
            for(int i=0; i<3; i++){
               nextNode.x[i]    = this.x[k+i];
               nextNode.y[i]    = this.y[k+i];
               nextNode.time[i] = this.time[k+i];
            }
            nextNode.addPoint(x, y, time);
            nextNode.firstUnpaintedPoint = 2;
            nextNode.xTrailer            = this.xTrailer;
            nextNode.yTrailer            = this.yTrailer;
            nextNode.wTrailer            = this.wTrailer;
            nextNode.color               = RsColor.copyColorList(this.color);
            nextNode.activationStatus    = true;
            nextNode.transitionInterval  = this.transitionInterval;
            nextNode.minTrailerSegmentLength    = this.minTrailerSegmentLength;
            nextNode.minTrailerSegmentLength2   = this.minTrailerSegmentLength2;
         }else{
            this.x[   nPointsInBuffer] = (float)x;
            this.y[   nPointsInBuffer] = (float)y;
            this.time[nPointsInBuffer] = time;
            nPointsInBuffer++;
         }
      }

      public void trim(){
         // triming is expensive. to avoid cases where we repeatedly trim
         // a very small amount of storage, we only trim if the buffer
         // has a non-trivial block of empty space.
         if(nPointsInBuffer>0 && nPointsAllocated-nPointsInBuffer>=32){
             float   px[], py[];
             double  t[];
             int     i;

             nPointsAllocated = nPointsInBuffer;

             px = new float[nPointsInBuffer];
             for(i=0; i<nPointsInBuffer; i++)
                px[i] = x[i];
             x  = px;
             px = null;  // done to enable garbage collection ASAP.

             py = new float[nPointsInBuffer];
             for(i=0; i<nPointsInBuffer; i++)
                py[i] = y[i];
             y  = py;
             py = null;

             t = new double[nPointsInBuffer];
             for(i=0; i<nPointsInBuffer; i++)
                t[i] = time[i];
             time = t;
             t = null;
          }
       }


       public void setColor(Color color[]){
          this.color = RsColor.copyColorList(color);
       }


       public RsRectangle paintAtTime(Graphics g, RsTransform graphicsTransform, RsRectangle r, double time){
         int i, iFirst;
         int ix1, iy1, ix2, iy2;
         double x0, y0, x1, y1, x2, y2, px, py, ps, z;
         double sx[], sy[];
         int    iS, nS, ix[], iy[];
         double w;


         w = wTrailer*graphicsTransform.getScale();


         sx = new double[7];
         sy = new double[7];
         ix = new int[7];
         iy = new int[7];

         if(nPointsInBuffer<2 || firstUnpaintedPoint>=nPointsInBuffer)
             return r;

         if(firstUnpaintedPoint==0)
             iFirst = 1;
         else
             iFirst = firstUnpaintedPoint;

         g.setColor(color[0]);

         x0 = 0;
         y0 = 0;
         x1 = 0;
         y1 = 0;

         // in the loop, we keep reusing the prior computation
         // so set x1,y1 to be the initial point for the first line segment.
         // if we are doing fat lines, we will also need the initial
         // point of the previous line segment x0, y0

         graphicsTransform.map(x[iFirst-1], y[iFirst-1], workPoint);
         x1 = workPoint.x;
         y1 = workPoint.y;
         if(r==null)
            r = new RsRectangle(x1, y1, 0.0, 0.0);
         else
            r.insert(x1, y1);


         if(wTrailer>1 && iFirst>1 && nPointsInBuffer>2){
            graphicsTransform.map(x[iFirst-2], y[iFirst-2], workPoint);
            x0 = workPoint.x;
            y0 = workPoint.y;
            r.insert(x0, y0);
         }

         for(i=iFirst; i<nPointsInBuffer; i++){
            graphicsTransform.map(x[i], y[i], workPoint);;
            x2 = workPoint.x;
            y2 = workPoint.y;
            r.insert(x2, y2);
            ix1 = (int)Math.floor(x1+0.5);
            iy1 = (int)Math.floor(y1+0.5);
            ix2 = (int)Math.floor(x2+0.5);
            iy2 = (int)Math.floor(y2+0.5);
            g.drawLine(ix1, iy1, ix2, iy2);

            // if width of trailer>=2, draw fat line using points 1, 2 (and zero if available);
            if(w>=2){
               px = -(y2-y1);
               py =   x2-x1;
               ps = (w/2.0)/Math.sqrt(px*px+py*py);  // scale (px,py) to length wTrailer/2
               px*= ps;
               py*= ps;
               sx[0] = x1+px;
               sy[0] = y1+py;
               sx[1] = x2+px;
               sy[1] = y2+py;
               sx[2] = x2-px;
               sy[2] = y2-py;
               sx[3] = x1-px;
               sy[3] = y1-py;
               nS = 4;

               if(i>1){
               // use the z-component of the cross product to determine which
               // way the line is bending (z gives us no sense of magnitude
               // only of direction)
                  z  = (x1-x0)*(y2-y1) - (y1-y0)*(x2-x1);
                  px = -(y1-y0);
                  py =   x1-x0;
                  ps = (w/2.0)/Math.sqrt(px*px+py*py);
                  px*=ps;
                  py*=ps;
                  if(z>0){
                     px = -px;
                     py = -py;
                  }
                  sx[4] = x1+px;
                  sy[4] = y1+py;
                  nS    = 5;
               }

               sx[nS] = sx[0];
               sy[nS] = sy[0];
               nS++;
               for(iS=0; iS<nS; iS++){
                 r.insert(sx[iS], sy[iS]);
                 ix[iS] = (int)Math.floor(sx[iS]+0.5);
                 iy[iS] = (int)Math.floor(sy[iS]+0.5);
               }
               g.fillPolygon(ix, iy, nS);
               g.drawPolygon(ix, iy, nS);
            }
            x0 = x1;
            y0 = y1;
            x1 = x2;
            y1 = y2;
         }
         firstUnpaintedPoint = i;

         return r;
      }

   }




   private PaintBoxNode  firstNode;
   private PaintBoxNode  lastNode;

   private int        bodyPartID;
   private int        paintBoxID;
   private static int paintBoxSequencer;
   private int        nodeIDSequencer;


   public SimPaintBox(int bodyPartID){
      firstNode       = null;
      lastNode        = null;
      this.bodyPartID = bodyPartID;
      paintBoxID      = paintBoxSequencer++;
   }

   public SimPaintBox(SimPaintBox source){
      firstNode       = null;
      lastNode        = null;
      this.bodyPartID = source.bodyPartID;
      this.paintBoxID = source.paintBoxID;
      copyStateData(source);
   }


   public int getBodyPartID(){
      return bodyPartID;
   }

   public int getPaintBoxID(){
      return paintBoxID;
   }


   private PaintBoxNode addNode(){
     if(lastNode!=null)
        lastNode.trim();

     PaintBoxNode node = new PaintBoxNode(nodeIDSequencer++);
     node.allocateGeometry();
     if(lastNode==null){
         firstNode = node;
         lastNode  = node;
     }else{
         lastNode.trim();
         node.copyProperties(lastNode);
         node.priorNode    = lastNode;
         lastNode.nextNode = node;
         lastNode          = node;
     }
     return node;
   }

   private PaintBoxNode addCopyNode(PaintBoxNode source){
     PaintBoxNode node = new PaintBoxNode(source.nodeID);
     if(lastNode==null){
         firstNode = node;
         lastNode  = node;
     }else{
         lastNode.trim();
         node.priorNode    = lastNode;
         lastNode.nextNode = node;
         lastNode          = node;
     }
     node.copyProperties(source);
     node.copyGeometry(source);
     return node;
   }


   private PaintBoxNode getNextNodeForPainting(PaintBoxNode node){
      if(node==null)
         node = firstNode;
      while(node!=null){
         if(node.nPointsInBuffer>node.firstUnpaintedPoint && node.nPointsInBuffer>2){
            return node;
         }
         node = node.nextNode;
      }
      return null;
   }

   private void removeNode(PaintBoxNode node){
     if(node==null)
        return;

     node.x    = null;
     node.y    = null;
     node.time = null;

     if(node.priorNode==null){
        firstNode = node.nextNode;
     }else{
        node.priorNode.nextNode = node.nextNode;
     }

     if(node.nextNode==null){
        lastNode = node.priorNode;
     }else{
        node.nextNode.priorNode = node.priorNode;
     }
   }




   /* about the property setting methods
        when we put down a trailer line, we assume that there is
        continuity between all points in the buffer.  but if we
        make a property change, then that continuity is broken
        and we need to create a new buffer.

        the set-property methods attemp to conserve resources,
        so that if the application calls a set method with the same
        parameters that are allready in effect, it does not allocate
        a new buffer.  Also, there is another interesting case. If
        the buffer is empty (as in it's been allocated, but no points
        have yet been stored in it), then there is no continuity to
        worry about.  we simply store the properties and continue.

        thus the set-propery methods call getUnusedNodeIfAvailable()
        to find an unused node (if the whole linked list is empty, it
        creates one).  If an unused node is available, we don't have
        to compare against existing properties, we just set them
        as the application specified.
   */

   private PaintBoxNode getUnusedNodeIfAvailable(){
     if(firstNode == null)
        return addNode();
     else if(lastNode.nPointsInBuffer==0)
        return lastNode;
     else
        return null;
   }

   public void setTrailerProperties(
                  double  xTrailer,
                  double  yTrailer,
                  double  wTrailer)
   {
      PaintBoxNode node = getUnusedNodeIfAvailable();
      if(node==null){
         node = lastNode;
         if(node.xTrailer==xTrailer && node.yTrailer==yTrailer && node.wTrailer==wTrailer)
            return;
         node = addNode();
      }
      node.xTrailer                = xTrailer;
      node.yTrailer                = yTrailer;
      node.wTrailer                = wTrailer;
   }

   public void setColorProperties(Color [] color){
      PaintBoxNode node = getUnusedNodeIfAvailable();
      if(node==null){
         node = lastNode;
         if(RsColor.areColorListsTheSame(node.color, color))
            return;
         node = addNode();
      }
      node.setColor(color);
   }

   public void setActivationProperty(boolean status){

      // in the case of activation status, we can apply special rules
      // to avoid the premature allocation of empty storage.
      if(status==false){
         if(lastNode==null)
             return;
         lastNode.activationStatus = false;
         return;
      }

      PaintBoxNode node = getUnusedNodeIfAvailable();
      if(node==null){
         node = lastNode;
         if(lastNode.activationStatus==status)
            return;
         node = addNode();
      }
      node.activationStatus = status;
   }


   public void setTransitionIntervalProperty(int interval){
      PaintBoxNode node = getUnusedNodeIfAvailable();
      if(node==null){
         node = lastNode;
         if(node.transitionInterval == interval)
            return;
      }

      node.transitionInterval = interval;
   }


   public void setMinTrailerLengthProperty(double minLength){
       if(lastNode==null)
          addNode();
       lastNode.minTrailerSegmentLength  = minLength;
       lastNode.minTrailerSegmentLength2 = minLength*minLength;
   }

   public void setPaintPatternProperty(RsPolygon p){
      // not implemented at this time
   }


   public void bufferTrailerPosition(RsMotion motion){
      if(lastNode==null){
         // this will happen only if we've never set properties.
         // in such a case, there is no point to buffering a point
         return;
      }

      if(!lastNode.activationStatus || lastNode.color==null || lastNode.wTrailer<=0){
          return;
      }


      PaintBoxNode node = lastNode;

      if(node.continuityID != motion.getContinuityID()){
         // there has been a break in the motion history
         // (probably in response to a new placement event)
         node = getUnusedNodeIfAvailable();
         if(node==null)
            node = addNode();
         node.continuityID = motion.getContinuityID();
      }

      motion.transform.map(node.xTrailer, node.yTrailer, node.workPoint);
      node.addPoint(node.workPoint.x, node.workPoint.y, motion.getTime());
    }




   public RsRectangle paintAtTime(Graphics g, RsTransform graphicsTransform, RsRectangle r, double time){
      PaintBoxNode node=null;
      while((node=getNextNodeForPainting(node))!=null){
          r    = node.paintAtTime(g, graphicsTransform, r, time);
      }
      return r;
   }

   public void repaint(Graphics g, RsTransform graphicsTransform){
      PaintBoxNode node;
      node = firstNode;
      RsRectangle r=null;
      while(node!=null){
         node.firstUnpaintedPoint = 0;
         r    = node.paintAtTime(g, graphicsTransform, r, 0.0);
         node = node.nextNode;
      }
   }

   public void clear(){
      if(firstNode!=null){
         // remove all but the last node
         while(firstNode!=lastNode){
             removeNode(firstNode);
         }
         // clear the data in the first node
         firstNode.nPointsInBuffer     = 0;
         firstNode.firstUnpaintedPoint = 0;
      }
   }


   public void copyStateData(SimPaintBox source){
      if(source==null || source.firstNode==null){
         while(firstNode!=null)
            removeNode(firstNode);
         return;
      }

      while(firstNode!=null && firstNode.nodeID<source.firstNode.nodeID)
          removeNode(firstNode);

      if(this.lastNode!=null && this.lastNode.nodeID==source.lastNode.nodeID){
         // potential short cut.  since changes are always made to
         // the last node, and the last node of both sets of nodes
         // have the same ID, then we can assume that only the last
         // node has changed and we need only copy data from
         // the last node
         this.lastNode.copyProperties(source.lastNode);
         this.lastNode.copyGeometry( source.lastNode);
      }


      PaintBoxNode sourceNode = source.firstNode;
      PaintBoxNode copyNode   = this.firstNode;

      while(sourceNode!=null && copyNode!=null && sourceNode.nodeID==copyNode.nodeID){
         copyNode.copyProperties(sourceNode);
         copyNode.copyGeometry(sourceNode);
         sourceNode = sourceNode.nextNode;
         copyNode   = copyNode.nextNode;
      }

      while(copyNode!=null){
         // the remainder of the nodes in the chain must be removed
         PaintBoxNode node = copyNode.nextNode;
         removeNode(copyNode);
         copyNode = node;
      }

      while(sourceNode!=null && sourceNode.nPointsInBuffer>0){
         addCopyNode(sourceNode);
         sourceNode = sourceNode.nextNode;
      }
   }
}
