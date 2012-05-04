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
import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.swing.JLabel;

/** 
 * The MouseListener used to listens for changes when the mouse is rolled over a bluetooth service icon.
 * @author Christian Lorenz
 */
public class ServiceRecordMouseListener implements MouseListener {
    JLabel devLabel;
    ServiceRecord record;
    BluetoothBrowser bluetoothBrowser;

    public ServiceRecordMouseListener(JLabel devLabel, BluetoothBrowser bluetoothBrowser, ServiceRecord record) {
        this.devLabel = devLabel;
        this.bluetoothBrowser = bluetoothBrowser;
        this.record = record;
    }

    public void mouseEntered(MouseEvent e) {
        bluetoothBrowser.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        DataElement connectionInfo = record.getAttributeValue(4);
        if (connectionInfo != null) { bluetoothBrowser.statusLabel.setText(connectionInfo.toString()); }
    }

    public void mouseExited(MouseEvent e) {
        bluetoothBrowser.statusLabel.setText(" ");
        bluetoothBrowser.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) {
        bluetoothBrowser.statusLabel.setText("Connecting to " +
            record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
        bluetoothBrowser.connectTo(record);
    }

    public void mouseClicked(MouseEvent e) {
        // do nothing
    }
}

