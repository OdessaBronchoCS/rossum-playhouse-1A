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

import java.io.IOException;



/**
 * An exception issued n response to improper properties specifications.
 * This class is overdue for a serious rewrite.
 */

public class SimPropertiesException extends IOException
{

   /**
	 * 
	 */
	private static final long serialVersionUID = -8717142289673760376L;
public SimPropertiesException(){}
   public SimPropertiesException(String gripe){
      super(gripe);
   }
}


