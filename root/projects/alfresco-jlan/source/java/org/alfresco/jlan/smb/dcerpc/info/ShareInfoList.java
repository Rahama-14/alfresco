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

package org.alfresco.jlan.smb.dcerpc.info;

import java.util.Vector;

import org.alfresco.jlan.smb.dcerpc.DCEBuffer;
import org.alfresco.jlan.smb.dcerpc.DCEBufferException;
import org.alfresco.jlan.smb.dcerpc.DCEList;
import org.alfresco.jlan.smb.dcerpc.DCEReadable;

/**
 * Server Share Information List Class
 * 
 * <p>Holds the details for a DCE/RPC share enumeration request or response.
 *
 * @author gkspencer
 */
public class ShareInfoList extends DCEList {

	/**
	 * Default constructor
	 */
	public ShareInfoList() {
	  super();
	}

  /**
   * Class constructor
   * 
   * @param buf DCEBuffer
   * @exception DCEBufferException
   */
  public ShareInfoList(DCEBuffer buf)
  	throws DCEBufferException {
    super(buf);
  }
  
	/**
	 * Class constructor
	 * 
	 * @param infoLevel int
	 */
	public ShareInfoList(int infoLevel) {
	  super(infoLevel);
	}

	/**
	 * Return share information object from the list
	 * 
	 * @param idx int
	 * @return ShareInfo
	 */
	public final ShareInfo getShare(int idx) {
	  return (ShareInfo) getElement(idx);
	}
	
  /**
   * Create a new share information object
   * 
   * @return DCEReadable
   */
  protected DCEReadable getNewObject() {
    return new ShareInfo(getInformationLevel());
  }

  /**
   * Add a share to the list
   * 
   * @param share ShareInfo
   */
  public final void addShare(ShareInfo share) {
    
    //	Check if the share list is valid
    
    if ( getList() == null)
      	setList(new Vector());
    
    //	Add the share
    
    getList().addElement(share);
  }
  
	/**
	 * Set the share information list
	 * 
	 * @param list Vector
	 */
	public final void setShareList(Vector list) {
	  setList(list);
	}
}
