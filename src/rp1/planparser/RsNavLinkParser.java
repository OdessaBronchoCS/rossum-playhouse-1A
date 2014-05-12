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

public class RsNavLinkParser extends RsDeclarationParser  {

   public RsNavLinkParser(RsTokenReader readerReference){
      reader=readerReference;
   }

   @Override
public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

     RsToken token;
     RsNavLink  navLink = new RsNavLink(objectName, plan);

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
            if(parseGenericSpecification(navLink, s)){
               // it's already handled
            }else if(s.equals("geometry"))
               throw temp.gripe("Link objects do not take a geometry specification");
            else if(s.equals("nodes"))
               processNodes(navLink);
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
      if(!navLink.isGeometrySet)
         throw token.gripe("Missing nodes specification");

      return navLink;

   }

   void processNodes(RsNavLink navLink) throws RsParsingException {
      String    [] nodeString = new String[2];
      RsNavNode [] node       = new RsNavNode[2];
      for(int i=0; i<2; i++){
         RsToken token = nextToken();
         if(token.getType()!=RsToken.TT_IDENTIFIER)
            throw token.gripe("Invalid syntax where node identifier expected");
         nodeString[i] = token.getString();
         RsObject object = navLink.plan.getObjectByName(nodeString[i]);
         if(object==null)
            throw token.gripe("Specified node was not defined \""+nodeString[i]+"\"");
         if(!(object instanceof RsNavNode))
            throw token.gripe("Specified identifier is not a node \""+nodeString[i]+"\"");
         node[i]=(RsNavNode)object;

            if(i==0){
            RsToken temp = nextToken();
            if(temp.getType()!=RsToken.TT_CHARACTER || temp.getChar()!=',')
               throw temp.gripe("Invalid syntax where comma expected in nodes specification");
         }
      }
      navLink.setNodes(node[0], node[1]);
   }
}

