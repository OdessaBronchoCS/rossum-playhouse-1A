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



import java.lang.Runnable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


/**
 * This interface, which is intended for RP1 internal use, extends the Java
 * Runnable interface, adding methods that are used for dynamically loaded clients.
 * The methods are given below in the order in which they should be invoked:
 * <ul>
 * <li>  setInputOutputStreams
 * <li>  setLogger
 * <li>  initialize
 * <li>  run
 * </ul>
 * <p>
 * Since the use of this interface requires a rather detailed knowledge of the
 * workings of RP1, most applications developers will find it convenient
 * to take advantage of the existing tools and implementing their RP1 clients
 * by writing an extension to the RsClient class.
 */
public interface RsRunnable extends Runnable {

   public void setInputOutputStreams(InputStream input, OutputStream output);
   public void initialize() throws IOException;
   public void setLogger(RsLogInterface logger);
}

