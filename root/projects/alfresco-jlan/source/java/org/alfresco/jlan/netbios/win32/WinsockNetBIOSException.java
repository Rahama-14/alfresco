/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.netbios.win32;

import java.io.IOException;

/**
 * Winsock NetBIOS Exception Class
 * 
 * <p>
 * Contains the Winsock error code from the failed Winsock call.
 *
 * @author gkspencer
 */
public class WinsockNetBIOSException extends IOException {

  // Object version
  
  private static final long serialVersionUID = -5776000315712407725L;
  
  // Winsock error code

  private int m_errCode;

  /**
   * Default constructor
   */
  public WinsockNetBIOSException() {
    super();
  }

  /**
   * Class constructor
   * 
   * @param msg
   *          String
   */
  public WinsockNetBIOSException(String msg) {
    super(msg);

    // Split out the error code

    if (msg != null) {
      int pos = msg.indexOf(":");
      if (pos != -1)
        m_errCode = Integer.valueOf(msg.substring(0, pos)).intValue();
    }
  }

  /**
   * Class constructor
   * 
   * @param sts
   *          int
   */
  public WinsockNetBIOSException(int sts) {
    super();

    m_errCode = sts;
  }

  /**
   * Return the Winsock error code
   * 
   * @return int
   */
  public final int getErrorCode() {
    return m_errCode;
  }

  /**
   * Set the error code
   * 
   * @param sts
   *          int
   */
  public final void setErrorCode(int sts) {
    m_errCode = sts;
  }

  /**
   * Return the error message string
   * 
   * @return String
   */
  public String getMessage() {
    StringBuffer msg = new StringBuffer();

    msg.append(super.getMessage());
    String winsockErr = WinsockError.asString(getErrorCode());
    if (winsockErr != null) {
      msg.append(" - ");
      msg.append(winsockErr);
    }

    return msg.toString();
  }
}
