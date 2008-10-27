package org.alfresco.wcm.webproject;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;


/**
 * Web Project Service fundamental API.
 * <p>
 * This service API is designed to support the public facing Web Project APIs. 
 * 
 * @author janv
 */
public interface WebProjectService
{
    //
    // Web project operations
    //
	
    /**
     * Create a new web project (with a default ROOT webapp)
     * <p>
     * Note: the DNS name will be used to generate the web project store id, which can be subsequently retrieved via WebProjectInfo.getStoreId()
     * 
     * @param dnsName          DNS name (required, must be unique)
     * @param name             name (require, must be unique)
     * @param title            title
     * @param description      description
     * @return WebProjectInfo  the created web project info
     */
    public WebProjectInfo createWebProject(String dnsName, String name, String title, String description);
    
    /**
     * Create a new web project (with a default ROOT webapp)
     * <p>
     * Note: the DNS name will be used to generate the web project store id, which can be subsequently retrieved via WebProjectInfo.getStoreId()
     * 
     * @param dnsName          DNS name (required, must be unique)
     * @param name             name (required, must be unique)
     * @param title            title
     * @param description      description
     * @param sourceNodeRef    web project node ref to branch from (can be null)
     * @return WebProjectInfo  the created web project info
     */
    public WebProjectInfo createWebProject(String dnsName, String name, String title, String description, NodeRef sourceNodeRef);
    
    /**
     * Create a new web project (with given default web app)
     * <p>
     * Note: the DNS name will be used to generate the web project store id, which can be subsequently retrieved via WebProjectInfo.getStoreId()
     * 
     * @param dnsName          DNS name (must be unique)
     * @param name             name (must be unique)
     * @param title            title
     * @param description      description
     * @param defaultWebApp    default webapp (if null, will default to ROOT webapp)
     * @param useAsTemplate    <tt>true</tt> if this web project can be used as a template to branch from
     * @param sourceNodeRef    web project node ref to branch from (can be null)
     * @return WebProjectInfo  the created web project info
     */
    public WebProjectInfo createWebProject(String dnsName, String name, String title, String description, String defaultWebApp, boolean useAsTemplate, NodeRef sourceNodeRef);
    
    /**
     * Returns the Web Projects container
     * 
     * @return NodeRef        the node ref of the "Web Projects" container node
     */
    public NodeRef getWebProjectsRoot();
    
    /**
     * Returns the Web Project for the given AVM path
     * 
     * @param absoluteAVMPath the AVM path from which to determine the Web Project
     * @return NodeRef        the web project node ref for the path or null if it could not be determined
     */
    public NodeRef findWebProjectNodeFromPath(String absoluteAVMPath);
    
    /**
     * Returns the Web Project for the given AVM store name (sandbox store id)
     * 
     * @param storeName       the AVM store name (sandbox store id) from which to determine the Web Project
     * @return NodeRef        the web project node ref for the path or null if it could not be determined
     */
    public NodeRef findWebProjectNodeFromStore(String storeName);
    
    /**
     * List the available web projects for the current user
     * 
     * @return List<WebProjectInfo>  list of web project info
     */
    public List<WebProjectInfo> listWebProjects();
    
    /**
     * List the web projects for the given user (based on the available web projects for the current user)
     * 
     * @param userName               user name
     * @return List<WebProjectInfo>  list of web project info
     */
    public List<WebProjectInfo> listWebProjects(String userName);
    
    /**
     * Return true if web project node ref is a web project
     * 
     * @param wpNodeRef  web project store id
     * @return boolean   true, if web project
     */
    public boolean isWebProject(String wpStoreId);
    
    /**
     * Return true if web project node ref is a web project
     * 
     * @param wpNodeRef  web project node ref
     * @return boolean   true, if web project
     */
    public boolean isWebProject(NodeRef wpNodeRef);
    
    /**
     * Gets web project info based on the store id of a web project
     * <p>
     * Returns null if the web project can not be found
     * 
     * @param wpStoreId        web project store id
     * @return WebProjectInfo  web project info
     */
    public WebProjectInfo getWebProject(String wpStoreId);
    
