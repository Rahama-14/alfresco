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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.module.org_alfresco_module_dod5015.script;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.TempFileProvider;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptRequest;
import org.alfresco.web.scripts.WebScriptResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns a JSON representation of a transfer report.
 * 
 * @author Gavin Cornwell
 */
public class TransferReportGet extends BaseTransferWebScript
{
    /** Logger */
    private static Log logger = LogFactory.getLog(TransferReportGet.class);
    
    protected static final String REPORT_FILE_PREFIX = "report_";
    protected static final String REPORT_FILE_SUFFIX = ".json";

    protected DictionaryService ddService;
    protected RecordsManagementService rmService;
    
    /**
     * Sets the DictionaryService instance
     * 
     * @param ddService The DictionaryService instance
     */
    public void setDictionaryService(DictionaryService ddService)
    {
        this.ddService = ddService;
    }
    
    /**
     * Sets the RecordsManagementService instance
     * 
     * @param rmService RecordsManagementService instance
     */
    public void setRecordsManagementService(RecordsManagementService rmService)
    {
        this.rmService = rmService;
    }
    
    @Override
    protected File executeTransfer(NodeRef transferNode,
                WebScriptRequest req, WebScriptResponse res, 
                Status status, Cache cache) throws IOException
    {
        // generate the report (will be in JSON format)
        File report = generateTransferReport(transferNode);
        
        // stream the report back to the client
        streamContent(req, res, report, false);
        
        // return the file for deletion
        return report;
    }
    
    /**
     * Generates a File containing the JSON representation of a transfer report.
     * 
     * @param transferNode The transfer node
     * @return File containing JSON representation of a transfer report
     * @throws IOException
     */
    File generateTransferReport(NodeRef transferNode) throws IOException
    {
        File report = TempFileProvider.createTempFile(REPORT_FILE_PREFIX, REPORT_FILE_SUFFIX);
        Writer writer = null;
        try
        {
            // get all 'transferred' nodes
            NodeRef[] itemsToTransfer = getTransferNodes(transferNode);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Generating transfer report for " + itemsToTransfer.length + 
                            " items into file: " + report.getAbsolutePath());
            }
            
            // create the writer
            writer = new FileWriter(report);
            
            // use RMService to get disposition authority
            String dispositionAuthority = null;
            if (itemsToTransfer.length > 0)
            {
                // use the first transfer item to get to disposition schedule
                DispositionSchedule ds = this.rmService.getDispositionSchedule(
                            itemsToTransfer[0]);
                if (ds != null)
                {
                    dispositionAuthority = ds.getDispositionAuthority();
                }
            }
            
            // write the JSON header
            writer.write("{\n\t\"data\":\n\t{");
            writer.write("\n\t\t\"transferDate\": \"");
            writer.write(ISO8601DateFormat.format(
                        (Date)this.nodeService.getProperty(transferNode, ContentModel.PROP_CREATED)));
            writer.write("\",\n\t\t\"transferPerformedBy\": \"");
            writer.write(AuthenticationUtil.getRunAsUser());
            writer.write("\",\n\t\t\"dispositionAuthority\": \"");
            writer.write(dispositionAuthority != null ? dispositionAuthority : "");
            writer.write("\",\n\t\t\"items\":\n\t\t[");
            
            // write out JSON representation of items to transfer
            generateTransferItemsJSON(writer, itemsToTransfer);
            
            // write the JSON footer
            writer.write("\n\t\t]\n\t}\n}");
        }
        finally
        {
            if (writer != null)
            {
                try { writer.close(); } catch (IOException ioe) {}
            }
        }
        
        return report;
    }
    
    /**
     * Generates the JSON to represent the given NodeRefs
     * 
     * @param writer Writer to write to
     * @param itemsToTransfer NodeRefs being transferred
     * @throws IOException
     */
    protected void generateTransferItemsJSON(Writer writer, NodeRef[] itemsToTransfer)
        throws IOException
    {
        boolean first = true;
        for (NodeRef item : itemsToTransfer)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                writer.write(",");
            }
            
            if (ddService.isSubClass(nodeService.getType(item), ContentModel.TYPE_FOLDER))
            {
                generateTransferFolderJSON(writer, item);
            }
            else
            {
                generateTransferRecordJSON(writer, item);
            }
        }
    }
    
    /**
     * Generates the JSON to represent the given folder.
     * 
     * @param writer Writer to write to
     * @param folderNode Folder being transferred
     * @throws IOException
     */
    protected void generateTransferFolderJSON(Writer writer, NodeRef folderNode)
        throws IOException
    {
        // TODO: Add identation
        
        writer.write("\n{\n\"type\":\"folder\",\n");
        writer.write("\"name\":\"");
        writer.write((String)nodeService.getProperty(folderNode, ContentModel.PROP_NAME));
        writer.write("\",\n\"nodeRef\":\"");
        writer.write(folderNode.toString());
        writer.write("\",\n\"children\":\n[");
        
        boolean first = true;
        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(folderNode, 
                    ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
        for (ChildAssociationRef child : assocs)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                writer.write(",");
            }
            
            NodeRef childRef = child.getChildRef();
            if (ddService.isSubClass(nodeService.getType(childRef), ContentModel.TYPE_FOLDER))
            {
                generateTransferFolderJSON(writer, childRef);
            }
            else
            {
                generateTransferRecordJSON(writer, childRef);
            }
        }
        
        writer.write("\n]\n}");
    }
    
    /**
     * Generates the JSON to represent the given record.
     * 
     * @param writer Writer to write to
     * @param recordNode Record being transferred
     * @throws IOException
     */
    protected void generateTransferRecordJSON(Writer writer, NodeRef recordNode)
        throws IOException
    {
        writer.write("\n{\n\"type\":\"record\",\n");
        writer.write("\"name\":\"");
        writer.write((String)nodeService.getProperty(recordNode, ContentModel.PROP_NAME));
        writer.write("\",\n\"nodeRef\":\"");
        writer.write(recordNode.toString());
        writer.write("\"\n}");
    }
}