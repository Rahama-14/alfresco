package org.alfresco.jlan.smb;

/*
 * LockingAndX.java
 *
 * Copyright (c) 2004 Starlasoft. All rights reserved.
 */
 
/**
 * LockingAndX SMB Constants Class
 * 
 * <p>Contains constants used by the LockingAndX SMB/CIFS request, plus methods for decoding the flags values.
 */
public class LockingAndX {

	//	Lock type flags
	
	public static final int SharedLock		= 0x0001;
	public static final int OplockBreak		= 0x0002;
	public static final int ChangeType		= 0x0004;
	public static final int Cancel				= 0x0008;
	public static final int LargeFiles		= 0x0010;

	/**
	 * Check if this is a normal lock/unlock, ie. no flags except the LargeFiles flag may
	 * be set
	 * 
	 * @param flags
	 * @return boolean
	 */	
	public final static boolean isNormalLockUnlock(int flags) {
		return (flags & 0x000F) == 0 ? true : false;
	}
	
	/**
	 * Check if the large files flag is set
	 *
	 * @param flags int
	 * @return boolean
	 */
	public final static boolean hasLargeFiles(int flags) {
		return (flags & LargeFiles) != 0 ? true : false;
	}

	/**
	 * Check if the shared lock flag is set
	 *
	 * @param flags int
	 * @return boolean
	 */
	public final static boolean hasSharedLock(int flags) {
		return (flags & SharedLock) != 0 ? true : false;
	}

	/**
	 * Check if the oplock break flag is set
	 *
	 * @param flags int
	 * @return boolean
	 */
	public final static boolean hasOplockBreak(int flags) {
		return (flags & OplockBreak) != 0 ? true : false;
	}

	/**
	 * Check if the change type flag is set
	 *
	 * @param flags int
	 * @return boolean
	 */
	public final static boolean hasChangeType(int flags) {
		return (flags & ChangeType) != 0 ? true : false;
	}

	/**
	 * Check if the cancel flag is set
	 *
	 * @param flags int
	 * @return boolean
	 */
	public final static boolean hasCancel(int flags) {
		return (flags & Cancel) != 0 ? true : false;
	}
}
