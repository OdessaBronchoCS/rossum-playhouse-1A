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

/**

  RsPolygonParser

  The purpose of this parser is to serve as a parent class
  for those parsers that ingest declarations for objects
  which feature a polygonal geometry (RsObstacleParser, RsPaintParser).
  Note that even though RsWallParser creates an object with a
  polygon feature, it does not derive from this class because
  its geometry specification is treated differently.

  The method parseClassSpecifications() is an abstract method that
  must be overridden by the derived classes so that it may
  ingest those specifications that are specific to particular
  objects.
*/



package rp1.planparser;


import rp1.rossum.*;

public abstract class RsPolygonParser extends RsDeclarationParser  {

   public RsPolygonParser(RsTokenReader readerReference){
      reader=readerReference;
   }

   public abstract RsObject makeNewObject(RsPlan plan, String objectName);
   public abstract boolean  parseClassSpecification(RsObject obj, String string) throws RsParsingException;

    @Override
	public RsObject process(RsPlan plan, String objectName) throws RsParsingException {

      RsToken token;

      RsObject obj = makeNewObject(plan, objectName);

      double  [] a=null;
      double  [] offset=null;
      double  [] orientation=null;

      int        nSide = 0;
      double     radius = 0;

      RsUnits    units = plan.getUnits();


      token=nextToken();
      if(!token.isOpenBrace())
         throw token.gripe("Invalid syntax where open brace expected in declaration");

      while(true){
         token=nextToken();
         if(token.isCloseBrace()){
            break;
         }else if(token.isIdentifier()){
            String  s = token.getString();
            RsToken temp = nextToken();
            if(!(temp.getType()==RsToken.TT_CHARACTER && temp.getChar()==':'))
               throw token.gripe("Invalid syntax where \':\' expected in object specification");
            if(parseGenericSpecification(obj, s)){
               // nothing to do
            }else if(parseClassSpecification(obj, s)){
               // nothing to do
            }else if(s.equals("geometry")){
               a = processRealSpec(units);
               if(a==null || a.length<6 || (a.length&1)==1)
                   throw reader.gripe("Invalid number of parameters in geometry specification (need at least 3 coordinate pairs)");
            }else if(s.equals("offset")){
                offset = processRealSpec(units, 2);
            }else if(s.equals("orientation")){
                orientation = processRealSpec(1);
            }else if(s.equals("polygon")){
                token=nextToken();
                if(token.getType()!=RsToken.TT_INTEGER)
                   throw token.gripe("Invalid syntax where integer  number-of-sides expected");
                nSide = token.getIntegerValue();
                if(nSide<3 || nSide>256)
                   throw token.gripe("Polygon number-of-sides must be in range 3 through 256");
                token=nextToken();
                if(!(token.getType()==RsToken.TT_CHARACTER && token.getChar()==','))
                   throw token.gripe("Invalid syntax where comma expected "+token.toString());
                token=nextToken();
                if(!token.isNumeric())
                   throw token.gripe("Invalid syntax where radius expected");
                radius = token.getDoubleValue();
                radius = units.userToInternal(radius);
                if(radius<0.005 || radius>10000)
                   throw token.gripe("Radius must be in range 0.5 cm to 10 km");
            }else{
               throw token.gripe("Invalid object specification: "+s);
            }
            token=nextToken();
            if(!token.isSemiColon())
               throw token.gripe("Invalid syntax where \';\' expected");
         }else{
            throw token.gripe("Invalid syntax where object specification expected");
         }
      }


      if(nSide>0){
        if(a!=null)
           throw reader.gripe("Obstacle declaration must not specify both polygon and geometry");

        a = new double[2*nSide];
        double theta;
        for(int i=0; i<nSide; i++){
           theta    = 2*Math.PI*i/nSide;
           a[i*2]   = radius*Math.cos(theta);
           a[i*2+1] = radius*Math.sin(theta);
        }
      }


      //  now we must check to see that all mandatory specifications were supplied.
      if(a==null)
         throw token.gripe("Missing or invalid geometry specification");


      // now check for optional offset and orientation adjustments

      if(orientation!=null || offset!=null ){
         RsTransform transform = new RsTransform();
         if(orientation!=null)
            transform.setTheta(orientation[0]*Math.PI/180.0);
         if(offset!=null)
           transform.setOffset(offset[0], offset[1]);

          RsPoint input = new RsPoint();
          RsPoint output = new RsPoint();
          for(int i=0; i<a.length/2; i++){
             input.x = a[i*2];
             input.y = a[i*2+1];
             transform.map2(input, output);
             a[i*2]   = output.x;
             a[i*2+1] = output.y;
          }
      }

      obj.setGeometry(a);
      return obj;

   }

}

