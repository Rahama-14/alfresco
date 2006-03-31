/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.benchmark.dataloader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.benchmark.dataprovider.DataProviderComponent;
import org.alfresco.benchmark.dataprovider.RepositoryProfile;
import org.alfresco.benchmark.util.AlfrescoUtils;
import org.alfresco.benchmark.util.RandUtils;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;

/**
 * @author Roy Wetherall
 */
public class AlfrescoDataLoaderComponentImpl implements DataLoaderComponent
{
    /** The spaces store reference */
    private StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    /** The node service */
    private NodeService nodeService;
    
    /** The search service */
    private SearchService searchService;
    
    /** The authentication service */
    private AuthenticationComponent authenticationComponent;
    
    /** The data provider componenent */
    private DataProviderComponent dataProviderComponent;
    
    /** The content service */
    private ContentService contentService;
    
    /** The transaction service */
    private TransactionService transactionService;
    
    /** The authentication service */
    private AuthenticationService authenticationService;
    
    /** The person service */
    private PersonService personService;
    
    private AuthorityService authorityService;
    
    /**
     * Set the node service 
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the search service
     * 
     * @param searchService     the serarch service
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    /**
     * Set the authentication service
     * 
     * @param authenticationComponent   the suthentication component
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }
    
    /**
     * Set the data provider component
     * 
     * @param dataProviderComponent     the data provider component
     */
    public void setDataProviderComponent(DataProviderComponent dataProviderComponent)
    {
        this.dataProviderComponent = dataProviderComponent;
    }
    
