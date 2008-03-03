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
package org.alfresco.web.bean.wcm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.DNSNameMangler;
import org.alfresco.util.ExpiringValueCache;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.alfresco.web.bean.wizard.BaseInviteUsersWizard.UserGroupRole;
import org.alfresco.web.forms.Form;
import org.alfresco.web.forms.FormNotFoundException;
import org.alfresco.web.forms.FormsService;
import org.alfresco.web.forms.RenderingEngineTemplate;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIListItem;
import org.alfresco.web.ui.common.component.UISelectList;
import org.alfresco.web.ui.wcm.WebResources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Backing bean for the Create Web Project wizard.
 * 
 * @author Kevin Roast
 */
public class CreateWebsiteWizard extends BaseWizardBean
{
   private static final long serialVersionUID = 6480869380508635173L;

   private static final String MSG_USERROLES = "create_website_summary_users";
   
   private static final String COMPONENT_FORMLIST = "form-list";
   private static final String COMPONENT_WORKFLOWLIST = "workflow-list";
   
   // wizard step names (that are referenced)
   private static final String STEP_DETAILS = "details";
   private static final String STEP_DEPLOYMENT = "deployment";
   
   private static final String MATCH_DEFAULT = ".*";

   private static final String WEBAPP_DEFAULT = "ROOT";
   
   // Create From drop-down control selection values
   private static final String CREATE_EMPTY    = "empty";
   private static final String CREATE_EXISTING = "existing";
   
   protected final static Log logger = LogFactory.getLog(CreateWebsiteWizard.class);
   
   protected boolean editMode = false;
   protected String dnsName;
   
   protected String title;
   protected String name;
   protected String description;
   protected String webapp = WEBAPP_DEFAULT;
   protected String createFrom = null;
   protected String[] sourceWebProject = null;
   protected ExpiringValueCache<List<UIListItem>> webProjectsList;
   protected boolean isSource;
   protected boolean showAllSourceProjects;
   
   transient private AVMService avmService;
   transient private WorkflowService workflowService;
   transient private PersonService personService;
   transient private AVMLockingService avmLockingService;
   transient private FormsService formsService;
   
   /** set true when an option in the Create From screen is changed - this is used as an
       indicator to reload the wizard data model from the selected source web project */
   private boolean createFromValueChanged;
   
   /** datamodel for table of selected forms */
   transient private DataModel formsDataModel = null;
   
   /** transient list of form UIListItem objects */
   protected List<UIListItem> formsList = null;
   
   /** list of form wrapper objects */
   protected List<FormWrapper> forms = null;
   
   /** Current form for dialog context */
   protected FormWrapper actionForm = null;
   
   /** datamodel for table of selected workflows */
   transient private DataModel workflowsDataModel = null;
   
   /** list of workflow wrapper objects */
   protected List<WorkflowWrapper> workflows = null;
   
   /** Current workflow for dialog context */
   protected WorkflowConfiguration actionWorkflow = null;
   
   /** Map and list of deployment servers */
   protected Map<String, DeploymentServerConfig> deployServersMap = null;
   protected List<DeploymentServerConfig> deployServersList = null;
   
   /** Current state of deploy server editing */
   protected DeploymentServerConfig currentDeployServer = null;
   protected Map<String, Object> editedDeployServerProps = null;
   protected boolean inAddDeployServerMode = false;
   protected String addDeployServerType = WCMAppModel.CONSTRAINT_FILEDEPLOY;
   
