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

/**
 * @author Roy Wetherall
 */
public class SiteInfo
{
    private String sitePreset;
    private String shortName;
    private String title;
    private String description;
    private boolean isPublic;
   
    public SiteInfo(String sitePreset, String shortName, String title, String description, boolean isPublic)
    {
        this.sitePreset = sitePreset;
        this.shortName = shortName;
        this.title = title;
        this.description = description;
        this.isPublic = isPublic;
    }
    
    public String getSitePreset()
    {
        return sitePreset;
    }
    
    public String getShortName()
    {
        return shortName;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public void setIsPublic(boolean isPublic)
    {
        this.isPublic = isPublic;
    }
    
    public boolean getIsPublic()
    {
        return this.isPublic;
    }
}
