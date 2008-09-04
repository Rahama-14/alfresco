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
package org.alfresco.cmis.dictionary;

import java.util.HashMap;
import java.util.Map;


/**
 * The scope for a CMIS name
 * 
 * Alfresco has different name spaces for relationships and objects.
 * This scope resolves any ambiguity.
 * This ambiguity is resolved in the CMIS type id mapping.
 * 
 * @author andyh
 *
 */
public enum CMISScope
{
    OBJECT ('O'), 
    RELATIONSHIP ('R'), 
    DOCUMENT ('D'), 
    FOLDER ('F'),
    POLICY ('P'),
    UNKNOWN ('U');

    private static Map<Character, CMISScope> discriminatorMap = new HashMap<Character, CMISScope>(10);
    static
    {
        for (CMISScope scope : CMISScope.values())
        {
            discriminatorMap.put(scope.discriminator, scope);
        }
    }

    private char discriminator;

    CMISScope(char discriminator)
    {
        this.discriminator = discriminator;
    }
    
    public char discriminator()
    {
        return discriminator;
    }

    public static CMISScope toScope(char discrimator)
    {
        return discriminatorMap.get(discrimator);
    }
    
}
