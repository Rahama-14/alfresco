/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.management;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * A utility class providing a method to dump a local or remote MBeanServer's entire object tree for support purposes.
 * Nested arrays and CompositeData objects in MBean attribute values are handled.
 * 
 * @author dward
 */
public class JmxDumpUtil
{
    /** Table header for attribute names. */
    private static final String NAME_HEADER = "Attribute Name";

    /** Table header for attribute values. */
    private static final String VALUE_HEADER = "Attribute Value";

    /** Place holder for nulls. */
    private static final String NULL_VALUE = "<null>";

    /** Place holder for unreadable values. */
    private static final String UNREADABLE_VALUE = "<not readable>";

    /**
     * Dumps a local or remote MBeanServer's entire object tree for support purposes. Nested arrays and CompositeData
     * objects in MBean attribute values are handled.
     * 
     * @param connection
     *            the server connection (or server itself)
     * @param out
     *            PrintWriter to write the output to
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void dumpConnection(MBeanServerConnection connection, PrintWriter out) throws IOException
    {
        // Get all the object names
        Set<ObjectName> objectNames = connection.queryNames(null, null);

        // Sort the names
        objectNames = new TreeSet<ObjectName>(objectNames);

        // Dump each MBean
        for (ObjectName objectName : objectNames)
        {
            try
            {
                printMBeanInfo(connection, objectName, out);
            }
            catch (JMException e)
            {
                // Sometimes beans can disappear while we are examining them
            }
        }
    }

    /**
     * Dumps the details of a single MBean.
     * 
     * @param connection
     *            the server connection (or server itself)
     * @param objectName
     *            the object name
     * @param out
     *            PrintWriter to write the output to
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws JMException
     *             Signals a JMX error
     */
    private static void printMBeanInfo(MBeanServerConnection connection, ObjectName objectName, PrintWriter out)
            throws IOException, JMException
    {
        Map<String, Object> attributes = new TreeMap<String, Object>();
        MBeanInfo info = connection.getMBeanInfo(objectName);
        attributes.put("** Object Name", objectName.toString());
        attributes.put("** Object Type", info.getClassName());
        for (MBeanAttributeInfo element : info.getAttributes())
        {
            Object value;
            if (element.isReadable())
            {
                try
                {
                    value = connection.getAttribute(objectName, element.getName());
                }
                catch (Exception e)
                {
                    value = JmxDumpUtil.UNREADABLE_VALUE;
                }
            }
            else
            {
                value = JmxDumpUtil.UNREADABLE_VALUE;
            }
            attributes.put(element.getName(), value);
        }
        tabulate(JmxDumpUtil.NAME_HEADER, JmxDumpUtil.VALUE_HEADER, attributes, out, 0);
    }

