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
package org.alfresco.repo.action.executer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionServiceException;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;

/**
 * Exporter action executor
 * 
 * @author gavinc
 */
public class ExporterActionExecuter extends ActionExecuterAbstractBase
{
    public static final String NAME = "export";
    public static final String PARAM_STORE = "store";
    public static final String PARAM_PACKAGE_NAME = "package-name";
    public static final String PARAM_DESTINATION_FOLDER = "destination";
    public static final String PARAM_INCLUDE_CHILDREN = "include-children";
    public static final String PARAM_INCLUDE_SELF = "include-self";
    public static final String PARAM_ENCODING = "encoding";

    private static final String TEMP_FILE_PREFIX = "alf";
    
    /**
     * The exporter service
     */
    private ExporterService exporterService;
    
    /**
     * The node service
     */
    private NodeService nodeService;
    
    /**
     * The content service
     */
    private ContentService contentService;
	
    /**
     * Sets the ExporterService to use
     * 
     * @param exporterService The ExporterService
     */
	public void setExporterService(ExporterService exporterService) 
	{
		this.exporterService = exporterService;
	}
    
    /**
     * Sets the NodeService to use
     * 
     * @param nodeService The NodeService
     */
    public void setNodeService(NodeService nodeService)
    {
       this.nodeService = nodeService;
    }
    
    /**
     * Sets the ContentService to use
     * 
     * @param contentService The ContentService
     */
    public void setContentService(ContentService contentService)
    {
       this.contentService = contentService;
    }

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuter#execute(org.alfresco.repo.ref.NodeRef, org.alfresco.repo.ref.NodeRef)
     */
    public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef)
    {
        File zipFile = null;
        try
        {
            String packageName = (String)ruleAction.getParameterValue(PARAM_PACKAGE_NAME);
            File dataFile = new File(packageName);
            File contentDir = new File(packageName);
           
            // create a temporary file to hold the zip
            zipFile = TempFileProvider.createTempFile(TEMP_FILE_PREFIX, ACPExportPackageHandler.ACP_EXTENSION);
            ACPExportPackageHandler zipHandler = new ACPExportPackageHandler(new FileOutputStream(zipFile), 
                 dataFile, contentDir);
           
            ExporterCrawlerParameters params = new ExporterCrawlerParameters();
            boolean includeChildren = true;
            Boolean withKids = (Boolean)ruleAction.getParameterValue(PARAM_INCLUDE_CHILDREN);
            if (withKids != null)
            {
                includeChildren = withKids.booleanValue();
            }
            params.setCrawlChildNodes(includeChildren);
           
            boolean includeSelf = false;
            Boolean andMe = (Boolean)ruleAction.getParameterValue(PARAM_INCLUDE_SELF);
            if (andMe != null)
            {
                includeSelf = andMe.booleanValue();
            }
            params.setCrawlSelf(includeSelf);
   
            params.setExportFrom(new Location(actionedUponNodeRef));
           
            // perform the actual export
            this.exporterService.exportView(zipHandler, params, null);
           
            // now the export is done we need to create a node in the repository
            // to hold the exported package
            NodeRef zip = createExportZip(ruleAction, actionedUponNodeRef);
            ContentWriter writer = this.contentService.getWriter(zip, ContentModel.PROP_CONTENT, true);
            writer.setEncoding((String)ruleAction.getParameterValue(PARAM_ENCODING));
            writer.setMimetype(MimetypeMap.MIMETYPE_ACP);
            writer.putContent(zipFile);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ActionServiceException("export.package.error", fnfe);
        }
        finally
        {
           // try and delete the temporary file
           if (zipFile != null)
           {
              zipFile.delete();
           }
        }
    }

	/**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefintions(java.util.List)
	 */
	protected void addParameterDefintions(List<ParameterDefinition> paramList) 
	{
        paramList.add(new ParameterDefinitionImpl(PARAM_PACKAGE_NAME, DataTypeDefinition.TEXT, true, 
              getParamDisplayLabel(PARAM_PACKAGE_NAME)));
        paramList.add(new ParameterDefinitionImpl(PARAM_ENCODING, DataTypeDefinition.TEXT, true, 
              getParamDisplayLabel(PARAM_ENCODING)));
		paramList.add(new ParameterDefinitionImpl(PARAM_STORE, DataTypeDefinition.TEXT, true, 
              getParamDisplayLabel(PARAM_STORE)));
        paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, 
              getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
        paramList.add(new ParameterDefinitionImpl(PARAM_INCLUDE_CHILDREN, DataTypeDefinition.BOOLEAN, false, 
              getParamDisplayLabel(PARAM_INCLUDE_CHILDREN)));
        paramList.add(new ParameterDefinitionImpl(PARAM_INCLUDE_SELF, DataTypeDefinition.BOOLEAN, false, 
              getParamDisplayLabel(PARAM_INCLUDE_SELF)));
	}

    /**
     * Creates the ZIP file node in the repository for the export
     * 
     * @param ruleAction The rule being executed
     * @return The NodeRef of the newly created ZIP file
     */
    private NodeRef createExportZip(Action ruleAction, NodeRef actionedUponNodeRef)
    {
        // create a node in the repository to represent the export package
        NodeRef exportDest = (NodeRef)ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER);
        String packageName = (String)ruleAction.getParameterValue(PARAM_PACKAGE_NAME);

        // add the default Alfresco content package extension if an extension hasn't been given
        if (packageName.indexOf(".") == -1)
        {
            packageName = packageName + "." + ACPExportPackageHandler.ACP_EXTENSION;
        }
        
        // set the name for the new node
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>(1);
        contentProps.put(ContentModel.PROP_NAME, packageName);
            
        // create the node to represent the zip file
        String assocName = QName.createValidLocalName(packageName);
        ChildAssociationRef assocRef = this.nodeService.createNode(
              exportDest, ContentModel.ASSOC_CONTAINS,
              QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, assocName),
              ContentModel.TYPE_CONTENT, contentProps);
         
        NodeRef zipNodeRef = assocRef.getChildRef();
        
        // build a description string to be set on the node representing the content package
        String desc = "";
        String spaceName = (String)this.nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
        String pattern = I18NUtil.getMessage("export.package.description");
        if (pattern != null && spaceName != null)
        {
           desc = MessageFormat.format(pattern, spaceName);
        }
        
        // apply the titled aspect to behave in the web client
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(3, 1.0f);
        titledProps.put(ContentModel.PROP_TITLE, packageName);
        titledProps.put(ContentModel.PROP_DESCRIPTION, desc);
        this.nodeService.addAspect(zipNodeRef, ContentModel.ASPECT_TITLED, titledProps);
        
        return zipNodeRef;
    }
}
