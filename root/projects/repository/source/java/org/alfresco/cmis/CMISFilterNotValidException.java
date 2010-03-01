/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.cmis;

public class CMISFilterNotValidException extends CMISServiceException
{
    private static final long serialVersionUID = -1947056142715192667L;

    public CMISFilterNotValidException(String filter)
    {
        super("Filter not valid: " + filter, "filterNotValid", 400);
    }

    public CMISFilterNotValidException(String filter, Throwable cause)
    {
        super("Filter not valid: " + filter, cause, "filterNotValid", 400);
    }
}