    /**
     * Set the content service
     * 
     * @param contentService    the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    /**
     * Set the transaction service
     * 
     * @param transactionService    the transaction service
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }
    
    /**
     * Set the authentication service
     * 
     * @param authenticationService     the authentication service
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }
    
    /**
     * Set the person service
     * 
     * @param personService     the person service
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    /**
     * Set the authority service
     * 
     * @param authorityService      the authority service
     */
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }
    
    /**
     * @see org.alfresco.benchmark.dataloader.DataLoaderComponent#loadData(org.alfresco.benchmark.dataprovider.RepositoryProfile)
     */
    public LoadedData loadData(RepositoryProfile repositoryProfile)
    {   
        // Set the authentication
        this.authenticationComponent.setSystemUserAsCurrentUser();
        
        // Get the company home node
        ResultSet rs = this.searchService.query(this.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home");
        final NodeRef companyHomeNodeRef = rs.getNodeRef(0);
        
        // Create a folder in company home within which we will create all the test data
        final Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>();
        folderProps.put(ContentModel.PROP_NAME, "Test Data " + System.currentTimeMillis());
        
        NodeRef dataFolderNodeRef = TransactionUtil.executeInUserTransaction(this.transactionService, new TransactionUtil.TransactionWork<NodeRef> ()
        {
            public NodeRef doWork() throws Exception
            {
                return AlfrescoDataLoaderComponentImpl.this.nodeService.createNode(
                        companyHomeNodeRef, 
                        ContentModel.ASSOC_CONTAINS, 
                        QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "test_data_" + System.currentTimeMillis()),
                        ContentModel.TYPE_FOLDER,
                        folderProps).getChildRef();
            }           
        });
        
        LoadedData loadedData = new LoadedData(dataFolderNodeRef);
        
        List<NodeRef> folders = new ArrayList<NodeRef>(1);
        folders.add(dataFolderNodeRef);
        populateFolders(loadedData, repositoryProfile, folders, 0);
        
        return loadedData;
    }
    
    /**
     * Populates the folders with the content and sub folders.
     * 
     * @param loadedData        details of the loaded data 
     * @param repositoryProfile the repository profile
     * @param folderNodeRefs    the folder nore references
     * @param depth             the current depth
     */
    private void populateFolders(final LoadedData loadedData, final RepositoryProfile repositoryProfile, final List<NodeRef> folderNodeRefs, int depth)
    {
        System.out.println("depth=" + depth + "; list_size=" + folderNodeRefs.size());
        
        // Increment the depth
        final int newDepth = depth + 1;
        final List<NodeRef> subFolders = new ArrayList<NodeRef>(10);
        
        TransactionUtil.executeInUserTransaction(this.transactionService, new TransactionUtil.TransactionWork<Object> ()
        {
            public Object doWork() throws Exception
            {
                for (NodeRef folderNodeRef : folderNodeRefs)
                {
                    // Now start adding data to the test data folder
                    int numberOfContentNodes = RandUtils.nextGaussianInteger(
                                                                repositoryProfile.getDocumentsInFolderCountAverage(), 
                                                                repositoryProfile.getDocumentsInFolderCountVariation());
                    int numberOfSubFolderNodes = RandUtils.nextGaussianInteger(
                                                                repositoryProfile.getSubFoldersCountAverage(),
                                                                repositoryProfile.getSubFoldersCountVariation());
                    int folderDepth = RandUtils.nextGaussianInteger(
                                                                repositoryProfile.getFolderDepthAverage(),
                                                                repositoryProfile.getFolderDepthVariation());
                    
                    // Create content
                    for (int i = 0; i < numberOfContentNodes; i++)
                    {
                        AlfrescoUtils.createContentNode(
                                AlfrescoDataLoaderComponentImpl.this.dataProviderComponent, 
                                AlfrescoDataLoaderComponentImpl.this.nodeService, 
                                AlfrescoDataLoaderComponentImpl.this.contentService, 
                                repositoryProfile, 
                                folderNodeRef);
                    }
                    loadedData.incrementContentCount(numberOfContentNodes);
                    
                    // Create folders
                    for (int i = 0; i < numberOfSubFolderNodes; i++)
                    {
                        if (newDepth <= folderDepth)
                        {
                            NodeRef subFolderNodeRef = AlfrescoUtils.createFolderNode(
                                    AlfrescoDataLoaderComponentImpl.this.dataProviderComponent, 
                                    AlfrescoDataLoaderComponentImpl.this.nodeService, 
                                    repositoryProfile, 
                                    folderNodeRef);
                            subFolders.add(subFolderNodeRef);
                            loadedData.incrementFolderCount(1);
                        }
                    }                                             
                }
                
                return null;
            }                   
        });    
        
        if (subFolders.size() > 0)
        {
            // Populate the sub folders
            populateFolders(loadedData, repositoryProfile, subFolders, newDepth);
        }
    }

    /**
     * @see org.alfresco.benchmark.dataloader.DataLoaderComponent#createUsers(int)
     */
    public List<String> createUsers(final int count)
    {
        return TransactionUtil.executeInUserTransaction(this.transactionService, new TransactionUtil.TransactionWork<List<String>> ()
        {
            public List<String> doWork() throws Exception
            {
                List<String> users = new ArrayList<String>(count);
                
                for (int i = 0; i < count; i++)
                {
                    // Create the users home folder
                    NodeRef companyHome = AlfrescoUtils.getCompanyHomeNodeRef(
                                                        AlfrescoDataLoaderComponentImpl.this.searchService,
                                                        AlfrescoDataLoaderComponentImpl.this.storeRef);
                    NodeRef homeFolder = AlfrescoUtils.createFolderNode(
                                                        AlfrescoDataLoaderComponentImpl.this.dataProviderComponent,
                                                        AlfrescoDataLoaderComponentImpl.this.nodeService,
                                                        new RepositoryProfile(),
                                                        companyHome,
                                                        "userHome_" + GUID.generate());
                    
                    // Create the authentication
                    String userName = "bm_" + Long.toString(System.currentTimeMillis());
                    String password = "password";
                    AlfrescoDataLoaderComponentImpl.this.authenticationService.createAuthentication(userName, password.toCharArray());
                    
                    // Create the person
                    Map<QName, Serializable> personProperties = new HashMap<QName, Serializable>();
                    personProperties.put(ContentModel.PROP_USERNAME, userName);
                    personProperties.put(ContentModel.PROP_HOMEFOLDER, homeFolder);
                    personProperties.put(ContentModel.PROP_FIRSTNAME, "benchmark");
                    personProperties.put(ContentModel.PROP_LASTNAME, "user");
                    personService.createPerson(personProperties);
                    
                    // TODO figure out why this doesn't work for now ...
                    // Set the permissions
                    //AlfrescoDataLoaderComponentImpl.this.authorityService.addAuthority(PermissionService.ADMINISTRATOR_AUTHORITY, userName);
                    
                    // Add the new user to the list
                    users.add(userName);
                }
                
                return users;
            }
        });        
    }
}
