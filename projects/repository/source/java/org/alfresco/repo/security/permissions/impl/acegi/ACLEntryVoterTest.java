/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.security.permissions.impl.acegi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.vote.AccessDecisionVoter;

import org.alfresco.repo.security.permissions.AccessStatus;
import org.alfresco.repo.security.permissions.impl.AbstractPermissionTest;
import org.alfresco.repo.security.permissions.impl.SimplePermissionEntry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;

public class ACLEntryVoterTest extends AbstractPermissionTest
{

    public ACLEntryVoterTest()
    {
        super();
    }

    public void testBasicDenyNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        try
        {
            method.invoke(proxy, new Object[] { rootNodeRef });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

    }
    
    
    public void testBasicDenyStore() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        try
        {
            method.invoke(proxy, new Object[] { rootNodeRef.getStoreRef() });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

    }

    public void testAllowNullNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null });

    }
    
    public void testAllowNullStore() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null });

    }

    public void testAllowNullParentOnRealChildAssoc() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_PARENT.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });

    }

    public void testAllowNullParent() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_PARENT.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null });

    }

    public void testAllowNullChild() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null });

    }

    public void testBasicDenyChildAssocNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        try
        {
            method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }
    }

    public void testBasicDenyParentAssocNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_PARENT.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        try
        {
            method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }
    }

    public void testBasicAllowNode() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { rootNodeRef });
    }
    
    public void testBasicAllowStore() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { rootNodeRef.getStoreRef() });
    }

    public void testBasicAllowChildAssocNode() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
    }

    public void testBasicAllowParentAssocNode() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_PARENT.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
    }

    public void testDenyParentAssocNode() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(systemNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_PARENT.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        try
        {
            method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }
    }

    public void testAllowChildAssocNode() throws Exception
    {
        runAs("andy");

        permissionService.setPermission(new SimplePermissionEntry(systemNodeRef, READ, "andy", AccessStatus.ALLOWED));
        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ_CHILDREN, "andy",
                AccessStatus.ALLOWED));

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testOneChildAssociationRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));

        proxyFactory.setTargetSource(new SingletonTargetSource(o));

        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });

    }

    public void testMultiNodeMethodsArg0() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testManyNodeRef",
                new Class[] { NodeRef.class, NodeRef.class, NodeRef.class, NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { rootNodeRef, null, null, null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { rootNodeRef, null, null, null });
    }

    public void testMultiNodeMethodsArg1() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testManyNodeRef",
                new Class[] { NodeRef.class, NodeRef.class, NodeRef.class, NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.1.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, rootNodeRef, null, null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, rootNodeRef, null, null });
    }

    public void testMultiNodeMethodsArg2() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testManyNodeRef",
                new Class[] { NodeRef.class, NodeRef.class, NodeRef.class, NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.2.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, null, rootNodeRef, null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, null, rootNodeRef, null });
    }

    public void testMultiNodeMethodsArg3() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("testManyNodeRef",
                new Class[] { NodeRef.class, NodeRef.class, NodeRef.class, NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.3.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, null, null, rootNodeRef });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, null, null, rootNodeRef });
    }

    public void testMultiChildAssocRefMethodsArg0() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod(
                "testManyChildAssociationRef",
                new Class[] { ChildAssociationRef.class, ChildAssociationRef.class, ChildAssociationRef.class,
                        ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.0.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef), null, null, null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef), null, null, null });
    }

    public void testMultiChildAssocRefMethodsArg1() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod(
                "testManyChildAssociationRef",
                new Class[] { ChildAssociationRef.class, ChildAssociationRef.class, ChildAssociationRef.class,
                        ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.1.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, nodeService.getPrimaryParent(rootNodeRef), null, null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, nodeService.getPrimaryParent(rootNodeRef), null, null });
    }

    public void testMultiChildAssocRefMethodsArg2() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod(
                "testManyChildAssociationRef",
                new Class[] { ChildAssociationRef.class, ChildAssociationRef.class, ChildAssociationRef.class,
                        ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.2.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, null, nodeService.getPrimaryParent(rootNodeRef), null });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, null, nodeService.getPrimaryParent(rootNodeRef), null });
    }

    public void testMultiChildAssocRefMethodsArg3() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod(
                "testManyChildAssociationRef",
                new Class[] { ChildAssociationRef.class, ChildAssociationRef.class, ChildAssociationRef.class,
                        ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("ACL_NODE.3.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        method.invoke(proxy, new Object[] { null, null, null, null });

        try
        {
            method.invoke(proxy, new Object[] { null, null, null, nodeService.getPrimaryParent(rootNodeRef) });
            assertNotNull(null);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));
        method.invoke(proxy, new Object[] { null, null, null, nodeService.getPrimaryParent(rootNodeRef) });
    }

    public static class ClassWithMethods
    {
        public void testOneStoreRef(StoreRef storeRef)
        {
            
        }
        
        public void testOneNodeRef(NodeRef nodeRef)
        {

        }

        public void testManyNodeRef(NodeRef nodeRef1, NodeRef nodeRef2, NodeRef nodeRef3, NodeRef nodeRef4)
        {

        }

        public void testOneChildAssociationRef(ChildAssociationRef car)
        {

        }

        public void testManyChildAssociationRef(ChildAssociationRef car1, ChildAssociationRef car2,
                ChildAssociationRef car3, ChildAssociationRef car4)
        {

        }
    }

    public class Interceptor implements MethodInterceptor
    {
        ConfigAttributeDefinition cad = new ConfigAttributeDefinition();

        Interceptor(final String config)
        {
            cad.addConfigAttribute(new ConfigAttribute()
            {

                /**
                 * Comment for <code>serialVersionUID</code>
                 */
                private static final long serialVersionUID = 1L;

                public String getAttribute()
                {
                    return config;
                }

            });
        }

        public Object invoke(MethodInvocation invocation) throws Throwable
        {
            ACLEntryVoter voter = new ACLEntryVoter();
            voter.setNamespacePrefixResolver(namespacePrefixResolver);
            voter.setPermissionService(permissionService);
            voter.setNodeService(nodeService);

            if (!(voter.vote(null, invocation, cad) == AccessDecisionVoter.ACCESS_DENIED))
            {
                return invocation.proceed();
            }
            else
            {
                throw new ACLEntryVoterException("Access denied");
            }

        }
    }
}
