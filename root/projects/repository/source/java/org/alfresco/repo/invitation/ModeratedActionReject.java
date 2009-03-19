package org.alfresco.repo.invitation;


import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.invitation.site.RejectInviteAction;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.BeanFactory;

/**
 * JBPM Action fired when a moderated invitation is rejected.
 */
public class ModeratedActionReject extends JBPMSpringActionHandler
{
    private static final long serialVersionUID = 4377660284993206875L;
    private static final Log logger = LogFactory.getLog(ModeratedActionReject.class);
    
    private MutableAuthenticationDao mutableAuthenticationDao;
    private PersonService personService;
    private WorkflowService workflowService;
    private ActionService actionService;
    private TemplateService templateService;
    //private String rejectTemplate = " PATH:\"app:company_home/app:dictionary/app:email_templates/cm:invite/cm:moderated-reject-email.ftl\"";
    private String rejectTemplate = "/alfresco/bootstrap/invite/moderated-reject-email.ftl";

    /* (non-Javadoc)
     * @see org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler#initialiseHandler(org.springframework.beans.factory.BeanFactory)
     */
    @Override
    protected void initialiseHandler(BeanFactory factory)
    {
        ServiceRegistry services = (ServiceRegistry)factory.getBean(ServiceRegistry.SERVICE_REGISTRY);
        mutableAuthenticationDao = (MutableAuthenticationDao) factory.getBean("authenticationDao");
        personService = (PersonService) services.getPersonService();
        workflowService = (WorkflowService) services.getWorkflowService();
        templateService = (TemplateService) services.getTemplateService();
        actionService = (ActionService) services.getActionService();
    }

    /* (non-Javadoc)
     * @see org.jbpm.graph.def.ActionHandler#execute(org.jbpm.graph.exe.ExecutionContext)
     * Reject Moderated
     */
    @SuppressWarnings("unchecked")
    public void execute(final ExecutionContext executionContext) throws Exception
    {
        final String resourceType = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarResourceType);
        final String resourceName = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarResourceName);
        final String inviteeUserName = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarInviteeUserName);
        final String inviteeRole = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarInviteeRole);
        final String reviewer = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarReviewer);
        final String reviewComments = (String)executionContext.getVariable(WorkflowModelModeratedInvitation.wfVarReviewComments);
        
        // send email to the invitee if possible - but don't fail the rejection if email cannot be sent
        try 
        {
        	Map<String, Object> model = new HashMap<String, Object>(8, 1.0f);
        	model.put("resourceName", resourceName);
        	model.put("resourceType", resourceType);
        	model.put("inviteeRole", inviteeRole);
        	model.put("reviewComments", reviewComments);
        	model.put("reviewer", reviewer);
        	model.put("inviteeUserName", inviteeUserName);
     
        	String emailMsg = templateService.processTemplate("freemarker", rejectTemplate,  model);
        	        
        	Action emailAction = actionService.createAction("mail");
        	emailAction.setParameterValue(MailActionExecuter.PARAM_TO, inviteeUserName);
        	emailAction.setParameterValue(MailActionExecuter.PARAM_FROM, reviewer);
        	emailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, "Rejected invitation to web site:" + resourceName);
        	emailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, emailMsg);
        	emailAction.setExecuteAsynchronously(true);
        	actionService.executeAction(emailAction, null);
        }
        catch(Exception e)
        {
        	// Swallow exception
        	logger.error("unable to send reject email", e);
        }
    }
}