    /**
     * Gets web project info based on the DM nodeRef of a web project
     * <p>
     * Returns null if the web project can not be found
     * 
     * @param wpNodeRef        web project node ref
     * @return WebProjectInfo  web project info
     */
    public WebProjectInfo getWebProject(NodeRef wpNodeRef);
    
    /**
     * Update the web project info
     * <p>
     * Note: the nodeRef and storeId (dnsName) of a web project cannot be updated once the web project has been created
     * 
     * @param wpInfo  web project info
     */
    public void updateWebProject(WebProjectInfo wpInfo);
    
    /**
     * Delete the web project
     * <p>
     * If the web project does not exist, will log a warning and succeed
     * <p>
     * Current user must be a content manager for the web project
     * 
     * @param name  web project store id
     */
    public void deleteWebProject(String wpStoreId);
    
    /**
     * Delete the web project
     * <p>
     * If the web project does not exist, will log a warning and succeed
     * <p>
     * Current user must be a content manager for the web project
     * <p>
     * Note: this will cascade delete all sandboxes associated with a web project
     * 
     * @param name  web project node ref
     */
    public void deleteWebProject(NodeRef wpNodeRef);
    
    //
    // Web app operations
    //
    
    /**
     * Create webapp for the given web project. 
     * <p>
     * Current user must be a content manager for the web project
     * 
     * @param wpStoreId      web project store id
     * @param name        webapp name (must be unique within a web project)
     * @param description webapp description
     */
    public void createWebApp(String wpStoreId, String name, String description);
    
    /**
     * Create webapp for the given web project. 
     * <p>
     * Current user must be a content manager for the web project
     * 
     * @param wpNodeRef   web project node ref
     * @param name        webapp name (must be unique within a web project)
     * @param description webapp description
     */
    public void createWebApp(NodeRef wpNodeRef, String name, String description);
    
    /**
     * List webapps for the web project
     * 
     * @param wpStoreId      web project store id
     * @return List<String>  list of webapp names
     */
    public List<String> listWebApps(String wpStoreId);
    
    /**
     * List webapps for the web project
     * 
     * @param wpNodeRef      web project node ref
     * @return List<String>  list of webapp names
     */
    public List<String> listWebApps(NodeRef wpNodeRef);
    
    /**
     * Delete webapp from the given web project
     * <p>
     * Current user must be a content manager for the web project
     * <p>
     * Note: this will cascade delete all assets within a webapp
     * 
     * @param wpStoreId   web project store id
     * @param name        webapp name
     */
    public void deleteWebApp(String wpStoreId, String name);
    
    /**
     * Delete webapp from the given web project
     * <p>
     * Current user must be a content manager for the web project
     * <p>
     * Note: this will cascade delete all assets within a webapp
     * 
     * @param wpNodeRef   web project node ref
     * @param name        webapp name
     */
    public void deleteWebApp(NodeRef wpNodeRef, String name);
    
    //
    // Web user operations
    //
    
    /**
     * Returns <tt>true</tt> if the current user is a manager of this web project
     * <p>
     * Note: This includes admin users but does not include the System user
     * 
     * @param wpStoreId   web project store id
     * @return boolean    <tt>true</tt> if the user is a manager (role = WCMUtil.ROLE_CONTENT_MANAGER), <tt>false</tt> otherwise
     */
    public boolean isContentManager(String wpStoreId);
    
    /**
     * Returns <tt>true</tt> if the current user is a manager of this web project
     *
     * @param  wpNodeRef   web project node ref
     * @return boolean     <tt>true</tt> if the user is a manager (role = WCMUtil.ROLE_CONTENT_MANAGER), <tt>false</tt> otherwise
     */
    public boolean isContentManager(NodeRef wpNodeRef);
    
    /**
     * Returns <tt>true</tt> if the user is a manager of this web project
     * <p>
     * Note: This includes admin users but does not include the System user
     * 
     * @param storeName   web project store id
     * @param username    user name
     * @return boolean    <tt>true</tt> if the user is a manager, <tt>false</tt> otherwise
     */
    public boolean isContentManager(String wpStoreId, String username);
    
