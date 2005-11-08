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
package org.alfresco.model;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;


/**
 * Content Model Constants
 */
public interface ContentModel
{
    //
    // System Model Definitions
    //
    
    // base type constants
    static final QName TYPE_BASE = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "base");
    static final QName ASPECT_REFERENCEABLE = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "referenceable");
    static final QName PROP_STORE_PROTOCOL = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "store-protocol");
    static final QName PROP_STORE_IDENTIFIER = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "store-identifier");
    static final QName PROP_NODE_UUID = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "node-uuid");
    
    // referenceable aspect constants
    static final QName TYPE_REFERENCE = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "reference");
    static final QName PROP_REFERENCE = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "reference");

    // container type constants
    static final QName TYPE_CONTAINER = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "container");
    /** child association type supported by {@link #TYPE_CONTAINER} */
    static final QName ASSOC_CHILDREN =QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "children");

    // roots
    static final QName ASPECT_ROOT = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "aspect_root");
    static final QName TYPE_STOREROOT = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "store_root");

    
    //
    // Content Model Definitions
    //
    
    // content management type constants
    static final QName TYPE_CMOBJECT = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "cmobject");
    static final QName PROP_NAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "name");
    
    // copy aspect constants
    static final QName ASPECT_COPIEDFROM = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "copiedfrom");
    static final QName PROP_COPY_REFERENCE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "source");
	
    // working copy aspect contants
    static final QName ASPECT_WORKING_COPY = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "workingcopy");
    static final QName PROP_WORKING_COPY_OWNER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "workingCopyOwner");
    
    // content type and aspect constants
    static final QName TYPE_CONTENT = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "content");
    static final QName PROP_CONTENT = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "content");
    
    // title aspect
    static final QName ASPECT_TITLED = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "titled");
    static final QName PROP_TITLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "title");
    static final QName PROP_DESCRIPTION = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "description");
    
    // auditable aspect
    static final QName ASPECT_AUDITABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "auditable");
    static final QName PROP_CREATED = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "created");
    static final QName PROP_CREATOR = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "creator");
    static final QName PROP_MODIFIED = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "modified");
    static final QName PROP_MODIFIER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "modifier");
    static final QName PROP_ACCESSED = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "accessed");
    
    // categories
    static final QName TYPE_CATEGORYROOT = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "category_root");  
    static final QName ASPECT_CLASSIFIABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "classifiable");
    //static final QName ASPECT_CATEGORISATION = QName.createQName(NamespaceService.ALFRESCO_URI, "aspect_categorisation");
    static final QName ASPECT_GEN_CLASSIFIABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "generalclassifiable");
    static final QName TYPE_CATEGORY = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "category");
    static final QName PROP_CATEGORIES = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "categories");
    static final QName ASSOC_CATEGORIES = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "categories");
    static final QName ASSOC_SUBCATEGORIES = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "subcategories");

    // lock aspect
    public final static QName ASPECT_LOCKABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "lockable");
    public final static QName PROP_LOCK_OWNER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "lockOwner");
    public final static QName PROP_LOCK_TYPE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "lockType");
    public final static QName PROP_EXPIRY_DATE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "expiryDate");
	
    // version aspect
	static final QName ASPECT_VERSIONABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "versionable");
	static final QName PROP_VERSION_LABEL = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "versionLabel");

    // folders
    static final QName TYPE_SYSTEM_FOLDER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "systemfolder");
    static final QName TYPE_FOLDER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "folder");
    /** child association type supported by {@link #TYPE_FOLDER} */
    static final QName ASSOC_CONTAINS = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "contains");
    
    // person
    static final QName TYPE_PERSON = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "person");
    static final QName PROP_USERNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "userName");
    static final QName PROP_HOMEFOLDER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "homeFolder");
    static final QName PROP_FIRSTNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "firstName");
    static final QName PROP_LASTNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "lastName");
    static final QName PROP_EMAIL = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "email");
    static final QName PROP_ORGID = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "organizationId");

    // Ownable aspect  
    static final QName ASPECT_OWNABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "ownable");
    static final QName PROP_OWNER = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "owner");
    
    // Templatable aspect
    static final QName ASPECT_TEMPLATABLE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "templatable");
    static final QName PROP_TEMPLATE = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "template");
    
    //
    // Application Model Definitions
    //
    
	// workflow
	static final QName ASPECT_SIMPLE_WORKFLOW = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "simpleworkflow");
	static final QName PROP_APPROVE_STEP = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "approveStep");
	static final QName PROP_APPROVE_FOLDER = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "approveFolder");
	static final QName PROP_APPROVE_MOVE = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "approveMove");
	static final QName PROP_REJECT_STEP = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "rejectStep");
	static final QName PROP_REJECT_FOLDER = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "rejectFolder");
	static final QName PROP_REJECT_MOVE = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "rejectMove");
	
    // ui facets aspect
    static final QName ASPECT_UIFACETS = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "uifacets");
    static final QName PROP_ICON = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "icon");

    // inlineeditable aspect
    static final QName ASPECT_INLINEEDITABLE = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "inlineeditable");
    static final QName PROP_EDITINLINE = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "editInline");

    // configurable
    static final QName ASPECT_CONFIGURABLE = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "configurable");
    static final QName TYPE_CONFIGURATIONS = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "configurations");
    static final QName ASSOC_CONFIGURATIONS = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "configurations");
        

    //
    // User Model Definitions
    //
    
    static final String USER_MODEL_URI = "http://www.alfresco.org/model/user/1.0";
    static final String USER_MODEL_PREFIX = "usr";
    
    static final QName TYPE_USER = QName.createQName(USER_MODEL_URI, "user");
    static final QName PROP_USER_USERNAME = QName.createQName(USER_MODEL_URI, "username");
    static final QName PROP_PASSWORD = QName.createQName(USER_MODEL_URI, "password");
    static final QName PROP_ENABLED = QName.createQName(USER_MODEL_URI, "enabled");
    static final QName PROP_ACCOUNT_EXPIRES = QName.createQName(USER_MODEL_URI, "accountExpires");
    static final QName PROP_ACCOUNT_EXPIRY_DATE = QName.createQName(USER_MODEL_URI, "accountExpiryDate");
    static final QName PROP_CREDENTIALS_EXPIRE = QName.createQName(USER_MODEL_URI, "credentialsExpire");
    static final QName PROP_CREDENTIALS_EXPIRY_DATE = QName.createQName(USER_MODEL_URI, "credentialsExpiryDate");
    static final QName PROP_ACCOUNT_LOCKED = QName.createQName(USER_MODEL_URI, "accountLocked");
    static final QName PROP_SALT = QName.createQName(USER_MODEL_URI, "salt");

    static final QName TYPE_AUTHORITY = QName.createQName(USER_MODEL_URI, "authority");
    
    static final QName TYPE_AUTHORITY_CONTAINER = QName.createQName(USER_MODEL_URI, "authorityContainer");
    static final QName PROP_AUTHORITY_NAME = QName.createQName(USER_MODEL_URI, "authorityName");
    static final QName ASSOC_MEMBER = QName.createQName(USER_MODEL_URI, "member");
    static final QName PROP_MEMBERS = QName.createQName(USER_MODEL_URI, "members");
    
    //
    // Version Model Definitions
    //

    static final String VERSION_MODEL_URI = "http://www.alfresco.org/model/version/1.0";
    static final String VERSION_MODEL_PREFIX = "ver";
    
    
    
}
