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
package org.alfresco.repo.domain.patch;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.admin.patch.AppliedPatch;

/**
 * Provides data access support for patch persistence in <b>alf_applied_patch</b>.
 * 
 * @since 3.3
 * @author Derek Hulley
 */
public interface AppliedPatchDAO
{
    /**
     * Creates and saves a new instance of the patch.
     * 
     * @param patchInfo         the patch ID and details
     */
    public void createAppliedPatch(AppliedPatch appliedPatch);
    
    public void updateAppliedPatch(AppliedPatch appliedPatch);
    
    /**
     * Retrieve an existing patch
     * 
     * @param id the patch unique ID
     * @return Returns the patch instance or <tt>null</tt> if one has not been persisted
     */
    public AppliedPatch getAppliedPatch(String id);
    
    /**
     * Get a list of all applied patches
     * 
     * @return Returns a list of all applied patches
     */
    public List<AppliedPatch> getAppliedPatches();
    
    /**
     * Get a list of all patches applied between the given dates.
     * 
     * @param from the lower date limit or null to ignore
     * @param to the upper date limit or null to ignore
     * @return Returns applied patches for the date range, but also patches without
     *      a date
     */
    public List<AppliedPatch> getAppliedPatches(Date from, Date to);
    
    /**
     * Update the patch <i>applied on</i> date.
     * 
     * @param id                    the patch ID
     * @param appliedOnDate         the date applied
     */
    public void setAppliedOnDate(String id, Date appliedOnDate);
}
