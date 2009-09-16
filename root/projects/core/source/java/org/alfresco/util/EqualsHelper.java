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
package org.alfresco.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing helper methods for various types of <code>equals</code> functionality
 * 
 * @author Derek Hulley
 */
public class EqualsHelper
{
    /**
     * Performs an equality check <code>left.equals(right)</code> after checking for null values
     * 
     * @param left the Object appearing in the left side of an <code>equals</code> statement
     * @param right the Object appearing in the right side of an <code>equals</code> statement
     * @return Return true or false even if one or both of the objects are null
     */
	public static boolean nullSafeEquals(Object left, Object right)
    {
        return (left == right) || (left != null && right != null && left.equals(right));
    }
	/**
	 * Performs an case-sensitive or case-insensitive equality check after checking for null values
	 * @param ignoreCase           <tt>true</tt> to ignore case
	 */
    public static boolean nullSafeEquals(String left, String right, boolean ignoreCase)
    {
        if (ignoreCase)
        {
            return (left == right) || (left != null && right != null && left.equalsIgnoreCase(right));
        }
        else
        {
            return (left == right) || (left != null && right != null && left.equals(right));
        }
    }
    
    /**
     * Compare two maps and generate a difference report between the actual and expected values.
     * This method is particularly useful during unit tests as the result (if not <tt>null</tt>)
     * can be appended to a failure message.
     * 
     * @param actual                the map in hand
     * @param expected              the map expected
     * @return                      Returns a difference report or <tt>null</tt> if there were no
     *                              differences.  The message starts with a new line and it neatly
     *                              formatted.
     */
    public static String getMapDifferenceReport(Map<?, ?> actual, Map<?, ?> expected)
    {
        Map<?, ?> copyResult = new HashMap<Object, Object>(actual);
        
        boolean failure = false;

        StringBuilder sb = new StringBuilder(1024);
        sb.append("\nValues that don't match the expected values: ");
        for (Map.Entry<?, ?> entry : expected.entrySet())
        {
            Object key = entry.getKey();
            Object expectedValue = entry.getValue();
            Object resultValue = actual.get(key);
            if (!EqualsHelper.nullSafeEquals(resultValue, expectedValue))
            {
                sb.append("\n")
                  .append("   Key: ").append(key).append("\n")
                  .append("      Result:   ").append(resultValue).append("\n")
                  .append("      Expected: ").append(expectedValue);
                failure = true;
            }
            copyResult.remove(key);
        }
        sb.append("\nValues that are present but should not be: ");
        for (Map.Entry<?, ?> entry : copyResult.entrySet())
        {
            Object key = entry.getKey();
            Object resultValue = entry.getValue();
            sb.append("\n")
              .append("   Key: ").append(key).append("\n")
              .append("      Result:   ").append(resultValue);
          failure = true;
        }
        if (failure)
        {
            return sb.toString();
        }
        else
        {
            return null;
        }
    }
}
