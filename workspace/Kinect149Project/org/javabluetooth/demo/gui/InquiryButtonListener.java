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

import java.awt.event.*;

/** 
 * The ActionListener used for the Inquiry Button in the Bluetooth Browser.
 * @author Christian Lorenz
 */
public class InquiryButtonListener implements ActionListener {
    BluetoothBrowser bluetoothBrowser;

    public InquiryButtonListener(BluetoothBrowser bluetoothBrowser) { this.bluetoothBrowser = bluetoothBrowser; }

    public void actionPerformed(ActionEvent e) {
        bluetoothBrowser.statusLabel.setText("Inquiry in Progress...");
        bluetoothBrowser.inquire();
    }
}

