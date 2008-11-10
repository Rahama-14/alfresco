/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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

import javax.servlet.http.HttpServletRequest;

import org.alfresco.web.framework.render.RenderContext;
import org.alfresco.web.site.FrameworkHelper;
import org.alfresco.web.site.WebFrameworkConstants;

/**
 * Helper object for dealing with the web application's environment.
 * 
 * This object can be used on both the production and preview tiers to gain access to the correct
 * web application mount points and more.
 * 
 * @author muzquiano
 */
public final class ScriptWebApplication extends ScriptBase
{
    /**
     * Constructs a new ScriptWebApplication object.
     * 
     * @param context   The RenderContext instance for the current request
     */
    public ScriptWebApplication(RenderContext context)
    {
        super(context);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebFrameworkScriptBase#buildProperties()
     */
    protected ScriptableMap buildProperties()
    {
        if (this.properties == null)
        {
        }
        
        return this.properties;
    }

    
    // --------------------------------------------------------------
    // JavaScript Properties
    
    /**
     * Returns the root web application context
     */
    public String getContext()
    {    	
		StringBuilder builder = new StringBuilder(512);
		
		// append the path to the application server hosted web application
		// on the production tier, this will be the correct context
		HttpServletRequest request = this.context.getRequest();		
		builder.append(request.getContextPath());

		// on the preview tier, we'll plug in a passthru to the AVM remote store
		if(FrameworkHelper.getConfig().isWebStudioEnabled())
		{
			// assume proxy to alfresco
			builder.append("/proxy/alfresco");
			
			// remote store
			builder.append("/remotestore/get");
			
			// throw down the store id (if applicable)
	    	String storeId = (String) context.getValue(WebFrameworkConstants.STORE_ID_REQUEST_CONTEXT_NAME);
	    	if(storeId != null)
	    	{
	    		builder.append("/s/");
	    		builder.append(storeId);
	    	}
	    	
	    	// throw down the webapp id (if applicable)
	    	String webappId = (String) context.getValue(WebFrameworkConstants.WEBAPP_ID_REQUEST_CONTEXT_NAME);
	    	if(webappId != null)
	    	{
	    		builder.append("/w/");
	    		builder.append(webappId);
	    	}	    	
		}
		
    	return builder.toString();
    }        
    
}