    /**
     * Returns <tt>true</tt> if the user is a manager of this web project
     * <p>
     * Note: This includes admin users but does not include the System user
     * 
     * @param wpNodeRef    web project node ref
     * @param userName     user name
     * @return boolean     <tt>true</tt> if the user is a manager (role = WCMUtil.ROLE_CONTENT_MANAGER), <tt>false</tt> otherwise
     */
    public boolean isContentManager(NodeRef wpNodeRef, String userName);
    
    /**
     * List the web users of the web project
     * <p>
     * Current user must be a content manager for the web project
     *      
     * @param wpStoreId    web project store id
     * @return Map<String, String> map of <authority name, role name> pairs
     */
    public Map<String, String> listWebUsers(String wpStoreId);
    
    /**
     * List the web users of the web project
     * <p>
     * Current user must be a content manager for the web project
     *      
     * @param wpNodeRef    web project node ref
     * @return Map<String, String> map of <authority name, role name> pairs
     */
    public Map<String, String> listWebUsers(NodeRef wpNodeRef);
    
    /**
     * Get the number of web users invited to this web project
     *      
     * @param wpNodeRef    web project node ref
     * @return int         number of invited web users
     */
    public int getWebUserCount(NodeRef wpNodeRef);
    
    /**
     * Gets the role of the specified user
     * 
     * @param wpStoreId     web project store id
     * @param userName      user name
     * @return String       web project role for this user, null if no assigned role
     */
    public String getWebUserRole(String wpStoreId, String userName);
    
    /**
     * Gets the role of the specified user
     * 
     * @param wpNodeRef     web project node ref
     * @param userName      user name
     * @return String       web project role for this user, null if no assigned role
     */
    public String getWebUserRole(NodeRef wpNodeRef, String userName);
    
    /**
     * Indicates whether given user is a web user of the web project or not
     * 
     * @param store id      web project store id
     * @param userName      user name
     * @return boolean      <tt>true</tt> if the user is a web user of the web project, <tt>false</tt> otherwise
     */
    public boolean isWebUser(String wpStoreId, String userName);
    
    /**
     * Indicates whether given user is a web user of the web project or not
     * 
     * @param wpNodeRef     web project node ref
     * @param userName      user name
     * @return boolean      <tt>true</tt> if the user is a web user of the web project, <tt>false</tt> otherwise
     */
    public boolean isWebUser(NodeRef wpNodeRef, String userName);
    
    /**
     * Invite users/groups to web project
     * <p>
     * Note: authority name can be user or group, although a group is flattened into a set of users
     * 
     * @param wpStoreId        web project store id
     * @param userGroupRoles   map of <authority name, role name> pairs
     */
    public void inviteWebUsersGroups(String wpStoreId, Map<String, String> userGroupRoles);
    
    /**
     * Invite users/groups to web project
     * <p>
     * Note: authority name can be user or group, although a group is flattened into a set of users
     * 
     * @param wpNodeRef        web project node ref
     * @param userGroupRoles   map of <authority name, role name> pairs
     */
    public void inviteWebUsersGroups(NodeRef wpNodeRef, Map<String, String> userGroupRoles);
    
    /**
     * Invite user to web project
     * 
     * @param wpStoreId   web project store id
     * @param userName    user name (not a group)
     * @param userRole    web project role
     */
    public void inviteWebUser(String wpStoreId, String userName, String userRole);
    
    /**
     * Invite user to web project
     * 
     * @param wpNodeRef   web project node ref
     * @param userName    user name (not a group)
     * @param userRole    web project role
     */
    public void inviteWebUser(NodeRef wpNodeRef, String userName, String userRole);
    
    /**
     * Uninvite user from a web project
     * <p>
     * Note: this will cascade delete the user's sandboxes without warning (even if there are modified items)
     * 
     * @param wpStoreId   web project store id
     * @param userName    user name
     */
    public void uninviteWebUser(String wpStoreId, String userName);
    
    /**
     * Uninvite user from a web project
     * <p>
     * Note: this will cascade delete the user's sandboxes without warning (even if there are modified items)
     * 
     * @param wpNodeRef      web project node ref
     * @param userName       user name
     */
    public void uninviteWebUser(NodeRef wpNodeRef, String userName);
}
