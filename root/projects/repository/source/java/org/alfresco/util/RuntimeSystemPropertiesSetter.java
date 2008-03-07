/*-----------------------------------------------------------------------------
*  Copyright 2006 Alfresco Inc.
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
 
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    RuntimeSystemPropertiesSetter.java
*----------------------------------------------------------------------------*/

package org.alfresco.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor; 
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;



/**
*   Sets runtime JVM system properties for Spring Framework. 
*
*   This class is used by the Spring framework to inject system properties into
*   the runtime environment (e.g.:  alfresco.jmx.dir).   The motivation for this 
*   is that certain values must be set within spring must be computed in advance
*   for org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
*   to work properly.
*
*/
public class      RuntimeSystemPropertiesSetter 
       implements BeanFactoryPostProcessor, Ordered
{
    private static org.apache.commons.logging.Log log=
                org.apache.commons.logging.LogFactory.getLog( 
                    RuntimeSystemPropertiesSetter.class );

    // default: just before PropertyPlaceholderConfigurer
    private int order = Integer.MAX_VALUE - 1;  

    public void RuntimeSystemPropertiesSetter() { }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 
                throws BeansException 
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String path=null;
        try 
        {
            // Typically, the value of 'path' will be something like:
            //
            //     $TOMCAT_HOME/webapps/alfresco/WEB-INF/classes/alfresco/alfresco-jmxrmi.password
            // or: $TOMCAT_HOME/shared/classes/alfresco/alfresco-jmxrmi.password
            //
            // However, if WCM isn't installed there won't be a JMX password file.
            // Therefore, while it's important to choke on bad paths, a missing
            // password file must be acceptable -- it just means that WCM virtualization
            // will be disabled later when org.alfresco.mbeans.VirtServerRegistry
            // refuses to bring up the serverConnector bean.

            path = loader.getResource("alfresco/alfresco-jmxrmi.password").toURI().getPath();
        }
        catch (java.net.URISyntaxException e ) { e.printStackTrace(); }
        catch (Exception e ) 
        { 
            if ( log.isWarnEnabled() )
                 log.warn( 
                 "Could not find alfresco-jmxrmi.password on classpath");
        }

        if ( path == null ) { System.setProperty("alfresco.jmx.dir", ""); }
        else
        {
            String alfresco_jmx_dir =   
                   path.substring(0,path.lastIndexOf("/alfresco-jmxrmi.password"));

            // The value of 'alfresco.jmx.dir' will be something like:
            // $TOMCAT_HOME/webapps/alfresco/WEB-INF/classes/alfresco

            System.setProperty("alfresco.jmx.dir", alfresco_jmx_dir);
        }
    }
    public void setOrder(int order) { this.order = order; }
    public int getOrder()           { return order; }               
}
