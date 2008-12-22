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
package org.alfresco.module.vti.web.ws;

import org.alfresco.module.vti.handler.VersionsServiceHandler;
import org.alfresco.module.vti.metadata.model.DocumentVersionBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;

/**
 * Class for handling DeleteAllVersions method from versions web service
 *
 * @author PavelYur
 */
public class DeleteAllVersionsEndpoint extends AbstractEndpoint
{

    // handler that provides methods for operating with documents and folders
    private VersionsServiceHandler handler;

    // xml namespace prefix
    private static String prefix = "versions";

    private static Log logger = LogFactory.getLog(DeleteAllVersionsEndpoint.class);

    public DeleteAllVersionsEndpoint(VersionsServiceHandler handler)
    {
        this.handler = handler;
    }

    /**
     * Deletes all versions of the document from site
     * 
     * @param soapRequest Vti soap request ({@link VtiSoapRequest})
     * @param soapResponse Vti soap response ({@link VtiSoapResponse}) 
     */
    public void execute(VtiSoapRequest soapRequest, VtiSoapResponse soapResponse) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Soap Method with name " + getName() + " is started.");
        // mapping xml namespace to prefix
        SimpleNamespaceContext nc = new SimpleNamespaceContext();
        nc.addNamespace(prefix, namespace);
        nc.addNamespace(soapUriPrefix, soapUri);
        
        if (logger.isDebugEnabled())
            logger.debug("Getting request params.");
        String host = getHost(soapRequest);
        String context = soapRequest.getAlfrescoContextName();
        String dws = getDwsFromUri(soapRequest);        
        
        // getting fileName parameter from request
        XPath fileNamePath = new Dom4jXPath(buildXPath(prefix, "/DeleteAllVersions/fileName"));
        fileNamePath.setNamespaceContext(nc);
        Element fileName = (Element) fileNamePath.selectSingleNode(soapRequest.getDocument().getRootElement());

        if (logger.isDebugEnabled())
            logger.debug("Deleting all versions for " + dws + "/" + fileName.getText() + ".");
        // deleting all versions of given file
        DocumentVersionBean current = handler.deleteAllVersions(dws + "/" + fileName.getText());
        
        // creating soap response
        Element root = soapResponse.getDocument().addElement("DeleteAllVersionsResponse", namespace);
        Element deleteAllVersionsResult = root.addElement("DeleteAllVersionsResult");

        Element results = deleteAllVersionsResult.addElement("results", namespace);

        results.addElement("list").addAttribute("id", "");
        results.addElement("versioning").addAttribute("enabled", "1");
        results.addElement("settings").addAttribute("url", host + context + dws + "/documentDetails.vti?doc=" + dws + "/" + fileName.getText());

        Element result = results.addElement("result");
        result.addAttribute("version", "@" + current.getVersion());
        String url = host + context + dws + "/" + fileName.getTextTrim();
        result.addAttribute("url", url);
        result.addAttribute("created", current.getCreatedTime());
        result.addAttribute("createdBy", current.getCreatedBy());
        result.addAttribute("size", String.valueOf(current.getSize()));
        result.addAttribute("comments", current.getComments());
        
        if (logger.isDebugEnabled())
            logger.debug("Soap Method with name " + getName() + " is finished.");   
        
    }

}
