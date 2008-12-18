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
package org.alfresco.module.vti.endpoints.versions;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.module.vti.endpoints.EndpointUtils;
import org.alfresco.module.vti.endpoints.VtiEndpoint;
import org.alfresco.module.vti.handler.soap.VersionsServiceHandler;
import org.alfresco.module.vti.metadata.soap.versions.DocumentVersionBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;

/**
 * Class for handling RestoreVersion method from versions web service
 *
 * @author PavelYur
 */
public class RestoreVersionEndpoint extends VtiEndpoint
{

    // handler that provides methods for operating with documents and folders
    private VersionsServiceHandler handler;

    // xml namespace prefix
    private static String prefix = "versions";

    private static Log logger = LogFactory.getLog(RestoreVersionEndpoint.class);
    
    public RestoreVersionEndpoint(VersionsServiceHandler handler)
    {
        this.handler = handler;
    }

    /**
     * Handle the RestoreVersion method from versions web service
     *
     * @param element part of soap message from request that contains methods parameters
     * @param document soap response
     */
    @Override
    protected Element invokeInternal(Element element, Document document) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Soap Method with name " + getName() + " is started.");
        // mapping xml namespace to prefix
        SimpleNamespaceContext nc = new SimpleNamespaceContext();
        nc.addNamespace(prefix, namespace);
        
        String host = EndpointUtils.getHost();
        String context = EndpointUtils.getContext();
        String dws = EndpointUtils.getDwsFromUri();        ;

        if (logger.isDebugEnabled())
            logger.debug("Getting fileName parameter from request.");
        XPath fileNameXPath = new Dom4jXPath(EndpointUtils.buildXPath(prefix, "/RestoreVersion/fileName"));
        fileNameXPath.setNamespaceContext(nc);
        Element fileName = (Element) fileNameXPath.selectSingleNode(element);

        if (logger.isDebugEnabled())
            logger.debug("Getting fileVersion parameter from request.");
        XPath fileVersionXPath = new Dom4jXPath(EndpointUtils.buildXPath(prefix, "/RestoreVersion/fileVersion"));
        fileVersionXPath.setNamespaceContext(nc);
        Element fileVersion = (Element) fileVersionXPath.selectSingleNode(element);

        // creating soap response
        Element root = document.addElement("RestoreVersionResponse", namespace);
        Element restoreVersionResult = root.addElement("RestoreVersionResult");

        Element results = restoreVersionResult.addElement("results", namespace);

        results.addElement("list").addAttribute("id", "");
        results.addElement("versioning").addAttribute("enabled", "1");
        results.addElement("settings").addAttribute("url", "http://" + host + context + dws + "/documentDetails.vti?doc=" + dws + "/" + fileName.getText());

        if (logger.isDebugEnabled())
            logger.debug("Restoring version " + fileVersion.getText() + " for file '" + dws + "/" + fileName.getText() + "'" );
        List<DocumentVersionBean> notSortedVersions = handler.restoreVersion(dws + "/" + fileName.getText(), fileVersion.getText());

        List<DocumentVersionBean> versions = new ArrayList<DocumentVersionBean>();
        
        versions.add(notSortedVersions.get(0));
        for (int i = notSortedVersions.size() - 1; i > 0; --i) {
            versions.add(notSortedVersions.get(i));
        }
        
        boolean isCurrent = true;
        for (DocumentVersionBean version : versions)
        {
            Element result = results.addElement("result");
            if (isCurrent)
            {
                // prefix @ means that it is current working version, it couldn't be restored or deleted
                result.addAttribute("version", "@" + version.getVersion());
                String url = "http://" + host + context + dws + "/" + fileName.getTextTrim();
                result.addAttribute("url", url);
                isCurrent = false;
            }
            else
            {
                result.addAttribute("version", version.getVersion());
                String url = "http://" + host + context + version.getUrl();
                result.addAttribute("url", url);
            }
            
            result.addAttribute("created", version.getCreatedTime());
            result.addAttribute("createdBy", version.getCreatedBy());
            result.addAttribute("size", String.valueOf(version.getSize()));
            result.addAttribute("comments", version.getComments());
        }
        
        if (logger.isDebugEnabled()) {
            String versionsStr = "";
            for (DocumentVersionBean version : versions)
            {
                versionsStr += version.getVersion() + " ";
            }
            logger.debug("Now document has the folloving versions [ "+ versionsStr + "]");
        }   
        
        if (logger.isDebugEnabled())
            logger.debug("Soap Method with name " + getName() + " is finished.");
        return root;
    }

}
