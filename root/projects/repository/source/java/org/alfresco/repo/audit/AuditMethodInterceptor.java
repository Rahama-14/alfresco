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
package org.alfresco.repo.audit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.StackTraceUtil;
import org.alfresco.repo.audit.model.AuditApplication;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.Auditable;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.transaction.TransactionService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * A method interceptor to wrap method invocations with auditing.
 * <p/>
 * <b><u>V3.2 Configuration</u>:</b>
 * As of V3.2, the pre- and post-invocation values are passed to the audit component
 * for processing.  Individual applications have to extract the desired audit values.
 * Values are audited before and after the invocation so that applications that desire
 * to extract derived data before the invocation can have a chance to do so; generally,
 * however, the post-invocation values will be the most useful.
 * <p/>
 * The values passed to the audit component (assuming auditing is enabled and the
 * new configuration is being used) are:
 * <pre>
 * /alfresco-api
 *    /pre
 *       /&lt;service&gt;
 *          /&lt;method&gt;
 *             /args
 *                /&lt;arg-name&gt;=&lt;value&gt;
 *                /&lt;arg-name&gt;=&lt;value&gt;
 *                ...
 *       /service
 *    /post
 *       /&lt;service&gt;
 *          /&lt;method&gt;
 *             /args
 *                /&lt;arg-name&gt;=&lt;value&gt;
 *                /&lt;arg-name&gt;=&lt;value&gt;
 *                ...
 *             /result=&lt;value&gt;
 *             /error=&lt;value&gt;
 * 
 * </pre>
 * Applications can remap the paths onto their configurations as appropriate.
 * <p/>
 * TODO: Audit configuration mapping needs to support conditionals
 * 
 * @author Andy Hind
 * @author Derek Hulley
 */
public class AuditMethodInterceptor implements MethodInterceptor
{
    public static final String AUDIT_PATH_API_PRE = "/alfresco-api/pre";
    public static final String AUDIT_PATH_API_POST = "/alfresco-api/post";
    public static final String AUDIT_SNIPPET_ARGS = "/args";
    public static final String AUDIT_SNIPPET_RESULT = "/result";
    public static final String AUDIT_SNIPPET_ERROR = "/error";
    
    private static final Log logger = LogFactory.getLog(AuditMethodInterceptor.class);

    private PublicServiceIdentifier publicServiceIdentifier;
    private AuditComponent auditComponent;
    private TransactionService transactionService;

    private boolean enabled = false;
    private boolean useNewConfig = false;
    
    private final ThreadLocal<Boolean> inAudit = new ThreadLocal<Boolean>();
    
    public AuditMethodInterceptor()
    {
        super();
    }

    /**
     * Enable or disable auditing at a high level (default: <b>false</b>)
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Use the new audit configuration (default: <b>false</b>)
     * 
     * @param useNewConfig              <tt>true</tt> to use the new audit configuration
     */
    public void setUseNewConfig(boolean useNewConfig)
    {
        this.useNewConfig = useNewConfig;
    }

    public void setPublicServiceIdentifier(PublicServiceIdentifier serviceIdentifier)
    {
        this.publicServiceIdentifier = serviceIdentifier;
    }

