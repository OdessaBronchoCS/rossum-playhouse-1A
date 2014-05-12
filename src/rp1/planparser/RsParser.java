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




/* Note:  the RsParser class implements a method called nextToken()
          which is simply a wrapper around the reader.nextToken() method.
          The purpose for this is to provide a convenient (read "lazy")
          method of handling premature end-of-file conditions.   All classes
          derived from RsParser are used for processing statements which have
          already been identified as such.  At the very least, a semi-colon
          terminator is requireed...  usually, much more.   So, if an EOF is
          discovered, it represents an error.   By using the nextToken()
          method instead of reader.nextToken(), the RsParser-derived classes
          can save a few lines and eliminate some clutter from the code.
*/

package rp1.planparser;

public abstract class RsParser {

   public RsToken nextToken() throws RsParsingException {
      RsToken token = reader.nextToken();
      if(token.endOfFile())
         throw token.gripe("Encountered premature end-of-file while reading statement");
      return token;
   }

   public void pushBackToken() throws RsParsingException {
      reader.pushBackToken();
   }

   protected RsTokenReader reader;

}

