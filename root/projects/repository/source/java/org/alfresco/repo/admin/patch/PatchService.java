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
package org.alfresco.repo.admin.patch;

import java.util.Date;
import java.util.List;

/**
 * Manages patches applied against the repository.
 * <p>
 * Patches are injected into this class and any attempted applications are recorded for later auditing.
 * 
 * @since 1.2
 * @author Derek Hulley
 */
public interface PatchService
{
    /**
     * Registers a patch with the service that executes them.
     * 
     * @param patch
     *            the patch to register
     */
    public void registerPatch(Patch patch);

    /**
     * Does some up-front validation on the patches, specifically to see if they all apply to the current server version
     * and not some future version. This is to prevent tampering with versioning information attached to a license.
     * 
     * @return true if validation is successful. Outputs errors and returns false otherwise.
     */
    public boolean validatePatches();

    /**
     * Apply all outstanding patches that are relevant to the repo. If there is a failure, then the patches that were
     * applied will remain so, but the process will not attempt to apply any further patches.
     * 
     * @return Returns true if all outstanding patches were applied, or false if the process was termintated before all
     *         patches could be applied.
     */
    public boolean applyOutstandingPatches();

    /**
     * Retrieves all applied patches between two specific times.
     * 
     * @param from
     *            the start date of the search, or null to get all patches from the start
     * @param to
     *            the end date of the search, or null to g
     * @return Returns all applied patches (successful or not)
     */
    public List<AppliedPatch> getPatches(Date fromDate, Date toDate);
}
