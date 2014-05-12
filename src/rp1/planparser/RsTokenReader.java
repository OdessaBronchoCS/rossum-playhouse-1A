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



/*
   nextToken reads the next token from file.   Internally, it
   calls the private method nextChar().

   TO DO:  Right now, I restrict input to ASCII char range [0,127].
           So identifiers can only be in ASCII range (quoted strings
           can be in Unicode).   This approach warrants some discussion.

   TO DO:  Need to add full suite of escape characters to quoted strings
           to support the C/C++ standard.


   About Numerics:

   By far, the most complicated tokenizing task is the processing
   of numbers.   There are two numeric token types, TT_INTEGER and TT_DOUBLE.
   Syntatical clues are used to determine the type for a string, and
   it is possible for the Tokenizer to decide that a string is an integer
   when you wish it had decided it was a double.  For example, if you
   include a string "1000" in your text, it will be interpreted as
   an integer.   Fortunately, the RtToken.getDoubleValue() works on
   both integer and double typed tokens.

   Note that leading + and - signs are not treated as part of the numeric
   value.   The string "-1.3" will return two tokens.   The first will
   be a TT_CHAR token with the character value "-" and the second will
   be a TT_DOUBLE token with the double value 1.3.

   The numeric formats recognized include:

      Decimal Integers
          Octal and HexiDecimal Integers not yet implemented,
          supports only up to the maximum size of a 4-byte SIGNED
          integer, not an unsigned.

      Floating Point
        type of "double", including exponential notation  1.0e+10, etc.


BUGS:   Not all tokens are stored in the string buffer as characters
        are processed (most notably, numerics).   This means that
        the token.getString() method will not always work properly.

        Token Reader may not be counting lines correctly for unix-formatted
        files (line-feed, not carriage return).  Since that consideration was
        the impetus for writing this class in the first place, it's a pretty
        sad state of affairs.

        Unicode and non-ASCII characters are not handled correctly.
*/


package rp1.planparser;

import java.io.*;

public class RsTokenReader  {

   public RsTokenReader(Reader _reader){
      setReader(_reader);
   }

   public void setReader(Reader _reader){
      reader = _reader;
      pushBackTokenBuffer=null;
      token=null;
      lineCount=1;
      fileName=null;
   }

   public void pushBackToken() throws RsParsingException {
      if(token==null){
         throw gripe("Incorrect use of RsTokenReader: pushBack of a null token");
      }
      pushBackTokenBuffer=token;
      token=null;
   }


   public RsToken nextToken() throws RsParsingException {
      int value;
      StringBuffer sbuff;

      if(pushBackTokenBuffer!=null){
          token=pushBackTokenBuffer;
          pushBackTokenBuffer=null;
          return token;
      }

      token = new RsToken();
      if(reader==null){
        token.setType(RsToken.TT_EOF);
        return token;
      }



      // the following items are set rather than established
      // through arguments to the constructor in anticipatation
      // of the day when I reuse tokens from a pool.
      token.setType(RsToken.TT_UNDETERMINED);
      token.setFileName(fileName);
      sbuff=token.getStringBuffer();

      while(true){
         value=nextChar();
         token.setLineNumber(lineCount);
         if(value==-1){
            token.setType(RsToken.TT_EOF);
            return token;
         }else if(value>0 && value<=32 || value>127){
            /** treat it as white space.  */
            continue;
         }else if (value=='/'){
            value=nextChar();
            if(value=='/')
               singleLineComment();
            else if(value=='*')
               multiLineComment();
            else{
               //  it's something other than a comment
               pushBackChar(value);
               token.setType(RsToken.TT_CHARACTER);
               sbuff.append((char)value);
               return token;
            }
         }else if(value=='\"'){
            return quotedString();
         }else if(value=='_' || value>='A' && value<='Z' || value>='a' && value<='z'){
            return identifier(value);
         }else if(value=='.' || value>='0' && value<='9'){
            return numeric(value);
         }else{
            token.setType(RsToken.TT_CHARACTER);
            sbuff.append((char)value);
            return token;
         }
      }
   }

   private void singleLineComment() throws RsParsingException {
      int value;
      do {
         value=nextChar();
      }while(value!='\n' && value!=-1);
   }

   private void multiLineComment() throws RsParsingException {
      int value;
      int refLine;

      refLine=lineCount;
      while(true){
         value=nextChar();
         if(value=='*'){
            value=nextChar();
            if(value=='/')
                return;
         }else if(value==-1)
            throw gripe("End of file encountered within comment starting at line: "+refLine);
      }
   }

   private RsToken quotedString() throws RsParsingException {
      int value;
      StringBuffer sbuff;

      sbuff=token.getStringBuffer();
      while(true){
         value=nextChar();
         // check for special characters
         if(value=='\"'){
            token.setType(RsToken.TT_STRING);
            return token;
         }else if(value=='\\'){
            value=nextChar();
            if(value=='n')
               value='\n';
            else if(value=='t')
               value='\t';
         }else if(value=='\n'){
            throw gripe("End-of-line within quoted string on line ");
         }
         sbuff.append((char)value);
      }
   }

   private RsToken identifier(int value) throws RsParsingException{
      StringBuffer sbuff;
      sbuff=token.getStringBuffer();
      sbuff.append((char)value);
      value=nextChar();
      while(value=='_'
         || value>='A' && value<='Z'
         || value>='a' && value<='z'
         || value>='0' && value<='9'){
         sbuff.append((char)value);
         value=nextChar();
      }
      pushBackChar(value);
      token.setType(RsToken.TT_IDENTIFIER);
      return token;
   }

