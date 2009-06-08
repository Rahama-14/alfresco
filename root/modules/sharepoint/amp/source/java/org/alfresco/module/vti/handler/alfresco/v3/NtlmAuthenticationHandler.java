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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.vti.handler.alfresco.v3;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.alfresco.jlan.server.auth.PasswordEncryptor;
import org.alfresco.jlan.server.auth.ntlm.NTLM;
import org.alfresco.jlan.server.auth.ntlm.NTLMLogonDetails;
import org.alfresco.jlan.server.auth.ntlm.NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.TargetInfo;
import org.alfresco.jlan.server.auth.ntlm.Type1NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type2NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type3NTLMMessage;
import org.alfresco.jlan.util.DataPacker;
import org.alfresco.model.ContentModel;
import org.alfresco.module.vti.handler.VtiHandlerException;
import org.alfresco.module.vti.handler.alfresco.AbstractAuthenticationHandler;
import org.alfresco.repo.SessionUser;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.MD4PasswordEncoder;
import org.alfresco.repo.security.authentication.MD4PasswordEncoderImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authentication.ntlm.NLTMAuthenticator;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>NTLM SSO web authentication implementation.</p>
 * 
 * @author PavelYur
 *
 */
public class NtlmAuthenticationHandler extends AbstractAuthenticationHandler
{
    
    private static Log logger = LogFactory.getLog(NtlmAuthenticationHandler.class);
    
    private MD4PasswordEncoder md4Encoder = new MD4PasswordEncoderImpl();
    private PasswordEncryptor encryptor = new PasswordEncryptor();
    private Random random = new Random(System.currentTimeMillis());
    
    private NLTMAuthenticator authenticationComponent;
    private TransactionService transactionService;
    private NodeService nodeService;
    

    private static final int ntlmFlags =  NTLM.Flag56Bit +
                                            NTLM.FlagLanManKey +
                                            NTLM.FlagNegotiateNTLM +
                                            NTLM.FlagNegotiateOEM +
                                            NTLM.FlagNegotiateUnicode;
    
