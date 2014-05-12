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

public class RsPlacementParser extends RsDeclarationParser  {

   public RsPlacementParser(RsTokenReader readerReference){
      reader=readerReference;
   }


   @Override
public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

     RsToken token;
     RsPlacement  placement = new RsPlacement(objectName, plan);

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
            if(parseGenericSpecification(placement, s)){
               // it's already handled
            }else if(s.equals("geometry")){
               processGeometry(placement);
            }else if(s.equals("lineWidth")){
               processLineWidth(placement);
            }else{
               throw token.gripe("Invalid object specification: "+token.getString());
            }
            token=nextToken();
            if(!token.isSemiColon())
               throw token.gripe("Invalid syntax where semi-colon expected");
         }else{
            token.gripe("Invalid syntax in declaration");
         }
      }

      //  now we must check to see that all mandatory specifications were supplied.
      if(!placement.isGeometrySet)
         throw token.gripe("Missing geometry specification");

      return placement;
   }


   void processLineWidth(RsPlacement placement) throws RsParsingException {
      RsToken  token = nextToken();
      if(token.getType()!=RsToken.TT_INTEGER)
         throw token.gripe("Invalid syntax where positive integer value expected");
      placement.lineWidth=token.getIntegerValue();
   }

    void processGeometry(RsPlacement placement) throws RsParsingException {
      RsUnits  units=placement.plan.getUnits();
      double [] a = processRealSpec(4);
      a[0] = units.userToInternal(a[0]);   // x-coordinate
      a[1] = units.userToInternal(a[1]);   // y-coordinate
      a[2] = a[2]*Math.PI/180;             // orientation, convert to radians
      a[3] = units.userToInternal(a[3]);   // radius (for depiction only)
      placement.setGeometry(a);
   }
}

