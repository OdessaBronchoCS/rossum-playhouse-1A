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


import java.io.*;
import java.util.*;

import rp1.rossum.*;

public class RsPlanReader
{
	   private String                 fileName;
	   private InputStreamReader      reader;
	   private RsTokenReader          ingest;
	   private RsPlan                 plan;
	   private Hashtable <String, RsSpecificationParser> specKeyword;
	   private Hashtable <String, RsDeclarationParser> decKeyword;
	
   public RsPlanReader(String fileNameRef){
      fileName = fileNameRef;
      reader = null;

      ingest = new RsTokenReader(reader);
      ingest.setFileName(fileName);

      // initialize the specification keyword table
      specKeyword = new Hashtable<String, RsSpecificationParser>(2);
      specKeyword.put("units", new RsUnitsSpecParser(ingest));
      specKeyword.put("caption", new RsCaptionSpecParser(ingest));

      // initialize the declaration keyword table
      decKeyword = new Hashtable<String, RsDeclarationParser>(8);
      decKeyword.put("wall",      new RsWallParser(ingest));
      decKeyword.put("obstacle",  new RsObstacleParser(ingest));
      decKeyword.put("target",    new RsTargetParser(ingest));
      decKeyword.put("placement", new RsPlacementParser(ingest));
      decKeyword.put("node",      new RsNavNodeParser(ingest));
      decKeyword.put("link",      new RsNavLinkParser(ingest));
      decKeyword.put("paint",     new RsPaintParser(ingest));
      decKeyword.put("doorway",   new RsDummyParser("doorway", ingest));
   }

   public RsPlan readPlan(InputStream _inStream) throws RsParsingException, IOException{

      reader = new InputStreamReader(new BufferedInputStream(_inStream));
      ingest.setReader(reader);

      RsToken                      token1;
      RsToken                      token2;
      RsSpecificationParser        specParser;
      RsDeclarationParser          decParser;
      String                       objectName;

      plan = new RsPlan(fileName);

      if(reader==null)
         return null;

      while(true){
          token1=ingest.nextToken();
          if(token1.endOfFile())
             break;
          if(token1.getType()!=RsToken.TT_IDENTIFIER)
             throw token1.gripe("Invalid syntax where identifier expected");
          token2=ingest.nextToken();
          if(token2.endOfFile())
             throw token2.gripe("Premature end-of-file encountered within statement");

          /*  Check token1 against all specification key-words
           (at present, "units" is it). If it is a specification
           check to see that token2 is a colon.  If it's not,
           throw an exception.   If it is process the specification
           and continue the loop;

           If token1 is not a specification, check token2 to see if it
           is a colon.  If so, throw an exception "unrecognized specification"

           Check token1 against list of valid declaration keywords.
           If it is not a valid declaration, thown an exception.
           If token2 is not an identifier, synthesize an identifier
           for the current declaration. If token2 is an identifier,
           check to see that it is unique (if not unique, throw an exception), then
           read the next token.   Token2 should now be the opening brace. */


          specParser=(RsSpecificationParser)specKeyword.get(token1.getString());
          if(specParser!=null){
             specParser.process(plan);
             continue;
          }

          if(token2.getType()==RsToken.TT_CHARACTER && token2.getChar()==':'){
             throw token1.gripe("Unrecognized specification keyword: "+token1.getString());
          }

          decParser = (RsDeclarationParser)decKeyword.get(token1.getString());
          if(decParser==null)
             throw token1.gripe("Unrecognized keyword where declaration expected: "+token1.getString());

         if(token2.getType()==RsToken.TT_IDENTIFIER){
            // is identifier unique

            objectName=token2.getString();
            RsObject object = plan.getObjectByName(objectName);
            if(object!=null)
               throw token2.gripe("Duplicate object name "+objectName);
         }else if(token2.getType()==RsToken.TT_INTEGER){
               objectName=plan.synthesizeName(token2.getIntegerValue());
               RsObject object = plan.getObjectByName(objectName);
               if(object!=null)
                   throw token2.gripe("Duplicate object name "+objectName);
               int newValue = token2.getIntegerValue();
               plan.maximizeSerialNumber(newValue);
         }else{
             // the token was not an identifer, most likely it is an open brace
             // push it back and let the appropriate RsParser use it.  Synthesize
             // a name for the object as needed.
             ingest.pushBackToken();
             objectName=plan.synthesizeName();  // will be hexidecimal serialNumber
          }
          plan.addObject(decParser.process(plan, objectName));
      }
      reader.close();
      reader=null;
      return plan;
   }

}