   private RsToken numeric(int value) throws RsParsingException{
      StringBuffer sbuff;
      double w=0;   // whole number
      double f=0;   // fractional component
      double e=0;   // exponential component
      sbuff=token.getStringBuffer();
      boolean decimal=false;      // decimal found
      boolean exponential=false;  // exponential found

      if(value=='0'){
         // this could be a hexidecimal string
         value=nextChar();
         if(value=='x' || value=='X'){
            value=nextChar();
            if(!isHexDigit(value))
                  throw gripe("Invalid hexidecimal number specification");
            sbuff.append("0x");
            int hex=hexDigit(value);
            int i=0;
            while(isHexDigit(value=nextChar())){
               sbuff.append((char)value);
               i++;
               if(i>8)
                  throw gripe("Too many digits in hexidecimal number specification");
               hex=(hex<<4)|hexDigit(value);
            }
            pushBackChar(value);
            token.setType(RsToken.TT_INTEGER);
            token.setValue(hex);
            return token;
         }else{
            // it's not a hex string, pass it on for regular numeric treatment
            pushBackChar(value);
            value='0';
         }
      }


      if(value!='.'){
          w=processNumbers(value);
          value=nextChar();
      }
      if(value=='.'){
          decimal=true;
          value=nextChar();
          if(value>='0' && value<='9'){
            f=processFraction(value);
            value=nextChar();
          }
      }
      if(value=='e' || value=='E'){
         exponential=true;
         value=nextChar();
         if(value=='-')
            e=-1.0;
         else if(value=='+')
            e=1.0;
         else {
            throw gripe("Invalid character where + or - expected in exponential");
         }
         value=nextChar();
         if(value<'0' || value>'9'){
            throw gripe("Invalid character where numeric expecited in exponential");
         }
         e*=processNumbers(value);
         value=nextChar();
         if(value=='.'){
            // we'll accept specifications like 1.0e+2.0
            do{
              value=nextChar();
            }while(value=='0');
            if(value>='1' && value<='9'){
               throw gripe("Exponential specification must be an integer value");
            }
         }
      }
      if(value=='.'){
         throw gripe("Decimal character out-of-place in numeric");
      }

      pushBackChar(value);

      if(decimal || exponential){
         token.setType(RsToken.TT_DOUBLE);
         if(decimal)
            w+=f;
         if(exponential)
            w*=Math.pow(10.0, e);
         token.setValue(w);
      }else{
         if(w>Integer.MAX_VALUE){
            throw gripe("Integer specification exceeds max value "+Integer.MAX_VALUE);
         }
         token.setType(RsToken.TT_INTEGER);
         token.setValue((int)w);
      }

      return token;
   }

   private boolean isHexDigit(int value){
      return (value>='0' && value<='9'
            || value>='a' && value<='f'
            || value>='A' && value<='F');
   }
   private int hexDigit(int value){
      if(value>='0' && value<='9')
         return (value-'0');
      else if(value>='a' && value<='f')
         return (value-'a')+10;
      else if(value>='A' && value<='F')
         return (value-'A')+10;
      else
         return -1;
   }

   private double processNumbers(int value) throws RsParsingException {
      double d=0;
      do{
         d=d*10.0+value-'0';
         value=nextChar();
      }while(value>='0' && value<='9');
      pushBackChar(value);
      return d;
   }

   private double processFraction(int value) throws RsParsingException {
      double d=0;
      double m=0.1;
      do{
          d+=(value-'0')*m;
          m/=10.0;
          value=nextChar();
      }while(value>='0' && value<='9');
      pushBackChar(value);
      return d;
   }


   private int nextChar() throws RsParsingException {
      int value;
      if(lineFeedTrigger){
         lineCount++;
         lineFeedTrigger=false;
      }
      if(pushBackValue>0){
         value=pushBackValue;
         pushBackValue=0;
      }else{
         try {
            value=reader.read();
         }catch(IOException e){
            throw gripe(e.toString());
         }
      }
      if(value=='\r'){
         // if it's a CR-LF, eat the LF
         try{
            value=reader.read();
         }catch(IOException e){
            throw gripe(e.toString());
         }
         if(value!='\n')
            pushBackValue=value;
         lineFeedTrigger=true;
         value='\n';
      }else if(value=='\n'){
         lineFeedTrigger=true;
      }
      return value;
   }

   private void pushBackChar(int value){
      if(pushBackValue!=0){
          System.err.println("Diagnostic: internal error, pushBack multiple char's");
          System.exit(-1);
      }
      if(value=='\n')
          lineFeedTrigger=false;
      pushBackValue=value;
   }

   public void setFileName(String fileNameRef){
      if(fileNameRef==null)
         fileName=null;
      else
         fileName=fileNameRef;
   }

  protected RsParsingException gripe (String string){
      if(fileName==null)
         return new RsParsingException(lineCount+": "+string);
      else
         return new RsParsingException(fileName+":"+lineCount+": "+string);
   }


   private String         fileName;

   private int            pushBackValue;
   private Reader         reader;

   private RsToken        pushBackTokenBuffer;
   private RsToken        token;

   private int            lineCount;
   private boolean        lineFeedTrigger;
}