    public void setAuditComponent(AuditComponent auditComponent)
    {
        this.auditComponent = auditComponent;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public Object invoke(MethodInvocation mi) throws Throwable
    {
        if(!enabled)
        {
            // No auditing
            return mi.proceed();
        }
        else if (useNewConfig)
        {
            // New configuration to be used
            return proceed(mi);
        }
        else
        {
            // Use previous configuration
            return auditComponent.audit(mi);
        }
        
    }
    
    /**
     * Allow the given method invocation to proceed, auditing values before invocation and
     * after returning or throwing.
     * 
     * @param mi                the invocation
     * @return                  Returns the method return (if a value is not thrown)
     * @throws Throwable        rethrows any exception generated by the invocation
     * 
     * @since 3.2
     */
    private Object proceed(MethodInvocation mi) throws Throwable
    {
        Auditable auditableDef = mi.getMethod().getAnnotation(Auditable.class);
        if (auditableDef == null)
        {
            // No annotation, so just continue as normal
            return mi.proceed();
        }
        
        // First get the argument map, if present
        Object[] args = mi.getArguments();
        Map<String, Serializable> namedArguments = getInvocationArguments(auditableDef, args);
        // Get the service name
        String serviceName = publicServiceIdentifier.getPublicServiceName(mi);
        if (serviceName == null)
        {
            // Not a public service
            return mi.proceed();
        }
        String methodName = mi.getMethod().getName();
        
        // Are we in a nested audit
        Boolean wasInAudit = inAudit.get();
        // TODO: Need to make this configurable for the interceptor or a conditional mapping for audit
        if (wasInAudit != null)
        {
            return mi.proceed();
        }
        // Record that we have entered an audit method
        inAudit.set(Boolean.TRUE);
        try
        {
            return proceedWithAudit(mi, auditableDef, serviceName, methodName, namedArguments);
        }
        finally
        {
            inAudit.set(wasInAudit);
        }
    }
    
    private Object proceedWithAudit(
            MethodInvocation mi,
            Auditable auditableDef,
            String serviceName,
            String methodName,
            Map<String, Serializable> namedArguments) throws Throwable
    {
        try
        {
            auditInvocationBefore(serviceName, methodName, namedArguments);
        }
        catch (Throwable e)
        {
            // Failure to audit should not break the invocation
            logger.error(
                    "Failed to audit pre-invocation: \n" +
                    "   Invocation: " + mi,
                    e);
        }
        
        // Execute the call
        Object ret = null;
        Throwable thrown = null;
        try
        {
            ret = mi.proceed();
        }
        catch (Throwable e)
        {
            thrown = e;
        }
        
        // We don't ALWAYS want to record the return value
        Object auditRet = auditableDef.recordReturnedObject() ? ret : null;
        try
        {
            auditInvocationAfter(serviceName, methodName, namedArguments, auditRet, thrown);
        }
        catch (Throwable e)
        {
            // Failure to audit should not break the invocation
            logger.error(
                    "Failed to audit post-invocation: \n" +
                    "   Invocation: " + mi,
                    e);
        }
        
        // Done
        if (thrown != null)
        {
            throw thrown;
        }
        else
        {
            return ret;
        }
    }
    
    /**
     * @return                  Returns the arguments mapped by name
     * 
     * @since 3.2
     */
    private Map<String, Serializable> getInvocationArguments(Auditable auditableDef, Object[] args)
    {
        // Use the annotation to name the arguments
        String[] params = auditableDef.parameters();
        boolean[] recordable = auditableDef.recordable();
        
        Map<String, Serializable> namedArgs = new HashMap<String, Serializable>(args.length * 2);
        for (int i = 0; i < args.length; i++)
        {
            if (i >= params.length)
            {
                // The name list is finished.  Unnamed arguments are not recorded.
                break;
            }
            if (i < recordable.length)
            {
                // Arguments are recordable by default
                if (!recordable[i])
                {
                    // Don't record the argument
                    continue;
                }
            }
            Serializable arg;
            if (args[i] == null)
            {
                arg = null;
            }
            else if (args[i] instanceof Serializable)
            {
                arg = (Serializable) args[i];
            }
            else
            {
                // TODO: How to treat non-serializable args
                // arg = args[i].toString();
                try
                {
                    arg = DefaultTypeConverter.INSTANCE.convert(String.class, args[i]);
                }
                catch (TypeConversionException e)
                {
                    // No viable conversion
                    continue;
                }
            }
            // It is named and recordable
            namedArgs.put(params[i], arg);
        }
        // Done
        return namedArgs;
    }
    
    /**
     * Audit values before the invocation
     * 
     * @param serviceName       the service name
     * @param methodName        the method name
     * @param namedArguments    the named arguments passed to the invocation
     * 
     * @since 3.2
     */
    private void auditInvocationBefore(
            final String serviceName,
            final String methodName,
            final Map<String, Serializable> namedArguments)
    {
        final String rootPath = AuditApplication.buildPath(AUDIT_PATH_API_PRE, serviceName, methodName, AUDIT_SNIPPET_ARGS);
        
        // Audit in a read-write txn
        Map<String, Serializable> auditedData = auditComponent.recordAuditValues(rootPath, namedArguments);
        // Done
        if (logger.isDebugEnabled() && auditedData.size() > 0)
        {
            logger.debug(
                    "Audited before invocation: \n" +
                    "   Values: " + auditedData);
        }
    }
    
    /**
     * Audit values after the invocation
     * 
     * @param serviceName       the service name
     * @param methodName        the method name
     * @param namedArguments    the named arguments passed to the invocation
     * @param ret               the result of the execution (may be <tt>null</tt>)
     * @param thrown            the error thrown by the invocation (may be <tt>null</tt>)
     * 
     * @since 3.2
     */
    private void auditInvocationAfter(
            String serviceName, String methodName, Map<String, Serializable> namedArguments,
            Object ret, Throwable thrown)
    {
        final String rootPath = AuditApplication.buildPath(AUDIT_PATH_API_POST, serviceName, methodName);
        
        final Map<String, Serializable> auditData = new HashMap<String, Serializable>(23);
        for (Map.Entry<String, Serializable> entry : namedArguments.entrySet())
        {
            String argName = entry.getKey();
            Serializable argValue = entry.getValue();
            auditData.put(
                    AuditApplication.buildPath(AUDIT_SNIPPET_ARGS, argName),
                    argValue);
        }
        if (ret != null)
        {
            if (ret instanceof Serializable)
            {
                auditData.put(AUDIT_SNIPPET_RESULT, (Serializable) ret);
            }
            else
            {
                // TODO: How do we treat non-serializable return values
                try
                {
                    ret = DefaultTypeConverter.INSTANCE.convert(String.class, ret);
                    auditData.put(AUDIT_SNIPPET_RESULT, (String) ret);
                }
                catch (TypeConversionException e)
                {
                    // No viable conversion
                }
            }
        }
        Map<String, Serializable> auditedData;
        if (thrown != null)
        {
            StringBuilder sb = new StringBuilder(1024);
            StackTraceUtil.buildStackTrace(
                    thrown.getMessage(), thrown.getStackTrace(), sb, Integer.MAX_VALUE);
            auditData.put(AUDIT_SNIPPET_ERROR, sb.toString());
            
            // An exception will generally roll the current transaction back
            RetryingTransactionCallback<Map<String, Serializable>> auditCallback =
                    new RetryingTransactionCallback<Map<String, Serializable>>()
            {
                public Map<String, Serializable> execute() throws Throwable
                {
                    return auditComponent.recordAuditValues(rootPath, auditData);
                }
            };
            auditedData = transactionService.getRetryingTransactionHelper().doInTransaction(auditCallback, false, true);
        }
        else
        {
            // The current transaction will be fine
            auditedData = auditComponent.recordAuditValues(rootPath, auditData);
        }
        
        // Done
        if (logger.isDebugEnabled() && auditedData.size() > 0)
        {
            logger.debug(
                    "Audited before invocation: \n" +
                    (thrown == null ? "" : "   Exception: " + thrown.getMessage() + "\n") +
                    "   Values: " + auditedData);
        }
    }
}
