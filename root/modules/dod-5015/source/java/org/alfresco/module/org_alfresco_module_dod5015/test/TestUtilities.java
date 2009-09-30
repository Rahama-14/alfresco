/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.module.org_alfresco_module_dod5015.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementSearchBehaviour;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.script.BootstrapTestDataGet;
import org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.view.ImporterBinding;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.springframework.context.ApplicationContext;

/**
 * This class is an initial placeholder for miscellaneous helper methods used in
 * the testing or test initialisation of the DOD5015 module.
 * 
 * @author neilm
 */
public class TestUtilities implements DOD5015Model
{
    protected static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    public static NodeRef loadFilePlanData(ApplicationContext applicationContext)
    {
        return TestUtilities.loadFilePlanData(applicationContext, true, false);
    }
    
    public static NodeRef loadFilePlanData(ApplicationContext applicationContext, boolean patchData, boolean alwaysLoad)
    {
        NodeService nodeService = (NodeService)applicationContext.getBean("NodeService"); 
        AuthorityService authorityService = (AuthorityService)applicationContext.getBean("AuthorityService");
        PermissionService permissionService = (PermissionService)applicationContext.getBean("PermissionService");       
        SearchService searchService = (SearchService)applicationContext.getBean("SearchService"); 
        ImporterService importerService = (ImporterService)applicationContext.getBean("importerComponent");
        RecordsManagementService recordsManagementService = (RecordsManagementService)applicationContext.getBean("RecordsManagementService");
        RecordsManagementActionService recordsManagementActionService = (RecordsManagementActionService)applicationContext.getBean("RecordsManagementActionService");
        RecordsManagementSecurityService recordsManagementSecurityService = (RecordsManagementSecurityService)applicationContext.getBean("RecordsManagementSecurityService");
        RecordsManagementSearchBehaviour recordsManagementSearchBehaviour = (RecordsManagementSearchBehaviour)applicationContext.getBean("recordsManagementSearchBehaviour");
        
        NodeRef filePlan = null;
        NodeRef rootNode = nodeService.getRootNode(SPACES_STORE);
        
        if (alwaysLoad == false)
        {
            // Try and find a file plan hanging from the root node
            List<ChildAssociationRef> assocs = nodeService.getChildAssocs(rootNode, ContentModel.ASSOC_CHILDREN, TYPE_FILE_PLAN);
            if (assocs.size() != 0)
            {
                filePlan = assocs.get(0).getChildRef();
                return filePlan;
            }                 
        }
        
        // For now creating the filePlan beneath the
        filePlan = nodeService.createNode(rootNode, ContentModel.ASSOC_CHILDREN,
                TYPE_FILE_PLAN,
                TYPE_FILE_PLAN).getChildRef();        

        // Do the data load into the the provided filePlan node reference
        // TODO ...
        InputStream is = TestUtilities.class.getClassLoader().getResourceAsStream(
                "alfresco/module/org_alfresco_module_dod5015/bootstrap/DODExampleFilePlan.xml");
        //"alfresco/module/org_alfresco_module_dod5015/bootstrap/temp.xml");
        Assert.assertNotNull("The DODExampleFilePlan.xml import file could not be found", is);
        Reader viewReader = new InputStreamReader(is);
        Location location = new Location(filePlan);
        importerService.importView(viewReader, location, REPLACE_BINDING, null);
          
        if (patchData == true)
        {
            // Tempory call out to patch data after AMP
            BootstrapTestDataGet.patchLoadedData(searchService, nodeService, recordsManagementService, 
                    recordsManagementActionService, permissionService, 
                    authorityService, recordsManagementSecurityService,
                    recordsManagementSearchBehaviour);
        }

        return filePlan;
    }
    
    public static NodeRef getRecordSeries(SearchService searchService, String seriesName)
    {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(SPACES_STORE);
        
        String query = "PATH:\"dod:filePlan/cm:" + ISO9075.encode(seriesName) + "\"";

        searchParameters.setQuery(query);
        searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
        ResultSet rs = searchService.query(searchParameters);
        
        //setComplete();
        //endTransaction();
        return rs.getNodeRefs().isEmpty() ? null : rs.getNodeRef(0);
    }
    
    public static NodeRef getRecordCategory(SearchService searchService, String seriesName, String categoryName)
    {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(SPACES_STORE);
        
        String query = "PATH:\"dod:filePlan/cm:" + ISO9075.encode(seriesName) + "/cm:" + ISO9075.encode(categoryName) + "\"";

        searchParameters.setQuery(query);
        searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
        ResultSet rs = searchService.query(searchParameters);
        
        //setComplete();
        //endTransaction();
        return rs.getNodeRefs().isEmpty() ? null : rs.getNodeRef(0);
    }
    
    public static NodeRef getRecordFolder(SearchService searchService, String seriesName, String categoryName, String folderName)
    {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.addStore(SPACES_STORE);
        String query = "PATH:\"dod:filePlan/cm:" + ISO9075.encode(seriesName)
            + "/cm:" + ISO9075.encode(categoryName)
            + "/cm:" + ISO9075.encode(folderName) + "\"";
        System.out.println("Query: " + query);
        searchParameters.setQuery(query);
        searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
        ResultSet rs = searchService.query(searchParameters);
        
        //setComplete();
        //endTransaction();
        
        return rs.getNodeRefs().isEmpty() ? null : rs.getNodeRef(0);
    }

    
    // TODO .. do we need to redeclare this here ??
    private static ImporterBinding REPLACE_BINDING = new ImporterBinding()
    {

        public UUID_BINDING getUUIDBinding()
        {
            return UUID_BINDING.REPLACE_EXISTING;
        }

        public String getValue(String key)
        {
            return null;
        }

        public boolean allowReferenceWithinTransaction()
        {
            return false;
        }

        public QName[] getExcludedClasses()
        {
            return null;
        }

    };

    public static void declareRecord(NodeRef recordToDeclare, NodeService nodeService,
            RecordsManagementActionService rmActionService)
    {
        // Declare record
        Map<QName, Serializable> propValues = nodeService.getProperties(recordToDeclare);        
        propValues.put(RecordsManagementModel.PROP_PUBLICATION_DATE, new Date());       
        List<String> smList = new ArrayList<String>(2);
//        smList.add(DOD5015Test.FOUO);
//        smList.add(DOD5015Test.NOFORN);
        propValues.put(RecordsManagementModel.PROP_SUPPLEMENTAL_MARKING_LIST, (Serializable)smList);        
        propValues.put(RecordsManagementModel.PROP_MEDIA_TYPE, "mediaTypeValue"); 
        propValues.put(RecordsManagementModel.PROP_FORMAT, "formatValue"); 
        propValues.put(RecordsManagementModel.PROP_DATE_RECEIVED, new Date());       
        propValues.put(RecordsManagementModel.PROP_ORIGINATOR, "origValue");
        propValues.put(RecordsManagementModel.PROP_ORIGINATING_ORGANIZATION, "origOrgValue");
        propValues.put(ContentModel.PROP_TITLE, "titleValue");
        nodeService.setProperties(recordToDeclare, propValues);
        rmActionService.executeRecordsManagementAction(recordToDeclare, "declareRecord");        
    }
}
