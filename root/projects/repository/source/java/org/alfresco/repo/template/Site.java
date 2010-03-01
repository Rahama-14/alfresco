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
package org.alfresco.repo.template;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * Site support in FreeMarker templates.
 * 
 * @author Mike Hatfield
 */
public class Site extends BaseTemplateProcessorExtension
{
    /** Repository Service Registry */
    private ServiceRegistry services;
    private SiteService siteService;
    
    
    /**
     * Set the service registry
     * 
     * @param serviceRegistry	the service registry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
    	this.services = serviceRegistry;
    }

    /**
     * Set the site service
     * 
     * @param siteService The siteService to set.
     */
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }
    
    /**
     * Gets the SiteInfo given the shortName
     * 
     * @param shortName  the shortName of the Site to get
     * @return the Site or null if no such site exists 
     */
    public SiteInfo getSiteInfo(String shortName)
    {
        ParameterCheck.mandatoryString("shortName", shortName);
        return siteService.getSite(shortName);
    }
}
