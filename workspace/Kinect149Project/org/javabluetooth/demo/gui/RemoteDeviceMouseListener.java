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
* Created on Jul 31, 2003
* by Christian Lorenz
*/

package org.javabluetooth.demo.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.bluetooth.RemoteDevice;
import javax.swing.JLabel;

/** 
 * The MouseListener used to listens for changes when the mouse is rolled over a bluetooth device icon.
 * @author Christian Lorenz
 */
public class RemoteDeviceMouseListener implements MouseListener {
    JLabel devLabel;
    RemoteDevice dev;
    BluetoothBrowser bluetoothBrowser;

    public RemoteDeviceMouseListener(JLabel devLabel, BluetoothBrowser bluetoothBrowser, RemoteDevice dev) {
        this.devLabel = devLabel;
        this.bluetoothBrowser = bluetoothBrowser;
        this.dev = dev;
    }

    public void mouseEntered(MouseEvent e) {
        bluetoothBrowser.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        try {
            String friendlyName = dev.getFriendlyName(false);
            devLabel.setText(friendlyName);
        }
        catch (IOException e1) { }
    }

    public void mouseExited(MouseEvent e) {
        bluetoothBrowser.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) {
        bluetoothBrowser.listServices(dev); // list the services for the specified device
    }

    public void mouseClicked(MouseEvent e) {
        // do nothing
    }
}

