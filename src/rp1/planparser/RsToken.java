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



/*  RsToken.java

  Do not confuse the toString() and getString() methods.
  toString() produces a string showing what was tokenized (data type
  and contents)...  it is useful only for diagnostic purposes.
  getString() returns the contents of the string buffer.

*/


package rp1.planparser;

public class RsToken {

   public RsToken() {
        dataType=0;
        sbuff=new StringBuffer();
        doubleValue=0;
        intValue=0;
        dataTypeString = new String[7];
        dataTypeString[0]="Undetermined ";
        dataTypeString[1]="Identifier   ";
        dataTypeString[2]="Quoted String";      // a quoted string
        dataTypeString[3]="Double Value ";
        dataTypeString[4]="Integer Value";
        dataTypeString[5]="Ordinary Char";
        dataTypeString[6]="End of File  ";
   }

   public int getType(){
      return dataType;
   }
   public void setType(int typeSpecification){
      dataType=typeSpecification;
   }

   public double getDoubleValue(){
      return doubleValue;
   }
   public int getIntegerValue(){
      return intValue;
   }
   public void setValue(int intSpecification){
      intValue=intSpecification;
      doubleValue=intValue;
   }
   public void setValue(double doubleSpecification){
      intValue=0;
      doubleValue=doubleSpecification;
   }
   @Override
public String toString(){
      if(dataType<0)
         return "Error";
      else if(dataType == TT_INTEGER)
         return dataTypeString[dataType]+": "+intValue;
      else if(dataType == TT_DOUBLE)
         return dataTypeString[dataType]+": "+doubleValue;
      else
         return dataTypeString[dataType]+": "+sbuff.toString();
   }

   public String getDataTypeString(){
      return dataTypeString[dataType];
   }
   public boolean endOfFile(){
       return (dataType==6);
   }

   public StringBuffer getStringBuffer(){
      return sbuff;
   }
   public String getString(){
      return sbuff.toString();
   }
   public char getChar(){
      if(dataType==TT_EOF)
         return 0;
      return sbuff.charAt(0);
   }
   public void setLineNumber(int value){
      lineNumber=value;
   }
   public int getLineNumber(){
      return lineNumber;
   }
   public void setFileName(String fileNameReference){
      fileName=fileNameReference;
   }
   public RsParsingException gripe(String string){
      if(fileName==null)
         return new RsParsingException(lineNumber+":"+string);
      else
         return new RsParsingException(fileName+":"+lineNumber+":"+string);
   }

   //  the following two methods may seem a bit silly, but it turns
   //  out that they answer a question which is asked
   //  quite a bit:  Is a token an open brace (or close brace)?
   //  the following expressions save both coding and also problems
   //  with the brace matching functions in some editors which get
   //  confused by the quoted brace character.
   public boolean isOpenBrace(){
      return (dataType==TT_CHARACTER && sbuff.charAt(0)=='{');
   }
   public boolean isCloseBrace(){
      return (dataType==TT_CHARACTER && sbuff.charAt(0)=='}');
   }

   public boolean isSemiColon(){
      return (dataType==TT_CHARACTER && sbuff.charAt(0)==';');
   }

   // The following methods are a lazy way to find out the type of the token
   public boolean isNumeric(){
      return (dataType==TT_INTEGER || dataType==TT_DOUBLE);
   }
   public boolean isIdentifier(){
      return dataType==TT_IDENTIFIER;
   }
   public boolean isString(){
      return dataType==TT_STRING;
   }



   private String        fileName;
   private int           dataType;
   private String []     dataTypeString;
   private StringBuffer  sbuff;
   private int           intValue;
   private double        doubleValue;
   private int           lineNumber;

   public final static int TT_UNDETERMINED=0;
   public final static int TT_IDENTIFIER=1;
   public final static int TT_STRING=2;
   public final static int TT_DOUBLE=3;
   public final static int TT_INTEGER=4;
   public final static int TT_CHARACTER=5;
   public final static int TT_EOF=6;
   public final static int TT_COUNT=7;
}

