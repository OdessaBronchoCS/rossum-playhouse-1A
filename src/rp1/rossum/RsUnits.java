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


package rp1.rossum;

/*

RsUnits.java

The RsUnits class is intended primarily for the support of user-interface,
data entry, and text-file applications.

Internally, the Rs software handles ALL coordinate specifications and
measurements in meters.  Using a uniform unit system simplifies the code and
helps avoid mistakes in the interpretation of data.  It does not, however,
meet the needs of the user. Real-world applications must deal with problems
of scale and convention.  In applications that deal with small objects,
it might be necessary to format measurements in centimeters, or even millimeters,
rather than in meters.   Furthermore, users might have requirements for working
in alternate systems, such as inches or feet.

The Rs software handles special systems of measure (i.e. inches, feet,
centimeters, kilometers, yards, etc.) through classes derived from
the abstract RsUnits class.   All unit conversions are performed at
the user interface.  The user may type in a value in feet, but internally
that value is converted to meters.  We refer to the two coordinate systems
as "user coordinates", which may be expressed in whatever system the
user prefers, and "internal coordinates" which are expressed in meters.

The choice of unit system affects more than just the formatting and parsing
of numeric quantities.  For example, if we are drawing a coordinate grid
on a display, we will choose different grid spacing depending on which system
is employed.   It would be reasonable to divided a 1-meter grid into subsections
of 1/10th of a meter...  but only rarely would it make sense to divide a 1-foot
grid into tenths (twelths would be substantially preferred).

The abstract class RsUnits implements some methods, but leaves others to
be implemented by derived classes specific to a particular unit system.
The methods can be divided into three broad categories, those which:

  a.  convert between user and internal coordinates;
  b.  those which format data for display and parse data for user input;
  c.  chose appropriate intervals for labeling and grid scales.

         NOTE:  AT THIS TIME, THE PARSING METHODS ARE "TO BE DETERMINED."


Conversion methods ------------------------------------------------------------------

   abstract double userToInternal(double userCoordinate);       // unimplemented
   abstract double internalToUser(double internalCoordinate);   // unimplemented

      These purpose of the above methods is self-evident. They must be
      implemented by the derived classes (i.e. "feet", "inches", etc.)

   double resolveUserCoordinate(double userCoordinate);                   // implemented
   double resolveInternalCoordinate(double internalCoordinate);           // implemented
   void   setResolutionInUserUnits(double resolutionInUserUnits);         // implemented
   void   setResolutionInInternalUnits(double resolutionInInternalUnits); // implemented
   double getResolutionInUserUnits();                                     // implemented
   double getResolutionInInternalUnits();                                 // implemented

      In real-world applications there are certain conventions for how precisely we can
      specify certain measurements.  For example, a carpenter can measure 1/16th inch,
      but not 1/20th.  Now suppose that we had a GUI based data entry system where the
      user moved the mouse pointer to a particular pixel and clicked to indicate a
      position.   The true position of that pixel might map to some unconventional
      fraction of an inch.  Our application might round it off according to some
      fixed resolution (in this example, to the nearest 1/16th of an inch) in order
      to simplify data entry.

      The resolveUserCoordinate() method takes a user coordinate and applies
      the resolution factor to round it off.   The resolveInternalCoordinate()
      works in an analogous fashion on internal Coordinates.   Mixing the two types
      would result in errornous results.   There is some debate about whether we should
      have provided strong typing to prevent mistakes (implementing a UserCoordinate
      class and an InternalCoordinate class), but this seemed like overkill.

      In practice, coders will generally choose the "user-units" methods for specifying
      resolutions.  "Internal-units" methods are supplied for completeness.

      Note that a resolution specification of zero means "infinite resolution"
      and suppresses any round-off operations.


Formatting methods --------------------------------------------------------------------

In test or GUI-based applications, it is often necessary to convert numerical
values to strings for output or display.   RsUnits implements four format()
methods:

   String format(double userCoordinate);
   String format(String formatString, double userCoordinate);
   String format(double [] userCoordinate);
   String format(String formatString, double [] userCoordinate);

   void  setDefaultFormat(String formatString);

The format string follows the syntax of the C standard library, but
supports only the %f converter.  It also adds its own converters,
%r and %R, for formatting fractions.   This particular set of methods is
ripe for further development and refinement... particularly in the area
of internationalization.  But for now, it is minimal.

In the metric system, the use of the format specifiers is quite straight-forward:

   RsMeters meters = new RsMeters();
   String   s      = meters.format("%8.3f", value);

When working in feet, it takes on an added complexity because it is common
practice to express measurements or coordinates in a mixture of units.
For example, the value 5.380208333 feet would be formatted as "5 ft. 4-3/64 in."
Note that non-integral values are expressed in fractions.  Since there is no
converter for fractions in the C standard library, we invented the %r and %R
converters ("r" for "ratio").   When the %r converter is used, the precision
specification is interpreted as the denominator.  The %r converter reduces
fractions to their minimal form (4/64 becomes 1/16), the %R uses the specified
denominator:

   RsFeet feet = new RsFeet();
   String   s  = feet.format("%4.0f ft. %1.64r in", value);

The trick here is that the format method must break the value out into multiple
parts.  To do so, it uses an array of conversion factors which is optionally supplied
by the constructor for any class derived from RsUnits.   In most classes, these
conversion factors are left null.  In the feet class, the array is specified as

     conversionFactor = new double[3];
     conversionFactor[0] = 1;     // feet remain feet
     conversionFactor[1] = 12.0   // feet to inches
     conversionFactor[2] = 12.0   // feet to inches, in case there are fractions

Note that if we wished to represent feet in decimal form, we could:

   String s = feet.format("%8.3f", 5.380208333);

would produce the string "   5.380".

Using Multiple Formatters for Column Alignment ----------------------

Particularly when working in English units (feet and inches), we have to
be a little creative to get column alignment.   For feet, we may use the
format:
        "%3.0f ft %2.0f-%05.32R in"

This will format values such as:

   "  1 ft  1-03/32 in"
   " 10 ft 11-17/32 in"
   "  0 ft  0-00/32 in

and so forth.   The trick is to use THREE format conversions.  The second
conversion, which is interpreted as inches, exhausts the whole-number
part of the measurement in inches.  We then use the big R formatter
(by not reducing the fraction, we always get a two-digit denominator)
and padding the numerator with leading zeros, we get well-aligned fractions.

Internationalization ---------------------------------

Nothing explicit has been implemented to support internationalization.
The %f format converter does allow you to specify what character you
which to use to separate the whole-number part of the expression from
the fractional part

   English:    23.5     "%4.1f"
   German:     23,5     "%4,1f"

but no knowledge of location is built into the classes.   We recommend that
you simply make it a habit of having any applications using these classes
to employ the setDefaultFormat() method explicitly in an easily identified
section of the code.

Grid and Axis intervals and labeling ----------------------------------------

This section is TO-BE-DETERMINED.   It will involve the same kind
of considerations that went into the old "NiceTics" functions we
used in wXstation.

*/



