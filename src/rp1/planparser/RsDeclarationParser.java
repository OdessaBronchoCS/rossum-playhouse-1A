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


import java.awt.Color;
import rp1.rossum.*;

public abstract class RsDeclarationParser extends RsParser {

   public abstract RsObject process(RsPlan plan, String objectName) throws RsParsingException;

   double [] processRealSpec(int nSpecifications) throws RsParsingException{
       double []a;
       a = processRealSpec();
       if(a==null || a.length!=nSpecifications){
          throw reader.gripe("Invalid number of parameters in numeric specification ("
                 +nSpecifications+ "expected)");
       }
       return a;
   }

   double [] processRealSpec(RsUnits units) throws RsParsingException {
      double [] a = processRealSpec();
      if(a!=null)
         for(int i=0; i<a.length; i++)
            a[i] = units.userToInternal(a[i]);
      return a;
   }

    double [] processRealSpec(RsUnits units, int nSpecifications) throws RsParsingException{
       double []a;
       a = processRealSpec(units);
       if(a==null || a.length!=nSpecifications){
          throw reader.gripe("Invalid number of parameters in numeric specification ("
                 +nSpecifications+ "expected)");
       }
       return a;
     }




   double [] processRealSpec() throws RsParsingException{

       double  [] a  = new double[32];
       RsToken  token;
       double   sign;

       int i=0;
       while(true){
         token=nextToken();
         if(token.getType()==RsToken.TT_CHARACTER && token.getChar()=='-'){
            sign=-1;
            token=nextToken();
         }else
            sign=1;
         if(!token.isNumeric())
            throw token.gripe("Invalid syntax where numeric specification expected ");
         a[i]=sign*token.getDoubleValue();
         i++;
         if((i%32)==0){
            double [] b = new double[i+32];
            for(int j=0; j<i; j++)
                b[j]=a[j];
            a = b;
            b = null;
         }

         token=nextToken();
         if(token.isSemiColon() || token.isCloseBrace()){
            pushBackToken();
            break;
         }
         if(token.getChar()!=',')
            throw token.gripe("Invalid syntax where comma expected "+token.toString());
       }

       double [] b = new double[i];
       for(int j=0; j<i; j++)
          b[j]=a[j];
       return b;
   }



   void processGeometry(RsObject rsObject, int nValues) throws RsParsingException {
      RsUnits  units=rsObject.plan.getUnits();
      double [] a = processRealSpec();
      if(a==null || a.length!=nValues)
         throw reader.gripe("Invalid number of parameters in geometry specification");
      for(int i=0; i<nValues; i++)
         a[i] = units.userToInternal(a[i]);
      rsObject.setGeometry(a);
   }


   public boolean parseGenericSpecification(RsObject obj, String string) throws RsParsingException {
      RsToken token;
      if(string.equals("color") ||
         string.equals("lineColor") ||
         string.equals("fillColor")){
         token=nextToken();
         if(token.getType()!=RsToken.TT_IDENTIFIER)
            throw token.gripe("Invalid syntax where color specification expected");
         String s=token.getString();
         Color color=RsColor.getColorForName(s);
         if(color==null)
            throw token.gripe("Invalid value where color name expected: \""+s+"\"");
         if(string.equals("color"))
            obj.setColor(color);
         else if(string.equals("lineColor"))
            obj.setLineColor(color);
         else if(string.equals("fillColor"))
            obj.setFillColor(color);
         return true;
      }else if(string.equals("label")){
         token = nextToken();
         if(token.getType()!=RsToken.TT_STRING)
            throw token.gripe("Invalid syntax where quoted string expected");
         obj.setLabel(token.getString());
         return true;
      }

      return false;
   }



}

