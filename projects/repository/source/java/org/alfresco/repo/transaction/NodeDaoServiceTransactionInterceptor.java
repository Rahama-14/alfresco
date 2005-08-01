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
package org.alfresco.repo.transaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.db.NodeDaoService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;

/**
 * Utility class that ensures that a <tt>NodeDaoService</tt> has been registered
 * with the current transaction.
 * <p>
 * It is designed to act as a <b>postInterceptor</b> on the <tt>NodeDaoService</tt>'s
 * {@link org.springframework.transaction.interceptor.TransactionProxyFactoryBean}. 
 * 
 * @author Derek Hulley
 */
public class NodeDaoServiceTransactionInterceptor implements MethodInterceptor, InitializingBean
{
    private NodeDaoService nodeDaoService;

    /**
     * @param nodeDaoService the <tt>NodeDaoService</tt> to register
     */
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * Checks that required values have been injected
     */
    public void afterPropertiesSet() throws Exception
    {
        if (nodeDaoService == null)
        {
            throw new AlfrescoRuntimeException("NodeDaoService is required: " + this);
        }
    }

    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        AlfrescoTransactionSupport.bindNodeDaoService(nodeDaoService);
        // propogate the call
        return invocation.proceed();
    }
}
