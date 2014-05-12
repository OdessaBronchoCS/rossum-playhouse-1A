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
   RsColor.java


*/


import java.awt.Color;



/**
 * A class providing static methods useful for translating color names.
 *
 */

public class RsColor {


   public static Color getColorForName(String name){
      try {
         Color c = Color.decode(name);
         return c;
      }catch(NumberFormatException e){}

      int test;
      for(int i=0;i<nameList.length;i++){
         test=nameList[i].compareTo(name);
         if(test==0)
            return colorList[i];
         else if(test>0)
            break;
      }

      return null;
   }

   public static String getNameForColor(Color c){
      for(int i=0; i<nameList.length;i++){
         if(c.getRed()==colorList[i].getRed() &&
            c.getGreen()==colorList[i].getGreen() &&
            c.getBlue()==colorList[i].getBlue() )
               return nameList[i];
      }

      // couldn't quite match a string, format for hex

      StringBuffer s = new StringBuffer(7);
      s.append("\"#");
      hexpend(s, c.getRed());
      hexpend(s, c.getGreen());
      hexpend(s, c.getBlue());
      s.append("\"");
      return s.toString();
   }

   private static void hexpend(StringBuffer s, int value){
      int test;
      test=value>>>4;
      if(test<10)
         s.append((char)(test+'0'));
      else
         s.append((char)(test-10+'a'));
      test=value&0x0f;
      if(test<10)
         s.append((char)(test+'0'));
      else
         s.append((char)(test-10+'a'));
   }


   public static boolean areColorListsTheSame(Color a[], Color b[]){
         if(a==null || b==null){
            return (a==b);
         }
         if(a.length!=b.length)
            return false;

         for(int i=0; i<a.length; i++){
           // screen out cases where either or both ith elements are null
           if(a[i] == null || b[i]==null){
              if(b[i]!=a[i])
                 return false;
              continue;
           }

           if(a[i].getRed()   != b[i].getRed()   ||
              a[i].getGreen() != b[i].getGreen() ||
              a[i].getBlue()  != b[i].getBlue()
           ){
              return false;
           }
         }

         return true;
   }

   public static Color [] copyColorList(Color a[]){
      if(a==null || a[0]==null){
       return null;
      }

      Color [] color = new Color[a.length];
      for(int i=0; i<a.length; i++){
         if(a[i]==null)
            color[i] =null;
         else
            color[i] = new Color( a[i].getRed(), a[i].getGreen(), a[i].getBlue());

      }

      return color;
   }



   private static final String [] nameList = {
      "black",
      "blue",
      "cyan",
      "darkGray",
      "gray",
      "green",
      "lightGray",
      "magenta",
      "orange",
      "pink",
      "red",
      "white",
      "yellow"
   };

   private static final Color [] colorList = {
      Color.black,
      Color.blue,
      Color.cyan,
      Color.darkGray,
      Color.gray,
      Color.green,
      Color.lightGray,
      Color.magenta,
      Color.orange,
      Color.pink,
      Color.red,
      Color.white,
      Color.yellow
   };
}

