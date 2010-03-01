/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.admin.patch.impl;

import java.io.InputStream;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.ConfigurationChecker;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.admin.PatchException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Ensures that the required content snippet is added to the system descriptor
 * to enable robust checking of the content store by the configuration checker.
 * 
 * @author Derek Hulley
 */
public class SystemDescriptorContentPatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.systemDescriptorContent.result";
    private static final String ERR_NO_VERSION_PROPERTIES = "patch.systemDescriptorContent.err.no_version_properties";
    private static final String ERR_NO_SYSTEM_DESCRIPTOR = "patch.systemDescriptorContent.err.no_descriptor";
    
    private ConfigurationChecker configurationChecker;
    private ContentService contentService;
    
    public SystemDescriptorContentPatch()
    {
    }
    
    public void setConfigurationChecker(ConfigurationChecker configurationChecker)
    {
        this.configurationChecker = configurationChecker;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    @Override
    protected void checkProperties()
    {
        super.checkProperties();
        checkPropertyNotNull(configurationChecker, "configurationChecker");
        checkPropertyNotNull(contentService, "contentService");
    }

    @Override
    protected String applyInternal() throws Exception
    {
        InputStream is = null;
        try
        {
            // get the version.properties
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource("classpath:alfresco/version.properties");
            if (!resource.exists())
            {
                throw new PatchException(ERR_NO_VERSION_PROPERTIES);
            }
            is = resource.getInputStream();
            // get the system descriptor
            NodeRef descriptorNodeRef = configurationChecker.getSystemDescriptor();
            if (descriptorNodeRef == null)
            {
                throw new PatchException(ERR_NO_SYSTEM_DESCRIPTOR);
            }
            // get the writer
            ContentWriter writer = contentService.getWriter(descriptorNodeRef, ContentModel.PROP_SYS_VERSION_PROPERTIES, true);
            // upload
            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
            writer.setEncoding("UTF8");
            writer.putContent(is);
            // done
            String msg = I18NUtil.getMessage(MSG_SUCCESS);
            return msg;
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (Throwable e) {}
            }
        }
    }
}




