   /** Data for virtualization server notification  */
   private SandboxInfo sandboxInfo;
   
   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   /**
    * Initialises the wizard
    */
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      this.name = null;
      this.dnsName = null;
      this.title = null;
      this.description = null;
      this.isSource = false;
      clearFormsWorkflowsDeploymentAndUsers();
      this.createFrom = CREATE_EMPTY;
      // requry existing web projects list every 10 seconds
      this.webProjectsList = new ExpiringValueCache<List<UIListItem>>(1000L*10L);
      this.sourceWebProject = null;
      this.createFromValueChanged = false;
      this.showAllSourceProjects = false;
   }

   private void clearFormsWorkflowsDeploymentAndUsers()
   {
      this.formsDataModel = null;
      this.forms = new ArrayList<FormWrapper>(8);
      this.workflowsDataModel = null;
      this.workflows = new ArrayList<WorkflowWrapper>(4);
      
      // reset all deployment data
      this.deployServersMap = new HashMap<String, DeploymentServerConfig>(4, 1.0f);
      this.deployServersList = new ArrayList<DeploymentServerConfig>(4);
      this.currentDeployServer = null;
      this.editedDeployServerProps = new HashMap<String, Object>(12, 1.0f);
      this.inAddDeployServerMode = false;
      this.addDeployServerType = WCMAppModel.CONSTRAINT_FILEDEPLOY;
      
      // init the dependant bean we are using for the invite users pages
      InviteWebsiteUsersWizard wiz = getInviteUsersWizard();
      wiz.init(null);
   }
   
   /**
    * @see org.alfresco.web.bean.dialog.BaseDialogBean#finishImpl(javax.faces.context.FacesContext, java.lang.String)
    */
   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      // the Finish button can be pressed early in the steps - ensure the model is up-to-date
      updateModelOnCreateFromChange();
      
      // create the website space in the correct parent folder
      final NodeRef websiteParent = WebProject.getWebsitesFolder();
      
      FileInfo fileInfo = this.getFileFolderService().create(
            websiteParent,
            this.name,
            WCMAppModel.TYPE_AVMWEBFOLDER);
      NodeRef nodeRef = fileInfo.getNodeRef();
      
      if (logger.isDebugEnabled())
         logger.debug("Created website folder node with name: " + this.name);
      
      // TODO: check that this dns is unique by querying existing store properties for a match
      String avmStore = DNSNameMangler.MakeDNSName(this.dnsName);
      
      // apply the uifacets aspect - icon, title and description props
      Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(4);
      uiFacetsProps.put(ApplicationModel.PROP_ICON, AVMUtil.SPACE_ICON_WEBSITE);
      uiFacetsProps.put(ContentModel.PROP_TITLE, this.title);
      uiFacetsProps.put(ContentModel.PROP_DESCRIPTION, this.description);
      getNodeService().addAspect(nodeRef, ApplicationModel.ASPECT_UIFACETS, uiFacetsProps);
      
      // use as template source flag
      getNodeService().setProperty(nodeRef, WCMAppModel.PROP_ISSOURCE, this.isSource);
      
      // set the default webapp name for the project
      String webapp = (this.webapp != null && this.webapp.length() != 0) ? this.webapp : WEBAPP_DEFAULT;
      getNodeService().setProperty(nodeRef, WCMAppModel.PROP_DEFAULTWEBAPP, webapp);
      
      // call a delegate wizard bean to provide invite user functionality
      InviteWebsiteUsersWizard wiz = getInviteUsersWizard();
      wiz.reset();
      wiz.setNode(new Node(nodeRef));
      wiz.setAvmStore(avmStore);
      wiz.setStandalone(false);
      // the wizard is responsible for notifying the invited users, setting the appropriate
      // node permissions and also for creating user sandboxes and associations to the web folder node
      outcome = wiz.finish();
      if (outcome != null)
      {
         // if the user selected Create From existing web project we will branch from it
         String branchStoreId = null;
         if (CREATE_EXISTING.equals(this.createFrom) && 
             (this.sourceWebProject != null && this.sourceWebProject.length != 0))
         {
            NodeRef sourceNodeRef = new NodeRef(this.sourceWebProject[0]);
            branchStoreId = (String)getNodeService().getProperty(sourceNodeRef, WCMAppModel.PROP_AVMSTORE);
         }
         
         // create the AVM staging store to represent the newly created location website
         this.sandboxInfo =  SandboxFactory.createStagingSandbox(avmStore, nodeRef, branchStoreId);
         
         // create the default webapp folder under the hidden system folders
         if (branchStoreId == null)
         {
            String stagingStore = AVMUtil.buildStagingStoreName(avmStore);
            String stagingStoreRoot = AVMUtil.buildSandboxRootPath(stagingStore);
            getAvmService().createDirectory(stagingStoreRoot, webapp);
            getAvmService().addAspect(AVMNodeConverter.ExtendAVMPath(stagingStoreRoot, webapp),
                                      WCMAppModel.ASPECT_WEBAPP);
         }
         
         // set the property on the node to reference the root AVM store
         getNodeService().setProperty(nodeRef, WCMAppModel.PROP_AVMSTORE, avmStore);
         
         // persist the forms, templates, workflows, workflow defaults and deployment 
         // config to the model for this web project
         saveWebProjectModel(nodeRef);
         
         // navigate to the Websites folder so we can see the newly created folder
         this.navigator.setCurrentNodeId(websiteParent.getId());
         
         // inform the locking service about this new instance
         this.getAvmLockingService().addWebProject(avmStore);
         
         outcome = AlfrescoNavigationHandler.CLOSE_WIZARD_OUTCOME;

         // Snapshot the store with the empty webapp
         getAvmService().createSnapshot( avmStore, null, null);
      }
      return outcome;
   }

   /**
    * @see org.alfresco.web.bean.dialog.BaseDialogBean#doPostCommitProcessing(javax.faces.context.FacesContext, java.lang.String)
    */
   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      if (this.sandboxInfo != null)
      {
         // update the virtualisation server with the default ROOT webapp path
         // performed after the main txn has committed successfully
         String newStoreName = AVMUtil.buildStagingStoreName(sandboxInfo.getMainStoreName());
         
         String path = AVMUtil.buildStoreWebappPath(newStoreName, WEBAPP_DEFAULT);
         
         AVMUtil.updateVServerWebapp(path, true);
      }
      return outcome;
   }
   
   @Override
   public boolean getFinishButtonDisabled()
   {
      // allow finish from any step other than the initial details page
      String stepName = Application.getWizardManager().getCurrentStepName();
      return (STEP_DETAILS.equals(stepName));
   }
   
   /**
    * Persist the forms, templates, workflows, workflow defaults and deployment config
    * to the model for this web project
    * 
    * @param nodeRef        NodeRef to the web project
    */
   protected void saveWebProjectModel(NodeRef nodeRef)
   {
      Map<QName, Serializable> props = new HashMap<QName, Serializable>(4, 1.0f);
      
      // first walk each form object, saving templates and workflow defaults for each
      for (FormWrapper form : this.forms)
      {
         // create web form with name as per the name of the form object in the DD
         props.put(WCMAppModel.PROP_FORMNAME, form.getName());
         NodeRef formRef = getNodeService().createNode(nodeRef,
                                                       WCMAppModel.ASSOC_WEBFORM,
                                                       WCMAppModel.ASSOC_WEBFORM,
                                                       WCMAppModel.TYPE_WEBFORM,
                                                       props).getChildRef();
         
         // add title aspect for user defined title and description labels
         props.clear();
         props.put(ContentModel.PROP_TITLE, form.getTitle());
         props.put(ContentModel.PROP_DESCRIPTION, form.getDescription());
         getNodeService().addAspect(formRef, ContentModel.ASPECT_TITLED, props);
         
         // add filename pattern aspect if a filename pattern has been applied
         if (form.getOutputPathPattern() != null)
         {
            props.clear();
            props.put(WCMAppModel.PROP_OUTPUT_PATH_PATTERN, form.getOutputPathPattern());
            getNodeService().addAspect(formRef, WCMAppModel.ASPECT_OUTPUT_PATH_PATTERN, props);
         }
         
         // associate to workflow defaults if any are present
         if (form.getWorkflow() != null)
         {
            WorkflowWrapper workflow = form.getWorkflow();
            props.clear();
            props.put(WCMAppModel.PROP_WORKFLOW_NAME, workflow.getName());
            NodeRef workflowRef = getNodeService().createNode(formRef,
                                                              WCMAppModel.ASSOC_WORKFLOWDEFAULTS,
                                                              WCMAppModel.ASSOC_WORKFLOWDEFAULTS,
                                                              WCMAppModel.TYPE_WORKFLOW_DEFAULTS,
                                                              props).getChildRef();
            
            // persist workflow default params
            if (workflow.getParams() != null)
            {
               AVMWorkflowUtil.serializeWorkflowParams((Serializable)workflow.getParams(), workflowRef);
            }
         }
         
         // associate to a web form template for each template applied to the form
         for (PresentationTemplate template : form.getTemplates())
         {
            props.clear();
            props.put(WCMAppModel.PROP_BASE_RENDERING_ENGINE_TEMPLATE_NAME, 
                      template.getRenderingEngineTemplate().getName());
            NodeRef templateRef = getNodeService().createNode(formRef,
                                                              WCMAppModel.ASSOC_WEBFORMTEMPLATE,
                                                              WCMAppModel.ASSOC_WEBFORMTEMPLATE,
                                                              WCMAppModel.TYPE_WEBFORMTEMPLATE,
                                                              props).getChildRef();
            
            // add filename pattern aspect if a filename pattern has been applied
            if (template.getOutputPathPattern() != null)
            {
               props.clear();
               props.put(WCMAppModel.PROP_OUTPUT_PATH_PATTERN, template.getOutputPathPattern());
               getNodeService().addAspect(templateRef, WCMAppModel.ASPECT_OUTPUT_PATH_PATTERN, props);
            }
         }
      }
      
      // walk each web project workflow definition and save defaults for each
      for (WorkflowWrapper workflow : this.workflows)
      {
         props.clear();
         props.put(WCMAppModel.PROP_WORKFLOW_NAME, workflow.getName());
         NodeRef workflowRef = getNodeService().createNode(nodeRef,
                                                           WCMAppModel.ASSOC_WEBWORKFLOWDEFAULTS,
                                                           WCMAppModel.ASSOC_WEBWORKFLOWDEFAULTS,
                                                           WCMAppModel.TYPE_WEBWORKFLOWDEFAULTS,
                                                           props).getChildRef();
         
         // persist workflow default params
         if (workflow.getParams() != null)
         {
            AVMWorkflowUtil.serializeWorkflowParams((Serializable)workflow.getParams(), workflowRef);
         }
         
         // add filename pattern aspect if a filename pattern has been applied
         if (workflow.getFilenamePattern() != null)
         {
            props.clear();
            props.put(WCMAppModel.PROP_FILENAMEPATTERN, workflow.getFilenamePattern());
            getNodeService().addAspect(workflowRef, WCMAppModel.ASPECT_FILENAMEPATTERN, props);
         }
      }
      
      // finally walk through the deployment config and save
      for (DeploymentServerConfig server : this.deployServersList)
      {
         Map<QName, Serializable> repoProps = server.getRepoProps();
         
         getNodeService().createNode(nodeRef, WCMAppModel.ASSOC_DEPLOYMENTSERVER,
                  WCMAppModel.ASSOC_DEPLOYMENTSERVER, WCMAppModel.TYPE_DEPLOYMENTSERVER,
                  repoProps);
         
         if (logger.isDebugEnabled())
         {
            // overwrite the password property before logging
            Map<QName, Serializable> tempProps = new HashMap<QName, Serializable>(repoProps.size());
            tempProps.putAll(repoProps);
            tempProps.put(WCMAppModel.PROP_DEPLOYSERVERPASSWORD, "*****");
            
            logger.debug("Saved deploymentserver node using repo props: " + tempProps);
         }
      }
   }
   
   /**
    * Restore the forms, templates, workflows and deployment config from the model for a web project. 
    * Can also optionally restore the basic node propetries and user details.
    * 
    * @param nodeRef        NodeRef to the web project to load model from
    * @param loadProperties Load the basic properties such as name, title, DNS.
    * @param loadUsers      Load the user details.
    */
   @SuppressWarnings("unchecked")
   protected void loadWebProjectModel(NodeRef nodeRef, boolean loadProperties, boolean loadUsers)
   {
      // simple properties are optionally loaded
      if (loadProperties)
      {
         Map<QName, Serializable> props = getNodeService().getProperties(nodeRef);
         this.name = (String)props.get(ContentModel.PROP_NAME);
         this.title = (String)props.get(ContentModel.PROP_TITLE);
         this.description = (String)props.get(ContentModel.PROP_DESCRIPTION);
         this.dnsName = (String)props.get(WCMAppModel.PROP_AVMSTORE);
         this.webapp = (String)props.get(WCMAppModel.PROP_DEFAULTWEBAPP);
         Boolean isSource = (Boolean)props.get(WCMAppModel.PROP_ISSOURCE);
         if (isSource != null)
         {
            this.isSource = isSource.booleanValue();
         }
      }
      
      if (loadUsers)
      {
         InviteWebsiteUsersWizard wiz = getInviteUsersWizard();
         wiz.reset();
         
         // load the users assigned to the web project
         List<ChildAssociationRef> userInfoRefs = getNodeService().getChildAssocs(
               nodeRef, WCMAppModel.ASSOC_WEBUSER, RegexQNamePattern.MATCH_ALL);
         for (ChildAssociationRef ref : userInfoRefs)
         {
            NodeRef userRef = ref.getChildRef();
            String username = (String)getNodeService().getProperty(userRef, WCMAppModel.PROP_WEBUSERNAME);
            String userrole = (String)getNodeService().getProperty(userRef, WCMAppModel.PROP_WEBUSERROLE);
            wiz.addAuthorityWithRole(username, userrole);
         }
      }
      
      // load the form templates
      List<ChildAssociationRef> webFormRefs = getNodeService().getChildAssocs(
            nodeRef, WCMAppModel.ASSOC_WEBFORM, RegexQNamePattern.MATCH_ALL);
      for (ChildAssociationRef ref : webFormRefs)
      {
         NodeRef formRef = ref.getChildRef();
         
         String name = (String)getNodeService().getProperty(formRef, WCMAppModel.PROP_FORMNAME);
         try
         {
            Form formImpl = getFormsService().getWebForm(name);
            FormWrapper form = new FormWrapper(formImpl);
            form.setTitle((String)getNodeService().getProperty(formRef, ContentModel.PROP_TITLE));
            form.setDescription((String)getNodeService().getProperty(formRef, ContentModel.PROP_DESCRIPTION));
            form.setOutputPathPattern((String)getNodeService().getProperty(formRef, WCMAppModel.PROP_OUTPUT_PATH_PATTERN));
            
            // the single workflow attached to the form 
            List<ChildAssociationRef> workflowRefs = getNodeService().getChildAssocs(
               formRef, WCMAppModel.ASSOC_WORKFLOWDEFAULTS, RegexQNamePattern.MATCH_ALL);
            if (workflowRefs.size() == 1)
            {
               NodeRef wfRef = workflowRefs.get(0).getChildRef();
               String wfName = (String)getNodeService().getProperty(wfRef, WCMAppModel.PROP_WORKFLOW_NAME);
               WorkflowDefinition wfDef = getWorkflowService().getDefinitionByName(wfName);
               if (wfDef != null)
               {
                  WorkflowWrapper wfWrapper = new WorkflowWrapper(wfName, wfDef.getTitle(), wfDef.getDescription());
                  wfWrapper.setParams((Map<QName, Serializable>)AVMWorkflowUtil.deserializeWorkflowParams(wfRef));
                  if (wfDef.getStartTaskDefinition() != null)
                  {
                     wfWrapper.setType(wfDef.getStartTaskDefinition().metadata.getName());
                  }
                  form.setWorkflow(wfWrapper);
               }
            }
            
            // the templates attached to the form
            List<ChildAssociationRef> templateRefs = getNodeService().getChildAssocs(
               formRef, WCMAppModel.ASSOC_WEBFORMTEMPLATE, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef tChildRef : templateRefs)
            {
               NodeRef templateRef = tChildRef.getChildRef();
               String renderingEngineTemplateName = (String)getNodeService().getProperty(
                     templateRef, WCMAppModel.PROP_BASE_RENDERING_ENGINE_TEMPLATE_NAME);
               RenderingEngineTemplate ret = formImpl.getRenderingEngineTemplate(renderingEngineTemplateName);
               if (ret != null)
               {
                  String outputPathPattern = (String)getNodeService().getProperty(
                        templateRef, WCMAppModel.PROP_OUTPUT_PATH_PATTERN);
                  form.addTemplate(new PresentationTemplate(ret, outputPathPattern));
               }
            }
            
            this.forms.add(form);
         }
         catch (FormNotFoundException fnfe)
         {
            // ignore as we cannot do anything about a missing form
            if (logger.isDebugEnabled())
               logger.debug("Unable to find Web Form named '" + fnfe.getFormName() +
                     "' as referenced in web project: " + nodeRef.toString());
         }
      }
      
      // load the workflows associated with the website
      List<ChildAssociationRef> workflowRefs = getNodeService().getChildAssocs(
            nodeRef, WCMAppModel.ASSOC_WEBWORKFLOWDEFAULTS, RegexQNamePattern.MATCH_ALL);
      for (ChildAssociationRef wChildRef : workflowRefs)
      {
         NodeRef wfRef = wChildRef.getChildRef();
         String wfName = (String)getNodeService().getProperty(wfRef, WCMAppModel.PROP_WORKFLOW_NAME);
         WorkflowDefinition wfDef = getWorkflowService().getDefinitionByName(wfName);
         if (wfDef != null)
         {
            WorkflowWrapper wfWrapper = new WorkflowWrapper(wfName, wfDef.getTitle(), wfDef.getDescription());
            wfWrapper.setParams((Map<QName, Serializable>)AVMWorkflowUtil.deserializeWorkflowParams(wfRef));
            wfWrapper.setFilenamePattern((String)getNodeService().getProperty(wfRef, 
                                                                              WCMAppModel.PROP_FILENAMEPATTERN));
            if (wfDef.getStartTaskDefinition() != null)
            {
               wfWrapper.setType(wfDef.getStartTaskDefinition().metadata.getName());
            }
            this.workflows.add(wfWrapper);
         }
      }
      
      // load the deployment server config objects
      List<ChildAssociationRef> serverRefs = getNodeService().getChildAssocs(
               nodeRef, WCMAppModel.ASSOC_DEPLOYMENTSERVER, RegexQNamePattern.MATCH_ALL);
      for (ChildAssociationRef sChildRef : serverRefs)
      {
         NodeRef serverRef = sChildRef.getChildRef();
         DeploymentServerConfig server = new DeploymentServerConfig(
                  serverRef, getNodeService().getProperties(serverRef));
         
         this.deployServersList.add(server);
         this.deployServersMap.put(server.getId(), server);
         
         if (logger.isDebugEnabled())
            logger.debug("Loaded deploy server config: " + server);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Service setters
   
   /**
    * @param avmService The AVMService to set.
    */
   public void setAvmService(AVMService avmService)
   {
      this.avmService = avmService;
   }
   
   protected AVMService getAvmService()
   {
      if (avmService == null)
      {
         avmService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAVMService();
      }
      return avmService;
   }
   
   /**
    * @param workflowService  The WorkflowService to set.
    */
   public void setWorkflowService(WorkflowService workflowService)
   {
      this.workflowService = workflowService;
   }
   
   protected WorkflowService getWorkflowService()
   {
      if (workflowService == null)
      {
         workflowService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getWorkflowService();
      }
      return workflowService;
   }

   /**
    * @param personService  The PersonService to set.
    */
   public void setPersonService(PersonService personService)
   {
      this.personService = personService;
   }
   
   protected PersonService getPersonService()
   {
      if (personService == null)
      {
         personService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPersonService();
      }
      return personService;
   }
   
   /**
    * @param avmLockingService The AVMLockingService to set
    */
   public void setAvmLockingService(AVMLockingService avmLockingService)
   {
      this.avmLockingService = avmLockingService;
   }
   
   protected AVMLockingService getAvmLockingService()
   {
      if (avmLockingService == null)
      {
         avmLockingService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAVMLockingService();
      }
      return avmLockingService;
   }

   /**
    * @param formsService    The FormsService to set.
    */
   public void setFormsService(final FormsService formsService)
   {
      this.formsService = formsService;
   }
   
   protected FormsService getFormsService()
   {
      if (formsService == null)
      {
         formsService = (FormsService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), "FormsService");
      }
      return formsService;
   }


   
   // ------------------------------------------------------------------------------
   // Bean getters and setters
   
   /**
    * @return Returns the wizard Edit Mode.
    */
   public boolean getEditMode()
   {
      return this.editMode;
   }

   /**
    * @param editMode The wizard Edit Mode to set.
    */
   public void setEditMode(boolean editMode)
   {
      this.editMode = editMode;
   }

   /**
    * @return Returns the name.
    */
   public String getName()
   {
      return name;
   }
   
   /**
    * @param name The name to set.
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return DNS name
    */
   public String getDnsName()
   {
      return this.dnsName;
   }

   /**
    * @param dnsName DNS name
    */
   public void setDnsName(String dnsName)
   {
      this.dnsName = dnsName;
   }

   /**
    * @return Returns the title.
    */
   public String getTitle()
   {
      return title;
   }
   
   /**
    * @param title The title to set.
    */
   public void setTitle(String title)
   {
      this.title = title;
   }
   
   /**
    * @return Returns the description.
    */
   public String getDescription()
   {
      return description;
   }
   
   /**
    * @param description The description to set.
    */
   public void setDescription(String description)
   {
      this.description = description;
   }
   
   /**
    * @return the default webapp name for the project
    */
   public String getWebapp()
   {
      return this.webapp;
   }

   /**
    * @param webapp  The default webapp name for the project
    */
   public void setWebapp(String webapp)
   {
      this.webapp = webapp;
   }
   
   /**
    * @return the create from selection value
    */
   public String getCreateFrom()
   {
      return this.createFrom;
   }

   /**
    * @param createFrom the create from selection value
    */
   public void setCreateFrom(String createFrom)
   {
      if (createFrom != null && createFrom.equals(this.createFrom) == false)
      {
         this.createFromValueChanged = true;
      }
      this.createFrom = createFrom;
   }
   
   /**
    * @return the existing Web Project to use
    */
   public String[] getSourceWebProject()
   {
      return this.sourceWebProject;
   }

   /**
    * @param existingWebProject the existing Web Project to set
    */
   public void setSourceWebProject(String[] existingWebProject)
   {
      if (this.sourceWebProject != null && this.sourceWebProject.length != 0)
      {
         if ((existingWebProject == null || existingWebProject.length == 0) ||
             (existingWebProject[0].equals(this.sourceWebProject[0]) == false))
         {
            this.createFromValueChanged = true;
         }
      }
      else
      {
         if (existingWebProject != null || existingWebProject.length != 0)
         {
            this.createFromValueChanged = true;
         }
      }
      this.sourceWebProject = existingWebProject;
   }
   
   /**
    * @return name of the source web project selected - or null if none set
    */
   public String getSourceWebProjectName()
   {
      String name = null;
      if (CREATE_EXISTING.equals(this.createFrom) && 
          (this.sourceWebProject != null && this.sourceWebProject.length != 0))
      {
         NodeRef sourceNodeRef = new NodeRef(this.sourceWebProject[0]);
         name = (String)getNodeService().getProperty(sourceNodeRef, ContentModel.PROP_NAME);
      }
      return name;
   }
   
   /**
    * @return true if this website is set to be a template source website for future web projects
    */
   public boolean isSource()
   {
      return this.isSource;
   }

   /**
    * @param isSource   true if this website is set to be a template source website for future web projects
    */
   public void setSource(boolean isSource)
   {
      this.isSource = isSource;
   }

   /**
    * @return the existingWebProjects
    */
   public List<UIListItem> getWebProjectsList()
   {
      List<UIListItem> webProjects = this.webProjectsList.get();
      if (webProjects == null)
      {
         FacesContext fc = FacesContext.getCurrentInstance();
         
         // construct the query to retrieve the web projects
         String path = Application.getRootPath(fc) + "/" + Application.getWebsitesFolderName(fc) + "/*";
         StringBuilder query = new StringBuilder(200);
         query.append("PATH:\"/").append(path).append("\"");
         query.append(" +TYPE:\"{").append(NamespaceService.WCMAPP_MODEL_1_0_URI).append("}webfolder\"");
         if (this.showAllSourceProjects == false)
         {
            // only query for web project templates by default
            query.append(" +@").append(Repository.escapeQName(WCMAppModel.PROP_ISSOURCE)).append(":true");
         }
         
         ResultSet results = null;
         try
         {
            // execute the query
            results = getSearchService().query(Repository.getStoreRef(), 
                                          SearchService.LANGUAGE_LUCENE, query.toString());
            webProjects = new ArrayList<UIListItem>(results.length());
            for (ResultSetRow row : results)
            {
               NodeRef ref = row.getNodeRef();
               String name = (String)getNodeService().getProperty(ref, ContentModel.PROP_NAME);
               String desc = (String)getNodeService().getProperty(ref, ContentModel.PROP_DESCRIPTION);
               UIListItem item = new UIListItem();
               item.setLabel(name);
               item.setDescription(desc);
               item.setValue(ref.toString());
               item.setImage(WebResources.IMAGE_WEBPROJECT_32);
               webProjects.add(item);
            }
         }
         finally
         {
            if (results != null)
            {
               results.close();
            }
         }
         
         this.webProjectsList.put(webProjects);
      }
      return webProjects;
   }
   
   /**
    * Action handler called when toggle Show All/Show Template Web Projects link is clicked
    */
   public void toggleWebProjectsList(ActionEvent event)
   {
      this.showAllSourceProjects = !this.showAllSourceProjects;
      this.webProjectsList.clear();
      this.createFromValueChanged = true;
   }
   
   /**
    * @return true to show all Web Projects in the Create From list,
    *         false to only show those marked as templates
    */
   public boolean getShowAllSourceProjects()
   {
      return this.showAllSourceProjects;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.BaseWizardBean#next()
    */
   @Override
   public String next()
   {
      String stepName = Application.getWizardManager().getCurrentStepName();
      if (STEP_DEPLOYMENT.equals(stepName))
      {
         // if we have just entered the deployment page and the Create From page data has changed
         // then we need to pre-populate the Forms etc. from the template web project
         updateModelOnCreateFromChange();
      }
      return super.next();
   }

   /**
    * Update the wizard model when the value in the Create From page changes
    */
   private void updateModelOnCreateFromChange()
   {
      if (this.createFromValueChanged)
      {
         if (CREATE_EXISTING.equals(this.createFrom))
         {
            if (this.sourceWebProject != null && this.sourceWebProject.length != 0)
            {
               clearFormsWorkflowsDeploymentAndUsers();
               loadWebProjectModel(new NodeRef(this.sourceWebProject[0]), false, true);
            }
         }
         else
         {
            clearFormsWorkflowsDeploymentAndUsers();
         }
         
         this.createFromValueChanged = false;
      }
   }

   /**
    * @return summary text for the wizard
    */
   @SuppressWarnings("unchecked")
   public String getSummary()
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      
      // build a summary section to list the invited users and there roles
      StringBuilder buf = new StringBuilder(128);
      List<UserGroupRole> invitedUserRoles =
         (List<UserGroupRole>)getInviteUsersWizard().getUserRolesDataModel().getWrappedData();
      String currentUser = Application.getCurrentUser(fc).getUserName();
      boolean foundCurrentUser = false;
      for (UserGroupRole userRole : invitedUserRoles)
      {
         if (currentUser.equals(userRole.getAuthority()))
         {
            foundCurrentUser = true;
         }
         buf.append(Utils.encode(userRole.getLabel()));
         buf.append("<br>");
      }
      if (foundCurrentUser == false)
      {
         buf.append(getInviteUsersWizard().buildLabelForUserAuthorityRole(
               currentUser, AVMUtil.ROLE_CONTENT_MANAGER));
      }
      
      return buildSummary(
            new String[] {Application.getMessage(fc, MSG_USERROLES)},
            new String[] {buf.toString()});
   }
   
   /**
    * @return the invited users for the project - as UserWrapper instances
    */
   @SuppressWarnings("unchecked")
   public List<UserWrapper> getInvitedUsers()
   {
      final FacesContext fc = FacesContext.getCurrentInstance();
      List<UserGroupRole> invitedUserRoles = (List<UserGroupRole>)
         getInviteUsersWizard().getUserRolesDataModel().getWrappedData();
      List<UserWrapper> result = new LinkedList<UserWrapper>();
      String currentUser = Application.getCurrentUser(fc).getUserName();
      boolean foundCurrentUser = false;
      for (UserGroupRole userRole : invitedUserRoles)
      {
         if (currentUser.equals(userRole.getAuthority()))
         {
            foundCurrentUser = true;
         }
         result.add(new UserWrapper(userRole.getAuthority(), userRole.getRole()));
      }
      if (foundCurrentUser == false)
      {
         result.add(new UserWrapper(currentUser, AVMUtil.ROLE_CONTENT_MANAGER));
      }
      return result;      
   }
   
   // ------------------------------------------------------------------------------
   // Deployment server configuration
   
   /**
    * @return Determines whether a deployment server is being added
    */
   public boolean isInAddDeployServerMode()
   {
      return this.inAddDeployServerMode;
   }

   /**
    * @return The type of server receiver to add, either 'alfresco' or 'file'
    */
   public String getAddDeployServerType()
   {
      return this.addDeployServerType;
   }
   
   /**
    * @return The deploy server currently being added or edited 
    */
   public DeploymentServerConfig getCurrentDeployServer()
   {
      return this.currentDeployServer;
   }
   
   /**
    * @return The properties of the deploy server currently being added or edited 
    */
   public Map<String, Object> getEditedDeployServerProperties()
   {
      return this.editedDeployServerProps;
   }

   /**
    * @return Map of the deployment servers currently configured for the web project
    */
   public List<DeploymentServerConfig> getDeployServers()
   {
      return this.deployServersList;
   }

   /**
    * Sets up the wizard for adding a new Alfresco Server Receiver
    * 
    * @return null outcome to stay on same page
    */
   public String addAlfrescoServerReceiver()
   {
      this.addDeployServerType = WCMAppModel.CONSTRAINT_ALFDEPLOY;
      this.inAddDeployServerMode = true;
      
      // create an empty server config
      this.currentDeployServer = new DeploymentServerConfig(this.addDeployServerType);
      this.editedDeployServerProps.clear();
      
      // refresh the current page
      return null;
   }
   
   /**
    * Sets up the wizard for adding a new File System Receiver
    * 
    * @return null outcome to stay on same page
    */
   public String addFileSystemReceiver()
   {
      this.addDeployServerType = WCMAppModel.CONSTRAINT_FILEDEPLOY;
      this.inAddDeployServerMode = true;
      
      // create an empty server config
      this.currentDeployServer = new DeploymentServerConfig(this.addDeployServerType);
      this.editedDeployServerProps.clear();
      
      // refresh the current page
      return null;
   }
   
   public void editDeploymentServerConfig(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         this.inAddDeployServerMode = false;
         
         // setup the config object to edit
         this.currentDeployServer = this.deployServersMap.get(id);
         this.editedDeployServerProps.clear();
         this.editedDeployServerProps.putAll(this.currentDeployServer.getProperties());
         
         if (logger.isDebugEnabled())
            logger.debug("Set current deploy server to: " + this.currentDeployServer);
      }
   }
   
   public void deleteDeploymentServerConfig(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         this.currentDeployServer = null;
         this.editedDeployServerProps.clear();
         this.inAddDeployServerMode = false;
         
         // remove the config object from the list and map
         DeploymentServerConfig dsc = this.deployServersMap.get(id);
         if (dsc != null)
         {
            this.deployServersList.remove(dsc);
            this.deployServersMap.remove(dsc.getId());
            
            if (logger.isDebugEnabled())
               logger.debug("Removed deploy server config with id: " + id);
         }
      }
   }
   
   public String addDeploymentServerConfig()
   {
      // add the new config to the list and map
      this.deployServersList.add(this.currentDeployServer);
      this.deployServersMap.put(this.currentDeployServer.getId(), 
               this.currentDeployServer);
      
      // save the changes
      return saveDeploymentServerConfig();
   }
   
   public String saveDeploymentServerConfig()
   {
      // set the edited properties
      this.currentDeployServer.setProperties(this.editedDeployServerProps);
      
      if (logger.isDebugEnabled())
         logger.debug("Saved transient deploy server config: " + this.currentDeployServer);
      
      // reset state
      this.currentDeployServer = null;
      this.editedDeployServerProps.clear();
      this.inAddDeployServerMode = false;
      
      // refresh the current page
      return null;
   }
   
   public String cancelDeploymentServerConfig()
   {
      this.currentDeployServer = null;
      this.editedDeployServerProps.clear();
      this.inAddDeployServerMode = false;
      
      return null;
   }
   
   // ------------------------------------------------------------------------------
   // Define Web Content Workflows page
   
   /**
    * @return JSF data model for the Form templates
    */
   public DataModel getFormsDataModel()
   {
      if (this.formsDataModel == null)
      {
         this.formsDataModel = new ListDataModel();
      }
      
      this.formsDataModel.setWrappedData(this.forms);
      
      return this.formsDataModel;
   }
   
   /**
    * @return the List of selected and configured Form objects (for summary screen) 
    */
   public List<FormWrapper> getForms()
   {
      return this.forms;
   }

   /**
    * @param formsDataModel   JSF data model for the Form templates
    */
   public void setFormsDataModel(DataModel formsDataModel)
   {
      this.formsDataModel = formsDataModel;
   }
   
   /**
    * @return List of UI items to represent the available Web Forms for all websites
    */
   public List<UIListItem> getFormsList()
   {
      Collection<Form> forms = this.getFormsService().getWebForms();
      List<UIListItem> items = new ArrayList<UIListItem>(forms.size());
      for (Form form : forms)
      {
         UIListItem item = new UIListItem();
         item.setValue(form);
         item.setLabel(form.getTitle());
         item.setDescription(form.getDescription());
         item.setImage(WebResources.IMAGE_WEBFORM_32);
         items.add(item);
      }
      this.formsList = items;
      return items;
   }
   
   /**
    * Action handler called when the Add to List button is pressed for a form template
    */
   public void addForm(ActionEvent event)
   {
      UISelectList selectList = (UISelectList)event.getComponent().findComponent(COMPONENT_FORMLIST);
      int index = selectList.getRowIndex();
      if (index != -1)
      {
         Form form = (Form)this.formsList.get(index).getValue();
         FormWrapper wrapper = new FormWrapper(form);
         wrapper.setTitle(form.getTitle());
         wrapper.setDescription(form.getDescription());
         this.forms.add(wrapper);
      }
   }
   
   /**
    * Remove a template form from the selected list
    */
   public void removeForm(ActionEvent event)
   {
      FormWrapper wrapper = (FormWrapper)this.getFormsDataModel().getRowData();
      if (wrapper != null)
      {
         this.forms.remove(wrapper);
      }
   }
   
   /**
    * Action handler to setup a form for dialog context for the current row
    */
   public void setupFormAction(ActionEvent event)
   {
      setActionForm( (FormWrapper)this.getFormsDataModel().getRowData() );
   }
   
   /**
    * @return the current action form for dialog context
    */
   public FormWrapper getActionForm()
   {
      return this.actionForm;
   }

   /**
    * @param actionForm    For dialog context
    */
   public void setActionForm(FormWrapper actionForm)
   {
      this.actionForm = actionForm;
      if (actionForm != null)
      {
         setActionWorkflow(actionForm.getWorkflow());
      }
      else
      {
         setActionWorkflow(null);
      }
   }
   
   /**
    * Action method to setup a workflow for dialog context for the current row
    */
   public void setupWorkflowAction(ActionEvent event)
   {
      setActionWorkflow( (WorkflowConfiguration)this.workflowsDataModel.getRowData() );
   }
   
   /**
    * @return Returns the action Workflow for dialog context
    */
   public WorkflowConfiguration getActionWorkflow()
   {
      return this.actionWorkflow;
   }

   /**
    * @param actionWorkflow   The action Workflow to set for dialog context
    */
   public void setActionWorkflow(WorkflowConfiguration actionWorkflow)
   {
      this.actionWorkflow = actionWorkflow;
   }
   
   
   // ------------------------------------------------------------------------------
   // Specify Settings (ah-hoc Workflows) page
   
   /**
    * @return JSF data model for the Workflow templates
    */
   public DataModel getWorkflowsDataModel()
   {
      if (this.workflowsDataModel == null)
      {
         this.workflowsDataModel = new ListDataModel();
      }
      
      this.workflowsDataModel.setWrappedData(this.workflows);
      
      return this.workflowsDataModel;
   }

   /**
    * @param workflowsDataModel   JSF data model for the Workflow templates
    */
   public void setWorkflowsDataModel(DataModel workflowsDataModel)
   {
      this.workflowsDataModel = workflowsDataModel;
   }
   
   /**
    * @return the list of workflows (for the summary screen)
    */
   public List<WorkflowWrapper> getWorkflows()
   {
      return this.workflows;
   }
   
   /**
    * @return List of UI items to represent the available Workflows for all websites
    */
   public List<UIListItem> getWorkflowList()
   {
      // get list of workflows from config definitions
      List<WorkflowDefinition> workflowDefs = AVMWorkflowUtil.getConfiguredWorkflows();
      List<UIListItem> items = new ArrayList<UIListItem>(workflowDefs.size());
      for (WorkflowDefinition workflowDef : workflowDefs)
      {
         UIListItem item = new UIListItem();
         item.setValue(workflowDef);
         item.setLabel(workflowDef.title);
         item.setDescription(workflowDef.description);
         item.setImage(WebResources.IMAGE_WORKFLOW_32);
         items.add(item);
      }
      
      return items;
   }
   
   /**
    * Action handler called when the Add to List button is pressed for a workflow
    */
   public void addWorkflow(ActionEvent event)
   {
      UISelectList selectList = (UISelectList)event.getComponent().findComponent(COMPONENT_WORKFLOWLIST);
      int index = selectList.getRowIndex();
      if (index != -1)
      {
         WorkflowDefinition workflow = (WorkflowDefinition)this.getWorkflowList().get(index).getValue();
         this.workflows.add(new WorkflowWrapper(
               workflow.getName(), workflow.getTitle(), workflow.getDescription(), MATCH_DEFAULT));
      }
   }
   
   /**
    * Remove a workflow from the selected list
    */
   public void removeWorkflow(ActionEvent event)
   {
      WorkflowWrapper wrapper = (WorkflowWrapper)this.getWorkflowsDataModel().getRowData();
      if (wrapper != null)
      {
         this.workflows.remove(wrapper);
      }
   }

   
   // ------------------------------------------------------------------------------
   // Invite users page
   
   /**
    * @return the InviteWebsiteUsersWizard delegate bean
    */
   private InviteWebsiteUsersWizard getInviteUsersWizard()
   {
      return (InviteWebsiteUsersWizard)FacesHelper.getManagedBean(
            FacesContext.getCurrentInstance(), "InviteWebsiteUsersWizard");
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Wrapper class for a configurable template Form instance
    */
   public static class FormWrapper implements Serializable
   {
      private static final long serialVersionUID = -1452145043222643362L;
      
      private Form form;
      private String title;
      private String description;
      private WorkflowWrapper workflow;
      private String outputPathPattern;
      private List<PresentationTemplate> templates = null;
      
      FormWrapper(Form form)
      {
         this.form = form;
         this.title = form.getName();
      }
      
      public Form getForm()
      {
         return this.form;
      }
      
      public String getName()
      {
         return this.form.getName();
      }
      
      public String getTitle()
      {
         return this.title;
      }
      
      public void setTitle(String title)
      {
         this.title = title;
      }
      
      public String getDescription()
      {
         return this.description;
      }

      public void setDescription(String description)
      {
         this.description = description;
      }

      /**
       * @return Returns the workflow.
       */
      public WorkflowWrapper getWorkflow()
      {
         WorkflowDefinition wf = this.form.getDefaultWorkflow();
         if (this.workflow == null && wf != null)
         {
            this.workflow = new WorkflowWrapper(wf.name, wf.getTitle(), wf.getDescription());
         }
         return this.workflow;
      }

      /**
       * @param workflow The workflow to set.
       */
      public void setWorkflow(WorkflowWrapper workflow)
      {
         this.workflow = workflow;
      }

      /**
       * @return Returns the output path pattern.
       */
      public String getOutputPathPattern()
      {
         if (this.outputPathPattern == null)
         {
            this.outputPathPattern = this.form.getOutputPathPattern();
         }
         return this.outputPathPattern;
      }

      /**
       * @param outputPathPattern The output path pattern to set.
       */
      public void setOutputPathPattern(String outputPathPattern)
      {
         this.outputPathPattern = outputPathPattern;
      }
      
      /**
       * @return Returns the presentation templates.
       */
      public List<PresentationTemplate> getTemplates()
      {
         if (this.templates == null)
         {
            List<RenderingEngineTemplate> templates = this.form.getRenderingEngineTemplates();
            this.templates = new ArrayList<PresentationTemplate>(templates.size());
            for (RenderingEngineTemplate template : templates)
            {
               this.templates.add(new PresentationTemplate(template));
            }
         }
         return this.templates;
      }
      
      public int getTemplatesSize()
      {
         return getTemplates() != null ? getTemplates().size() : 0;
      }
      
      /**
       * @param template   to add to the list of PresentationTemplate
       */
      public void addTemplate(PresentationTemplate template)
      {
         if (this.templates == null)
         {
            this.templates = new ArrayList<PresentationTemplate>(4);
         }
         this.templates.add(template);
      }

      /**
       * @param templates The presentation templates to set.
       */
      public void setTemplates(List<PresentationTemplate> templates)
      {
         this.templates = templates;
      }
   }
   

   /**
    * Class to represent a single configured Presentation Template instance
    */
   public static class PresentationTemplate implements Serializable
   {
      private static final long serialVersionUID = 5148139895329524483L;
      
      private RenderingEngineTemplate ret;
      private String title;
      private String description;
      private String outputPathPattern;
      
      public PresentationTemplate(RenderingEngineTemplate ret)
      {
         this(ret, null);
      }
      
      public PresentationTemplate(RenderingEngineTemplate ret, String outputPathPattern)
      {
         this.ret = ret;
         this.outputPathPattern = outputPathPattern;
      }
      
      public RenderingEngineTemplate getRenderingEngineTemplate()
      {
         return this.ret;
      }
      
      public String getTitle()
      {
         if (this.title == null)
         {
            this.title = ret.getName();
         }
         return this.title;
      }
      
      public String getDescription()
      {
         if (this.description == null)
         {
            this.description = ret.getDescription();
         }
         return this.description;
      }
      
      /**
       * @return Returns the output path pattern.
       */
      public String getOutputPathPattern()
      {
         if (this.outputPathPattern == null)
         {
            this.outputPathPattern = ret.getOutputPathPattern();
         }
         return this.outputPathPattern;
      }

      /**
       * @param outputPathPattern The output path pattern to set.
       */
      public void setOutputPathPattern(String outputPathPattern)
      {
         this.outputPathPattern = outputPathPattern;
      }
   }

   
   /**
    * Class to represent a single configured Workflow instance
    */
   public static class WorkflowWrapper implements WorkflowConfiguration
   {
      private static final long serialVersionUID = -5570490335442743685L;
      
      private String name;
      private String title;
      private String description;
      private String filenamePattern;
      private QName type;
      private Map<QName, Serializable> params;
      
      public WorkflowWrapper(String name, String title, String description)
      {
         this.name = name;
         this.title = title;
         this.description = description;
      }
      
      public WorkflowWrapper(String name, String title, String description, String filenamePattern)
      {
         this.name = name;
         this.title = title;
         this.description = description;
         this.filenamePattern = filenamePattern;
      }
      
      /**
       * @return Returns the name key of the workflow.
       */
      public String getName()
      {
         return this.name;
      }

      /**
       * @return the display label of the workflow.
       */
      public String getTitle()
      {
         return this.title;
      }

      /**
       * @return the display label of the workflow.
       */
      public String getDescription()
      {
         return this.description;
      }

      /**
       * @return Returns the filename pattern.
       */
      public String getFilenamePattern()
      {
         return this.filenamePattern;
      }

      /**
       * @param filenamePattern The filename pattern to set.
       */
      public void setFilenamePattern(String filenamePattern)
      {
         this.filenamePattern = filenamePattern;
      }

      /**
       * @return Returns the workflow params.
       */
      public Map<QName, Serializable> getParams()
      {
         return this.params;
      }

      /**
       * @param params The workflow params to set.
       */
      public void setParams(Map<QName, Serializable> params)
      {
         this.params = params;
      }

      /**
       * @return Returns the type.
       */
      public QName getType()
      {
         return this.type;
      }

      /**
       * @param type The type to set.
       */
      public void setType(QName type)
      {
         this.type = type;
      }
   }


   public class UserWrapper implements Serializable
   {
      private static final long serialVersionUID = 6546685548198253273L;
      
      private final String name, role;
      
      public UserWrapper(final String authority, final String role)
      {
         
         if (AuthorityType.getAuthorityType(authority) == AuthorityType.USER ||
             AuthorityType.getAuthorityType(authority) == AuthorityType.GUEST)
         {
            NodeRef ref = getPersonService().getPerson(authority);
            String firstName = (String)getNodeService().getProperty(ref, ContentModel.PROP_FIRSTNAME);
            String lastName = (String)getNodeService().getProperty(ref, ContentModel.PROP_LASTNAME);
            this.name = firstName + (lastName != null ? " " + lastName : "");
         }
         else
         {
            this.name = authority.substring(PermissionService.GROUP_PREFIX.length());
         }
         this.role = Application.getMessage(FacesContext.getCurrentInstance(), role);
      }

      public String getName() { return this.name; }
      public String getRole() { return this.role; }
   }
}
