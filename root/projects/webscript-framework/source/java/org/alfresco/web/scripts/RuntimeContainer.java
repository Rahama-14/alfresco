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
package org.alfresco.web.scripts;

import java.io.IOException;

import org.alfresco.web.scripts.Description.RequiredAuthentication;

/**
 * Web Script Container extensions for a Web Script Runtime
 * 
 * @author davidc
 */
public interface RuntimeContainer extends Container
{
    /**
     * Execute the script in the context of the provided request and response
     * 
     * @param scriptReq
     * @param scriptRes
     */
    public void executeScript(WebScriptRequest scriptReq, WebScriptResponse scriptRes, Authenticator auth)
        throws IOException;
    
    /**
     * Gets the required container authentication level (for matching web scripts)
     * 
     * @return  the required authentication level
     */
    public RequiredAuthentication getRequiredAuthentication();
    
    /**
     * Pre-authenticate container, if required
     * 
     * @param auth
     * @param required
     */
    public boolean authenticate(Authenticator auth, RequiredAuthentication required);
}
