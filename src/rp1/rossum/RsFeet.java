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






/**
 * A class for performing units conversions for values given in feet.
 *
 */

public class RsFeet extends RsUnits {

   public RsFeet(){
      super("feet");
      conversionFactor = new double[3];
      conversionFactor[0]=1;
      conversionFactor[1]=12.0;
      conversionFactor[2]=12.0;
      setDefaultFormat("%3.0f ft %2.0f-%05.32R in");
   }

   @Override
public  double userToInternal(double userValue){
      return userValue/(3.0*1.0936);
   }
   @Override
public  double internalToUser(double internalValue){
      return internalValue*(3.0*1.0936);
   }


}



