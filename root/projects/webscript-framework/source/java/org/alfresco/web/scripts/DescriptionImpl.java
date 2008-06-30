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
package org.alfresco.web.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;


/**
 * Implementation of a Web Script Description
 * 
 * @author davidc
 */
public class DescriptionImpl implements Description
{
    private Store store;
    private String scriptPath;
    private String descPath;
    private String id;
    private String kind;
    private String shortName;
    private String description;
    private String family;
    private RequiredAuthentication requiredAuthentication;
    private RequiredTransaction requiredTransaction;
    private RequiredCache requiredCache;
    private FormatStyle formatStyle;
    private String httpMethod;
    private String[] uris;
    private String defaultFormat;
    private NegotiatedFormat[] negotiatedFormats;
    private Map<String, Serializable> extensions;

    
    /**
     * Sets the web description store
     * 
     * @param store  store
     */
    public void setStore(Store store)
    {
        this.store = store;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getStorePath()
     */
    public String getStorePath()
    {
        return store.getBasePath();
    }

    /**
     * Sets the script path
     * 
     * @param scriptPath
     */
    public void setScriptPath(String scriptPath)
    {
        this.scriptPath = scriptPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getScriptPath()
     */
    public String getScriptPath()
    {
        return scriptPath;
    }

    /**
     * Sets the desc path
     * 
     * @param descPath
     */
    public void setDescPath(String descPath)
    {
        this.descPath = descPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getDescPath()
     */
    public String getDescPath()
    {
        return descPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getDescDocument()
     */
    public InputStream getDescDocument()
        throws IOException
    {
        return store.getDocument(descPath);
    }

    /**
     * Sets the service id
     * 
     * @param id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getId()
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the service kind
     * 
     * @param kind
     */
    public void setKind(String kind)
    {
        this.kind = kind;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getKind()
     */
    public String getKind()
    {
        return this.kind;
    }

    /**
     * Sets the service short name
     * 
     * @param shortName
     */
    public void setShortName(String shortName)
    {
        this.shortName = shortName;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getShortName()
     */
    public String getShortName()
    {
        return this.shortName;
    }

    /**
     * Sets the service description
     * 
     * @param description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param family the family to set
     */
    public void setFamily(String family)
    {
        this.family = family;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getFamily()
     */
    public String getFamily()
    {
        return this.family;
    }

    /**
     * Sets the required level of authentication
     * 
     * @param requiredAuthentication
     */
    public void setRequiredAuthentication(RequiredAuthentication requiredAuthentication)
    {
        this.requiredAuthentication = requiredAuthentication;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getRequiredAuthentication()
     */
    public RequiredAuthentication getRequiredAuthentication()
    {
        return this.requiredAuthentication;
    }

    /**
     * Sets the required level of transaction
     * 
     * @param requiredTransaction
     */
    public void setRequiredTransaction(RequiredTransaction requiredTransaction)
    {
        this.requiredTransaction = requiredTransaction;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getRequiredTransaction()
     */
    public RequiredTransaction getRequiredTransaction()
    {
        return this.requiredTransaction;
    }

    /**
     * Sets the required cache
     * 
     * @param requiredCache
     */
    public void setRequiredCache(RequiredCache requiredCache)
    {
        this.requiredCache = requiredCache;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getRequiredCache()
     */
    public RequiredCache getRequiredCache()
    {
        return this.requiredCache;
    }
    
    /**
     * Sets the format style
     * 
     * @param formatStyle
     */
    public void setFormatStyle(FormatStyle formatStyle)
    {
        this.formatStyle = formatStyle;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getFormatStyle()
     */
    public FormatStyle getFormatStyle()
    {
        return this.formatStyle;
    }
    
    /**
     * Sets the service http method
     * 
     * @param httpMethod
     */
    public void setMethod(String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getMethod()
     */
    public String getMethod()
    {
        return this.httpMethod;
    }

    /**
     * Sets the service URIs
     * 
     * @param uris
     */
    public void setUris(String[] uris)
    {
        this.uris = uris;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getURIs()
     */
    public String[] getURIs()
    {
        return this.uris;
    }

    /**
     * Sets the default response format
     * 
     * @param defaultFormat
     */
    public void setDefaultFormat(String defaultFormat)
    {
        this.defaultFormat = defaultFormat;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getDefaultFormat()
     */
    public String getDefaultFormat()
    {
        return this.defaultFormat;
    }

    /**
     * Sets the negotiated formats
     * 
     * @param defaultFormat
     */
    public void setNegotiatedFormats(NegotiatedFormat[] negotiatedFormats)
    {
        this.negotiatedFormats = negotiatedFormats;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getNegotiatedFormats()
     */
    public NegotiatedFormat[] getNegotiatedFormats()
    {
        return this.negotiatedFormats;
    }

    /**
     * Sets Web Script custom extensions
     * 
     * @param extensions
     */
    public void setExtensions(Map<String, Serializable> extensions)
    {
        this.extensions = extensions;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Description#getExtensions()
     */
    public Map<String, Serializable> getExtensions()
    {
        return extensions;
    }

}
