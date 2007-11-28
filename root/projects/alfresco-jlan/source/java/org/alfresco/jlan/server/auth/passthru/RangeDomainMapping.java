package org.alfresco.jlan.server.auth.passthru;

/*
 * RangeDomainMapping.java
 *
 * Copyright (c) 2007 Starlasoft. All rights reserved.
 */
 
import org.alfresco.jlan.util.IPAddress;

/**
 * Address Range Domain Mapping Class
 *
 * @author gkspencer
 */
public class RangeDomainMapping extends DomainMapping {

	// Range from/to addresses
	
	private int m_rangeFrom;
	private int m_rangeTo;
	
	/**
	 * class constructor
	 * 
	 * @param domain String
	 * @param rangeFrom int
	 * @param rangeTo int
	 */
	public RangeDomainMapping( String domain, int rangeFrom, int rangeTo)
	{
		super( domain);
		
		m_rangeFrom = rangeFrom;
		m_rangeTo   = rangeTo;
	}
	
	/**
	 * Return the from range address
	 * 
	 * @return int
	 */
	public final int getRangeFrom()
	{
		return m_rangeFrom;
	}
	
	/**
	 * Return the to range address
	 * 
	 * @return int
	 */
	public final int getRangeTo()
	{
		return m_rangeTo;
	}
	
	/**
	 * Check if the client address is a member of this domain
	 * 
	 * @param clientIP int
	 * @return boolean
	 */
	public boolean isMemberOfDomain( int clientIP)
	{
		if (clientIP >= m_rangeFrom && clientIP <= m_rangeTo)
			return true;
		return false;
	}
	
	/**
	 * Return the domain mapping as a string
	 * 
	 * @return String
	 */
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		str.append("[");
		str.append(getDomain());
		str.append(",");
		str.append(IPAddress.asString( getRangeFrom()));
		str.append(":");
		str.append(IPAddress.asString( getRangeTo()));
		str.append("]");
		
		return str.toString();
	}
}
