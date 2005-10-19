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
package org.alfresco.service;

import java.util.Collection;

import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;


/**
 * This interface represents the registry of public Repository Services.
 * The registry provides meta-data about each service and provides
 * access to the service interface. 
 * 
 * @author David Caruana
 */
public interface ServiceRegistry
{
    // Service Bean Names
    
    static final String SERVICE_REGISTRY = "ServiceRegistry";

    static final QName REGISTRY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ServiceRegistry");
    static final QName DESCRIPTOR_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "DescriptorService");
    static final QName TRANSACTION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "TransactionService");
    static final QName NAMESPACE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "NamespaceService");
    static final QName DICTIONARY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "DictionaryService");
    static final QName NODE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "NodeService");
    static final QName CONTENT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ContentService");
    static final QName MIMETYPE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "MimetypeService");
    static final QName SEARCH_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "SearchService");
    static final QName CATEGORY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "CategoryService");
    static final QName COPY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "CopyService");
    static final QName LOCK_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "LockService");
    static final QName VERSION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "VersionService");
    static final QName COCI_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "CheckoutCheckinService");
    static final QName RULE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "RuleService");
    static final QName IMPORTER_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ImporterService");
    static final QName EXPORTER_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ExporterService");
    static final QName ACTION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ActionService");
    static final QName PERMISSIONS_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "PermissionService");
    static final QName AUTHORITY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AuthorityService");
    
    /**
     * Get the list of services provided by the Repository
     *
     * @return  list of provided Services
     */
    Collection<QName> getServices();

    /**
     * Is the specified service provided by the Repository?
     * 
     * @param service  name of service to test provision of
     * @return true => provided, false => not provided
     */
    boolean isServiceProvided(QName service);

    /**
     * Get meta-data about the specified service
     *
     * @param service  name of service to retrieve meta data for
     * @return the service meta data
     */
    ServiceDescriptor getServiceDescriptor(QName service);

    /** 
     * Get the specified service.
     *
     * @param service  name of service to retrieve
     * @return the service interface (must cast to interface as described in service meta-data)
     */  
    Object getService(QName service);
    
    /**
     * @return the descriptor service
     */
    DescriptorService getDescriptorService();
    
    /**
     * @return the transaction service
     */
    TransactionService getTransactionService();

    /**
     * @return the namespace service (or null, if one is not provided)
     */
    NamespaceService getNamespaceService();
    
    /**
     * @return the node service (or null, if one is not provided)
     */
    NodeService getNodeService();

    /**
     * @return the content service (or null, if one is not provided)
     */
    ContentService getContentService();
    
    /**
     * @return the mimetype service (or null, if one is not provided)
     */
    MimetypeService getMimetypeService();

    /**
     * @return the search service (or null, if one is not provided)
     */
    SearchService getSearchService();
    
    /**
     * @return the version service (or null, if one is not provided)
     */
    VersionService getVersionService();
    
    /**
     * @return the lock service (or null, if one is not provided)
     */
    LockService getLockService();

    /**
     * @return the dictionary service (or null, if one is not provided)
     */
    DictionaryService getDictionaryService();
 
    /**
     * @return the copy service (or null, if one is not provided)
     */
    CopyService getCopyService();
    
    /**
     * @return the checkout / checkin service (or null, if one is not provided)
     */
    CheckOutCheckInService getCheckOutCheckInService();   
    
    /**
     * @return the category service (or null, if one is not provided)
     */
    CategoryService getCategoryService();
    
    /**
     * @return the importer service or null if not present
     */
    ImporterService getImporterService();
    
    /**
     * @return the exporter service or null if not present
     */
    ExporterService getExporterService();
    
    /**
     * @return the rule service (or null, if one is not provided)
     */
    RuleService getRuleService();
    
    /**
     * @return the action service (or null if one is not provided)
     */
    ActionService getActionService();
    
    /**
     * @return the permission service (or null if one is not provided)
     */
    PermissionService getPermissionService();
    
    /**
     * @return the authority service (or null if one is not provided)
     */
    AuthorityService getAuthorityService();
}
