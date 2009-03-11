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

package org.alfresco.filesys.auth.ftp;

import javax.transaction.UserTransaction;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.AlfrescoConfigSection;
import org.alfresco.jlan.ftp.FTPAuthenticator;
import org.alfresco.jlan.ftp.FTPSrvSession;
import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author gkspencer
 */
public abstract class FTPAuthenticatorBase implements FTPAuthenticator {

    // Logging
	  
    protected static final Log logger = LogFactory.getLog("org.alfresco.ftp.protocol.auth");

	// Alfresco configuration section

	private AlfrescoConfigSection m_alfrescoConfig;

	/**
	 * Default constructor
	 */
	public FTPAuthenticatorBase() {
	}

	/**
	 * Initialize the authenticator
	 * 
	 * @param config ServerConfiguration
	 * @param params ConfigElement
	 * @exception InvalidConfigurationException
	 */
	public void initialize(ServerConfiguration config, ConfigElement params)
		throws InvalidConfigurationException {

		// Get the alfresco configuration section, required to get hold of various
		// services/components

		m_alfrescoConfig = (AlfrescoConfigSection) config.getConfigSection(AlfrescoConfigSection.SectionName);

		// Check that the required authentication classes are available

		if ( m_alfrescoConfig == null || getAuthenticationComponent() == null)
			throw new InvalidConfigurationException("Authentication component not available");
	}

	/**
	 * Authenticate the user
	 * 
	 * @param client ClientInfo
	 * @param sess FTPSrvSession
	 * @return boolean
	 */
	public abstract boolean authenticateUser(ClientInfo info, FTPSrvSession sess);

	/**
	 * Close the authenticator, perform any cleanup
	 */
	public void closeAuthenticator()
	{
	}

	/**
	 * Return the authentication componenet
	 * 
	 * @return AuthenticationComponent
	 */
	protected final AuthenticationComponent getAuthenticationComponent() {
		return m_alfrescoConfig.getAuthenticationComponent();
	}

	/**
	 * Return the authentication service
	 * 
	 * @return AuthenticationService
	 */
	protected final AuthenticationService getAuthenticationService() {
		return m_alfrescoConfig.getAuthenticationService();
	}

	/**
	 * Return the transaction service
	 * 
	 * @return TransactionService
	 */
	protected final TransactionService getTransactionService() {
		return m_alfrescoConfig.getTransactionService();
	}

	/**
	 * Return the authority service
	 * 
	 * @return AuthorityService
	 */
	protected final AuthorityService getAuthorityService() {
		return m_alfrescoConfig.getAuthorityService();
	}

	/**
	 * Check if the user is an administrator user name
	 * 
	 * @param cInfo ClientInfo
	 */
	protected final void checkForAdminUserName(ClientInfo cInfo) {
		
		// Check if the user name is an administrator

		UserTransaction tx = getTransactionService().getUserTransaction();

		try {
			tx.begin();

			if ( cInfo.getLogonType() == ClientInfo.LogonNormal && getAuthorityService().isAdminAuthority(cInfo.getUserName())) {
				
				// Indicate that this is an administrator logon

				cInfo.setLogonType(ClientInfo.LogonAdmin);
			}
			tx.commit();
		}
		catch (Throwable ex) {
			try {
				tx.rollback();
			}
			catch (Throwable ex2) {
				logger.error("Failed to rollback transaction", ex2);
			}

			// Re-throw the exception

			if ( ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			else {
				throw new RuntimeException("Error during execution of transaction.", ex);
			}
		}
	}
}
