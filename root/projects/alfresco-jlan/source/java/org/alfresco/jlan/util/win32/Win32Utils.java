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

package org.alfresco.jlan.util.win32;

import org.alfresco.jlan.util.X64;

/**
 * Win32 Specific Utility Class
 *
 * @author gkspencer
 */
public class Win32Utils {

  /**
   * Set the process working set size so that the Java VM does not get swapped out of memory.
   * 
   * <p>Setting a value of -1 for the minSize or maxSize can force the Java process to be swapped out
   * of memory.
   * 
   * @param minSize long
   * @param maxSize long
   * @return boolean
   */
  public static native boolean SetWorkingSetSize(long minSize, long maxSize);
  
  /**
   * Map a network drive optinally assigning a local drive letter to the mapped drive.
   * 
   * @param remPath 	UNC path to the remote disk share to map to
   * @param localDev 	Local device name (such as 'Z:') to map the remote share to
   * @param userName 	If null the default username is used.
   * @param password 	If null the default password is used, if an empty string then no password is used.
   * @param interactive		If true Windows can display a dialog to prompt for the username/password if the
   * 									specified username/password are not valid.
   * @param prompt 		If true then always allow the user to override the specified username/password.
   * @return int
   */
  public static native int MapNetworkDrive(String remPath, String localDev, String userName, String password,
      																		 boolean interactive, boolean prompt);
  
  /**
   * Disconnect a mapped drive and delete the local drive.
   * 
   * @param devName		Either the remote UNC path or the local drive name
   * @param updProfile	If true then the user profile is updated to indicate the mapped drive is no longer
   * 									persistent and will not be restored next time the user logs on
   * @param force			If true the drive is disconnected even if there are open files
   * @return int
   */
  public static native int DeleteNetworkDrive(String devName, boolean updProfile, boolean force);
  
  /**
   * Static initializer used to load the native code library
   */
  static {
    
    // Check if we are running under 64 bit Windows
    
    String dllName = "Win32Utils";
    
    if ( X64.isWindows64())
      dllName = "Win32Utilsx64";
    
    //	Load the Win32 utils library
    
    try {
      System.loadLibrary( dllName);
    }
    catch (Exception ex) {
      ex.printStackTrace(System.err);
    }
  }
}
