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

public class RsNavNodeParser extends RsDeclarationParser  {

   public RsNavNodeParser(RsTokenReader readerReference){
      reader=readerReference;
   }


   @Override
public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

     RsToken token;
     RsNavNode  navNode = new RsNavNode(objectName, plan);

     token=nextToken();

     if(!token.isOpenBrace())
        throw token.gripe("Invalid syntax where open brace expected in declaration");

      while(true){
         token=nextToken();
         if(token.isCloseBrace()){
            break;
         }else if(token.isIdentifier()){
            String s=token.getString();
            RsToken temp = nextToken();
            if(temp.getType()!=RsToken.TT_CHARACTER || temp.getChar()!=':')
               throw temp.gripe("Invalid syntax in specification (colon expected)");
            if(parseGenericSpecification(navNode, s)){
               // it's already handled
            }else if(s.equals("geometry"))
               processGeometry(navNode, 2);
            else
               throw token.gripe("Invalid object specification: "+token.getString());
            token=nextToken();
            if(!token.isSemiColon())
               throw token.gripe("Invalid syntax where semi-colon expected");
         }else{
            token.gripe("Invalid syntax in declaration");
         }
      }

      //  now we must check to see that all mandatory specifications were supplied.
      if(!navNode.isGeometrySet)
         throw token.gripe("Missing geometry specification");

      return navNode;
   }
}

