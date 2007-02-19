/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.webservice.util;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.xml.rpc.ServiceException;

import org.alfresco.webservice.accesscontrol.AccessControlServiceLocator;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.action.ActionServiceLocator;
import org.alfresco.webservice.action.ActionServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationServiceLocator;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.authentication.AuthenticationServiceLocator;
import org.alfresco.webservice.authentication.AuthenticationServiceSoapBindingStub;
import org.alfresco.webservice.authoring.AuthoringServiceLocator;
import org.alfresco.webservice.authoring.AuthoringServiceSoapBindingStub;
import org.alfresco.webservice.classification.ClassificationServiceLocator;
import org.alfresco.webservice.classification.ClassificationServiceSoapBindingStub;
import org.alfresco.webservice.content.ContentServiceLocator;
import org.alfresco.webservice.content.ContentServiceSoapBindingStub;
import org.alfresco.webservice.dictionary.DictionaryServiceLocator;
import org.alfresco.webservice.dictionary.DictionaryServiceSoapBindingStub;
import org.alfresco.webservice.repository.RepositoryServiceLocator;
import org.alfresco.webservice.repository.RepositoryServiceSoapBindingStub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 
 * 
 * @author Roy Wetherall
 */
public final class WebServiceFactory
{
    /** Log */
    private static Log logger = LogFactory.getLog(WebServiceFactory.class);
    
    /** Property file name */
    private static final String PROPERTY_FILE_NAME = "alfresco/webserviceclient.properties";
    private static final String REPO_LOCATION = "repository.location";
    
    /** Default endpoint address **/
    private static final String DEFAULT_ENDPOINT_ADDRESS = "http://localhost:8080/alfresco/api";
    private static String endPointAddress;
    
    /** Service addresses */
    private static final String AUTHENTICATION_SERVICE_ADDRESS  = "/AuthenticationService";
    private static final String REPOSITORY_SERVICE_ADDRESS      = "/RepositoryService";
    private static final String CONTENT_SERVICE_ADDRESS         = "/ContentService";
    private static final String AUTHORING_SERVICE_ADDRESS       = "/AuthoringService";
    private static final String CLASSIFICATION_SERVICE_ADDRESS  = "/ClassificationService";
    private static final String ACTION_SERVICE_ADDRESS          = "/ActionService";
    private static final String ACCESS_CONTROL_ADDRESS          = "/AccessControlService";
    private static final String ADMINISTRATION_ADDRESS          = "/AdministrationService";
    private static final String DICTIONARY_SERVICE_ADDRESS      = "/DictionaryService";
    
    /**
     * Sets the endpoint address manually, overwrites the current value
     * 
     * @param endPointAddress	the end point address
     */
    public static void setEndpointAddress(String endPointAddress)
    {
    	WebServiceFactory.endPointAddress = endPointAddress;
    }
    
    /**
     * Get the current endpoints host
     * 
     * @return  the current endpoint host name
     */
    public static String getHost()
    {
        try
        {
            URL url = new URL(getEndpointAddress());
            return url.getHost();
        }
        catch (MalformedURLException exception)
        {
            throw new RuntimeException("Unable to get host string", exception);
        }
    }
    
    /**
     * Get the current endpoints port
     * 
     * @return  the current endpoint port number
     */
    public static int getPort()
    {
        try
        {
            URL url = new URL(getEndpointAddress());
            return url.getPort();
        }
        catch (MalformedURLException exception)
        {
            throw new RuntimeException("Unable to get host string", exception);
        }
    }
    