import java.lang.Math;
import java.lang.StringBuffer;
import java.lang.Character;



/**
 * An abstract base class for RsInches, RsMeters, and RsFeet.
 *
 */

public abstract class RsUnits {

   public RsUnits(String nameRef){
      name=nameRef;
      userResolution=0;       // "infinite" resolution
      defaultFormat="%8.3f";  // we recommend that applications explicitly set this value
      conversionFactor=null;  // do not confuse this with the units-conversion value
   }

   // unimplemented methods (supplied by derived classes)

   public abstract double userToInternal(double userValue);
   public abstract double internalToUser(double internalValue);



   // implemented methods

   public double resolveUserCoordinate(double userCoordinate){
      if(userResolution == 0)
         return userCoordinate;
      return Math.floor(userCoordinate*userResolution+0.5)/userResolution;
   }

   public void setUserResolution(double specification){
      if(specification<0)
         specification=0;  // we won't throw an exception, though it's tempting to do so
      userResolution=specification;
      internalResolution=userToInternal(specification);
   }

   public double getUserResolution(){
      return userResolution;
   }


   public double resolveInternalCoordinate(double internalCoordinate){
      if(internalResolution == 0)
         return internalCoordinate;
      return Math.floor(internalCoordinate*internalResolution+0.5)/internalResolution;
   }

   public void setInternalResolution(double specification){
      if(specification<0)
         specification=0;  // we won't throw an exception, though it's tempting to do so
      internalResolution=specification;
      userResolution=internalToUser(specification);
   }

   public double getInternalResolution(){
      return internalResolution;
   }



   public void setDefaultFormat(String string){
      defaultFormat=string;
   }
   public String format(double value){
      return format(defaultFormat, value);
   }

