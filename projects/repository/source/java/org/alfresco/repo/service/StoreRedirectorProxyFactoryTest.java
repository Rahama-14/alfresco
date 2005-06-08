package org.alfresco.repo.service;

import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.StoreRef;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import junit.framework.TestCase;

public class StoreRedirectorProxyFactoryTest extends TestCase
{

    private ApplicationContext factory = null;

    public void setUp()
    {
        factory = new ClassPathXmlApplicationContext("org/alfresco/repo/service/testredirector.xml");
    }

    public void testRedirect()
    {
        StoreRef storeRef1 = new StoreRef("Type1", "id");
        StoreRef storeRef2 = new StoreRef("Type2", "id");
        StoreRef storeRef3 = new StoreRef("Type3", "id");
        StoreRef storeRef4 = new StoreRef("Type3", "woof");
        NodeRef nodeRef1 = new NodeRef(storeRef1, "id");
        NodeRef nodeRef2 = new NodeRef(storeRef2, "id");

        TestServiceInterface service = (TestServiceInterface) factory.getBean("service1");

        String result1 = service.defaultBinding("service1");
        assertEquals("Type1:service1", result1);
        String result2 = service.storeRef(storeRef1);
        assertEquals("Type1:" + storeRef1, result2);
        String result3 = service.storeRef(storeRef2);
        assertEquals("Type2:" + storeRef2, result3);
        String result4 = service.nodeRef(nodeRef1);
        assertEquals("Type1:" + nodeRef1, result4);
        String result5 = service.nodeRef(nodeRef2);
        assertEquals("Type2:" + nodeRef2, result5);
        String result6 = service.multiStoreRef(storeRef1, storeRef1);
        assertEquals("Type1:" + storeRef1 + "," + storeRef1, result6);
        String result7 = service.multiStoreRef(storeRef2, storeRef2);
        assertEquals("Type2:" + storeRef2 + "," + storeRef2, result7);
        String result8 = service.multiNodeRef(nodeRef1, nodeRef1);
        assertEquals("Type1:" + nodeRef1 + "," + nodeRef1, result8);
        String result9 = service.multiNodeRef(nodeRef2, nodeRef2);
        assertEquals("Type2:" + nodeRef2 + "," + nodeRef2, result9);
        String result10 = service.mixedStoreNodeRef(storeRef1, nodeRef1);
        assertEquals("Type1:" + storeRef1 + "," + nodeRef1, result10);
        String result11 = service.mixedStoreNodeRef(storeRef2, nodeRef2);
        assertEquals("Type2:" + storeRef2 + "," + nodeRef2, result11);
        String result12 = service.mixedStoreNodeRef(null, null);
        assertEquals("Type1:null,null", result12);
        String result13 = service.mixedStoreNodeRef(storeRef1, null);
        assertEquals("Type1:" + storeRef1 + ",null", result13);

        // Direct store refs
        String result14 = service.storeRef(storeRef3);
        assertEquals("Type3:" + storeRef3, result14);

        String result15 = service.storeRef(storeRef4);
        assertEquals("Type1:" + storeRef4, result15);

    }

    public void testInvalidArgs()
    {
        StoreRef defaultRef = new StoreRef("Type1", "id");
        StoreRef storeRef1 = new StoreRef("InvalidType1", "id");
        NodeRef nodeRef1 = new NodeRef(storeRef1, "id");

        TestServiceInterface service = (TestServiceInterface) factory.getBean("service1");

        String result1 = service.storeRef(storeRef1);
        assertEquals("Type1:" + storeRef1, result1);

        String result2 = service.nodeRef(nodeRef1);
        assertEquals("Type1:" + nodeRef1, result2);

    }

    public interface TestServiceInterface
    {
        public String defaultBinding(String arg);

        public String storeRef(StoreRef ref1);

        public String nodeRef(NodeRef ref1);

        public String multiStoreRef(StoreRef ref1, StoreRef ref2);

        public String multiNodeRef(NodeRef ref1, NodeRef ref2);

        public String mixedStoreNodeRef(StoreRef ref2, NodeRef ref1);
    }

    public static abstract class Component implements TestServiceInterface
    {
        private String type;

        private Component(String type)
        {
            this.type = type;
        }

        public String defaultBinding(String arg)
        {
            return type + ":" + arg;
        }

        public String nodeRef(NodeRef ref1)
        {
            return type + ":" + ref1;
        }

        public String storeRef(StoreRef ref1)
        {
            return type + ":" + ref1;
        }

        public String multiNodeRef(NodeRef ref1, NodeRef ref2)
        {
            return type + ":" + ref1 + "," + ref2;
        }

        public String multiStoreRef(StoreRef ref1, StoreRef ref2)
        {
            return type + ":" + ref1 + "," + ref2;
        }

        public String mixedStoreNodeRef(StoreRef ref1, NodeRef ref2)
        {
            return type + ":" + ref1 + "," + ref2;
        }
    }

    public static class Type1Component extends Component
    {
        private Type1Component()
        {
            super("Type1");
        }
    }

    public static class Type2Component extends Component
    {
        private Type2Component()
        {
            super("Type2");
        }
    }

    public static class Type3Component extends Component
    {
        private Type3Component()
        {
            super("Type3");
        }
    }

}
