/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 */
package org.alfresco.repo.security.authentication.ldap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ExportSource;
import org.alfresco.repo.importer.ExportSourceImporterException;
import org.alfresco.service.Managed;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class LDAPPersonExportSource implements ExportSource
{
    private static Log s_logger = LogFactory.getLog(LDAPPersonExportSource.class);

    private String personQuery = "(objectclass=inetOrgPerson)";

    private String searchBase;

    private String userIdAttributeName;

    private LDAPInitialDirContextFactory ldapInitialContextFactory;

    private PersonService personService;

    private Map<String, String> attributeMapping;

    private NamespaceService namespaceService;

    private Map<String, String> attributeDefaults;

    private boolean errorOnMissingUID;

    public LDAPPersonExportSource()
    {
        super();
    }

    @Managed(category="Security")
    public void setPersonQuery(String personQuery)
    {
        this.personQuery = personQuery;
    }

    @Managed(category="Security")
    public void setSearchBase(String searchBase)
    {
        this.searchBase = searchBase;
    }

    @Managed(category="Security")
    public void setUserIdAttributeName(String userIdAttributeName)
    {
        this.userIdAttributeName = userIdAttributeName;
    }

    @Managed(category="Security")
    public void setLDAPInitialDirContextFactory(LDAPInitialDirContextFactory ldapInitialDirContextFactory)
    {
        this.ldapInitialContextFactory = ldapInitialDirContextFactory;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    @Managed(category="Security")
    public void setAttributeDefaults(Map<String, String> attributeDefaults)
    {
        this.attributeDefaults = attributeDefaults;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    @Managed(category="Security")
    public void setAttributeMapping(Map<String, String> attributeMapping)
    {
        this.attributeMapping = attributeMapping;
    }

    @Managed(category="Security")
    public void setErrorOnMissingUID(boolean errorOnMissingUID)
    {
        this.errorOnMissingUID = errorOnMissingUID;
    }

    public void generateExport(XMLWriter writer)
    {
        QName nodeUUID = QName.createQName("sys:node-uuid", namespaceService);

        Collection<String> prefixes = namespaceService.getPrefixes();
        QName childQName = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, "childName", namespaceService);

        try
        {
            writer.startDocument();

            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    String uri = namespaceService.getNamespaceURI(prefix);
                    writer.startPrefixMapping(prefix, uri);
                }
            }

            writer.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, "view",
                    NamespaceService.REPOSITORY_VIEW_PREFIX + ":" + "view", new AttributesImpl());

            HashSet<String> uids = new HashSet<String>();

            InitialDirContext ctx = null;
            try
            {
                ctx = ldapInitialContextFactory.getDefaultIntialDirContext();

                // Authentication has been successful.
                // Set the current user, they are now authenticated.

                SearchControls userSearchCtls = new SearchControls();
                userSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                userSearchCtls.setCountLimit(Integer.MAX_VALUE);

                NamingEnumeration searchResults = ctx.search(searchBase, personQuery, userSearchCtls);
                RESULT_LOOP: while (searchResults.hasMoreElements())
                {
                    SearchResult result = (SearchResult) searchResults.next();
                    Attributes attributes = result.getAttributes();
                    Attribute uidAttribute = attributes.get(userIdAttributeName);
                    if (uidAttribute == null)
                    {
                        if (errorOnMissingUID)
                        {
                            throw new ExportSourceImporterException(
                                    "User returned by user search does not have mandatory user id attribute "
                                            + attributes);
                        }
                        else
                        {
                            s_logger.warn("User returned by user search does not have mandatory user id attribute "
                                    + attributes);
                            continue RESULT_LOOP;
                        }
                    }
                    String uid = (String) uidAttribute.get(0);

                    if (uids.contains(uid))
                    {
                        s_logger.warn("Duplicate uid found - there will be more than one person object for this user - "
                                + uid);
                    }

                    uids.add(uid);

                    if (s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Adding user for " + uid);
                    }

                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, childQName.getLocalName(), childQName
                            .toPrefixString(), null, QName.createQName("cm", uid, namespaceService).toPrefixString(namespaceService));

                    writer.startElement(ContentModel.TYPE_PERSON.getNamespaceURI(), ContentModel.TYPE_PERSON
                            .getLocalName(), ContentModel.TYPE_PERSON.toPrefixString(namespaceService), attrs);

                    // permissions

                    // owner

                    writer.startElement(ContentModel.ASPECT_OWNABLE.getNamespaceURI(), ContentModel.ASPECT_OWNABLE
                            .getLocalName(), ContentModel.ASPECT_OWNABLE.toPrefixString(namespaceService),
                            new AttributesImpl());

                    writer.endElement(ContentModel.ASPECT_OWNABLE.getNamespaceURI(), ContentModel.ASPECT_OWNABLE
                            .getLocalName(), ContentModel.ASPECT_OWNABLE.toPrefixString(namespaceService));

                    writer.startElement(ContentModel.PROP_OWNER.getNamespaceURI(), ContentModel.PROP_OWNER
                            .getLocalName(), ContentModel.PROP_OWNER.toPrefixString(namespaceService),
                            new AttributesImpl());

                    writer.characters(uid.toCharArray(), 0, uid.length());

                    writer.endElement(ContentModel.PROP_OWNER.getNamespaceURI(),
                            ContentModel.PROP_OWNER.getLocalName(), ContentModel.PROP_OWNER
                                    .toPrefixString(namespaceService));

                    for (String key : attributeMapping.keySet())
                    {
                        QName keyQName = QName.createQName(key, namespaceService);

                        writer.startElement(keyQName.getNamespaceURI(), keyQName.getLocalName(), keyQName
                                .toPrefixString(namespaceService), new AttributesImpl());

                        // cater for null
                        String attributeName = attributeMapping.get(key);
                        if (attributeName != null)
                        {
                            Attribute attribute = attributes.get(attributeName);
                            if (attribute != null)
                            {
                                String value = (String) attribute.get(0);
                                if (value != null)
                                {
                                    writer.characters(value.toCharArray(), 0, value.length());
                                }
                            }
                            else
                            {
                                String defaultValue = attributeDefaults.get(key);
                                if (defaultValue != null)
                                {
                                    writer.characters(defaultValue.toCharArray(), 0, defaultValue.length());
                                }
                            }
                        }
                        else
                        {
                            String defaultValue = attributeDefaults.get(key);
                            if (defaultValue != null)
                            {
                                writer.characters(defaultValue.toCharArray(), 0, defaultValue.length());
                            }
                        }

                        writer.endElement(keyQName.getNamespaceURI(), keyQName.getLocalName(), keyQName
                                .toPrefixString(namespaceService));
                    }

                    if (personService.personExists(uid))
                    {
                        String uguid = personService.getPerson(uid).getId();

                        writer.startElement(nodeUUID.getNamespaceURI(), nodeUUID.getLocalName(), nodeUUID
                                .toPrefixString(namespaceService), new AttributesImpl());

                        writer.characters(uguid.toCharArray(), 0, uguid.length());

                        writer.endElement(nodeUUID.getNamespaceURI(), nodeUUID.getLocalName(), nodeUUID
                                .toPrefixString(namespaceService));
                    }
                    writer.endElement(ContentModel.TYPE_PERSON.getNamespaceURI(), ContentModel.TYPE_PERSON
                            .getLocalName(), ContentModel.TYPE_PERSON.toPrefixString(namespaceService));

                }

            }
            catch (NamingException e)
            {
                throw new ExportSourceImporterException("Failed to import people.", e);
            }
            finally
            {
                if (ctx != null)
                {
                    try
                    {
                        ctx.close();
                    }
                    catch (NamingException e)
                    {
                        throw new ExportSourceImporterException("Failed to import people.", e);
                    }
                }
            }

            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    writer.endPrefixMapping(prefix);
                }
            }

            writer.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, "view", NamespaceService.REPOSITORY_VIEW_PREFIX
                    + ":" + "view");

            writer.endDocument();
        }
        catch (SAXException e)
        {
            throw new ExportSourceImporterException("Failed to create file for import.", e);
        }
    }

    public static void main(String[] args) throws Exception
    {
        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
        ExportSource source = (ExportSource) ctx.getBean("ldapPeopleExportSource");
        TransactionService txs = (TransactionService) ctx.getBean("transactionComponent");
        UserTransaction tx = txs.getUserTransaction();
        tx.begin();

        File file = new File(args[0]);
        Writer writer = new BufferedWriter(new FileWriter(file));
        XMLWriter xmlWriter = createXMLExporter(writer);
        source.generateExport(xmlWriter);
        xmlWriter.close();

        tx.commit();
    }

    private static XMLWriter createXMLExporter(Writer writer)
    {
        // Define output format
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setIndentSize(3);
        format.setEncoding("UTF-8");

        // Construct an XML Exporter

        XMLWriter xmlWriter = new XMLWriter(writer, format);
        return xmlWriter;
    }
}