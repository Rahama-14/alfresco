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
package org.alfresco.repo.domain.patch;

import org.alfresco.repo.admin.patch.AppliedPatch;

/**
 * Entity for <b>alf_applied_patch</b> persistence. 
 * 
 * @author Derek Hulley
 * @since 3.3
 */
public class AppliedPatchEntity extends AppliedPatch
{
    public AppliedPatchEntity()
    {
        super();
    }
    
    public AppliedPatchEntity(AppliedPatch appliedPatch)
    {
        super(appliedPatch);
    }
}
