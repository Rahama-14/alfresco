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

/**
 *  SMB file access mode class.
 *
 *  <p>The SMB access mode values are used when opening a file using one of the
 *  SMBDiskSession OpenFile (), OpenInputStream () or OpenOutputStream () methods.
 *
 * @author gkspencer
 */
public final class AccessMode {

  // Access mode constants

  public static final int ReadOnly 			= 0x0000;
  public static final int WriteOnly 		= 0x0001;
  public static final int ReadWrite 		= 0x0002;
  public static final int Execute 			= 0x0003;
  public static final int Compatability = 0x0000;
  public static final int Exclusive 		= 0x0010;
  public static final int DenyWrite 		= 0x0020;
  public static final int DenyRead 			= 0x0030;
  public static final int DenyNone 			= 0x0040;
  public static final int NoCaching 		= 0x1000;
  public static final int WriteThrough 	= 0x4000;
  protected static final int FCBOpen 		= 0x00FF;

	//	NT access mode constants
	
	public static final int	NTRead						= 0x00000001;
	public static final int	NTWrite						= 0x00000002;
	public static final int	NTAppend					= 0x00000004;
	public static final int	NTReadEA					= 0x00000008;
	public static final int	NTWriteEA					= 0x00000010;
	public static final int	NTExecute					= 0x00000020;
	public static final int	NTDeleteChild			= 0x00000040;
	public static final int	NTReadAttrib			= 0x00000080;
	public static final int	NTWriteAttrib			= 0x00000100;
	
	public static final int	NTDelete					= 0x00010000;
	public static final int	NTReadControl			= 0x00020000;
	public static final int	NTWriteDAC				= 0x00040000;
	public static final int	NTWriteOwner			= 0x00080000;
	public static final int	NTSynchronize			= 0x00100000;
	public static final int NTSystemSecurity	= 0x01000000;
	
	public static final int NTGenericRead			= 0x80000000;
	public static final int NTGenericWrite		= 0x40000000;
	public static final int NTGenericExecute	= 0x20000000;
	public static final int NTGenericAll			= 0x10000000;
	
	public static final int NTMaximumAllowed	= 0x02000000;
  
  public static final int NTReadWrite				= NTRead + NTWrite;
  
  /**
   * Return the file access mode from the specified flags value.
   *
   * @param val  File flags value.
   * @return     File access mode.
   */
  public static final int getAccessMode(int val) {
    return val & 0x03;
  }
  
  /**
   * Return the file sharing mode from the specified flags value.
   *
   * @param val  File flags value.
   * @return     File sharing mode.
   */
  public static final int getSharingMode(int val) {
    return val & 0x70;
  }
}
