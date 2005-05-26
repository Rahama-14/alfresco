/*
 * Created on 23-May-2005
 *
 * TODO Comment this class
 * 
 * 
 */
package org.alfresco.repo.search;

import org.alfresco.repo.dictionary.DictionaryService;
import org.alfresco.repo.dictionary.NamespaceService;
import org.alfresco.repo.ref.QName;
import org.dom4j.Element;
import org.dom4j.Namespace;

public class QueryParameterRefImpl implements NamedQueryParameterDefinition
{

    private static final org.dom4j.QName ELEMENT_QNAME = new org.dom4j.QName("parameter-ref", new Namespace(NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI));

    private static final org.dom4j.QName DEF_QNAME = new org.dom4j.QName("qname", new Namespace(NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI));
    
    private QName qName;
    
    private QueryCollection container;
    
    public QueryParameterRefImpl(QName qName, QueryCollection container)
    {
        super();
        this.qName = qName;
        this.container = container;
    }
 
    public QName getQName()
    {
        return qName;
    }
    
    public static NamedQueryParameterDefinition createParameterReference(Element element, DictionaryService dictionaryService, QueryCollection container)
    {
       
        if (element.getQName().getName().equals(ELEMENT_QNAME.getName()))
        {
            QName qName = null;
            Element qNameElement = element.element(DEF_QNAME.getName());
            if(qNameElement != null)
            {
               qName = QName.createQName(qNameElement.getText(), container.getNamespacePrefixResolver());
            }
            
            return new QueryParameterRefImpl(qName, container);
        }
        else
        {
            return null;
        }
    }

    public static org.dom4j.QName getElementQName()
    {
        return ELEMENT_QNAME;
    }

    public QueryParameterDefinition getQueryParameterDefinition()
    {
        return container.getParameterDefinition(getQName());
    }

}
