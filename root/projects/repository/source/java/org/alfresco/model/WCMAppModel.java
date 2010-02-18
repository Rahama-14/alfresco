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
package org.alfresco.model;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * @author Kevin Roast
 */
public interface WCMAppModel
{
    // AVM web folder
    static final QName TYPE_AVMWEBFOLDER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webfolder");
    static final QName PROP_AVMSTORE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "avmstore");
    static final QName PROP_DEFAULTWEBAPP = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "defaultwebapp");
    static final QName PROP_DEPLOYTO = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployto");
    static final QName PROP_SELECTEDDEPLOYTO = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "selecteddeployto");
    static final QName PROP_SELECTEDDEPLOYVERSION = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "selecteddeployversion");
    static final QName PROP_ISSOURCE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "issource");
    static final QName PROP_PREVIEW_PROVIDER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "previewprovidername");
    static final QName ASSOC_WEBUSER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webuser");
    static final QName ASSOC_WEBFORM = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webform");
    static final QName ASSOC_WEBWORKFLOWDEFAULTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webworkflowdefaults");
    static final QName ASSOC_DEPLOYMENTSERVER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentserver");
    static final QName ASSOC_DEPLOYMENTATTEMPT = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentattempt");
    static final QName ASSOC_DEPLOYMENTREPORT = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentreport");
    
    // AVM web user reference
    static final QName TYPE_WEBUSER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webuser");
    static final QName PROP_WEBUSERNAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "username");
    static final QName PROP_WEBUSERROLE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "role");
    
    // AVM web form
    static final QName TYPE_WEBFORM = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webform");
    static final QName PROP_FORMNAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "formname");
    static final QName ASSOC_WEBFORMTEMPLATE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webformtemplate");
    static final QName ASSOC_WORKFLOWDEFAULTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "workflowdefaults");
    
    // AVM web form template
    static final QName TYPE_WEBFORMTEMPLATE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webformtemplate");
    static final QName PROP_BASE_RENDERING_ENGINE_TEMPLATE_NAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "baserenderingenginetemplatename");
    
    // AVM workflow defaults
    static final QName TYPE_WORKFLOW_DEFAULTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "workflowdefaults");
    static final QName PROP_WORKFLOW_NAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "workflowname");
    static final QName PROP_WORKFLOW_DEFAULT_PROPERTIES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "workflowdefaultproperties");
    
    // AVM web workflow defaults
    static final QName TYPE_WEBWORKFLOWDEFAULTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webworkflowdefaults");
    
    // AVM website deployment server
    static final String CONSTRAINT_ALFDEPLOY = "alfresco";
    static final String CONSTRAINT_FILEDEPLOY = "file";
    static final String CONSTRAINT_LIVESERVER = "live";
    static final String CONSTRAINT_TESTSERVER = "test";
    static final QName TYPE_DEPLOYMENTSERVER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentserver");
    static final QName PROP_DEPLOYTYPE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploytype");
    static final QName PROP_DEPLOYSERVERTYPE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservertype");
    static final QName PROP_DEPLOYSERVERHOST = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverhost");
    static final QName PROP_DEPLOYSERVERPORT = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverport");
    static final QName PROP_DEPLOYSERVERNAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservername");
    static final QName PROP_DEPLOYSERVERUSERNAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverusername");
    static final QName PROP_DEPLOYSERVERPASSWORD = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverpassword");
    static final QName PROP_DEPLOYSERVERGROUP = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservergroup");
    static final QName PROP_DEPLOYSERVERADPTERNAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserveradaptername");
    static final QName PROP_DEPLOYSERVERURL = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverurl");
    static final QName PROP_DEPLOYSERVERTARGET = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservertarget");
    static final QName PROP_DEPLOYSOURCEPATH = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploysourcepath");
    static final QName PROP_DEPLOYEXCLUDES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployexcludes");
    static final QName PROP_DEPLOYSERVERALLOCATEDTO = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverallocatedto");
    static final QName PROP_DEPLOYONAPPROVAL = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployonapproval");
    
    // AVM website deployment attempt
    static final QName TYPE_DEPLOYMENTATTEMPT = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentattempt");
    static final QName PROP_DEPLOYATTEMPTID = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattemptid");
    static final QName PROP_DEPLOYATTEMPTTYPE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattempttype");
    static final QName PROP_DEPLOYATTEMPTSTORE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattemptstore");
    static final QName PROP_DEPLOYATTEMPTVERSION = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattemptversion");
    static final QName PROP_DEPLOYATTEMPTSERVERS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattemptservers");
    static final QName PROP_DEPLOYATTEMPTTIME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployattempttime");
    static final QName ASSOC_DEPLOYMENTREPORTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentreports");
    
    // AVM website deployment report
    static final QName TYPE_DEPLOYMENTREPORT = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploymentreport");
    static final QName PROP_DEPLOYSERVER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserver");
    static final QName PROP_DEPLOYVERSION = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployversion");
    static final QName PROP_DEPLOYSUCCESSFUL = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploysuccessful");
    static final QName PROP_DEPLOYFAILEDREASON = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployfailedreason");
    static final QName PROP_DEPLOYSTARTTIME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploystarttime");
    static final QName PROP_DEPLOYENDTIME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployendtime");
    static final QName PROP_DEPLOYSERVERNAMEUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservernameused");
    static final QName PROP_DEPLOYSERVERUSERNAMEUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverusernameused");
    static final QName PROP_DEPLOYSERVERTARGETUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployservertargetused");
    static final QName PROP_DEPLOYSOURCEPATHUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deploysourcepathused");
    static final QName PROP_DEPLOYEXCLUDESUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployexcludesused");
    static final QName PROP_DEPLOYSERVERURLUSED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployserverurlused");
    
    // AVM webapp aspect
    static final QName ASPECT_WEBAPP = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "webapp");

    // AVM filename pattern aspect
    static final QName ASPECT_FILENAMEPATTERN = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "filenamepattern");
    static final QName PROP_FILENAMEPATTERN = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "filenamepattern");

    // AVM output path pattern aspect
    static final QName ASPECT_OUTPUT_PATH_PATTERN = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "outputpathpattern");
    static final QName PROP_OUTPUT_PATH_PATTERN = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "outputpathpattern");
    
    // The XForms data capture form aspect.
    static final QName TYPE_FORMFOLDER = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "formfolder");
    static final QName ASPECT_FORM = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "form");

    /** 
     * there was a mismapping between this definition of PROP_XML_SCHEMA here and in wcmAppModel.xml.
     * leaving in this definition for now as a failover.
     */
    static final QName PROP_XML_SCHEMA_OLD = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "schema");
    static final QName PROP_XML_SCHEMA = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "xmlschema");
    static final QName PROP_XML_SCHEMA_ROOT_ELEMENT_NAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "xmlschemarootelementname");
    static final QName ASSOC_RENDERING_ENGINE_TEMPLATES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "renderingenginetemplates");
    static final QName ASSOC_FORM_WORKFLOW_DEFAULTS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "formworkflowdefaults");
    
    // An XML to something else tranformer aspect.
    static final QName ASPECT_RENDERING_ENGINE_TEMPLATE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "renderingenginetemplate");
    static final QName PROP_PARENT_RENDERING_ENGINE_NAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "parentrenderingenginename");
    static final QName PROP_FORM_SOURCE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "formsource");
    static final QName ASSOC_RENDITION_PROPERTIES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "renditionproperties");
    
    static final QName ASPECT_FORM_INSTANCE_DATA = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "forminstancedata");
    static final QName PROP_PARENT_FORM_NAME = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "parentformname");
    static final QName PROP_RENDITIONS = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "renditions");
    static final QName PROP_ORIGINAL_PARENT_PATH = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "orginalparentpath");
    
    static final QName ASPECT_RENDITION = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "rendition");
    static final QName PROP_PARENT_RENDERING_ENGINE_TEMPLATE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "parentrenderingenginetemplate");
    static final QName PROP_PARENT_RENDITION_PROPERTIES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "parentrenditionproperties");
    static final QName PROP_PRIMARY_FORM_INSTANCE_DATA = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "primaryforminstancedata");
    
    static final QName TYPE_RENDITION_PROPERTIES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "renditionproperties");
    static final QName PROP_MIMETYPE_FOR_RENDITION = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "mimetypeforrendition");
    
    // Aspect to track content that expires
    static final QName ASPECT_EXPIRES = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "expires");
    static final QName PROP_EXPIRATIONDATE = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "expirationDate");

    // Aspect to track dm deployed content
    static final QName ASPECT_DEPLOYED = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "deployed");
    static final QName PROP_GUID = QName.createQName(NamespaceService.WCMAPP_MODEL_1_0_URI, "guid");

}
