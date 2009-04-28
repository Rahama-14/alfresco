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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.config;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * A ResourcePatternResolver capable of recursing JBoss VFS file structures. It wraps its resource loader with a
 * {@link JBossEnabledResourceLoader} that intercepts VFS urls.
 * 
 * @author dward
 */
public class JBossEnabledResourcePatternResolver extends ServletContextResourcePatternResolver
{
    /** The helper. */
    private final PathMatchingHelper helper;

    /**
     * Create a new JBossEnabledResourcePatternResolver.
     * 
     * @param servletContext
     *            the ServletContext to load resources with
     * @see ServletContextResourceLoader#ServletContextResourceLoader(javax.servlet.ServletContext)
     */
    public JBossEnabledResourcePatternResolver(ServletContext servletContext)
    {
        this(new ServletContextResourceLoader(servletContext));
    }

    /**
     * Create a new JBossEnabledResourcePatternResolver.
     * 
     * @param resourceLoader
     *            the ResourceLoader to load root directories and actual resources with
     */
    public JBossEnabledResourcePatternResolver(ResourceLoader resourceLoader)
    {
        super(new JBossEnabledResourceLoader(resourceLoader));
        this.helper = (PathMatchingHelper) getResourceLoader();
    }

    /**
     * Overridden version which checks whether its helper can handle the path matching before falling back to the
     * superclass.
     * 
     * @param rootDirResource
     *            the root dir resource to search
     * @param subPattern
     *            the sub pattern
     * @return the matching resources
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Set doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException
    {
        if (this.helper.canHandle(rootDirResource.getURL()))
        {
            return this.helper.getResources(getPathMatcher(), rootDirResource.getURL(), subPattern);
        }
        return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
    }
}
