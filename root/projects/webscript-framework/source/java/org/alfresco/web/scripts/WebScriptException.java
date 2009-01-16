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

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.error.AlfrescoRuntimeException;

/**
 * Web Script Exceptions.
 * 
 * @author David Caruana
 */
public class WebScriptException extends AlfrescoRuntimeException implements StatusTemplateFactory
{
    private static final long serialVersionUID = -7338963365877285084L;

    private int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    private StatusTemplateFactory statusTemplateFactory;


    public WebScriptException(String msgId)
    {
       super(msgId);
    }

    public WebScriptException(int status, String msgId)
    {
        this(msgId);
        this.status = status;
    }
    
    public WebScriptException(String msgId, Throwable cause)
    {
       super(msgId, cause);
    }

    public WebScriptException(int status, String msgId, Throwable cause)
    {
       super(msgId, cause);
       this.status = status;
    }

    public WebScriptException(String msgId, Object ... args)
    {
        super(msgId, args);
    }

    public WebScriptException(int status, String msgId, Object ... args)
    {
        super(msgId, args);
        this.status = status;
    }

    public WebScriptException(String msgId, Throwable cause, Object ... args)
    {
        super(msgId, args, cause);
    }

    public WebScriptException(int status, String msgId, Throwable cause, Object ... args)
    {
        super(msgId, args, cause);
        this.status = status;
    }

    /**
     * Attach an advanced description of the status code associated to this exception
     * 
     * @param template  status template
     * @param model  template model
     * @deprecated
     */
    public void setStatusTemplate(final StatusTemplate statusTemplate, final Map<String, Object> statusModel)
    {
        setStatusTemplateFactory(new StatusTemplateFactory()
        {

            public Map<String, Object> getStatusModel()
            {
                return statusModel;
            }

            public StatusTemplate getStatusTemplate()
            {
                return statusTemplate;
            }
        });
    }

    
    /**
     * Associates a factory for the lazy retrieval of an advanced description of the status code associated with this
     * exception
     * 
     * @param statusTemplateFactory
     *            the factory to set
     */
    public void setStatusTemplateFactory(StatusTemplateFactory statusTemplateFactory)
    {
        this.statusTemplateFactory = statusTemplateFactory;
    }

    /**
     * Get status code
     * 
     * @return  status code
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Get status template
     * 
     * @return  template
     */
    public StatusTemplate getStatusTemplate()
    {
        return this.statusTemplateFactory == null ? null : this.statusTemplateFactory.getStatusTemplate();
    }

    /**
     * Get status model
     * 
     * @return  model
     */
    public Map<String, Object> getStatusModel()
    {
        Map <String,Object> statusModel = null;
        if (this.statusTemplateFactory != null)
        {
            statusModel = this.statusTemplateFactory.getStatusModel();
        }
        return statusModel == null ? Collections.<String, Object> emptyMap() : statusModel;
    }

}
