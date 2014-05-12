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

/*

   RsDummyParser is a "do nothing" parser which will read in the
   tokens for a particular statement (be it a Declaration or a Specification)
   and advance past it.   It purpose is strictly for development.  It serves
   as a stand-in for while the actual class is being written.   For example,
   if we implemented a new kind of Declaration (instead of "wall", say
   "partition"), we could use this to test the various parsers while
   we were writing the real thing.

*/


import rp1.rossum.*;

public class RsDummyParser extends RsDeclarationParser {

   public RsDummyParser(String keywordReference, RsTokenReader readerReference){
      keyword=keywordReference;
      reader=readerReference;
   }

   @Override
public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

      boolean level = false;
      RsToken token;

      System.out.println("Processing unimplemented statement "+keyword);

      while(true){
          token=reader.nextToken();
          if(token.endOfFile())
               throw token.gripe("Encountered end-of-file before statement termination");

          if(token.getType()==RsToken.TT_CHARACTER){
              char c=token.getChar();
              if(c=='{'){
                 if(level)
                    throw token.gripe("Nesting of statements not supported");
                 level=true;
              }else if(c==';'){
                 if(!level)
                     break;   // completion of statement
              }else if(c=='}'){
                  if(!level)
                     throw token.gripe("Closing brace out of place");
                   break;
              }
         }
      }

      return null;
   }

   private String keyword;
   private RsTokenReader reader;
}

