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
package org.alfresco.web.site;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.web.framework.model.Theme;

/**
 * A helper class for working with themes.
 * 
 * This basically assists in synchronizing the current theme between
 * the request and the session.
 * 
 * It is useful for determining the current theme id during the execution
 * of a JSP component, for example, or within a custom Java bean.
 * 
 * @author muzquiano
 */
public class ThemeUtil
{
    /**
     * Gets the current theme id.
     * 
     * @param context the context
     * 
     * @return the current theme id
     */
    public static String getCurrentThemeId(RequestContext context)
    {
        return context.getThemeId();
    }

    /**
     * Gets the current theme.
     * 
     * @param context the context
     * 
     * @return the current theme
     */
    public static Theme getCurrentTheme(RequestContext context)
    {
        return context.getTheme();
    }

    /**
     * This method is called by the dispatcher servlet to "push" the
     * current theme from the session into the request-scoped RequestContext
     * 
     * @param context the context
     * @param request the request
     */
    public static void applyTheme(RequestContext context, HttpServletRequest request)
    {
        String themeId = context.getThemeId();
        if (themeId == null)
        {
        	themeId = FrameworkHelper.getConfig().getDefaultThemeId();
            if (themeId == null)
            {
                themeId = WebFrameworkConstants.DEFAULT_THEME_ID;
            }
            
            if (themeId != null)
            {
            	context.setThemeId(themeId);
            }
        }
    }
}
