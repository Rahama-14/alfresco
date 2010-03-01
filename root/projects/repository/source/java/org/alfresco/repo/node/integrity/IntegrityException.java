/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.node.integrity;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;

/**
 * Thrown when an integrity check fails
 * 
 * @author Derek Hulley
 */
public class IntegrityException extends AlfrescoRuntimeException
{
    private static final long serialVersionUID = -5036557255854195669L;

    private List<IntegrityRecord> records;
    
    public IntegrityException(List<IntegrityRecord> records)
    {
        super("Integrity failure");
        this.records = records;
    }

    public IntegrityException(String msg, List<IntegrityRecord> records)
    {
        super(msg);
        this.records = records;
    }

    /**
     * @return Returns a list of all the integrity violations
     */
    public List<IntegrityRecord> getRecords()
    {
        return records;
    }
}
