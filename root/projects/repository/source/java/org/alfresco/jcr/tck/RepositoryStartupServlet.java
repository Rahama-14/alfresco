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
package org.alfresco.jcr.tck;

import java.util.Hashtable;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.alfresco.jcr.repository.RepositoryFactory;
import org.alfresco.jcr.repository.RepositoryImpl;
import org.alfresco.jcr.test.TestData;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * Setup Repository for access via JNDI by TCK Web Application
 * 
 * @author David Caruana
 */
public class RepositoryStartupServlet extends HttpServlet
{
    private static final long serialVersionUID = -4763518135895358778L;

    private static InitialContext jndiContext;
    
    private final static String repositoryName = "Alfresco.Repository";

    
    /**
     * Initializes the servlet
     * 
     * @throws ServletException
     */
    public void init()
        throws ServletException
    {
        super.init();
        
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        RepositoryImpl repository = (RepositoryImpl)context.getBean(RepositoryFactory.REPOSITORY_BEAN);
        repository.setDefaultWorkspace(TestData.TEST_WORKSPACE);
        
        try
        {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.PROVIDER_URL, "http://www.alfresco.org");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.day.crx.jndi.provider.MemoryInitialContextFactory");
            jndiContext = new InitialContext(env);
            jndiContext.bind(repositoryName, (Repository)repository);
        }
        catch (NamingException e)
        {
            throw new ServletException(e);
        }
    }

    /**
     * Destroy the servlet
     */
    public void destroy()
    {
        super.destroy();

        if (jndiContext != null)
        {
            try
            {
                jndiContext.unbind(repositoryName);
            }
            catch (NamingException e)
            {
                // Note: Itentionally ignore...
            }
        }
    }


}
