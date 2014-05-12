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

  The Rossum Interlock interface

  The interlock interface is implemented as a way to allow
  client applications to control the operation of the
  simulator.  The naming conventions for the methods are
  analogous to an interlock on a piece of electronic equipment.
  When the equipment cabinet is opened, the equipment is
  de-energized to protect careless technicians from electrocution.
  When the cabinet is closed, the equiment is re-energized.
  In Rossum, when the interlock is "open" the simulator clock is
  halted.  When the interlock is "closed" the clock is restored.

  It is anticipated that the openInterlock() method will return
  a sequentially increasing index.  The closeInterlock method
  will only return TRUE (meaning, re-start the simulator clock)
  when given an index that matches (or exceeds) the
  last "open" value it issued.

  The mimimum interlock value is 1.  The value 0 is reserved
  by the RsProtocol as a flag meaning "no interlock is implemented."

*/



package rp1.rossum;

/**
 * An interface used to support the RP1 interlock function.
 *
 */

public interface RsInterlock
{
   public int     openInterlock();
   public boolean closeInterlock(int interlock);
   public int     getMaximumInterlockIndex();
}
