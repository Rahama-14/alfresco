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
package org.alfresco.repo.site;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.QName;

/**
 * Site Information Class
 * 
 * @author Roy Wetherall
 */
public class SiteInfoImpl implements SiteInfo
{
    /** Site node reference */    
    private NodeRef nodeRef;
    
    /** Site preset */
    private String sitePreset;
    
    /** Site short name */
    private String shortName;
    
    /** Site title */
    private String title;
    
    /** Site description */
    private String description;
    
    /** Site visibility */
    private SiteVisibility visibility;
    
    /** Set of custom properties that have been defined for site */
    private Map<QName, Serializable> customProperties = new HashMap<QName, Serializable>(1);
   
    /**
     * Constructor
     * 
     * @param sitePreset    site preset
     * @param shortName     short name
     * @param title         title
     * @param description   description
     * @param visibility    site visibility
     * @param nodeRef       site node reference
     */
    /*package*/ SiteInfoImpl(String sitePreset, String shortName, String title, String description, SiteVisibility visibility, Map<QName, Serializable> customProperties, NodeRef nodeRef)
    {
        this(sitePreset, shortName, title, description, visibility, customProperties);
        this.nodeRef = nodeRef;
    }
    
    /**
     * Constructor
     * 
     * @param sitePreset    site preset
     * @param shortName     short name
     * @param title         title
     * @param description   description
     * @param visibility    site visibility
     */
    /*package*/ SiteInfoImpl(String sitePreset, String shortName, String title, String description, SiteVisibility visibility, Map<QName, Serializable> customProperties)
    {
        this.sitePreset = sitePreset;
        this.shortName = shortName;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        if (customProperties != null)
        {
            this.customProperties = customProperties;
        }
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getNodeRef()
     */
    public NodeRef getNodeRef()
    {
        return nodeRef;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getSitePreset()
     */
    public String getSitePreset()
    {
        return sitePreset;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getShortName()
     */
    public String getShortName()
    {
        return shortName;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getTitle()
     */
    public String getTitle()
    {
        return title;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getDescription()
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#setDescription(java.lang.String)
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#setIsPublic(boolean)
     */
    public void setIsPublic(boolean isPublic)
    {
        if (isPublic == true)
        {
            setVisibility(SiteVisibility.PUBLIC);
        }
        else
        {
            setVisibility(SiteVisibility.PRIVATE);
        }
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getIsPublic()
     */
    public boolean getIsPublic()
    {
        boolean result = false;
        if (SiteVisibility.PUBLIC.equals(this.visibility) == true)
        {
            result = true;
        }
        return result;
    }
    
    /**
     * @see org.alfresco.service.cmr.site.SiteInfo#getVisibility()
     */
    public SiteVisibility getVisibility()
    {
        return this.visibility;
    }

    /**
     * @see org.alfresco.service.cmr.site.SiteInfo#setVisibility(org.alfresco.service.cmr.site.SiteVisibility)
     */
    public void setVisibility(SiteVisibility visibility)
    {
        this.visibility = visibility;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getCustomProperties()
     */
    public Map<QName, Serializable> getCustomProperties()
    {
        return this.customProperties;
    }
    
    /**
     * @see org.alfresco.repo.site.SiteInfo#getCustomProperty(org.alfresco.service.namespace.QName)
     */
    public Serializable getCustomProperty(QName name)
    {
        Serializable result = null;
        if (this.customProperties != null)
        {
            result = this.customProperties.get(name);
        }
        return result;
    }    
    
    /**
     * Override equals for this ref type
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof SiteInfoImpl)
        {
            SiteInfoImpl that = (SiteInfoImpl) obj;
            return (this.shortName.equals(that.shortName));
        }
        else
        {
            return false;
        }
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return this.shortName.hashCode();
    }
}
