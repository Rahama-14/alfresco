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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.results.ChildAssocRefResultSet;
import org.alfresco.repo.security.permissions.AccessStatus;
import org.alfresco.repo.security.permissions.impl.AbstractPermissionTest;
import org.alfresco.repo.security.permissions.impl.SimplePermissionEntry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;

public class ACLEntryAfterInvocationTest extends AbstractPermissionTest
{

    public ACLEntryAfterInvocationTest()
    {
        super();
    }

    public void testBasicAllowNullNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { null });
        assertNull(answer);
    }
    
    public void testBasicAllowNullStore() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { null });
        assertNull(answer);
    }

    public void testBasicAllowUnrecognisedObject() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoObject", new Class[] { Object.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { "noodle" });
        assertNotNull(answer);
    }

    public void testBasicDenyStore() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        try
        {
            Object answer = method.invoke(proxy, new Object[] { rootNodeRef.getStoreRef() });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

    }
    
    public void testBasicDenyNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        try
        {
            Object answer = method.invoke(proxy, new Object[] { rootNodeRef });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

    }

    public void testBasicAllowNode() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object answer = method.invoke(proxy, new Object[] { rootNodeRef });
        assertEquals(answer, rootNodeRef);

    }
    
    public void testBasicAllowStore() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoStoreRef", new Class[] { StoreRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object answer = method.invoke(proxy, new Object[] { rootNodeRef.getStoreRef() });
        assertEquals(answer, rootNodeRef.getStoreRef());

    }

    public void testBasicAllowNodeParent() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoNodeRef", new Class[] { NodeRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { rootNodeRef });
        assertEquals(answer, rootNodeRef);

        try
        {
            answer = method.invoke(proxy, new Object[] { systemNodeRef });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        answer = method.invoke(proxy, new Object[] { systemNodeRef });
        assertEquals(answer, systemNodeRef);
    }

    public void testBasicAllowNullChildAssociationRef1() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { null });
        assertNull(answer);
    }

    public void testBasicAllowNullChildAssociationRef2() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { null });
        assertNull(answer);
    }

    public void testBasicDenyChildAssocRef1() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        try
        {
            Object answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

        try
        {
            Object answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

    }

    public void testBasicDenyChildAssocRef2() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        Object answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
        assertNotNull(answer);

        try
        {
            answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
            assertNotNull(answer);
        }
        catch (InvocationTargetException e)
        {

        }

    }

    public void testBasicAllowChildAssociationRef1() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
        assertEquals(answer, nodeService.getPrimaryParent(rootNodeRef));

        answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
        assertEquals(answer, nodeService.getPrimaryParent(systemNodeRef));

    }

    public void testBasicAllowChildAssociationRef2() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method method = o.getClass().getMethod("echoChildAssocRef", new Class[] { ChildAssociationRef.class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        Object answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(rootNodeRef) });
        assertEquals(answer, nodeService.getPrimaryParent(rootNodeRef));

        answer = method.invoke(proxy, new Object[] { nodeService.getPrimaryParent(systemNodeRef) });
        assertEquals(answer, nodeService.getPrimaryParent(systemNodeRef));
    }

    public void testBasicAllowNullResultSet() throws Exception
    {
        runAs("andy");

        Object o = new ClassWithMethods();
        Method methodResultSet = o.getClass().getMethod("echoResultSet", new Class[] { ResultSet.class });
        Method methodCollection = o.getClass().getMethod("echoCollection", new Class[] { Collection.class });
        Method methodArray = o.getClass().getMethod("echoArray", new Class[] { Object[].class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        List<NodeRef> nodeRefList = new ArrayList<NodeRef>();
        NodeRef[] nodeRefArray = new NodeRef[0];

        Set<NodeRef> nodeRefSet = new HashSet<NodeRef>();
        
        List<ChildAssociationRef> carList = new ArrayList<ChildAssociationRef>();

        ChildAssociationRef[] carArray = new ChildAssociationRef[0];

        Set<ChildAssociationRef> carSet = new HashSet<ChildAssociationRef>();
       
        ChildAssocRefResultSet rsIn = new ChildAssocRefResultSet(nodeService, nodeRefList, null, false);

        assertEquals(0, rsIn.length());
        ResultSet answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(0, answerResultSet.length());
        Collection answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(0, answerCollection.size());
        Object[] answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(0, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(0, answerArray.length);
        
        assertEquals(0, rsIn.length());
        answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { null });
        assertNull(answerResultSet);
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { null });
        assertNull(answerCollection);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { null });
        assertNull(answerArray);
    }

    public void testResultSetFilterAll() throws Exception
    {
        NodeRef n1 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName("{namespace}one"), ContentModel.TYPE_FOLDER).getChildRef();

        runAs("andy");

        Object o = new ClassWithMethods();
        Method methodResultSet = o.getClass().getMethod("echoResultSet", new Class[] { ResultSet.class });
        Method methodCollection = o.getClass().getMethod("echoCollection", new Class[] { Collection.class });
        Method methodArray = o.getClass().getMethod("echoArray", new Class[] { Object[].class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        List<NodeRef> nodeRefList = new ArrayList<NodeRef>();
        nodeRefList.add(rootNodeRef);
        nodeRefList.add(systemNodeRef);
        nodeRefList.add(n1);
        nodeRefList.add(n1);

        NodeRef[] nodeRefArray = nodeRefList.toArray(new NodeRef[] {});

        Set<NodeRef> nodeRefSet = new HashSet<NodeRef>();
        nodeRefSet.addAll(nodeRefList);

        List<ChildAssociationRef> carList = new ArrayList<ChildAssociationRef>();
        carList.add(nodeService.getPrimaryParent(rootNodeRef));
        carList.add(nodeService.getPrimaryParent(systemNodeRef));
        carList.add(nodeService.getPrimaryParent(n1));
        carList.add(nodeService.getPrimaryParent(n1));

        ChildAssociationRef[] carArray = carList.toArray(new ChildAssociationRef[] {});

        Set<ChildAssociationRef> carSet = new HashSet<ChildAssociationRef>();
        carSet.addAll(carList);

        ChildAssocRefResultSet rsIn = new ChildAssocRefResultSet(nodeService, nodeRefList, null, false);

        assertEquals(4, rsIn.length());
        ResultSet answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(0, answerResultSet.length());
        Collection answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(0, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(0, answerCollection.size());
        Object[] answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(0, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(0, answerArray.length);
    }

    public void testResultSetFilterForNullParentOnly() throws Exception
    {
        NodeRef n1 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName("{namespace}one"), ContentModel.TYPE_FOLDER).getChildRef();

        runAs("andy");

        Object o = new ClassWithMethods();
        Method methodResultSet = o.getClass().getMethod("echoResultSet", new Class[] { ResultSet.class });
        Method methodCollection = o.getClass().getMethod("echoCollection", new Class[] { Collection.class });
        Method methodArray = o.getClass().getMethod("echoArray", new Class[] { Object[].class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        List<NodeRef> nodeRefList = new ArrayList<NodeRef>();
        nodeRefList.add(rootNodeRef);
        nodeRefList.add(systemNodeRef);
        nodeRefList.add(n1);
        nodeRefList.add(n1);

        NodeRef[] nodeRefArray = nodeRefList.toArray(new NodeRef[] {});

        Set<NodeRef> nodeRefSet = new HashSet<NodeRef>();
        nodeRefSet.addAll(nodeRefList);

        List<ChildAssociationRef> carList = new ArrayList<ChildAssociationRef>();
        carList.add(nodeService.getPrimaryParent(rootNodeRef));
        carList.add(nodeService.getPrimaryParent(systemNodeRef));
        carList.add(nodeService.getPrimaryParent(n1));
        carList.add(nodeService.getPrimaryParent(n1));

        ChildAssociationRef[] carArray = carList.toArray(new ChildAssociationRef[] {});

        Set<ChildAssociationRef> carSet = new HashSet<ChildAssociationRef>();
        carSet.addAll(carList);

        ChildAssocRefResultSet rsIn = new ChildAssocRefResultSet(nodeService, nodeRefList, null, false);


        assertEquals(4, rsIn.length());
        ResultSet answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(1, answerResultSet.length());
        Collection answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(1, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(1, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(1, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(1, answerCollection.size());
        Object[] answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(1, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(1, answerArray.length);
    }

    public void testResultSetFilterNone1() throws Exception
    {
        NodeRef n1 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName("{namespace}one"), ContentModel.TYPE_FOLDER).getChildRef();

        runAs("andy");

        Object o = new ClassWithMethods();
        Method methodResultSet = o.getClass().getMethod("echoResultSet", new Class[] { ResultSet.class });
        Method methodCollection = o.getClass().getMethod("echoCollection", new Class[] { Collection.class });
        Method methodArray = o.getClass().getMethod("echoArray", new Class[] { Object[].class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_NODE.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        List<NodeRef> nodeRefList = new ArrayList<NodeRef>();
        nodeRefList.add(rootNodeRef);
        nodeRefList.add(systemNodeRef);
        nodeRefList.add(n1);
        nodeRefList.add(n1);

        NodeRef[] nodeRefArray = nodeRefList.toArray(new NodeRef[] {});

        Set<NodeRef> nodeRefSet = new HashSet<NodeRef>();
        nodeRefSet.addAll(nodeRefList);

        List<ChildAssociationRef> carList = new ArrayList<ChildAssociationRef>();
        carList.add(nodeService.getPrimaryParent(rootNodeRef));
        carList.add(nodeService.getPrimaryParent(systemNodeRef));
        carList.add(nodeService.getPrimaryParent(n1));
        carList.add(nodeService.getPrimaryParent(n1));

        ChildAssociationRef[] carArray = carList.toArray(new ChildAssociationRef[] {});

        Set<ChildAssociationRef> carSet = new HashSet<ChildAssociationRef>();
        carSet.addAll(carList);

        ChildAssocRefResultSet rsIn = new ChildAssocRefResultSet(nodeService, nodeRefList, null, false);

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        assertEquals(4, rsIn.length());
        ResultSet answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(4, answerResultSet.length());
        Collection answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(3, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(3, answerCollection.size());
        Object[] answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(4, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(4, answerArray.length);

        permissionService.setPermission(new SimplePermissionEntry(n1, READ, "andy", AccessStatus.DENIED));

        assertEquals(4, rsIn.length());
        answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(2, answerResultSet.length());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(2, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(2, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(2, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(2, answerCollection.size());
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(2, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(2, answerArray.length);

    }

    public void testResultSetFilterNone2() throws Exception
    {

        NodeRef n1 = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName("{namespace}one"), ContentModel.TYPE_FOLDER).getChildRef();

        runAs("andy");

        Object o = new ClassWithMethods();
        Method methodResultSet = o.getClass().getMethod("echoResultSet", new Class[] { ResultSet.class });
        Method methodCollection = o.getClass().getMethod("echoCollection", new Class[] { Collection.class });
        Method methodArray = o.getClass().getMethod("echoArray", new Class[] { Object[].class });

        AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvisor(advisorAdapterRegistry.wrap(new Interceptor("AFTER_ACL_PARENT.sys:base.Read")));
        proxyFactory.setTargetSource(new SingletonTargetSource(o));
        Object proxy = proxyFactory.getProxy();

        List<NodeRef> nodeRefList = new ArrayList<NodeRef>();
        nodeRefList.add(rootNodeRef);
        nodeRefList.add(systemNodeRef);
        nodeRefList.add(n1);
        nodeRefList.add(n1);

        NodeRef[] nodeRefArray = nodeRefList.toArray(new NodeRef[] {});

        Set<NodeRef> nodeRefSet = new HashSet<NodeRef>();
        nodeRefSet.addAll(nodeRefList);

        List<ChildAssociationRef> carList = new ArrayList<ChildAssociationRef>();
        carList.add(nodeService.getPrimaryParent(rootNodeRef));
        carList.add(nodeService.getPrimaryParent(systemNodeRef));
        carList.add(nodeService.getPrimaryParent(n1));
        carList.add(nodeService.getPrimaryParent(n1));

        ChildAssociationRef[] carArray = carList.toArray(new ChildAssociationRef[] {});

        Set<ChildAssociationRef> carSet = new HashSet<ChildAssociationRef>();
        carSet.addAll(carList);

        ChildAssocRefResultSet rsIn = new ChildAssocRefResultSet(nodeService, nodeRefList, null, false);

        permissionService.setPermission(new SimplePermissionEntry(rootNodeRef, READ, "andy", AccessStatus.ALLOWED));

        assertEquals(4, rsIn.length());
        ResultSet answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(4, answerResultSet.length());
        Collection answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(3, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(3, answerCollection.size());
        Object[] answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(4, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(4, answerArray.length);

        permissionService.setPermission(new SimplePermissionEntry(n1, READ, "andy", AccessStatus.DENIED));

        assertEquals(4, rsIn.length());
        answerResultSet = (ResultSet) methodResultSet.invoke(proxy, new Object[] { rsIn });
        assertEquals(4, answerResultSet.length());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { nodeRefSet });
        assertEquals(3, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carList });
        assertEquals(4, answerCollection.size());
        answerCollection = (Collection) methodCollection.invoke(proxy, new Object[] { carSet });
        assertEquals(3, answerCollection.size());
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { nodeRefArray });
        assertEquals(4, answerArray.length);
        answerArray = (Object[]) methodArray.invoke(proxy, new Object[] { carArray });
        assertEquals(4, answerArray.length);

    }

    public static class ClassWithMethods
    {

        public Object echoObject(Object o)
        {
            return o;
        }

        public StoreRef echoStoreRef(StoreRef storeRef)
        {
            return storeRef;
        }
        
        public NodeRef echoNodeRef(NodeRef nodeRef)
        {
            return nodeRef;
        }

        public ChildAssociationRef echoChildAssocRef(ChildAssociationRef car)
        {
            return car;
        }

        public ResultSet echoResultSet(ResultSet rs)
        {
            return rs;
        }

        public <T> Collection<T> echoCollection(Collection<T> nrc)
        {
            return nrc;
        }

        public <T> T[] echoArray(T[] nra)
        {
            return nra;
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
            ACLEntryAfterInvocationProvider after = new ACLEntryAfterInvocationProvider();
            after.setNamespacePrefixResolver(namespacePrefixResolver);
            after.setPermissionService(permissionService);
            after.setNodeService(nodeService);

            Object returnObject = invocation.proceed();
            return after.decide(null, invocation, cad, returnObject);
        }
    }
}
