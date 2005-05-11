package org.alfresco.repo.ref;

import junit.framework.TestCase;

/**
 * @see org.alfresco.repo.ref.Path
 * 
 * @author Derek Hulley
 */
public class PathTest extends TestCase
{
    private Path absolutePath;
    private Path relativePath;
    private QName qname;
    private StoreRef storeRef;
    private NodeRef parentRef;
    private NodeRef childRef;
    
    public PathTest(String name)
    {
        super(name);
    }
    
    public void setUp() throws Exception
    {
        super.setUp();
        absolutePath = new Path();
        relativePath = new Path();
        qname = QName.createQName("http://www.google.com", "documentx");
        storeRef = new StoreRef("x", "y");
        parentRef = new NodeRef(storeRef, "P");
        childRef = new NodeRef(storeRef, "C");
    }
    
    public void testQNameElement() throws Exception
    {
        // plain
        Path.Element element = new Path.ChildAssocElement(new ChildAssocRef(parentRef, qname, childRef));
        assertEquals("Element string incorrect",
                qname.toString(),
                element.getElementString());
        // sibling
        element = new Path.ChildAssocElement(new ChildAssocRef(parentRef, qname, childRef, true, 5));
        assertEquals("Element string incorrect", "{http://www.google.com}documentx[5]", element.getElementString());
    }
    
    public void testElementTypes() throws Exception
    {
        Path.Element element = new Path.DescendentOrSelfElement();
        assertEquals("DescendentOrSelf element incorrect",
                "descendant-or-self::node()",
                element.getElementString());
        
        element = new Path.ParentElement();
        assertEquals("Parent element incorrect", "..", element.getElementString());
        
        element = new Path.SelfElement();
        assertEquals("Self element incorrect", ".", element.getElementString());
    }
    
    public void testAppendingAndPrepending() throws Exception
    {
        Path.Element element0 = new Path.ChildAssocElement(new ChildAssocRef(null, null, parentRef));
        Path.Element element1 = new Path.ChildAssocElement(new ChildAssocRef(parentRef, qname, childRef, true, 4));
        Path.Element element2 = new Path.DescendentOrSelfElement();
        Path.Element element3 = new Path.ParentElement();
        Path.Element element4 = new Path.SelfElement();
        // append them all to the path
        absolutePath.append(element0).append(element1).append(element2).append(element3).append(element4);
        relativePath.append(element1).append(element2).append(element3).append(element4);
        // check
        assertEquals("Path appending didn't work",
                "/{http://www.google.com}documentx[4]/descendant-or-self::node()/../.",
                absolutePath.toString());
        
        // copy the path
        Path copy = new Path();
        copy.append(relativePath).append(relativePath);
        // check
        assertEquals("Path appending didn't work",
                relativePath.toString() + "/" + relativePath.toString(),
                copy.toString());
        
        // prepend
        relativePath.prepend(element2);
        // check
        assertEquals("Prepending didn't work",
                "descendant-or-self::node()/{http://www.google.com}documentx[4]/descendant-or-self::node()/../.",
                relativePath.toString());
    }
}