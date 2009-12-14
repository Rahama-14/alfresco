/*-----------------------------------------------------------------------------
*  Copyright 2007 Alfresco Inc.
*  
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful, but
*  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
*  
*  You should have received a copy of the GNU General Public License along
*  with this program; if not, write to the Free Software Foundation, Inc.,
*  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.  As a special
*  exception to the terms and conditions of version 2.0 of the GPL, you may
*  redistribute this Program in connection with Free/Libre and Open Source
*  Software ("FLOSS") applications as described in Alfresco's FLOSS exception.
*  You should have received a copy of the text describing the FLOSS exception,
*  and it is also available here:   http://www.alfresco.com/legal/licensing
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    LinkValidationServiceBootstrap.java
*----------------------------------------------------------------------------*/
package org.alfresco.linkvalidation;

import org.alfresco.repo.avm.util.RawServices;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * Bootstrap for LinkValidationService
 */
public class LinkValidationServiceBootstrap extends AbstractLifecycleBean
{
    private static Log logger = LogFactory.getLog(LinkValidationServiceBootstrap.class);
    private LinkValidationService linkValidationService_;
    
    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        ApplicationContext springContext = RawServices.Instance().getContext();
        linkValidationService_ = (LinkValidationService) 
            springContext.getBean("LinkValidationService");

        linkValidationService_.onBootstrap();
    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        try
        {
            linkValidationService_.onShutdown();
        }
        catch (Throwable e)
        {
            logger.warn("Failed to shut down LinkValidationService", e);
        }
    }
}
