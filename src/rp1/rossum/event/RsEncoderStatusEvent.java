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


package rp1.rossum.event;





/**
 * Provides data for encoder data; because this is a transitional implementation
 * it is not treated as a true sensor event. There are two counters associated
 * with the encoder: accumulatorValue and absoluteAccumulatorValue.
 * Essentially, these simulate the behavior of an incremental encoder.
 * Each time the encoder undergoes a transition (moves from one encoder
 * interval to the next), the absolute accumulator is incremented.
 * If the increment is in a positive direction, the standard accumulator
 * is incremented. If it is in a negative direction, the standard accumulator
 * is decremented.
 *
 * Both accumulators are cleared when request to do so is issued by the client application.
 *
 */

public class RsEncoderStatusEvent extends RsEvent  {

   /**
	 * 
	 */
	private static final long serialVersionUID = 6211971120031851473L;
/** Request index from RsClient.sendEncoderStatusRequest.*/
   public final int     requestIndex;
   /** ID of the body part associated with the encoder.*/
   public final int     encoderID;
   /** Signed value, the sum of accumulated +/1 encoder transitions. */
   public final int     accumulatorValue;
   /** Total count of encoder transitions. */
   public final int     absoluteAccumulatorValue;
   /** Signed value indicating direction of most recent transition. */
   public final int     quadratureValue;
   /** The current encoder value (essentially, simulated an absolute encoder). */
   public final int     encoderInterval;
   /** The current rotational angle of the encoder (intended for Quality Assuance purposes). */
   public final double  encoderAngle;
   /** Indicates that the accumulators were cleared when the event was reported. */
   public final boolean clearOnReport;

   /**
    * @param simTime      Simulation time at which event was issued.
    * @param requestIndex Request index from RsClient.sendEncoderStatusRequest.
    * @param encoderID    ID of the body part associated with the encoder.
    * @param accumulatorValue Signed value, the sum of accumulated +/1 encoder transitions.
    * @param absoluteAccumulatorValue Total count of encoder transitions.
    * @param quadratureValue Signed value indicating direction of most recent transition.
    * @param encoderInterval The current encoder value (essentially, simulated an absolute encoder).
    * @param encoderAngle    The current rotational angle of the encoder (intended for Quality Assuance purposes).
    * @param clearOnReport   Indicates that the accumulators were cleared when the event was reported.
    */

   public RsEncoderStatusEvent(
             double  simTime,
             int     requestIndex,
             int     encoderID,
             int     accumulatorValue,
             int     absoluteAccumulatorValue,
             int     quadratureValue,
             int     encoderInterval,
             double  encoderAngle,
             boolean clearOnReport
             )
     {
      super(EVT_ENCODER_STATUS, simTime);

      this.requestIndex             = requestIndex;
      this.encoderID                = encoderID;
      this.accumulatorValue         = accumulatorValue;
      this.absoluteAccumulatorValue = absoluteAccumulatorValue;
      this.quadratureValue          = quadratureValue;
      this.encoderInterval          = encoderInterval;
      this.encoderAngle             = encoderAngle;
      this.clearOnReport            = clearOnReport;
   }
}

