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

public class RsWallParser extends RsDeclarationParser  {

   public RsWallParser(RsTokenReader readerReference){
      reader=readerReference;
   }

   @Override
public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

      RsToken token;
      RsWall  wall = new RsWall(objectName, plan);

      token=nextToken();
      if(!token.isOpenBrace())
         throw token.gripe("Invalid syntax where open brace expected in declaration");

      while(true){
         token=nextToken();
         if(token.isCloseBrace()){
            break;
         }else if(token.isIdentifier()){
            String s = token.getString();
            RsToken temp = nextToken();
            if(!(temp.getType()==RsToken.TT_CHARACTER && temp.getChar()==':'))
               throw token.gripe("Invalid syntax where \':\' expected in object specification");
            if(parseGenericSpecification(wall, s)){
               // already handled
            }else if(s.equals("geometry"))
               processGeometry(wall);
            else if(s.equals("constraint"))
               processConstraint(wall);
            else
               throw token.gripe("Invalid object specification: "+token.getString());
            token=nextToken();
            if(!token.isSemiColon())
               throw token.gripe("Invalid syntax where \';\' expected");
         }else{
            throw token.gripe("Invalid syntax where object specification expected");
         }
      }

      //  now we must check to see that all mandatory specifications were supplied.
      if(!wall.isGeometrySet)
         throw token.gripe("Missing or invalid geometry specification");
      return wall;

   }


   void processConstraint(RsWall wall) throws RsParsingException {
      RsToken token=nextToken();
      if(token.getType()!=RsToken.TT_IDENTIFIER)
         throw token.gripe("Invalid syntax where object constraint specification expected");
   }

   void processGeometry(RsWall wall) throws RsParsingException {

      double [] a = processRealSpec();
      if(a==null || a.length!=5)
         throw reader.gripe("Invalid number of parameters in geometry specification");

      RsUnits  units=wall.plan.getUnits();
      for(int i=0; i<5; i++)
         a[i] = units.userToInternal(a[i]);

      double x0, y0, x1, y1, thickness;

      x0=a[0];
      y0=a[1];
      x1=a[2];
      y1=a[3];
      thickness=a[4];

      RsSegment segment = new RsSegment(x0, y0, x1, y1);

      if(segment.m<=RsVector.NEARLY_ZERO)
         return;

      if(thickness<=RsVector.NEARLY_ZERO){
         throw reader.gripe("Invalid thickness specification for wall");
      }

      double [] p = new double[8];

      RsVector n = (new RsVector(segment.v)).normal();
      n.scale(thickness/2.0/segment.m);

      p[0] =  x0-n.x;
      p[1] =  y0-n.y;
      p[2] =  x1-n.x;
      p[3] =  y1-n.y;
      p[4] =  x1+n.x;
      p[5] =  y1+n.y;
      p[6] =  x0+n.x;
      p[7] =  y0+n.y;

      wall.setGeometry(p);

   }

}

