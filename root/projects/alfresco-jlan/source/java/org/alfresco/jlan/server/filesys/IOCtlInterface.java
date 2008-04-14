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

package org.alfresco.jlan.server.filesys;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.smb.SMBException;
import org.alfresco.jlan.util.DataBuffer;

/**
 * IO Control Interface
 * 
 * <p>Optional interface that a DiskInterface driver can implement to enable NT I/O control function processing.
 *
 * @author gkspencer
 */
public interface IOCtlInterface {

  /**
   * Process a filesystem I/O control request
   * 
   * @param sess			Server session
   * @param tree     	Tree connection.
   * @param ctrlCode	I/O control code
   * @param fid				File id
   * @param dataBuf		I/O control specific input data
   * @param isFSCtrl	true if this is a filesystem control, or false for a device control
   * @param filter		if bit0 is set indicates that the control applies to the share root handle
   * @return DataBuffer
   * @exception IOControlNotImplementedException
   * @exception SMBException
   */
  public DataBuffer processIOControl(SrvSession sess, TreeConnection tree, int ctrlCode, int fid, DataBuffer dataBuf, 
      															 boolean isFSCtrl, int filter)
  	throws IOControlNotImplementedException, SMBException;
}