   public String format(String formatString, double value){
      if(formatString==null)
         return null;

      int            iConversion;
      int            nConversion;
      StringBuffer   b       = new StringBuffer();
      Extractor      extract;

      char           c;
      boolean        negate;
      if(value<0){
         value = -value;
         negate=true;
      }else
         negate=false;

      iConversion=0;
      if(conversionFactor!=null)
         nConversion=conversionFactor.length;
      else
         nConversion=1;
      extract = new Extractor(formatString);
      while((c=extract.next())>0){
         if(c!='%'){
            b.append(c);
         }else{
            // there are times when I REALLY miss the C standard library...
            // here's hoping for java 1.2
            c=extract.next();
            if(c=='%'){
               // the specification %% gets turned to simple %
               b.append(c);
               continue;
            }

            // we've decided that we're going to do format a value.
            // check to make sure that the user didn't supply us with too many
            // formatters for the number of values we support, then extract
            // the value and remainder.

            int  width=0;
            int  precision=6;
            char decimalChar='.';
            char padChar=' ';
            int i, n;
            double test, remainder, decimal;
            int itest;
            StringBuffer p = new StringBuffer();

            if(conversionFactor!=null){
               test=value*conversionFactor[iConversion];
               remainder=test-Math.floor(test);
               value=remainder/conversionFactor[iConversion];
            }else{
               test=value;
               remainder=test-Math.floor(test);
               value=remainder;
            }

            if(iConversion<nConversion-1)
               iConversion++;

            if(negate){
               p.append('-');
               negate=false;
            }

            if(Character.isDigit(c)){
               if(c=='0')
                  padChar='0';
               do{
                  width=width*10+c-'0';
                  c=extract.next();
               }while(Character.isDigit(c));
            }
            if(!Character.isLetter(c)){
               decimalChar = c;
               c=extract.next();
               if(Character.isDigit(c)){
                  precision=0;
                  do{
                     precision=precision*10+c-'0';
                     c=extract.next();
                  }while(Character.isDigit(c));
               }
            }


            if(c=='f'){
               // floating point formatter...  After a bit of algrbra,
               // I suspect that there's a far more elegant way to do this,
               // but since I view this task an unwelcome interruption in the
               // main goal (which is building a simulator), I'm not going to
               // research it any further than I have.
               if(precision>0){
                  decimal=remainder+0.5/Math.floor(Math.pow(10.0,precision)+0.1);
               }else{
                  decimal=0;
               }
               if(test>(Integer.MAX_VALUE-1)){
                  p.append("***");
               }else{
                  itest=(int)Math.floor(test);
                  if(decimal>1){
                     // with specified precision, round-off took it up over 1.
                     itest++;
                     decimal=0;
                  }
                  p.append((new Integer(itest)).toString());
                  if(precision>0){
                     value=0;  // we've exhausted the value for any further conversions
                     p.append(decimalChar);
                     for(i=0;i<precision;i++){
                        decimal*=10.0;
                        itest=(int)Math.floor(decimal);
                        decimal-=itest;
                        p.append((char)(itest+'0'));
                     }
                  }
               }
            }else if(c=='r' || c=='R'){
               if(precision==0)
                  break;
               value = 0; // we've exhausted the value for any further conversions
               int numerator =(int)Math.floor(remainder*precision+0.5);
               int denominator=precision;

               if(test>(Integer.MAX_VALUE-1)){
                  p.append("***");
               }else{
                  itest=(int)Math.floor(test);
                  if(numerator==denominator){
                     // here's where my whole scheme unravels...
                     // with specified precision, round-off took it up to a whole
                     // number.  if this is the only formatter in the list, no problem.
                     // but say we want to use multiple formatters...  the round up
                     // should have been applied to the text already formatted.
                     // but we can do that at this late stage in the process.
                     // so I'm going to lie (see notes above on using multiple formatters)
                     if(iConversion==1){
                        itest++;
                        numerator=0;
                     }else{
                        numerator=denominator-1;
                     }
                  }
                  if(c=='r'){
                     // we need to reduce the fraction
                     i=2;
                     while(i<=numerator){
                        if(numerator == i*(numerator/i) && denominator == i*(denominator/i)){
                           numerator/=i;
                           denominator/=i;
                           continue;
                        }
                        i++;
                     }
                  }
                  if(itest>0){
                    p.append((new Integer(itest)).toString());
                    p.append(decimalChar);
                  }
                  p.append((new Integer(numerator)).toString());
                  p.append('/');
                  p.append((new Integer(denominator)).toString());
               }
            }else
               break;
            n=width-p.length();
            for(i=0;i<n;i++)
               b.append(padChar);
            b.append(p.toString());
         }
      }

      return b.toString();
   }

   public  String getName(){
      return name;
   }


   // private elements
   private String    name;
   private double    userResolution;
   private double    internalResolution;
   private String    defaultFormat;
   protected double [] conversionFactor;
}


class Extractor{
   public Extractor(String s){
         ref=s;
         i=0;
   }
   public char next(){
         char x;
         if(i<ref.length()){
            x=ref.charAt(i);
            i++;
         }else{
            x=(char)0;
         }
         return x;
      }

   int i;
   String ref;
}