    /**
     * Get the authentication service
     * 
     * @return
     */
    public static AuthenticationServiceSoapBindingStub getAuthenticationService()
    {
    	AuthenticationServiceSoapBindingStub authenticationService = null;
        try 
        {
            // Get the authentication service
            AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
            locator.setAuthenticationServiceEndpointAddress(getEndpointAddress() + AUTHENTICATION_SERVICE_ADDRESS);                
            authenticationService = (AuthenticationServiceSoapBindingStub)locator.getAuthenticationService();
        }
        catch (ServiceException jre) 
        {
        	if (logger.isDebugEnabled() == true)
            {
        		if (jre.getLinkedCause() != null)
                {
        			jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating authentication service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        authenticationService.setTimeout(60000);
        
        return authenticationService;
    }
    
    public static RepositoryServiceSoapBindingStub getRepositoryService()
    {
    	return getRepositoryService(getEndpointAddress());
    }
    
    /**
     * Get the repository service
     * 
     * @return
     */
    public static RepositoryServiceSoapBindingStub getRepositoryService(String endpointAddress)
    {
    	RepositoryServiceSoapBindingStub repositoryService = null;           
		try 
		{
		    // Get the repository service
		    RepositoryServiceLocator locator = new RepositoryServiceLocator(AuthenticationUtils.getEngineConfiguration());
		    locator.setRepositoryServiceEndpointAddress(endpointAddress + REPOSITORY_SERVICE_ADDRESS);                
		    repositoryService = (RepositoryServiceSoapBindingStub)locator.getRepositoryService();
            repositoryService.setMaintainSession(true);
		 }
		 catch (ServiceException jre) 
		 {
			 if (logger.isDebugEnabled() == true)
		     {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
		     }
		   
			 throw new WebServiceException("Error creating repositoryService service: " + jre.getMessage(), jre);
		}        
	
		// Time out after a minute
		repositoryService.setTimeout(60000);      
        
        return repositoryService;
    }
    
    public static AuthoringServiceSoapBindingStub getAuthoringService()
    {
    	return getAuthoringService(getEndpointAddress());
    }
    
    /**
     * Get the authoring service
     * 
     * @return
     */
    public static AuthoringServiceSoapBindingStub getAuthoringService(String endpointAddress)
    {
    	AuthoringServiceSoapBindingStub authoringService = null;
                  
        try 
        {
            // Get the authoring service
            AuthoringServiceLocator locator = new AuthoringServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAuthoringServiceEndpointAddress(endpointAddress + AUTHORING_SERVICE_ADDRESS);                
            authoringService = (AuthoringServiceSoapBindingStub)locator.getAuthoringService();
            authoringService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating authoring service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        authoringService.setTimeout(60000);       
        
        return authoringService;
    }
    
    public static ClassificationServiceSoapBindingStub getClassificationService()
    {
    	return getClassificationService(getEndpointAddress());
    }
    
    /**
     * Get the classification service
     * 
     * @return
     */
    public static ClassificationServiceSoapBindingStub getClassificationService(String endpointAddress)
    {
    	ClassificationServiceSoapBindingStub classificationService = null;
            
        try 
        {
            // Get the classification service
            ClassificationServiceLocator locator = new ClassificationServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setClassificationServiceEndpointAddress(endpointAddress + CLASSIFICATION_SERVICE_ADDRESS);                
            classificationService = (ClassificationServiceSoapBindingStub)locator.getClassificationService();
            classificationService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating classification service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        classificationService.setTimeout(60000);        
        
        return classificationService;
    }
    
    public static ActionServiceSoapBindingStub getActionService()
    {
    	return getActionService(getEndpointAddress());
    }
    
    /**
     * Get the action service
     * 
     * @return
     */
    public static ActionServiceSoapBindingStub getActionService(String endpointAddress)
    {
    	ActionServiceSoapBindingStub actionService = null;
            
        try 
        {
            // Get the action service
            ActionServiceLocator locator = new ActionServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setActionServiceEndpointAddress(endpointAddress + ACTION_SERVICE_ADDRESS);                
            actionService = (ActionServiceSoapBindingStub)locator.getActionService();
            actionService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating action service: " + jre.getMessage(), jre);
        }        
            
        // Time out after a minute
        actionService.setTimeout(60000);      
        
        return actionService;
    }
    
    public static ContentServiceSoapBindingStub getContentService()
    {
    	return getContentService(getEndpointAddress());
    }
    
    /**
     * Get the content service
     * 
     * @return  the content service
     */
    public static ContentServiceSoapBindingStub getContentService(String endpointAddress)
    {
    	ContentServiceSoapBindingStub contentService = null;           
        try 
        {
            // Get the content service
            ContentServiceLocator locator = new ContentServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setContentServiceEndpointAddress(endpointAddress + CONTENT_SERVICE_ADDRESS);                
            contentService = (ContentServiceSoapBindingStub)locator.getContentService();
            contentService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating content service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        contentService.setTimeout(60000);       
        
        return contentService;
    }
    
    public static AccessControlServiceSoapBindingStub getAccessControlService()
    {
    	return getAccessControlService(getEndpointAddress());
    }
    
    /**
     * Get the access control service
     * 
     * @return  the access control service
     */
    public static AccessControlServiceSoapBindingStub getAccessControlService(String enpointAddress)
    {
    	AccessControlServiceSoapBindingStub accessControlService = null;           
        try 
        {
            // Get the access control service
            AccessControlServiceLocator locator = new AccessControlServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAccessControlServiceEndpointAddress(enpointAddress + ACCESS_CONTROL_ADDRESS);                
            accessControlService = (AccessControlServiceSoapBindingStub)locator.getAccessControlService();
            accessControlService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating access control service: " + jre.getMessage(), jre);
        }        
            
        // Time out after a minute
        accessControlService.setTimeout(60000);
        
        return accessControlService;
    }
    
    public static AdministrationServiceSoapBindingStub getAdministrationService()
    {
    	return getAdministrationService(getEndpointAddress());
    }
    
    /**
     * Get the administation service
     * 
     * @return  the administration service
     */
    public static AdministrationServiceSoapBindingStub getAdministrationService(String endpointAddress)
    {
    	AdministrationServiceSoapBindingStub administrationService = null;
            
        try 
        {
            // Get the adminstration service
            AdministrationServiceLocator locator = new AdministrationServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAdministrationServiceEndpointAddress(endpointAddress + ADMINISTRATION_ADDRESS);                
            administrationService = (AdministrationServiceSoapBindingStub)locator.getAdministrationService();
            administrationService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating administration service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        administrationService.setTimeout(60000);       
        
        return administrationService;
    }
    
    public static DictionaryServiceSoapBindingStub getDictionaryService()
    {
    	return getDictionaryService(getEndpointAddress());
    }

    /**
     * Get the dictionary service
     * 
     * @return  the dictionary service
     */
    public static DictionaryServiceSoapBindingStub getDictionaryService(String endpointAddress)
    {
    	DictionaryServiceSoapBindingStub dictionaryService = null;
           
        try 
        {
            // Get the dictionary service
            DictionaryServiceLocator locator = new DictionaryServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setDictionaryServiceEndpointAddress(endpointAddress + DICTIONARY_SERVICE_ADDRESS);                
            dictionaryService = (DictionaryServiceSoapBindingStub)locator.getDictionaryService();
            dictionaryService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating dictionary service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        dictionaryService.setTimeout(60000);

        return dictionaryService;
    }
    
    /**
     * Gets the end point address from the properties file
     * 
     * @return
     */
    private static String getEndpointAddress()
    {
    	if (endPointAddress == null)
    	{
    		endPointAddress = DEFAULT_ENDPOINT_ADDRESS;
	
	        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTY_FILE_NAME); 
	        if (is != null)
	        {
	            Properties props = new Properties();
	            try
	            {
	                props.load(is);            
	                endPointAddress = props.getProperty(REPO_LOCATION);
	                
	                if (logger.isDebugEnabled() == true)
	                {
	                    logger.debug("Using endpoint " + endPointAddress);
	                }
	            }
	            catch (Exception e)
	            {
	                // Do nothing, just use the default endpoint
	                if (logger.isDebugEnabled() == true)
	                {
	                    logger.debug("Unable to file web service client proerties file.  Using default.");
	                }
	            }
	        }
    	}
        
        return endPointAddress;
    }
}
