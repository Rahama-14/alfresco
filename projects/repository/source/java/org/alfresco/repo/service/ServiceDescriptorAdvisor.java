/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.service;

import org.springframework.aop.support.DefaultIntroductionAdvisor;

/**
 * Service Descriptor Advisor
 * 
 * @author David Caruana
 */
public class ServiceDescriptorAdvisor extends DefaultIntroductionAdvisor
{
    private static final long serialVersionUID = -3327182176681357761L;

    
    /**
     * Construct Service Descriptor Advisor
     * 
     * @param namespace  service name namespace
     * @param description  service description
     * @param interfaceClass  service interface class
     */
    public ServiceDescriptorAdvisor(String namespace, String description, Class interfaceClass)
    {
        super(new ServiceDescriptorMixin(namespace, description, interfaceClass), ServiceDescriptorMetaData.class);
    }
    
}
