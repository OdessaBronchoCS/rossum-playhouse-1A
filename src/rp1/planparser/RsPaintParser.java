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


package rp1.planparser;

import rp1.rossum.*;


public class RsPaintParser extends RsPolygonParser  {

   public RsPaintParser(RsTokenReader readerReference){
      super(readerReference);
   }


   @Override
public RsObject makeNewObject(RsPlan plan, String objectName) {
      return new RsPaint(objectName, plan);
   }

   @Override
public boolean parseClassSpecification(RsObject object, String specString) throws RsParsingException{
      if(specString.equals("region")){
         processRegion((RsPaint)object);
         return true;
      }
      return false;  // the specification is not unique to the RsPaint object
   }

   void processRegion (RsPaint rsPaint) throws RsParsingException {
      RsToken token=nextToken();
      if(token.getType()!=RsToken.TT_INTEGER)
         throw token.gripe("Invalid syntax where integer region specification expected");
      rsPaint.setRegion(token.getIntegerValue());
   }

}

