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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.deploy;

import java.text.MessageFormat;

import org.alfresco.deployment.DeploymentReceiverTransport;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

/**
 * This class adapts the Hessian protocol to DeploymentReceiverTransport
 * 
 * @author mrogers
 */
public class DeploymentReceiverTransportAdapterHessian extends AbstractDeploymentReceiverTransportAdapter implements DeploymentReceiverTransportAdapter 
{
	/**
	 * The pattern to use when constructing the URL from hostname and port
	 * 
	 * eg http://localhost:8080/FSR/deployment
	 */
	private String urlPattern = "http://{1}:{2}/FSR/deployment";
	
	public DeploymentReceiverTransport getTransport(String host,
			int port, int version, String srcPath) 
	{
       	MessageFormat f = new MessageFormat(urlPattern);
    	Object[] objs = { host, port };
    	String URL = f.format(objs);
		
		// Code to use Hessian transport provided via Spring
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
    	factory.setServiceInterface(DeploymentReceiverTransport.class);
        factory.setServiceUrl(URL);
    	factory.afterPropertiesSet();
    	DeploymentReceiverTransport transport = (DeploymentReceiverTransport) factory.getObject();

    	return transport;

	}

	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern;
	}

	public String getUrlPattern() {
		return urlPattern;
	}
}
