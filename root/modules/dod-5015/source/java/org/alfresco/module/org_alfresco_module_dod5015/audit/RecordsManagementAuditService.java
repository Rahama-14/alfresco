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
package org.alfresco.module.org_alfresco_module_dod5015.audit;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Records management audit service.
 * 
 * @author Gavin Cornwell
 */
public interface RecordsManagementAuditService
{
    public enum ReportFormat { HTML, JSON }
    
    public static final String RM_AUDIT_APPLICATION_NAME = "DOD5015";
    public static final String RM_AUDIT_PATH_ROOT = "/DOD5015";
    public static final String RM_AUDIT_SNIPPET_EVENT = "/event";
    public static final String RM_AUDIT_SNIPPET_PERSON = "/person";
    public static final String RM_AUDIT_SNIPPET_DESCRIPTION = "/description";
    public static final String RM_AUDIT_SNIPPET_NODE = "/node";
    public static final String RM_AUDIT_SNIPPET_CHANGES = "/changes";
    public static final String RM_AUDIT_SNIPPET_BEFORE = "/before";
    public static final String RM_AUDIT_SNIPPET_AFTER = "/after";

    public static final String RM_AUDIT_DATA_PERSON_FULLNAME = "/DOD5015/event/person/fullName";
    public static final String RM_AUDIT_DATA_PERSON_ROLES = "/DOD5015/event/person/roles";
    public static final String RM_AUDIT_DATA_EVENT_DESCRIPTION = "/DOD5015/event/description/value";
    public static final String RM_AUDIT_DATA_NODE_NODEREF = "/DOD5015/event/node/noderef";
    public static final String RM_AUDIT_DATA_NODE_NAME = "/DOD5015/event/node/name";
    public static final String RM_AUDIT_DATA_NODE_NAMEPATH = "/DOD5015/event/node/namePath";
    public static final String RM_AUDIT_DATA_NODE_CHANGES_BEFORE = "/DOD5015/event/node/changes/before/value";
    public static final String RM_AUDIT_DATA_NODE_CHANGES_AFTER = "/DOD5015/event/node/changes/after/value";
    
    /**
     * Starts RM auditing.
     */
    void start();
    
    /**
     * Stops RM auditing.
     */
    void stop();
    
    /**
     * Clears the RM audit trail.
     */
    void clear();
    
    /**
     * Determines whether the RM audit log is currently enabled.
     * 
     * @return true if RM auditing is active false otherwise
     */
    boolean isEnabled();
    
    /**
     * Returns the date the RM audit was last started.
     * 
     * @return Date the audit was last started
     */
    Date getDateLastStarted();
    
    /**
     * Returns the date the RM audit was last stopped.
     * 
     * @return Date the audit was last stopped
     */
    Date getDateLastStopped();
    
    /**
     * An explicit call that RM actions can make to have the events logged.
     * 
     * @param action                    the action that will be performed
     * @param nodeRef                   the component being acted on
     * @param parameters                the action's parameters
     */
    void auditRMAction(RecordsManagementAction action, NodeRef nodeRef, Map<String, Serializable> parameters);
    
    /**
     * Retrieves a list of audit log entries using the provided parameters
     * represented by the RecordsManagementAuditQueryParameters instance.
     * <p>
     * The parameters are all optional so an empty RecordsManagementAuditQueryParameters
     * object will result in ALL audit log entries for the RM system being
     * returned. Setting the various parameters effectively filters the full
     * audit trail.
     * 
     * @param params        Parameters to use to retrieve audit trail (never <tt>null</tt>)
     * @param format        The format the report should be produced in
     * @return              File containing JSON representation of audit trail
     */
    File getAuditTrailFile(RecordsManagementAuditQueryParameters params, ReportFormat format);
    
    /**
     * Retrieves a list of audit log entries using the provided parameters
     * represented by the RecordsManagementAuditQueryParameters instance.
     * <p>
     * The parameters are all optional so an empty RecordsManagementAuditQueryParameters
     * object will result in ALL audit log entries for the RM system being
     * returned. Setting the various parameters effectively filters the full
     * audit trail.
     * 
     * @param params        Parameters to use to retrieve audit trail (never <tt>null</tt>)
     * @return              All entries for the audit trail
     */
    List<RecordsManagementAuditEntry> getAuditTrail(RecordsManagementAuditQueryParameters params);
    
    /**
     * Retrieves a list of audit log entries using the provided parameters
     * represented by the RecordsManagementAuditQueryParameters instance and
     * then files the resulting log as an undeclared record in the record folder 
     * represented by the given NodeRef.
     * <p>
     * The parameters are all optional so an empty RecordsManagementAuditQueryParameters
     * object will result in ALL audit log entries for the RM system being
     * returned. Setting the various parameters effectively filters the full
     * audit trail.
     * 
     * @param params        Parameters to use to retrieve audit trail (never <tt>null</tt>)
     * @param destination   NodeRef representing a record folder in which to file the audit log
     * @param format        The format the report should be produced in
     * @return              NodeRef of the undeclared record filed
     */
    NodeRef fileAuditTrailAsRecord(RecordsManagementAuditQueryParameters params, 
                NodeRef destination, ReportFormat format);
}
