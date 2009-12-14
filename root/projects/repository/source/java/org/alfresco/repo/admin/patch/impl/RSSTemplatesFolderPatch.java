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
package org.alfresco.repo.admin.patch.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ACPImportPackageHandler;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.admin.PatchException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures that the <b>RSS Templates</b> folder is present.
 * <p>
 * This uses the bootstrap importer to get the paths to look for.  If not present,
 * the required structures are created.
 * <p>
 * This class should be replaced with a more generic <code>ImporterPatch</code>
 * that can do conditional importing into given locations.
 * <p>
 * JIRA: {@link http://www.alfresco.org/jira/browse/AR-342 AR-342}
 * 
 * @author Kevin Roast
 */
public class RSSTemplatesFolderPatch extends AbstractPatch
{
    private static final String MSG_EXISTS = "patch.rssTemplatesFolder.result.exists";
    private static final String MSG_CREATED = "patch.rssTemplatesFolder.result.created";
    
    public static final String PROPERTY_COMPANY_HOME_CHILDNAME = "spaces.company_home.childname";
    public static final String PROPERTY_DICTIONARY_CHILDNAME = "spaces.dictionary.childname";
    public static final String PROPERTY_RSS_FOLDER_CHILDNAME = "spaces.templates.rss.childname";
    private static final String PROPERTY_RSS_FOLDER_NAME = "spaces.templates.rss.name";
    private static final String PROPERTY_RSS_FOLDER_DESCRIPTION = "spaces.templates.rss.description";
    private static final String PROPERTY_ICON = "space-icon-default";
    
    private ImporterBootstrap importerBootstrap;
    private ImporterService importerService;
    private MessageSource messageSource;
    private PermissionService permissionService;
    
    protected NodeRef dictionaryNodeRef;
    protected Properties configuration;
    protected NodeRef rssFolderNodeRef;
    
    private String rssTemplatesACP;
    
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }
    
    public void setImporterBootstrap(ImporterBootstrap importerBootstrap)
    {
        this.importerBootstrap = importerBootstrap;
    }
    
    public void setImporterService(ImporterService importerService)
    {
        this.importerService = importerService;
    }

    public void setMessageSource(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    public void setRssTemplatesACP(String rssTemplatesACP)
    {
        this.rssTemplatesACP = rssTemplatesACP;
    }

   /**
     * Ensure that required common properties have been set
     */
    protected void checkCommonProperties() throws Exception
    {
        checkPropertyNotNull(importerBootstrap, "importerBootstrap");
        checkPropertyNotNull(importerService, "importerService");
        checkPropertyNotNull(messageSource, "messageSource");
        if (namespaceService == null)
        {
            throw new PatchException("'namespaceService' property has not been set");
        }
        else if (searchService == null)
        {
            throw new PatchException("'searchService' property has not been set");
        }
        else if (nodeService == null)
        {
            throw new PatchException("'nodeService' property has not been set");
        }
        checkPropertyNotNull(rssTemplatesACP, "rssTemplatesACP");
    }
    
    /**
     * Extracts pertinent references and properties that are common to execution
     * of this and derived patches.
     */
    protected void setUp() throws Exception
    {
        // get the node store that we must work against
        StoreRef storeRef = importerBootstrap.getStoreRef();
        if (storeRef == null)
        {
            throw new PatchException("Bootstrap store has not been set");
        }
        NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);

        this.configuration = importerBootstrap.getConfiguration();
        // get the association names that form the path
        String companyHomeChildName = configuration.getProperty(PROPERTY_COMPANY_HOME_CHILDNAME);
        if (companyHomeChildName == null || companyHomeChildName.length() == 0)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_COMPANY_HOME_CHILDNAME + "' is not present");
        }
        String dictionaryChildName = configuration.getProperty(PROPERTY_DICTIONARY_CHILDNAME);
        if (dictionaryChildName == null || dictionaryChildName.length() == 0)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_DICTIONARY_CHILDNAME + "' is not present");
        }
        String rssChildName = configuration.getProperty(PROPERTY_RSS_FOLDER_CHILDNAME);
        if (rssChildName == null || rssChildName.length() == 0)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_RSS_FOLDER_CHILDNAME + "' is not present");
        }
        
        // build the search string to get the dictionary node
        StringBuilder sb = new StringBuilder(256);
        sb.append("/").append(companyHomeChildName)
          .append("/").append(dictionaryChildName);
        String xpath = sb.toString();
        
        // get the dictionary node
        List<NodeRef> nodeRefs = searchService.selectNodes(storeRootNodeRef, xpath, null, namespaceService, false);
        if (nodeRefs.size() == 0)
        {
            throw new PatchException("XPath didn't return any results: \n" +
                    "   root: " + storeRootNodeRef + "\n" +
                    "   xpath: " + xpath);
        }
        else if (nodeRefs.size() > 1)
        {
            throw new PatchException("XPath returned too many results: \n" +
                    "   root: " + storeRootNodeRef + "\n" +
                    "   xpath: " + xpath + "\n" +
                    "   results: " + nodeRefs);
        }
        this.dictionaryNodeRef = nodeRefs.get(0);
        
        // Now we have the optional part - check for the existence of the RSS Templates folder
        xpath = rssChildName;
        nodeRefs = searchService.selectNodes(dictionaryNodeRef, xpath, null, namespaceService, false);
        if (nodeRefs.size() > 1)
        {
            throw new PatchException("XPath returned too many results: \n" +
                    "   dictionary node: " + dictionaryNodeRef + "\n" +
                    "   xpath: " + xpath + "\n" +
                    "   results: " + nodeRefs);
        }
        else if (nodeRefs.size() == 0)
        {
            // the node does not exist
            this.rssFolderNodeRef = null;
        }
        else
        {
            // we have the RSS Templates folder noderef
            this.rssFolderNodeRef = nodeRefs.get(0);
        }
    }
    
    @Override
    protected String applyInternal() throws Exception
    {
        // properties must be set
        checkCommonProperties();
        if (messageSource == null)
        {
            throw new PatchException("'messageSource' property has not been set");
        }
        
        // get useful values
        setUp();
        
        String msg = null;
        if (rssFolderNodeRef == null)
        {
            // create it
            createFolder();
            
            // apply Guest permission to the folder
            permissionService.setPermission(
                rssFolderNodeRef,
                AuthenticationUtil.getGuestUserName(),
                PermissionService.CONSUMER,
                true);
            
            // import the content
           
            importContent();
           
            msg = I18NUtil.getMessage(MSG_CREATED, rssFolderNodeRef);
        }
        else
        {
            // it already exists
            permissionService.setPermission(
                    rssFolderNodeRef,
                    AuthenticationUtil.getGuestUserName(),
                    PermissionService.CONSUMER,
                    true);
            msg = I18NUtil.getMessage(MSG_EXISTS, rssFolderNodeRef);
        }
        // done
        return msg;
    }
    
    private void createFolder()
    {
        // get required properties
        String rssChildName = configuration.getProperty(PROPERTY_RSS_FOLDER_CHILDNAME);
        if (rssChildName == null)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_RSS_FOLDER_CHILDNAME + "' is not present");
        }
        
        String folderName = messageSource.getMessage(
                PROPERTY_RSS_FOLDER_NAME,
                null,
                I18NUtil.getLocale());
        if (folderName == null || folderName.length() == 0)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_RSS_FOLDER_NAME + "' is not present");
        }

        String folderDescription = messageSource.getMessage(
                PROPERTY_RSS_FOLDER_DESCRIPTION,
                null,
                I18NUtil.getLocale());
        if (folderDescription == null || folderDescription.length() == 0)
        {
            throw new PatchException("Bootstrap property '" + PROPERTY_RSS_FOLDER_DESCRIPTION + "' is not present");
        }
        
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(7);
        properties.put(ContentModel.PROP_NAME, folderName);
        properties.put(ContentModel.PROP_TITLE, folderName);
        properties.put(ContentModel.PROP_DESCRIPTION, folderDescription);
        properties.put(ApplicationModel.PROP_ICON, PROPERTY_ICON);
        
        // create the node
        ChildAssociationRef childAssocRef = nodeService.createNode(
                dictionaryNodeRef,
                ContentModel.ASSOC_CONTAINS,
                QName.resolveToQName(namespaceService, rssChildName),
                ContentModel.TYPE_FOLDER,
                properties);
        this.rssFolderNodeRef = childAssocRef.getChildRef();
        
        // finally add the required aspects
        nodeService.addAspect(rssFolderNodeRef, ApplicationModel.ASPECT_UIFACETS, null);
    }
    
    private void importContent() throws IOException
    {
        // import the content
        ClassPathResource acpResource = new ClassPathResource(this.rssTemplatesACP);
        ACPImportPackageHandler acpHandler = new ACPImportPackageHandler(acpResource.getFile(), null);
        Location importLocation = new Location(this.rssFolderNodeRef);
        importerService.importView(acpHandler, importLocation, null, null);
    }
}
