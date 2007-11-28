package org.alfresco.jlan.locking;

/*
 * FileLock.java
 *
 * Copyright (c) 2004 Starlasoft. All rights reserved.
 */
 
/**
 * File Lock Class
 *
 * <p>Contains the details of a single file lock.
 */
public class FileLock {

	//	Constants
	
	public static final long LockWholeFile		=	0xFFFFFFFFFFFFFFFFL;
	
  //  Start lock offset and length

  private long m_offset;
  private long m_length;

  //  Owner process id

  private int m_pid;
  
  /**
   * Class constructor
   * 
   * @param offset long
   * @param len long
   * @param pid int
   */
  public FileLock(long offset, long len, int pid) {
    setOffset(offset);
    setLength(len);
    setProcessId(pid);
  }
  
  /**
   * Get the starting lock offset
   *
   * @return long Starting lock offset.
   */
  public final long getOffset() {
    return m_offset;
  }
  
  /**
   * Set the starting lock offset.
   *
   * @param offset Starting lock offset
   */
  public final void setOffset(long offset) {
    m_offset = offset;
  }
  
  /**
   * Get the locked section length
   *
   * @return long Locked section length
   */
  public final long getLength() {
    return m_length;
  }
  
  /**
   * Set the locked section length
   *
   * @param len Locked section length
   */
  public final void setLength(long len) {
    if ( len < 0)
      m_length = LockWholeFile;
    else
      m_length = len;
  }
  
  /**
   * Get the owner process id for the lock
   *
   * @return int
   */
  public final int getProcessId() {
    return m_pid;
  }

	/**
	 * Deterine if the lock is locking the whole file
	 * 
	 * @return boolean
	 */
	public final boolean isWholeFile() {
		return m_length == LockWholeFile ? true : false;
	}
	
  /**
   * Set the process id of the owner of this lock
   *
   * @param pid int
   */
  public final void setProcessId(int pid) {
    m_pid = pid;
  }
  
  /**
   * Check if the specified locks byte range overlaps this locks byte range.
   *
   * @param lock FileLock
   */
  public final boolean hasOverlap(FileLock lock) {
    return hasOverlap(lock.getOffset(), lock.getLength());
  }
  
	/**
	 * Check if the specified locks byte range overlaps this locks byte range.
	 *
	 * @param offset long
	 * @param len long
	 */
	public final boolean hasOverlap(long offset, long len) {

		//	Check if the lock is for the whole file
		
		if ( isWholeFile())
			return true;
			
		//  Check if the locks overlap

		long endOff = getOffset() + ( getLength() - 1);
		
		if (getOffset() < offset && endOff < offset)
			return false;

		endOff = offset + ( len - 1);
		
		if ( getOffset() > endOff)
			return false;

		//  Locks overlap

		return true;
	}
  
  /**
   * Return the lock details as a string
   * 
   * @return String
   */
  public String toString() {
  	StringBuffer str = new StringBuffer();
  	
  	str.append("[PID="); 	str.append(getProcessId()); 	str.append(",Offset=");
  	str.append(getOffset());
  	str.append(",Len=");
  	str.append(getLength());
  	str.append("]");
  	
  	return str.toString();
  }
}