    public void setAuthenticationComponent(NLTMAuthenticator authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }
    
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }    
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    @Override
    public String getWWWAuthenticate()
    {        
        return NTLM_START;
    }

    public SessionUser authenticateRequest(HttpServletRequest request, HttpServletResponse response, String alfrescoContext)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Start NTML authentication for request: " + request.getRequestURI());
        }
        
        HttpSession session = request.getSession();
        SessionUser user = (SessionUser)session.getAttribute(AUTHENTICATION_USER);
        
        String authHdr = request.getHeader(HEADER_AUTHORIZATION);
        
        boolean needToAuthenticate = false;

        if (authHdr != null && authHdr.startsWith(NTLM_START))
        {            
            needToAuthenticate = true;
        }
        
        if (user != null && needToAuthenticate == false)
        {
            try
            {
                authenticationService.validate(user.getTicket());
                needToAuthenticate = false;
            }
            catch (AuthenticationException e)
            {
                session.removeAttribute(AUTHENTICATION_USER);
                needToAuthenticate = true;
            }
        }
        
        if (needToAuthenticate == false && user != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("NTLM header doesn't presents. Authenticated by user from session. Username: " + user.getUserName());
            }
            return user;
        }

        if (authHdr == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("NTLM header doesn't presents. No user was found in session. Return 401 status.");
            }
            removeNtlmLogonDetailsFromSession(request);
            forceClientToPromptLogonDetails(response); 
            return null;
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("NTLM header presents in request.");
            }
            // Decode the received NTLM blob and validate
            final byte[] ntlmByts = Base64.decodeBase64(authHdr.substring(5).getBytes());
            int ntlmTyp = NTLMMessage.isNTLMType(ntlmByts);
            if (ntlmTyp == NTLM.Type1)
            {
                Type1NTLMMessage type1Msg = new Type1NTLMMessage(ntlmByts);
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Start process type 1 message.");
                    }
                    processType1(type1Msg, request, response, session);
                    user = null;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Finish process type 1 message.");
                    }
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Process type 1 message fail with error: " + e.getMessage());
                    }
                    session.removeAttribute(AUTHENTICATION_USER);
                    removeNtlmLogonDetailsFromSession(request);
                    return null;
                }
                
            }
            else if (ntlmTyp == NTLM.Type3)
            {
                Type3NTLMMessage type3Msg = new Type3NTLMMessage(ntlmByts);
                
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Start process message type 3.");
                    }
                    user = processType3(type3Msg, request, response, session, alfrescoContext);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Finish process message type 3.");
                    }
                }
                catch (Exception e)
                {
                    if (e instanceof VtiHandlerException)
                    {
                        throw (VtiHandlerException)e;                        
                    }
                    if (user != null)
                    {
                        try
                        {
                            authenticationService.validate(user.getTicket());
                            return user;
                        }
                        catch(AuthenticationException ae)
                        {                               
                        }
                    }    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Process message type 3 fail with message: " + e.getMessage());
                    }
                    session.removeAttribute(AUTHENTICATION_USER);
                    removeNtlmLogonDetailsFromSession(request);
                    return null;                    
                }
            }
    
            return user;
        }
    }
    
    private void processType1(Type1NTLMMessage type1Msg, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException 
    {
        removeNtlmLogonDetailsFromSession(request);
        
        NTLMLogonDetails ntlmDetails = new NTLMLogonDetails();
            
        // Set the 8 byte challenge for the new logon request
        byte[] challenge = null;                    
            
        // Generate a random 8 byte challenge        
        challenge = new byte[8];
        DataPacker.putIntelLong(random.nextLong(), challenge, 0);            
            
        // Get the flags from the client request and mask out unsupported features
        int flags = type1Msg.getFlags() & ntlmFlags;

        // Build a type2 message to send back to the client, containing the challenge
        List<TargetInfo> tList = new ArrayList<TargetInfo>();
        String srvName = getServerName();
        tList.add(new TargetInfo(NTLM.TargetServer, srvName));

        Type2NTLMMessage type2Msg = new Type2NTLMMessage();
        type2Msg.buildType2(flags, srvName, challenge, null, tList);

        // Store the NTLM logon details, cache the type2 message, and token if using passthru
        ntlmDetails.setType2Message(type2Msg);
        ntlmDetails.setAuthenticationToken(null);

        putNtlmLogonDetailsToSession(request, ntlmDetails);

        // Send back a request for NTLM authentication
        byte[] type2Bytes = type2Msg.getBytes();
        String ntlmBlob = "NTLM " + new String(Base64.encodeBase64(type2Bytes));

        response.setHeader(HEADER_WWW_AUTHENTICATE, ntlmBlob);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.flushBuffer();
        response.getOutputStream().close();
        
    }
    
    private SessionUser processType3(Type3NTLMMessage type3Msg, HttpServletRequest request, HttpServletResponse response, HttpSession session, String alfrescoContext) throws IOException, ServletException {
        
        // Get the existing NTLM details
        NTLMLogonDetails ntlmDetails = null;
        SessionUser user = null;

        if (session != null)
        {
            ntlmDetails = getNtlmLogonDetailsFromSession(request);    
            user = (SessionUser) session.getAttribute(AUTHENTICATION_USER);
        }

        // Get the NTLM logon details
        String userName = type3Msg.getUserName();
        String workstation = type3Msg.getWorkstation();
        String domain = type3Msg.getDomain();

        boolean authenticated = false;
        
        // Get the stored MD4 hashed password for the user, or null if the user does not exist
        String md4hash = getMD4Hash(userName);
        
        if (md4hash != null)
        {
            authenticated = validateLocalHashedPassword(type3Msg, ntlmDetails, authenticated, md4hash);
        }
        else
        {                
            authenticated = false;
        }         
        
        // Check if the user has been authenticated, if so then setup the user environment
        if (authenticated == true && isSiteMember(request, alfrescoContext, userName))
        {            
            String uri = request.getRequestURI();
            
            if (request.getMethod().equals("POST") && !uri.endsWith(".asmx"))
            {
                response.setHeader("Connection", "Close");            
                response.setContentType("application/x-vermeer-rpc");
            }
            
            if (user == null)
            {
                user = createUserEnvironment(session, userName);
            }
            else
            {
                // user already exists - revalidate ticket to authenticate the current user thread
                try
                {                    
                    authenticationService.validate(user.getTicket());
                }
                catch (AuthenticationException ex)
                {
                    session.removeAttribute(AUTHENTICATION_USER);
                    removeNtlmLogonDetailsFromSession(request);
                    return null;
                }
            }
            
            // Update the NTLM logon details in the session
            String srvName = getServerName();
            if (ntlmDetails == null)
            {
                // No cached NTLM details
                ntlmDetails = new NTLMLogonDetails(userName, workstation, domain, false, srvName);                
                putNtlmLogonDetailsToSession(request, ntlmDetails);
            }
            else
            {
                // Update the cached NTLM details
                ntlmDetails.setDetails(userName, workstation, domain, false, srvName);                
                putNtlmLogonDetailsToSession(request, ntlmDetails);
            }
        }
        else
        {
            removeNtlmLogonDetailsFromSession(request);
            session.removeAttribute(AUTHENTICATION_USER);
            return null;
        }
        return user;
    }    
    
    /*
     * returns server name
     */
    private String getServerName()
    {
        return "Alfresco Server";        
    }
    
    /*
     * Create the SessionUser object that represent currently authenticated user.
     */
    private SessionUser createUserEnvironment(HttpSession session, final String userName)
        throws IOException, ServletException
    {
        SessionUser user = null;
        
        UserTransaction tx = transactionService.getUserTransaction();
        
        try
        {
            tx.begin();
            
            RunAsWork<NodeRef> getUserNodeRefRunAsWork = new RunAsWork<NodeRef>()
            {
                public NodeRef doWork() throws Exception
                {
                    
                    return personService.getPerson(userName);
                }
            };
            
            NodeRef personNodeRef = AuthenticationUtil.runAs(getUserNodeRefRunAsWork, AuthenticationUtil.SYSTEM_USER_NAME);
            
            // Use the system user context to do the user lookup
            RunAsWork<String> getUserNameRunAsWork = new RunAsWork<String>()
            {
                public String doWork() throws Exception
                {
                    final NodeRef personNodeRef = personService.getPerson(userName);
                    return (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
                }
            };
            String username = AuthenticationUtil.runAs(getUserNameRunAsWork, AuthenticationUtil.SYSTEM_USER_NAME);
            
            authenticationComponent.setCurrentUser(userName);
            String currentTicket = authenticationService.getCurrentTicket();            
            
            
            // Create the user object to be stored in the session
            user = new User(username, currentTicket, personNodeRef);            
            
            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                tx.rollback();
            }
            catch (Exception err)
            {
                logger.error("Failed to rollback transaction", err);
            }
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException)ex;
            }
            else if (ex instanceof IOException)
            {
                throw (IOException)ex;
            }
            else if (ex instanceof ServletException)
            {
                throw (ServletException)ex;
            }
            else
            {
                throw new RuntimeException("Authentication setup failed", ex);
            }
        }
        
        // Store the user on the session
        session.setAttribute(AUTHENTICATION_USER, user);
        
        return user;
    }    
    
    /*
     * returns the hash of password
     */
    protected String getMD4Hash(String userName)
    {
        String md4hash = null;
        
        // Wrap the auth component calls in a transaction
        UserTransaction tx = transactionService.getUserTransaction();
        try
        {
            tx.begin();
            
            // Get the stored MD4 hashed password for the user, or null if the user does not exist
            md4hash = authenticationComponent.getMD4HashedPassword(userName);
            
            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                tx.rollback();
            }
            catch (Exception e)
            {
            }           
        }        
        
        return md4hash;
    }
    
    /*
     * Validate local hash for user password and hash that was sent by client
     */
    private boolean validateLocalHashedPassword(Type3NTLMMessage type3Msg, NTLMLogonDetails ntlmDetails, boolean authenticated, String md4hash)
    {   
        if ( ntlmDetails == null || ntlmDetails.getType2Message() == null)
        {   
            return false;
        }
        
        authenticated = checkNTLMv1(md4hash, ntlmDetails.getChallengeKey(), type3Msg, false);            
        
        return authenticated;
    }
    
    private final boolean checkNTLMv1(String md4hash, byte[] challenge, Type3NTLMMessage type3Msg, boolean checkLMHash)
    {
        // Generate the local encrypted password using the challenge that was sent to the client
        byte[] p21 = new byte[21];
        byte[] md4byts = md4Encoder.decodeHash(md4hash);
        System.arraycopy(md4byts, 0, p21, 0, 16);

        // Generate the local hash of the password using the same challenge
        byte[] localHash = null;

        try
        {
            localHash = encryptor.doNTLM1Encryption(p21, challenge);
        }
        catch (NoSuchAlgorithmException ex)
        {
        }

        // Validate the password
        byte[] clientHash = checkLMHash ? type3Msg.getLMHash() : type3Msg.getNTLMHash();

        if (clientHash != null && localHash != null && clientHash.length == localHash.length)
        {
            int i = 0;

            while (i < clientHash.length && clientHash[i] == localHash[i])
            {
                i++;
            }

            if (i == clientHash.length)
            {
                // Hashed passwords match
                return true;
            }
        }

        // Hashed passwords do not match
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private void putNtlmLogonDetailsToSession(HttpServletRequest request, NTLMLogonDetails details)
    {
        Object detailsMap = request.getSession().getAttribute(NTLM_AUTH_DETAILS);
        
        if (detailsMap != null)
        {
            ((Map<String, NTLMLogonDetails>)detailsMap).put(request.getRequestURI(), details);
            return;
        }
        else
        {
            Map<String, NTLMLogonDetails> newMap = new HashMap<String, NTLMLogonDetails>();
            newMap.put(request.getRequestURI(), details);
            request.getSession().setAttribute(NTLM_AUTH_DETAILS, newMap);
        }
    }
    
    @SuppressWarnings("unchecked")
    private NTLMLogonDetails getNtlmLogonDetailsFromSession(HttpServletRequest request)
    {
        Object detailsMap = request.getSession().getAttribute(NTLM_AUTH_DETAILS);
        if (detailsMap != null)
        {
            return ((Map<String, NTLMLogonDetails>)detailsMap).get(request.getRequestURI());
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void removeNtlmLogonDetailsFromSession(HttpServletRequest request)
    {
        Object detailsMap = request.getSession().getAttribute(NTLM_AUTH_DETAILS);
        if (detailsMap != null)
        {
            ((Map<String, NTLMLogonDetails>)detailsMap).remove(request.getRequestURI());
        }
    }
}