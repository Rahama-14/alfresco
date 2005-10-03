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
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.importer.ACPImportPackageHandler;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.util.TempFileProvider;

/**
 * Importer action executor
 * 
 * @author gavinc
 */
public class ImporterActionExecuter extends ActionExecuterAbstractBase
{
    public static final String NAME = "import";
    public static final String PARAM_ENCODING = "encoding";
    public static final String PARAM_DESTINATION_FOLDER = "destination";
    
    private static final String ENCODING = "UTF-8";
    private static final String MIMETYPE = "application/acp";
    private static final String TEMP_FILE_PREFIX = "alf";
    private static final String TEMP_FILE_SUFFIX = ".acp";
    
    /**
     * The importer service
     */
    private ImporterService importerService;
	
    /**
     * The node service
     */
    private NodeService nodeService;
    
    /**
     * The content service
     */
    private ContentService contentService;
    
    /**
     * Sets the ImporterService to use
     * 
     * @param importerService The ImporterService
     */
	public void setImporterService(ImporterService importerService) 
	{
		this.importerService = importerService;
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
        if (this.nodeService.exists(actionedUponNodeRef) == true)
        {
           // The node being passed in should be an Alfresco content package
           ContentReader reader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
           if (reader != null)
           {
               if (MIMETYPE.equals(reader.getMimetype()))
               {
                   File zipFile = null;
                   try
                   {
                       // unfortunately a ZIP file can not be read directly from an input stream so we have to create
                       // a temporary file first
                       zipFile = TempFileProvider.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
                       reader.getContent(zipFile);
                      
                       ACPImportPackageHandler importHandler = new ACPImportPackageHandler(zipFile, ENCODING);
                       NodeRef importDest = (NodeRef)ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER);
                      
                       this.importerService.importView(importHandler, new Location(importDest), null, null);
                   }
                   finally
                   {
                      // now the import is done, delete the temporary file
                      if (zipFile != null)
                      {
                         zipFile.delete();
                      }
                   }
               }
           }
        }
    }

	@Override
	protected void addParameterDefintions(List<ParameterDefinition> paramList) 
	{
        paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, 
              true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
        paramList.add(new ParameterDefinitionImpl(PARAM_ENCODING, DataTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_ENCODING)));
	}

}
