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

public class RsCaptionSpecParser extends RsSpecificationParser {

   public RsCaptionSpecParser(RsTokenReader readerReference){
       reader=readerReference;
   }
   @Override
public void process(RsPlan plan) throws RsParsingException{

      RsToken token;
      token = reader.nextToken();
      if(!token.isString())
         throw token.gripe("Invalid syntax where quoted string expected");
      plan.setCaption(token.getString());

      token = nextToken();
      if(token.isSemiColon())
         return;
      throw token.gripe("Invalid syntax where semi-colon expected");
   }
}

