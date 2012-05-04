/*
*  (c) Copyright 2003 Christian Lorenz  ALL RIGHTS RESERVED.
* 
* This file is part of the JavaBluetooth Stack.
* 
* The JavaBluetooth Stack is free software; you can redistribute it 
* and/or modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2 of
* the License, or (at your option) any later version.
* 
* The JavaBluetooth Stack is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* Created on Jul 6, 2003
* by Christian Lorenz
*/

package javax.microedition.io;

/** 
 * This interface is used by the JSR-82 <code>javax.bluetooth</code> API.
 * JSR-82 partially relies on Java CDC. Since J2SE and the TINI Java VM, do not support the CDC and
 * <code>javax.microedition.io</code>, it's recreated here for compatability.
 * @author Christian Lorenz
 */
public abstract interface Connection { public void close(); }

