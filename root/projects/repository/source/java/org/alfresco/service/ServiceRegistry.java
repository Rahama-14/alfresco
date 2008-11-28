/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.service;

import java.util.Collection;

import org.alfresco.mbeans.VirtServerRegistry;
import org.alfresco.repo.forms.FormService;
import org.alfresco.repo.site.SiteService;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.deploy.DeploymentService;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.ml.ContentFilterLanguagesService;
import org.alfresco.service.cmr.ml.EditionService;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.CrossRepositoryCopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.wcm.sandbox.SandboxService;
import org.alfresco.wcm.webproject.WebProjectService;


/**
 * This interface represents the registry of public Repository Services.
 * The registry provides meta-data about each service and provides
 * access to the service interface.
 *
 * @author David Caruana
 */
@PublicService
public interface ServiceRegistry
{
    // Service Bean Names

    static final String SERVICE_REGISTRY = "ServiceRegistry";

    static final QName REGISTRY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ServiceRegistry");
    static final QName DESCRIPTOR_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "DescriptorService");
    static final QName TRANSACTION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "TransactionService");
    static final QName RETRYING_TRANSACTION_HELPER = QName.createQName(NamespaceService.ALFRESCO_URI, "retryingTransactionHelper");
    static final QName AUTHENTICATION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AuthenticationService");
    static final QName NAMESPACE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "NamespaceService");
    static final QName DICTIONARY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "DictionaryService");
    static final QName NODE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "NodeService");
    static final QName CONTENT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ContentService");
    static final QName MIMETYPE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "MimetypeService");
    static final QName CONTENT_FILTER_LANGUAGES_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ContentFilterLanguagesService");
    static final QName MULTILINGUAL_CONTENT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "MultilingualContentService");
    static final QName EDITION_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "EditionService");
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
    static final QName TEMPLATE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "TemplateService");
    static final QName FILE_FOLDER_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "FileFolderService");
    static final QName SCRIPT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ScriptService");
    static final QName WORKFLOW_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "WorkflowService");
    static final QName AUDIT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AuditService");
    static final QName OWNABLE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "OwnableService");
    static final QName PERSON_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "PersonService");
    static final QName SITE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "SiteService");
    static final QName AVM_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AVMService");
    static final QName AVM_LOCKING_AWARE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AVMLockingAwareService");
    static final QName AVM_SYNC_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AVMSyncService");
    static final QName CROSS_REPO_COPY_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "CrossRepositoryCopyService");
    static final QName ATTRIBUTE_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AttributeService");
    static final QName AVM_LOCKING_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "AVMLockingService");
    static final QName VIRT_SERVER_REGISTRY = QName.createQName(NamespaceService.ALFRESCO_URI, "VirtServerRegistry");
    static final QName THUMBNAIL_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "ThumbnailService");
    static final QName TAGGING_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "TaggingService");
    static final QName DEPLOYMENT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "DeploymentService");
    static final QName WEBPROJECT_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "WebProjectService");
    static final QName SANDBOX_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "SandboxService");
    static final QName FORM_SERVICE = QName.createQName(NamespaceService.ALFRESCO_URI, "FormService");

    /**
     * Get the list of services provided by the Repository
     *
     * @return  list of provided Services
     */
    @NotAuditable
    Collection<QName> getServices();

    /**
     * Is the specified service provided by the Repository?
     *
     * @param service  name of service to test provision of
     * @return true => provided, false => not provided
     */
    @NotAuditable
    boolean isServiceProvided(QName service);

    /**
     * Get the specified service.
     *
     * @param service  name of service to retrieve
     * @return the service interface (must cast to interface as described in service meta-data)
     */
    @NotAuditable
    Object getService(QName service);

    /**
     * @return the descriptor service
     */
    @NotAuditable
    DescriptorService getDescriptorService();

    /**
     * @return the transaction service
     */
    @NotAuditable
    TransactionService getTransactionService();

    /**
     * @return the transaction service
     */
    @NotAuditable
    RetryingTransactionHelper getRetryingTransactionHelper();

    /**
     * @return the namespace service (or null, if one is not provided)
     */
    @NotAuditable
    NamespaceService getNamespaceService();

    /**
     * @return the authentication service (or null, if one is not provided)
     */
    @NotAuditable
    AuthenticationService getAuthenticationService();

    /**
     * @return the node service (or null, if one is not provided)
     */
    @NotAuditable
    NodeService getNodeService();

    /**
     * @return the content service (or null, if one is not provided)
     */
    @NotAuditable
    ContentService getContentService();

    /**
     * @return the mimetype service (or null, if one is not provided)
     */
    @NotAuditable
    MimetypeService getMimetypeService();

    /**
     * @return the content filter languages service (or null, if one is not provided)
     */
    @NotAuditable
    ContentFilterLanguagesService getContentFilterLanguagesService();

    /**
     * @return the search service (or null, if one is not provided)
     */
    @NotAuditable
    SearchService getSearchService();

    /**
     * @return the version service (or null, if one is not provided)
     */
    @NotAuditable
    VersionService getVersionService();

    /**
     * @return the lock service (or null, if one is not provided)
     */
    @NotAuditable
    LockService getLockService();

    /**
     * @return the dictionary service (or null, if one is not provided)
     */
    @NotAuditable
    DictionaryService getDictionaryService();

    /**
     * @return the copy service (or null, if one is not provided)
     */
    @NotAuditable
    CopyService getCopyService();

    /**
     * @return the checkout / checkin service (or null, if one is not provided)
     */
    @NotAuditable
    CheckOutCheckInService getCheckOutCheckInService();

    /**
     * @return the category service (or null, if one is not provided)
     */
    @NotAuditable
    CategoryService getCategoryService();

    /**
     * @return the importer service or null if not present
     */
    @NotAuditable
    ImporterService getImporterService();

    /**
     * @return the exporter service or null if not present
     */
    @NotAuditable
    ExporterService getExporterService();

    /**
     * @return the rule service (or null, if one is not provided)
     */
    @NotAuditable
    RuleService getRuleService();

    /**
     * @return the action service (or null if one is not provided)
     */
    @NotAuditable
    ActionService getActionService();

    /**
     * @return the permission service (or null if one is not provided)
     */
    @NotAuditable
    PermissionService getPermissionService();

    /**
     * @return the authority service (or null if one is not provided)
     */
    @NotAuditable
    AuthorityService getAuthorityService();

    /**
     * @return the template service (or null if one is not provided)
     */
    @NotAuditable
    TemplateService getTemplateService();

    /**
     * @return the file-folder manipulation service (or null if one is not provided)
     */
    @NotAuditable
    FileFolderService getFileFolderService();

    /**
     * @return the script execution service (or null if one is not provided)
     */
    @NotAuditable
    ScriptService getScriptService();

    /**
     * @return the workflow service (or null if one is not provided)
     */
    @NotAuditable
    WorkflowService getWorkflowService();

    /**
     * @return the audit service (or null if one is not provided)
     */
    @NotAuditable
    AuditService getAuditService();

    /**
     * Get the AVMService.
     * @return The AVM service (or null if one is not provided);
     */
    @NotAuditable
    AVMService getAVMService();

    /**
    * Get the AVMLockingAwareService.
     * @return The AVM locking aware service (or null if one is not provided);    
     */
    @NotAuditable
    AVMService getAVMLockingAwareService();
    
    /**
     * Get the AVM Sync Service.
     * @return The AVM Sync Service.
     */
    @NotAuditable
    AVMSyncService getAVMSyncService();

    /**
     * Get the ownable service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    OwnableService getOwnableService();

    /**
     * Get the person service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    PersonService getPersonService();

    /**
     * Get the site service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    SiteService getSiteService();

    /**
     * Get the cross repository copy service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    CrossRepositoryCopyService getCrossRepositoryCopyService();

    /**
     * Get the attribute service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    AttributeService getAttributeService();

    /**
     * Get the AVM locking service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    AVMLockingService getAVMLockingService();

    /**
     * Get the Virtualisation Server registry service bean
     * @return
     */
    @NotAuditable
    VirtServerRegistry getVirtServerRegistry();

    /**
     * Get the Multilingual Content Service
     * @return
     */
    @NotAuditable
    MultilingualContentService getMultilingualContentService();

    /**
     * Get the Edition Service
     * @return
     */
    @NotAuditable
    EditionService getEditionService();
    
    /**
     * Get the Thumbnail Service
     * @return
     */
    @NotAuditable
    ThumbnailService getThumbnailService();
    
    /**
     * Get the Tagging Service
     * @return
     */
    @NotAuditable
    TaggingService getTaggingService();
    
    /**
     * Get the Deployment Service
     * @return the deployment service (or null, if one is not provided)
     */
    @NotAuditable
    DeploymentService getDeploymentService();
    
    /**
     * Get the WebProject Service
     * @return
     */
    @NotAuditable
    WebProjectService getWebProjectService();
    
    /**
     * Get the Sandbox Service
     * @return
     */
    @NotAuditable
    SandboxService getSandboxService();
    
    /**
     * Get the form service (or null if one is not provided)
     * @return
     */
    @NotAuditable
    FormService getFormService();
}