    /**
     * Dumps the details of a single CompositeData object.
     * 
     * @param composite
     *            the composite object
     * @param out
     *            PrintWriter to write the output to
     * @param nestLevel
     *            the nesting level
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void printCompositeInfo(CompositeData composite, PrintWriter out, int nestLevel) throws IOException
    {
        Map<String, Object> attributes = new TreeMap<String, Object>();
        for (String key : composite.getCompositeType().keySet())
        {
            Object value;
            try
            {
                value = composite.get(key);
            }
            catch (Exception e)
            {
                value = JmxDumpUtil.UNREADABLE_VALUE;
            }
            attributes.put(key, value);
        }
        tabulate(JmxDumpUtil.NAME_HEADER, JmxDumpUtil.VALUE_HEADER, attributes, out, nestLevel);
    }

    /**
     * Tabulates a given String -> Object Map.
     * 
     * @param keyHeader
     *            the key header
     * @param valueHeader
     *            the value header
     * @param rows
     *            Map containing key value pairs forming the rows
     * @param out
     *            PrintWriter to write the output to
     * @param nestLevel
     *            the nesting level
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void tabulate(String keyHeader, String valueHeader, Map<String, Object> rows, PrintWriter out,
            int nestLevel) throws IOException
    {
        if (rows.isEmpty())
        {
            return;
        }
        // Calculate column lengths
        int maxKeyLength = keyHeader.length(), maxValLength = valueHeader.length();
        for (Map.Entry<String, Object> entry : rows.entrySet())
        {
            maxKeyLength = Math.max(maxKeyLength, entry.getKey().length());
            maxValLength = Math.max(maxValLength, getValueLength(entry.getValue()));
        }
        // Output Header
        outputRow(out, maxKeyLength, keyHeader, valueHeader, nestLevel);
        indent(out, nestLevel);
        for (int col = 0; col < maxKeyLength; col++)
        {
            out.print('-');
        }
        out.print(' ');
        for (int col = 0; col < maxValLength; col++)
        {
            out.print('-');
        }
        out.println();

        // Output Body
        for (Map.Entry<String, Object> entry : rows.entrySet())
        {
            outputRow(out, maxKeyLength, entry.getKey(), entry.getValue(), nestLevel);
        }
        out.println();
    }

    /**
     * Outputs spaces in the left hand margin appropriate for the given nesting level.
     * 
     * @param out
     *            PrintWriter to write the output to
     * @param nestLevel
     *            the nesting level
     */
    private static void indent(PrintWriter out, int nestLevel)
    {
        int size = nestLevel * 3;
        for (int i = 0; i < size; i++)
        {
            out.print(' ');
        }
    }

    /**
     * Outputs a single row in a two-column table. The first column is padded with spaces so that the second column is
     * aligned.
     * 
     * @param out
     *            PrintWriter to write the output to
     * @param maxKeyLength
     *            maximum number of characters in the first column
     * @param key
     *            the first column value
     * @param value
     *            the second column value
     * @param nestLevel
     *            the nesting level
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void outputRow(PrintWriter out, int maxKeyLength, String key, Object value, int nestLevel)
            throws IOException
    {
        indent(out, nestLevel);
        out.print(key);
        for (int i = key.length() - 1; i < maxKeyLength; i++)
        {
            out.print(' ');
        }
        outputValue(out, value, nestLevel);
    }

    /**
     * Outputs a single value, dealing with nested arrays and CompositeData objects.
     * 
     * @param out
     *            PrintWriter to write the output to
     * @param value
     *            the value to output
     * @param nestLevel
     *            the nesting level
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void outputValue(PrintWriter out, Object value, int nestLevel) throws IOException
    {
        if (value == null)
        {
            out.println(JmxDumpUtil.NULL_VALUE);
        }
        else if (value.getClass().isArray())
        {
            int length = Array.getLength(value);
            if (length == 0)
            {
                out.println("[]");
            }
            else
            {
                out.println();
                indent(out, nestLevel + 1);
                out.println('[');
                for (int i = 0; i < length; i++)
                {
                    indent(out, nestLevel + 2);
                    outputValue(out, Array.get(value, i), nestLevel + 2);
                    if (i + 1 < length)
                    {
                        indent(out, nestLevel + 1);
                        out.println(',');
                    }
                }
                indent(out, nestLevel + 1);
                out.println(']');
            }
        }
        else if (value instanceof CompositeData)
        {
            out.println();
            indent(out, nestLevel + 1);
            out.println('[');
            printCompositeInfo((CompositeData) value, out, nestLevel + 2);
            indent(out, nestLevel + 1);
            out.println(']');
        }
        else
        {
            out.println(value.toString());
        }
    }

    /**
     * Gets the number of characters required to encode a value.
     * 
     * @param value
     *            the value to be encoded
     * @return the number of characters
     */
    private static int getValueLength(Object value)
    {
        if (value == null)
        {
            return JmxDumpUtil.NULL_VALUE.length();
        }
        else if (value.getClass().isArray() || value instanceof CompositeData)
        {
            // We continue arrays and composites on a new line
            return 0;
        }
        else
        {
            return value.toString().length();
        }
    }
}
