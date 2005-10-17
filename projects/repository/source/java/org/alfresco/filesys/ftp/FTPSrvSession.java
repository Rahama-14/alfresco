/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.filesys.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.transaction.UserTransaction;

import org.alfresco.filesys.server.SrvSession;
import org.alfresco.filesys.server.auth.ClientInfo;
import org.alfresco.filesys.server.auth.SrvAuthenticator;
import org.alfresco.filesys.server.auth.acl.AccessControl;
import org.alfresco.filesys.server.auth.acl.AccessControlManager;
import org.alfresco.filesys.server.core.SharedDevice;
import org.alfresco.filesys.server.core.SharedDeviceList;
import org.alfresco.filesys.server.filesys.AccessMode;
import org.alfresco.filesys.server.filesys.DiskDeviceContext;
import org.alfresco.filesys.server.filesys.DiskFullException;
import org.alfresco.filesys.server.filesys.DiskInterface;
import org.alfresco.filesys.server.filesys.DiskSharedDevice;
import org.alfresco.filesys.server.filesys.FileAction;
import org.alfresco.filesys.server.filesys.FileAttribute;
import org.alfresco.filesys.server.filesys.FileInfo;
import org.alfresco.filesys.server.filesys.FileOpenParams;
import org.alfresco.filesys.server.filesys.FileStatus;
import org.alfresco.filesys.server.filesys.NetworkFile;
import org.alfresco.filesys.server.filesys.NotifyChange;
import org.alfresco.filesys.server.filesys.SearchContext;
import org.alfresco.filesys.server.filesys.TreeConnection;
import org.alfresco.filesys.server.filesys.TreeConnectionHash;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FTP Server Session Class
 * 
 * @author GKSpencer
 */
public class FTPSrvSession extends SrvSession implements Runnable
{

    // Debug logging

    private static final Log logger = LogFactory.getLog("org.alfresco.ftp.protocol");

    // Constants
    //
    // Debug flag values

    public static final int DBG_STATE = 0x00000001; // Session state changes

    public static final int DBG_SEARCH = 0x00000002; // File/directory search

    public static final int DBG_INFO = 0x00000004; // Information requests

    public static final int DBG_FILE = 0x00000008; // File open/close/info

    public static final int DBG_FILEIO = 0x00000010; // File read/write

    public static final int DBG_ERROR = 0x00000020; // Errors

    public static final int DBG_PKTTYPE = 0x00000040; // Received packet type

    public static final int DBG_TIMING = 0x00000080; // Time packet

    // processing

    public static final int DBG_DATAPORT = 0x00000100; // Data port

    public static final int DBG_DIRECTORY = 0x00000200; // Directory commands

    // Anonymous user name

    private static final String USER_ANONYMOUS = "anonymous";

    // Root directory and FTP directory seperator

    private static final String ROOT_DIRECTORY = "/";

    private static final String FTP_SEPERATOR = "/";

    private static final char FTP_SEPERATOR_CHAR = '/';

    // Share relative path directory seperator

    private static final String DIR_SEPERATOR = "\\";

    private static final char DIR_SEPERATOR_CHAR = '\\';

    // File transfer buffer size

    private static final int DEFAULT_BUFFERSIZE = 64000;

    // Carriage return/line feed combination required for response messages

    protected final static String CRLF = "\r\n";

    // LIST command options

    protected final static String LIST_OPTION_HIDDEN = "-a";

    // Session socket

    private Socket m_sock;

    // Input/output streams to remote client

    private InputStreamReader m_in;

    private char[] m_inbuf;

    private OutputStreamWriter m_out;

    private StringBuffer m_outbuf;

    // Data connection

    private FTPDataSession m_dataSess;

    // Current working directory details
    //
    // First level is the share name then a path relative to the share root

    private FTPPath m_cwd;

    // Binary mode flag

    private boolean m_binary = false;

    // Restart position for binary file transfer

    private long m_restartPos = 0;

    // Rename from path details

    private FTPPath m_renameFrom;

    // Filtered list of shared filesystems available to this session

    private SharedDeviceList m_shares;

    // List of shared device connections used by this session

    private TreeConnectionHash m_connections;

    /**
     * Class constructor
     * 
     * @param sock
     *            Socket
     * @param srv
     *            FTPServer
     */
    public FTPSrvSession(Socket sock, FTPNetworkServer srv)
    {
        super(-1, srv, "FTP", null);

        // Save the local socket

        m_sock = sock;

        // Set the socket linger options, so the socket closes immediately when
        // closed

        try
        {
            m_sock.setSoLinger(false, 0);
        }
        catch (SocketException ex)
        {
        }

        // Indicate that the user is not logged in

        setLoggedOn(false);

        // Allocate the FTP path

        m_cwd = new FTPPath();

        // Allocate the tree connection cache

        m_connections = new TreeConnectionHash();
    }

    /**
     * Close the FTP session, and associated data socket if active
     */
    public final void closeSession()
    {

        // Call the base class

        super.closeSession();

        // Close the data connection, if active

        if (m_dataSess != null)
        {
            getFTPServer().releaseDataSession(m_dataSess);
            m_dataSess = null;
        }

        // Close the socket first, if the client is still connected this should
        // allow the
        // input/output streams
        // to be closed

        if (m_sock != null)
        {
            try
            {
                m_sock.close();
            }
            catch (Exception ex)
            {
            }
            m_sock = null;
        }

        // Close the input/output streams

        if (m_in != null)
        {
            try
            {
                m_in.close();
            }
            catch (Exception ex)
            {
            }
            m_in = null;
        }

        if (m_out != null)
        {
            try
            {
                m_out.close();
            }
            catch (Exception ex)
            {
            }
            m_out = null;
        }

        // Remove session from server session list

        getFTPServer().removeSession(this);

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
            logger.debug("Session closed, " + getSessionId());
    }

    /**
     * Return the current working directory
     * 
     * @return String
     */
    public final String getCurrentWorkingDirectory()
    {
        return m_cwd.getFTPPath();
    }

    /**
     * Return the server that this session is associated with.
     * 
     * @return FTPServer
     */
    public final FTPNetworkServer getFTPServer()
    {
        return (FTPNetworkServer) getServer();
    }

    /**
     * Return the client network address
     * 
     * @return InetAddress
     */
    public final InetAddress getRemoteAddress()
    {
        return m_sock.getInetAddress();
    }

    /**
     * Check if there is a current working directory
     * 
     * @return boolean
     */
    public final boolean hasCurrentWorkingDirectory()
    {
        return m_cwd != null ? true : false;
    }

    /**
     * Set the default path for the session
     * 
     * @param rootPath
     *            FTPPath
     */
    public final void setRootPath(FTPPath rootPath)
    {

        // Initialize the current working directory using the root path

        m_cwd = new FTPPath(rootPath);
        m_cwd.setSharedDevice(getShareList(), this);
    }

    /**
     * Get the path details for the current request
     * 
     * @param req
     *            FTPRequest
     * @param filePath
     *            boolean
     * @return FTPPath
     */
    protected final FTPPath generatePathForRequest(FTPRequest req, boolean filePath)
    {
        return generatePathForRequest(req, filePath, true);
    }

    /**
     * Get the path details for the current request
     * 
     * @param req
     *            FTPRequest
     * @param filePath
     *            boolean
     * @param checkExists
     *            boolean
     * @return FTPPath
     */
    protected final FTPPath generatePathForRequest(FTPRequest req, boolean filePath, boolean checkExists)
    {

        // Convert the path to an FTP format path

        String path = convertToFTPSeperators(req.getArgument());

        // Check if the path is the root directory and there is a default root
        // path configured

        FTPPath ftpPath = null;

        if (path.compareTo(ROOT_DIRECTORY) == 0)
        {

            // Check if the FTP server has a default root directory configured

            FTPNetworkServer ftpSrv = (FTPNetworkServer) getServer();
            if (ftpSrv.hasRootPath())
                ftpPath = ftpSrv.getRootPath();
            else
            {
                try
                {
                    ftpPath = new FTPPath("/");
                }
                catch (Exception ex)
                {
                }
                return ftpPath;
            }
        }

        // Check if the path is relative

        else if (FTPPath.isRelativePath(path) == false)
        {

            // Create a new path for the directory

            try
            {
                ftpPath = new FTPPath(path);
            }
            catch (InvalidPathException ex)
            {
                return null;
            }

            // Find the associated shared device

            if (ftpPath.setSharedDevice(getShareList(), this) == false)
                return null;

            // Return the new path

            return ftpPath;
        }
        else
        {

            // Check for the special '.' directory, just return the current
            // working directory

            if (path.equals("."))
                return m_cwd;

            // Check for the special '..' directory, if already at the root
            // directory return an
            // error

            if (path.equals(".."))
            {

                // Check if we are already at the root path

                if (m_cwd.isRootPath() == false)
                {

                    // Remove the last directory from the path

                    m_cwd.removeDirectory();
                    m_cwd.setSharedDevice(getShareList(), this);
                    return m_cwd;
                }
                else
                    return null;
            }

            // Create a copy of the current working directory and append the new
            // file/directory name

            ftpPath = new FTPPath(m_cwd);

            // Check if the root directory/share has been set

            if (ftpPath.isRootPath())
            {

                // Path specifies the share name

                try
                {
                    ftpPath.setSharePath(path, null);
                }
                catch (InvalidPathException ex)
                {
                    return null;
                }
            }
            else
            {
                if (filePath)
                    ftpPath.addFile(path);
                else
                    ftpPath.addDirectory(path);
            }

            // Find the associated shared device, if not already set

            if (ftpPath.hasSharedDevice() == false && ftpPath.setSharedDevice(getShareList(), this) == false)
                return null;
        }

        // Check if the generated path exists

        if (checkExists)
        {

            // Check if the new path exists and is a directory

            DiskInterface disk = null;
            TreeConnection tree = null;

            try
            {

                // Create a temporary tree connection

                tree = getTreeConnection(ftpPath.getSharedDevice());

                // Access the virtual filesystem driver

                disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();

                // Check if the path exists

                int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

                if (sts == FileStatus.NotExist)
                {

                    // Get the path string, check if there is a leading
                    // seperator

                    String pathStr = req.getArgument();
                    if (pathStr.startsWith(FTP_SEPERATOR) == false)
                        pathStr = FTP_SEPERATOR + pathStr;

                    // Create the root path

                    ftpPath = new FTPPath(pathStr);

                    // Find the associated shared device

                    if (ftpPath.setSharedDevice(getShareList(), this) == false)
                        ftpPath = null;
                }
                else if ((sts == FileStatus.FileExists && filePath == false)
                        || (sts == FileStatus.DirectoryExists && filePath == true))
                {

                    // Path exists but is the wrong type (directory or file)

                    ftpPath = null;
                }
            }
            catch (Exception ex)
            {
                ftpPath = null;
            }
        }

        // Return the new path

        return ftpPath;
    }

    /**
     * Convert a path string from share path seperators to FTP path seperators
     * 
     * @param path
     *            String
     * @return String
     */
    protected final String convertToFTPSeperators(String path)
    {

        // Check if the path is valid

        if (path == null || path.indexOf(DIR_SEPERATOR) == -1)
            return path;

        // Replace the path seperators

        return path.replace(DIR_SEPERATOR_CHAR, FTP_SEPERATOR_CHAR);
    }

    /**
     * Find the required disk shared device
     * 
     * @param name
     *            String
     * @return DiskSharedDevice
     */
    protected final DiskSharedDevice findShare(String name)
    {

        // Check if the name is valid

        if (name == null)
            return null;

        // Find the required disk share

        SharedDevice shr = getFTPServer().getShareList().findShare(m_cwd.getShareName());

        if (shr != null && shr instanceof DiskSharedDevice)
            return (DiskSharedDevice) shr;

        // Disk share not found

        return null;
    }

    /**
     * Set the binary mode flag
     * 
     * @param bin
     *            boolean
     */
    protected final void setBinary(boolean bin)
    {
        m_binary = bin;
    }

    /**
     * Send an FTP command response
     * 
     * @param stsCode
     *            int
     * @param msg
     *            String
     * @exception IOException
     */
    protected final void sendFTPResponse(int stsCode, String msg) throws IOException
    {

        // Build the output record

        m_outbuf.setLength(0);
        m_outbuf.append(stsCode);
        m_outbuf.append(" ");

        if (msg != null)
            m_outbuf.append(msg);

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_ERROR) && stsCode >= 500)
            logger.debug("Error status=" + stsCode + ", msg=" + msg);

        // Add the CR/LF

        m_outbuf.append(CRLF);

        // Output the FTP response

        if (m_out != null)
        {
            m_out.write(m_outbuf.toString());
            m_out.flush();
        }
    }

    /**
     * Send an FTP command response
     * 
     * @param msg
     *            StringBuffer
     * @exception IOException
     */
    protected final void sendFTPResponse(StringBuffer msg) throws IOException
    {

        // Output the FTP response

        if (m_out != null)
        {
            m_out.write(msg.toString());
            m_out.write(CRLF);
            m_out.flush();
        }
    }

    /**
     * Process a user command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procUser(FTPRequest req) throws IOException
    {

        // Clear the current client information

        setClientInformation(null);
        setLoggedOn(false);

        // Check if a user name has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error in parameters or arguments");
            return;
        }

        // Check for an anonymous login

        if (getFTPServer().allowAnonymous() == true
                && req.getArgument().equalsIgnoreCase(getFTPServer().getAnonymousAccount()))
        {

            // Anonymous login, create guest client information

            ClientInfo cinfo = new ClientInfo(getFTPServer().getAnonymousAccount(), null);
            cinfo.setGuest(true);
            setClientInformation(cinfo);

            // Return the anonymous login response

            sendFTPResponse(331, "Guest login ok, send your complete e-mail address as password");
            return;
        }

        // Create client information for the user

        setClientInformation(new ClientInfo(req.getArgument(), null));

        // Valid user, wait for the password

        sendFTPResponse(331, "User name okay, need password for " + req.getArgument());
    }

    /**
     * Process a password command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procPassword(FTPRequest req) throws IOException
    {

        // Check if the client information has been set, this indicates a user
        // command has been
        // received

        if (hasClientInformation() == false)
        {
            sendFTPResponse(500, "Syntax error, command "
                    + FTPCommand.getCommandName(req.isCommand()) + " unrecognized");
            return;
        }

        // Check for an anonymous login, accept any password string

        if (getClientInformation().isGuest())
        {

            // Save the anonymous login password string

            getClientInformation().setPassword(req.getArgument());

            // Accept the login

            setLoggedOn(true);
            sendFTPResponse(230, "User logged in, proceed");

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
                logger.debug("Anonymous login, info=" + req.getArgument());
        }

        // Validate the user

        else
        {

            // Get the client information and store the received plain text
            // password

            getClientInformation().setPassword(req.getArgument());

            // Authenticate the user

            SrvAuthenticator auth = getServer().getConfiguration().getAuthenticator();

            int access = auth.authenticateUserPlainText(getClientInformation(), this);

            if (access == SrvAuthenticator.AUTH_ALLOW)
            {

                // User successfully logged on

                sendFTPResponse(230, "User logged in, proceed");
                setLoggedOn(true);

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
                    logger.debug("User " + getClientInformation().getUserName() + ", logon successful");
            }
            else
            {

                // Return an access denied error

                sendFTPResponse(530, "Access denied");

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
                    logger.debug("User " + getClientInformation().getUserName() + ", logon failed");

                // Close the connection

                closeSession();
            }
        }

        // If the user has successfully logged on to the FTP server then inform
        // listeners

        if (isLoggedOn())
            getFTPServer().sessionLoggedOn(this);
    }

    /**
     * Process a port command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procPort(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if the parameter has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Required argument missing");
            return;
        }

        // Parse the address/port string into a IP address and port

        StringTokenizer token = new StringTokenizer(req.getArgument(), ",");
        if (token.countTokens() != 6)
        {
            sendFTPResponse(501, "Invalid argument");
            return;
        }

        // Parse the client address

        String addrStr = token.nextToken()
                + "." + token.nextToken() + "." + token.nextToken() + "." + token.nextToken();
        InetAddress addr = null;

        try
        {
            addr = InetAddress.getByName(addrStr);
        }
        catch (UnknownHostException ex)
        {
            sendFTPResponse(501, "Invalid argument (address)");
            return;
        }

        // Parse the client port

        int port = -1;

        try
        {
            port = Integer.parseInt(token.nextToken()) * 256;
            port += Integer.parseInt(token.nextToken());
        }
        catch (NumberFormatException ex)
        {
            sendFTPResponse(501, "Invalid argument (port)");
            return;
        }

        // Create an active data session, the actual socket connection will be
        // made later

        m_dataSess = getFTPServer().allocateDataSession(this, addr, port);

        // Return a success response to the client

        sendFTPResponse(200, "Port OK");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_DATAPORT))
            logger.debug("Port open addr=" + addr + ", port=" + port);
    }

    /**
     * Process a passive command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procPassive(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Create a passive data session

        try
        {
            m_dataSess = getFTPServer().allocateDataSession(this, null, 0);
        }
        catch (IOException ex)
        {
            m_dataSess = null;
        }

        // Check if the data session is valid

        if (m_dataSess == null)
        {
            sendFTPResponse(550, "Requested action not taken");
            return;
        }

        // Get the passive connection address/port and return to the client

        int pasvPort = m_dataSess.getPassivePort();

        StringBuffer msg = new StringBuffer();

        msg.append("227 Entering Passive Mode (");
        msg.append(getFTPServer().getLocalFTPAddressString());
        msg.append(",");
        msg.append(pasvPort >> 8);
        msg.append(",");
        msg.append(pasvPort & 0xFF);
        msg.append(")");

        sendFTPResponse(msg);

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_DATAPORT))
            logger.debug("Passive open addr=" + getFTPServer().getLocalFTPAddressString() + ", port=" + pasvPort);
    }

    /**
     * Process a print working directory command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procPrintWorkDir(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Return the current working directory virtual path

        sendFTPResponse(257, "\"" + m_cwd.getFTPPath() + "\"");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_DIRECTORY))
            logger.debug("Pwd ftp="
                    + m_cwd.getFTPPath() + ", share=" + m_cwd.getShareName() + ", path=" + m_cwd.getSharePath());
    }

    /**
     * Process a change working directory command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procChangeWorkDir(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if the request has a valid argument

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Path not specified");
            return;
        }

        // Create the new working directory path

        FTPPath newPath = generatePathForRequest(req, false);
        if (newPath == null)
        {
            sendFTPResponse(550, "Invalid path " + req.getArgument());
            return;
        }

        // Set the new current working directory

        m_cwd = newPath;

        // Return a success status

        sendFTPResponse(250, "Requested file action OK");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_DIRECTORY))
            logger.debug("Cwd ftp="
                    + m_cwd.getFTPPath() + ", share=" + m_cwd.getShareName() + ", path=" + m_cwd.getSharePath());
    }

    /**
     * Process a change directory up command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procCdup(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if there is a current working directory path

        if (m_cwd.isRootPath())
        {

            // Already at the root directory, return an error status

            sendFTPResponse(550, "Already at root directory");
            return;
        }
        else
        {

            // Remove the last directory from the path

            m_cwd.removeDirectory();
            if (m_cwd.isRootPath() == false && m_cwd.getSharedDevice() == null)
                m_cwd.setSharedDevice(getShareList(), this);
        }

        // Return a success status

        sendFTPResponse(250, "Requested file action OK");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_DIRECTORY))
            logger.debug("Cdup ftp="
                    + m_cwd.getFTPPath() + ", share=" + m_cwd.getShareName() + ", path=" + m_cwd.getSharePath());
    }

    /**
     * Process a long directory listing command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procList(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if the client has requested hidden files, via the '-a' option

        boolean hidden = false;

        if (req.hasArgument() && req.getArgument().startsWith(LIST_OPTION_HIDDEN))
        {
            // Indicate that we want hidden files in the listing

            hidden = true;

            // Remove the option from the command argument, and update the
            // request

            String arg = req.getArgument();
            int pos = arg.indexOf(" ");
            if (pos > 0)
                arg = arg.substring(pos + 1);
            else
                arg = null;

            req.updateArgument(arg);
        }

        // Create the path for the file listing

        FTPPath ftpPath = m_cwd;
        if (req.hasArgument())
            ftpPath = generatePathForRequest(req, true);

        if (ftpPath == null)
        {
            sendFTPResponse(500, "Invalid path");
            return;
        }

        // Check if the session has the required access

        if (ftpPath.isRootPath() == false)
        {

            // Check if the session has access to the filesystem

            TreeConnection tree = getTreeConnection(ftpPath.getSharedDevice());
            if (tree == null || tree.hasReadAccess() == false)
            {

                // Session does not have access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }
        }

        // Send the intermediate response

        sendFTPResponse(150, "File status okay, about to open data connection");

        // Check if there is an active data session

        if (m_dataSess == null)
        {
            sendFTPResponse(425, "Can't open data connection");
            return;
        }

        // Get the data connection socket

        Socket dataSock = null;

        try
        {
            dataSock = m_dataSess.getSocket();
        }
        catch (Exception ex)
        {
            logger.debug(ex);
        }

        if (dataSock == null)
        {
            sendFTPResponse(426, "Connection closed; transfer aborted");
            return;
        }

        // Output the directory listing to the client

        Writer dataWrt = null;

        try
        {

            // Open an output stream to the client

            dataWrt = new OutputStreamWriter(dataSock.getOutputStream());

            // Check if a path has been specified to list

            Vector<FileInfo> files = null;

            if (req.hasArgument())
            {
            }

            // Get a list of file information objects for the current directory

            files = listFilesForPath(ftpPath, false, hidden);

            // Output the file list to the client

            if (files != null)
            {

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_SEARCH))
                    logger.debug("List found " + files.size() + " files in " + ftpPath.getFTPPath());

                // Output the file information to the client

                StringBuffer str = new StringBuffer(256);

                for (FileInfo finfo : files)
                {

                    // Build the output record

                    str.setLength(0);

                    str.append(finfo.isDirectory() ? "d" : "-");
                    str.append("rw-rw-rw-   1 user group ");
                    str.append(finfo.getSize());
                    str.append(" ");

                    FTPDate.packUnixDate(str, new Date(finfo.getModifyDateTime()));

                    str.append(" ");
                    str.append(finfo.getFileName());
                    str.append(CRLF);

                    // Output the file information record

                    dataWrt.write(str.toString());
                }

                // Flush the data stream

                dataWrt.flush();
            }

            // Close the data stream and socket

            dataWrt.close();
            dataWrt = null;

            getFTPServer().releaseDataSession(m_dataSess);
            m_dataSess = null;

            // End of file list transmission

            sendFTPResponse(226, "Closing data connection");
        }
        catch (Exception ex)
        {

            // Failed to send file listing

            sendFTPResponse(451, "Error reading file list");
        } finally
        {

            // Close the data stream to the client

            if (dataWrt != null)
                dataWrt.close();

            // Close the data connection to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }
        }
    }

    /**
     * Process a short directory listing command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procNList(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Create the path for the file listing

        FTPPath ftpPath = m_cwd;
        if (req.hasArgument())
            ftpPath = generatePathForRequest(req, true);

        if (ftpPath == null)
        {
            sendFTPResponse(500, "Invalid path");
            return;
        }

        // Check if the session has the required access

        if (ftpPath.isRootPath() == false)
        {

            // Check if the session has access to the filesystem

            TreeConnection tree = getTreeConnection(ftpPath.getSharedDevice());
            if (tree == null || tree.hasReadAccess() == false)
            {

                // Session does not have access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }
        }

        // Send the intermediate response

        sendFTPResponse(150, "File status okay, about to open data connection");

        // Check if there is an active data session

        if (m_dataSess == null)
        {
            sendFTPResponse(425, "Can't open data connection");
            return;
        }

        // Get the data connection socket

        Socket dataSock = null;

        try
        {
            dataSock = m_dataSess.getSocket();
        }
        catch (Exception ex)
        {
            logger.error("Data socket error", ex);
        }

        if (dataSock == null)
        {
            sendFTPResponse(426, "Connection closed; transfer aborted");
            return;
        }

        // Output the directory listing to the client

        Writer dataWrt = null;

        try
        {

            // Open an output stream to the client

            dataWrt = new OutputStreamWriter(dataSock.getOutputStream());

            // Check if a path has been specified to list

            Vector<FileInfo> files = null;

            if (req.hasArgument())
            {
            }

            // Get a list of file information objects for the current directory

            files = listFilesForPath(ftpPath, false, false);

            // Output the file list to the client

            if (files != null)
            {

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_SEARCH))
                    logger.debug("List found " + files.size() + " files in " + ftpPath.getFTPPath());

                // Output the file information to the client

                for (FileInfo finfo : files)
                {

                    // Output the file information record

                    dataWrt.write(finfo.getFileName());
                    dataWrt.write(CRLF);
                }
            }

            // End of file list transmission

            sendFTPResponse(226, "Closing data connection");
        }
        catch (Exception ex)
        {

            // Failed to send file listing

            sendFTPResponse(451, "Error reading file list");
        } finally
        {

            // Close the data stream to the client

            if (dataWrt != null)
                dataWrt.close();

            // Close the data connection to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }
        }
    }

    /**
     * Process a system status command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procSystemStatus(FTPRequest req) throws IOException
    {

        // Return the system type

        sendFTPResponse(215, "UNIX Type: Java FTP Server");
    }

    /**
     * Process a server status command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procServerStatus(FTPRequest req) throws IOException
    {

        // Return server status information

        sendFTPResponse(211, "JLAN Server - Java FTP Server");
    }

    /**
     * Process a help command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procHelp(FTPRequest req) throws IOException
    {

        // Return help information

        sendFTPResponse(211, "HELP text");
    }

    /**
     * Process a no-op command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procNoop(FTPRequest req) throws IOException
    {

        // Return a response

        sendFTPResponse(200, "");
    }

    /**
     * Process a quit command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procQuit(FTPRequest req) throws IOException
    {

        // Return a response

        sendFTPResponse(221, "Bye");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
            logger.debug("Quit closing connection(s) to client");

        // Close the session(s) to the client

        closeSession();
    }

    /**
     * Process a type command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procType(FTPRequest req) throws IOException
    {

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Check if ASCII or binary mode is enabled

        String arg = req.getArgument().toUpperCase();
        if (arg.startsWith("A"))
            setBinary(false);
        else if (arg.startsWith("I") || arg.startsWith("L"))
            setBinary(true);
        else
        {

            // Invalid argument

            sendFTPResponse(501, "Syntax error, invalid parameter");
            return;
        }

        // Return a success status

        sendFTPResponse(200, "Command OK");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
            logger.debug("Type arg=" + req.getArgument() + ", binary=" + m_binary);
    }

    /**
     * Process a restart command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procRestart(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Validate the restart position

        try
        {
            m_restartPos = Integer.parseInt(req.getArgument());
        }
        catch (NumberFormatException ex)
        {
            sendFTPResponse(501, "Invalid restart position");
            return;
        }

        // Return a success status

        sendFTPResponse(350, "Restart OK");

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_FILEIO))
            logger.debug("Restart pos=" + m_restartPos);
    }

    /**
     * Process a return file command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procReturnFile(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Create the path for the file listing

        FTPPath ftpPath = generatePathForRequest(req, true);
        if (ftpPath == null)
        {
            sendFTPResponse(500, "Invalid path");
            return;
        }

        // Check if the path is the root directory

        if (ftpPath.isRootPath() || ftpPath.isRootSharePath())
        {
            sendFTPResponse(550, "That is a directory");
            return;
        }

        // Send the intermediate response

        sendFTPResponse(150, "Connection accepted");

        // Check if there is an active data session

        if (m_dataSess == null)
        {
            sendFTPResponse(425, "Can't open data connection");
            return;
        }

        // Get the data connection socket

        Socket dataSock = null;

        try
        {
            dataSock = m_dataSess.getSocket();
        }
        catch (Exception ex)
        {
        }

        if (dataSock == null)
        {
            sendFTPResponse(426, "Connection closed; transfer aborted");
            return;
        }

        // DEBUG

        if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
            logger.debug("Returning ftp="
                    + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path=" + ftpPath.getSharePath());

        // Send the file to the client

        OutputStream os = null;
        DiskInterface disk = null;
        TreeConnection tree = null;
        NetworkFile netFile = null;

        try
        {

            // Open an output stream to the client

            os = dataSock.getOutputStream();

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the file exists and it is a file, if so then open the
            // file

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();

            // Create the file open parameters

            FileOpenParams params = new FileOpenParams(ftpPath.getSharePath(), FileAction.OpenIfExists,
                    AccessMode.ReadOnly, 0);

            // Check if the file exists and it is a file

            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.FileExists)
            {

                // Open the file

                netFile = disk.openFile(this, tree, params);
            }

            // Check if the file has been opened

            if (netFile == null)
            {
                sendFTPResponse(550, "File " + req.getArgument() + " not available");
                return;
            }

            // Allocate the buffer for the file data

            byte[] buf = new byte[DEFAULT_BUFFERSIZE];
            long filePos = m_restartPos;

            int len = -1;

            while (filePos < netFile.getFileSize())
            {

                // Read another block of data from the file

                len = disk.readFile(this, tree, netFile, buf, 0, buf.length, filePos);

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_FILEIO))
                    logger.debug(" Write len=" + len + " bytes");

                // Write the current data block to the client, update the file
                // position

                if (len > 0)
                {

                    // Write the data to the client

                    os.write(buf, 0, len);

                    // Update the file position

                    filePos += len;
                }
            }

            // Close the output stream to the client

            os.close();
            os = null;

            // Indicate that the file has been transmitted

            sendFTPResponse(226, "Closing data connection");

            // Close the data session

            getFTPServer().releaseDataSession(m_dataSess);
            m_dataSess = null;

            // Close the network file

            disk.closeFile(this, tree, netFile);
            netFile = null;

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_FILEIO))
                logger.debug(" Transfer complete, file closed");
        }
        catch (SocketException ex)
        {

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_ERROR))
                logger.debug(" Error during transfer", ex);

            // Close the data socket to the client

            if (m_dataSess != null)
            {
                m_dataSess.closeSession();
                m_dataSess = null;
            }

            // Indicate that there was an error during transmission of the file
            // data

            sendFTPResponse(426, "Data connection closed by client");
        }
        catch (Exception ex)
        {

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_ERROR))
                logger.debug(" Error during transfer", ex);

            // Indicate that there was an error during transmission of the file
            // data

            sendFTPResponse(426, "Error during transmission");
        } finally
        {

            // Close the network file

            if (netFile != null && disk != null && tree != null)
                disk.closeFile(this, tree, netFile);

            // Close the output stream to the client

            if (os != null)
                os.close();

            // Close the data connection to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }
        }
    }

    /**
     * Process a store file command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procStoreFile(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Create the path for the file listing

        FTPPath ftpPath = generatePathForRequest(req, true, false);
        if (ftpPath == null)
        {
            sendFTPResponse(500, "Invalid path");
            return;
        }

        // Send the file to the client

        InputStream is = null;
        DiskInterface disk = null;
        TreeConnection tree = null;
        NetworkFile netFile = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the file exists

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.DirectoryExists)
            {

                // Return an error status

                sendFTPResponse(500, "Invalid path (existing directory)");
                return;
            }

            // Create the file open parameters

            FileOpenParams params = new FileOpenParams(ftpPath.getSharePath(),
                    sts == FileStatus.FileExists ? FileAction.TruncateExisting : FileAction.CreateNotExist,
                    AccessMode.ReadWrite, 0);

            // Create a new file to receive the data

            if (sts == FileStatus.FileExists)
            {

                // Overwrite the existing file

                netFile = disk.openFile(this, tree, params);
            }
            else
            {

                // Create a new file

                netFile = disk.createFile(this, tree, params);
            }

            // Notify change listeners that a new file has been created

            DiskDeviceContext diskCtx = (DiskDeviceContext) tree.getContext();

            if (diskCtx.hasChangeHandler())
                diskCtx.getChangeHandler().notifyFileChanged(NotifyChange.ActionAdded, ftpPath.getSharePath());

            // Send the intermediate response

            sendFTPResponse(150, "File status okay, about to open data connection");

            // Check if there is an active data session

            if (m_dataSess == null)
            {
                sendFTPResponse(425, "Can't open data connection");
                return;
            }

            // Get the data connection socket

            Socket dataSock = null;

            try
            {
                dataSock = m_dataSess.getSocket();
            }
            catch (Exception ex)
            {
            }

            if (dataSock == null)
            {
                sendFTPResponse(426, "Connection closed; transfer aborted");
                return;
            }

            // Open an input stream from the client

            is = dataSock.getInputStream();

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
                logger.debug("Storing ftp="
                        + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                        + ftpPath.getSharePath());

            // Allocate the buffer for the file data

            byte[] buf = new byte[DEFAULT_BUFFERSIZE];
            long filePos = 0;
            int len = is.read(buf, 0, buf.length);

            while (len > 0)
            {

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_FILEIO))
                    logger.debug(" Receive len=" + len + " bytes");

                // Write the current data block to the file, update the file
                // position

                disk.writeFile(this, tree, netFile, buf, 0, len, filePos);
                filePos += len;

                // Read another block of data from the client

                len = is.read(buf, 0, buf.length);
            }

            // Close the input stream from the client

            is.close();
            is = null;

            // Close the network file

            disk.closeFile(this, tree, netFile);
            netFile = null;

            // Indicate that the file has been received

            sendFTPResponse(226, "Closing data connection");

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_FILEIO))
                logger.debug(" Transfer complete, file closed");
        }
        catch (SocketException ex)
        {

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_ERROR))
                logger.debug(" Error during transfer", ex);

            // Close the data socket to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }

            // Indicate that there was an error during transmission of the file
            // data

            sendFTPResponse(426, "Data connection closed by client");
        }
        catch (DiskFullException ex)
        {

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_ERROR))
                logger.debug(" Error during transfer", ex);

            // Close the data socket to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }

            // Indicate that there was an error during writing of the file

            sendFTPResponse(451, "Disk full");
        }
        catch (Exception ex)
        {

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_ERROR))
                logger.debug(" Error during transfer", ex);
            ex.printStackTrace();

            // Indicate that there was an error during transmission of the file
            // data

            sendFTPResponse(426, "Error during transmission");
        } finally
        {

            // Close the network file

            if (netFile != null && disk != null && tree != null)
                disk.closeFile(this, tree, netFile);

            // Close the input stream to the client

            if (is != null)
                is.close();

            // Close the data connection to the client

            if (m_dataSess != null)
            {
                getFTPServer().releaseDataSession(m_dataSess);
                m_dataSess = null;
            }
        }
    }

    /**
     * Process a delete file command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procDeleteFile(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Create the path for the file

        FTPPath ftpPath = generatePathForRequest(req, true);
        if (ftpPath == null)
        {
            sendFTPResponse(550, "Invalid path specified");
            return;
        }

        // Delete the specified file

        DiskInterface disk = null;
        TreeConnection tree = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the file exists and it is a file

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.FileExists)
            {

                // Delete the file

                disk.deleteFile(this, tree, ftpPath.getSharePath());

                // Check if there are any file/directory change notify requests
                // active

                DiskDeviceContext diskCtx = (DiskDeviceContext) tree.getContext();
                if (diskCtx.hasChangeHandler())
                    diskCtx.getChangeHandler().notifyFileChanged(NotifyChange.ActionRemoved, ftpPath.getSharePath());

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
                    logger.debug("Deleted ftp="
                            + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                            + ftpPath.getSharePath());
            }
            else
            {

                // File does not exist or is a directory

                sendFTPResponse(550, "File "
                        + req.getArgument() + (sts == FileStatus.NotExist ? " not available" : " is a directory"));
                return;
            }
        }
        catch (Exception ex)
        {
            sendFTPResponse(450, "File action not taken");
            return;
        }

        // Return a success status

        sendFTPResponse(250, "File " + req.getArgument() + " deleted");
    }

    /**
     * Process a rename from command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procRenameFrom(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Clear the current rename from path details, if any

        m_renameFrom = null;

        // Create the path for the file/directory

        FTPPath ftpPath = generatePathForRequest(req, false, false);
        if (ftpPath == null)
        {
            sendFTPResponse(550, "Invalid path specified");
            return;
        }

        // Check that the file exists, and it is a file

        DiskInterface disk = null;
        TreeConnection tree = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the file exists and it is a file

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts != FileStatus.NotExist)
            {

                // Save the rename from file details, rename to command should
                // follow

                m_renameFrom = ftpPath;

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
                    logger.debug("RenameFrom ftp="
                            + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                            + ftpPath.getSharePath());
            }
            else
            {

                // File/directory does not exist

                sendFTPResponse(550, "File "
                        + req.getArgument() + (sts == FileStatus.NotExist ? " not available" : " is a directory"));
                return;
            }
        }
        catch (Exception ex)
        {
            sendFTPResponse(450, "File action not taken");
            return;
        }

        // Return a success status

        sendFTPResponse(350, "File " + req.getArgument() + " OK");
    }

    /**
     * Process a rename to command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procRenameTo(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Check if the rename from has already been set

        if (m_renameFrom == null)
        {
            sendFTPResponse(550, "Rename from not set");
            return;
        }

        // Create the path for the new file name

        FTPPath ftpPath = generatePathForRequest(req, true, false);
        if (ftpPath == null)
        {
            sendFTPResponse(550, "Invalid path specified");
            return;
        }

        // Check that the rename is on the same share

        if (m_renameFrom.getShareName().compareTo(ftpPath.getShareName()) != 0)
        {
            sendFTPResponse(550, "Cannot rename across shares");
            return;
        }

        // Rename the file

        DiskInterface disk = null;
        TreeConnection tree = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the file exists and it is a file

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.NotExist)
            {

                // Rename the file/directory

                disk.renameFile(this, tree, m_renameFrom.getSharePath(), ftpPath.getSharePath());

                // Check if there are any file/directory change notify requests
                // active

                DiskDeviceContext diskCtx = (DiskDeviceContext) tree.getContext();
                if (diskCtx.hasChangeHandler())
                    diskCtx.getChangeHandler().notifyRename(m_renameFrom.getSharePath(), ftpPath.getSharePath());

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
                    logger.debug("RenameTo ftp="
                            + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                            + ftpPath.getSharePath());
            }
            else
            {

                // File does not exist or is a directory

                sendFTPResponse(550, "File "
                        + req.getArgument() + (sts == FileStatus.NotExist ? " not available" : " is a directory"));
                return;
            }
        }
        catch (Exception ex)
        {
            sendFTPResponse(450, "File action not taken");
            return;
        } finally
        {

            // Clear the rename details

            m_renameFrom = null;
        }

        // Return a success status

        sendFTPResponse(250, "File renamed OK");
    }

    /**
     * Process a create directory command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procCreateDirectory(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Check if the new directory contains multiple directories

        FTPPath ftpPath = generatePathForRequest(req, false, false);
        if (ftpPath == null)
        {
            sendFTPResponse(550, "Invalid path " + req.getArgument());
            return;
        }

        // Create the new directory

        DiskInterface disk = null;
        TreeConnection tree = null;
        NetworkFile netFile = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the directory exists

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.NotExist)
            {

                // Create the new directory

                FileOpenParams params = new FileOpenParams(ftpPath.getSharePath(), FileAction.CreateNotExist,
                        AccessMode.ReadWrite, FileAttribute.NTDirectory);

                disk.createDirectory(this, tree, params);

                // Notify change listeners that a new directory has been created

                DiskDeviceContext diskCtx = (DiskDeviceContext) tree.getContext();

                if (diskCtx.hasChangeHandler())
                    diskCtx.getChangeHandler().notifyFileChanged(NotifyChange.ActionAdded, ftpPath.getSharePath());

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_DIRECTORY))
                    logger.debug("CreateDir ftp="
                            + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                            + ftpPath.getSharePath());
            }
            else
            {

                // File/directory already exists with that name, return an error

                sendFTPResponse(450, sts == FileStatus.FileExists ? "File exists with that name"
                        : "Directory already exists");
                return;
            }
        }
        catch (Exception ex)
        {
            sendFTPResponse(450, "Failed to create directory");
            return;
        }

        // Return the FTP path to the client

        sendFTPResponse(250, ftpPath.getFTPPath());
    }

    /**
     * Process a delete directory command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procRemoveDirectory(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Check if the directory path contains multiple directories

        FTPPath ftpPath = generatePathForRequest(req, false);
        if (ftpPath == null)
        {
            sendFTPResponse(550, "Invalid path " + req.getArgument());
            return;
        }

        // Check if the path is the root directory, cannot delete directories
        // from the root
        // directory
        // as it maps to the list of available disk shares.

        if (ftpPath.isRootPath() || ftpPath.isRootSharePath())
        {
            sendFTPResponse(550, "Access denied, cannot delete directory in root");
            return;
        }

        // Delete the directory

        DiskInterface disk = null;
        TreeConnection tree = null;
        NetworkFile netFile = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Check if the session has the required access to the filesystem

            if (tree == null || tree.hasWriteAccess() == false)
            {

                // Session does not have write access to the filesystem

                sendFTPResponse(550, "Access denied");
                return;
            }

            // Check if the directory exists

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();
            int sts = disk.fileExists(this, tree, ftpPath.getSharePath());

            if (sts == FileStatus.DirectoryExists)
            {

                // Delete the new directory

                disk.deleteDirectory(this, tree, ftpPath.getSharePath());

                // Check if there are any file/directory change notify requests
                // active

                DiskDeviceContext diskCtx = (DiskDeviceContext) tree.getContext();
                if (diskCtx.hasChangeHandler())
                    diskCtx.getChangeHandler().notifyFileChanged(NotifyChange.ActionRemoved, ftpPath.getSharePath());

                // DEBUG

                if (logger.isDebugEnabled() && hasDebug(DBG_DIRECTORY))
                    logger.debug("DeleteDir ftp="
                            + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", path="
                            + ftpPath.getSharePath());
            }
            else
            {

                // File already exists with that name or directory does not
                // exist return an error

                sendFTPResponse(550, sts == FileStatus.FileExists ? "File exists with that name"
                        : "Directory does not exist");
                return;
            }
        }
        catch (Exception ex)
        {
            sendFTPResponse(550, "Failed to delete directory");
            return;
        }

        // Return a success status

        sendFTPResponse(250, "Directory deleted OK");
    }

    /**
     * Process a modify date/time command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procModifyDateTime(FTPRequest req) throws IOException
    {

        // Return a success response

        sendFTPResponse(550, "Not implemented yet");
    }

    /**
     * Process a file size command
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procFileSize(FTPRequest req) throws IOException
    {

        // Check if the user is logged in

        if (isLoggedOn() == false)
        {
            sendFTPResponse(500, "");
            return;
        }

        // Check if an argument has been specified

        if (req.hasArgument() == false)
        {
            sendFTPResponse(501, "Syntax error, parameter required");
            return;
        }

        // Create the path for the file listing

        FTPPath ftpPath = generatePathForRequest(req, true);
        if (ftpPath == null)
        {
            sendFTPResponse(500, "Invalid path");
            return;
        }

        // Get the file information

        DiskInterface disk = null;
        TreeConnection tree = null;

        try
        {

            // Create a temporary tree connection

            tree = getTreeConnection(ftpPath.getSharedDevice());

            // Access the virtual filesystem driver

            disk = (DiskInterface) ftpPath.getSharedDevice().getInterface();

            // Get the file information

            FileInfo finfo = disk.getFileInformation(null, tree, ftpPath.getSharePath());

            if (finfo == null)
            {
                sendFTPResponse(550, "File " + req.getArgument() + " not available");
                return;
            }

            // Return the file size

            sendFTPResponse(213, "" + finfo.getSize());

            // DEBUG

            if (logger.isDebugEnabled() && hasDebug(DBG_FILE))
                logger.debug("File size ftp="
                        + ftpPath.getFTPPath() + ", share=" + ftpPath.getShareName() + ", size=" + finfo.getSize());
        }
        catch (Exception ex)
        {
            sendFTPResponse(550, "Error retrieving file size");
        }
    }

    /**
     * Process a structure command. This command is obsolete.
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procStructure(FTPRequest req) throws IOException
    {

        // Check for the file structure argument

        if (req.hasArgument() && req.getArgument().equalsIgnoreCase("F"))
            sendFTPResponse(200, "OK");

        // Return an error response

        sendFTPResponse(504, "Obsolete");
    }

    /**
     * Process a mode command. This command is obsolete.
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procMode(FTPRequest req) throws IOException
    {

        // Check for the stream transfer mode argument

        if (req.hasArgument() && req.getArgument().equalsIgnoreCase("S"))
            sendFTPResponse(200, "OK");

        // Return an error response

        sendFTPResponse(504, "Obsolete");
    }

    /**
     * Process an allocate command. This command is obsolete.
     * 
     * @param req
     *            FTPRequest
     * @exception IOException
     */
    protected final void procAllocate(FTPRequest req) throws IOException
    {

        // Return a response

        sendFTPResponse(202, "Obsolete");
    }

    /**
     * Build a list of file name or file information objects for the specified
     * server path
     * 
     * @param path
     *            FTPPath
     * @param nameOnly
     *            boolean
     * @param hidden
     *            boolean
     * @return Vector<FileInfo>
     */
    protected final Vector<FileInfo> listFilesForPath(FTPPath path, boolean nameOnly, boolean hidden)
    {

        // Check if the path is valid

        if (path == null)
            return null;

        // Check if the path is the root path

        Vector<FileInfo> files = new Vector<FileInfo>();

        if (path.hasSharedDevice() == false)
        {

            // The first level of directories are mapped to the available shares

            SharedDeviceList shares = getShareList();
            if (shares != null)
            {

                // Search for disk shares

                Enumeration<SharedDevice> enm = shares.enumerateShares();

                while (enm.hasMoreElements())
                {

                    // Get the current shared device

                    SharedDevice shr = enm.nextElement();

                    // Add the share name or full information to the list

                    if (nameOnly == false)
                    {

                        // Create a file information object for the top level
                        // directory details

                        FileInfo finfo = new FileInfo(shr.getName(), 0L, FileAttribute.Directory);
                        files.add(finfo);
                    }
                    else
                        files.add(new FileInfo(shr.getName(), 0L, FileAttribute.Directory));
                }
            }
        }
        else
        {

            // Append a wildcard to the search path

            String searchPath = path.getSharePath();

            if (path.isDirectory())
                searchPath = path.makeSharePathToFile("*");

            // Create a temporary tree connection

            TreeConnection tree = new TreeConnection(path.getSharedDevice());

            // Start a search on the specified disk share

            DiskInterface disk = null;
            SearchContext ctx = null;

            int searchAttr = FileAttribute.Directory + FileAttribute.Normal;
            if (hidden)
                searchAttr += FileAttribute.Hidden;

            try
            {
                disk = (DiskInterface) path.getSharedDevice().getInterface();
                ctx = disk.startSearch(this, tree, searchPath, searchAttr);
            }
            catch (Exception ex)
            {
            }

            // Add the files to the list

            if (ctx != null)
            {

                // Get the file names/information

                while (ctx.hasMoreFiles())
                {

                    // Check if a file name or file information is required

                    if (nameOnly)
                    {

                        // Add a file name to the list

                        files.add(new FileInfo(ctx.nextFileName(), 0L, 0));
                    }
                    else
                    {

                        // Create a file information object

                        FileInfo finfo = new FileInfo();

                        if (ctx.nextFileInfo(finfo) == false)
                            break;
                        if (finfo.getFileName() != null)
                            files.add(finfo);
                    }
                }
            }
        }

        // Return the list of file names/information

        return files;
    }

    /**
     * Get the list of filtered shares that are available to this session
     * 
     * @return SharedDeviceList
     */
    protected final SharedDeviceList getShareList()
    {

        // Check if the filtered share list has been initialized

        if (m_shares == null)
        {

            // Get a list of shared filesystems

            SharedDeviceList shares = getFTPServer().getShareMapper().getShareList(getFTPServer().getServerName(),
                    this, false);

            // Search for disk shares

            m_shares = new SharedDeviceList();
            Enumeration enm = shares.enumerateShares();

            while (enm.hasMoreElements())
            {

                // Get the current shared device

                SharedDevice shr = (SharedDevice) enm.nextElement();

                // Check if the share is a disk share

                if (shr instanceof DiskSharedDevice)
                    m_shares.addShare(shr);
            }

            // Check if there is an access control manager available, if so then
            // filter the list of
            // shared filesystems

            if (getServer().hasAccessControlManager())
            {

                // Get the access control manager

                AccessControlManager aclMgr = getServer().getAccessControlManager();

                // Filter the list of shared filesystems

                m_shares = aclMgr.filterShareList(this, m_shares);
            }
        }

        // Return the filtered shared filesystem list

        return m_shares;
    }

    /**
     * Get a tree connection for the specified shared device. Creates and caches
     * a new tree connection if required.
     * 
     * @param share
     *            SharedDevice
     * @return TreeConnection
     */
    protected final TreeConnection getTreeConnection(SharedDevice share)
    {

        // Check if the share is valid

        if (share == null)
            return null;

        // Check if there is a tree connection in the cache

        TreeConnection tree = m_connections.findConnection(share.getName());
        if (tree == null)
        {

            // Create a new tree connection

            tree = new TreeConnection(share);
            m_connections.addConnection(tree);

            // Set the access permission for the shared filesystem

            if (getServer().hasAccessControlManager())
            {

                // Set the access permission to the shared filesystem

                AccessControlManager aclMgr = getServer().getAccessControlManager();

                int access = aclMgr.checkAccessControl(this, share);
                if (access != AccessControl.Default)
                    tree.setPermission(access);
            }
        }

        // Return the connection

        return tree;
    }

    /**
     * Start the FTP session in a seperate thread
     */
    public void run()
    {
        // Transaction used to wrap all processing
        
        UserTransaction tx = null;
        TransactionService transactionService = getFTPServer().getConfiguration().getTransactionService();
        
        try
        {

            // Debug

            if (logger.isDebugEnabled() && hasDebug(DBG_STATE))
                logger.debug("FTP session started");

            // Create the input/output streams

            m_in = new InputStreamReader(m_sock.getInputStream());
            m_out = new OutputStreamWriter(m_sock.getOutputStream());

            m_inbuf = new char[512];
            m_outbuf = new StringBuffer(256);

            // Return the initial response

            sendFTPResponse(220, "FTP server ready");

            // Start/end times if timing debug is enabled

            long startTime = 0L;
            long endTime = 0L;

            // Create an FTP request to hold command details

            FTPRequest ftpReq = new FTPRequest();

            // The server session loops until the NetBIOS hangup state is set.

            int rdlen = -1;
            String cmd = null;

            while (m_sock != null)
            {

                // Wait for a data packet

                rdlen = m_in.read(m_inbuf);

                // Check if there is no more data, the other side has dropped
                // the connection

                if (rdlen == -1)
                {
                    closeSession();
                    continue;
                }

                // Trim the trailing <CR><LF>

                if (rdlen > 0)
                {
                    while (rdlen > 0 && m_inbuf[rdlen - 1] == '\r' || m_inbuf[rdlen - 1] == '\n')
                        rdlen--;
                }

                // Get the command string

                cmd = new String(m_inbuf, 0, rdlen);

                // Debug

                if (logger.isDebugEnabled() && hasDebug(DBG_TIMING))
                    startTime = System.currentTimeMillis();

                if (logger.isDebugEnabled() && hasDebug(DBG_PKTTYPE))
                    logger.debug("Cmd " + ftpReq);

                // Create a transaction for the request
                
                tx = transactionService.getUserTransaction();
                tx.begin();
                
                // Parse the received command, and validate

                ftpReq.setCommandLine(cmd);

                switch (ftpReq.isCommand())
                {

                // User command

                case FTPCommand.User:
                    procUser(ftpReq);
                    break;

                // Password command

                case FTPCommand.Pass:
                    procPassword(ftpReq);
                    break;

                // Quit command

                case FTPCommand.Quit:
                    procQuit(ftpReq);
                    break;

                // Type command

                case FTPCommand.Type:
                    procType(ftpReq);
                    break;

                // Port command

                case FTPCommand.Port:
                    procPort(ftpReq);
                    break;

                // Passive command

                case FTPCommand.Pasv:
                    procPassive(ftpReq);
                    break;

                // Restart position command

                case FTPCommand.Rest:
                    procRestart(ftpReq);
                    break;

                // Return file command

                case FTPCommand.Retr:
                    procReturnFile(ftpReq);

                    // Reset the restart position

                    m_restartPos = 0;
                    break;

                // Store file command

                case FTPCommand.Stor:
                    procStoreFile(ftpReq);
                    break;

                // Print working directory command

                case FTPCommand.Pwd:
                case FTPCommand.XPwd:
                    procPrintWorkDir(ftpReq);
                    break;

                // Change working directory command

                case FTPCommand.Cwd:
                case FTPCommand.XCwd:
                    procChangeWorkDir(ftpReq);
                    break;

                // Change to previous directory command

                case FTPCommand.Cdup:
                case FTPCommand.XCup:
                    procCdup(ftpReq);
                    break;

                // Full directory listing command

                case FTPCommand.List:
                    procList(ftpReq);
                    break;

                // Short directory listing command

                case FTPCommand.Nlst:
                    procNList(ftpReq);
                    break;

                // Delete file command

                case FTPCommand.Dele:
                    procDeleteFile(ftpReq);
                    break;

                // Rename file from command

                case FTPCommand.Rnfr:
                    procRenameFrom(ftpReq);
                    break;

                // Rename file to comand

                case FTPCommand.Rnto:
                    procRenameTo(ftpReq);
                    break;

                // Create new directory command

                case FTPCommand.Mkd:
                case FTPCommand.XMkd:
                    procCreateDirectory(ftpReq);
                    break;

                // Delete directory command

                case FTPCommand.Rmd:
                case FTPCommand.XRmd:
                    procRemoveDirectory(ftpReq);
                    break;

                // Return file size command

                case FTPCommand.Size:
                    procFileSize(ftpReq);
                    break;

                // Set modify date/time command

                case FTPCommand.Mdtm:
                    procModifyDateTime(ftpReq);
                    break;

                // System status command

                case FTPCommand.Syst:
                    procSystemStatus(ftpReq);
                    break;

                // Server status command

                case FTPCommand.Stat:
                    procServerStatus(ftpReq);
                    break;

                // Help command

                case FTPCommand.Help:
                    procHelp(ftpReq);
                    break;

                // No-op command

                case FTPCommand.Noop:
                    procNoop(ftpReq);
                    break;

                // Structure command (obsolete)

                case FTPCommand.Stru:
                    procStructure(ftpReq);
                    break;

                // Mode command (obsolete)

                case FTPCommand.Mode:
                    procMode(ftpReq);
                    break;

                // Allocate command (obsolete)

                case FTPCommand.Allo:
                    procAllocate(ftpReq);
                    break;

                // Unknown/unimplemented command

                default:
                    if (ftpReq.isCommand() != FTPCommand.InvalidCmd)
                        sendFTPResponse(502, "Command "
                                + FTPCommand.getCommandName(ftpReq.isCommand()) + " not implemented");
                    else
                        sendFTPResponse(502, "Command not implemented");
                    break;
                }

                // Debug

                if (logger.isDebugEnabled() && hasDebug(DBG_TIMING))
                {
                    endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    if (duration > 20)
                        logger.debug("Processed cmd "
                                + FTPCommand.getCommandName(ftpReq.isCommand()) + " in " + duration + "ms");
                }

                // Commit the transaction
                
                if ( tx != null)
                {
                    try
                    {
                        // Commit the transaction
                        
                        tx.commit();
                        tx = null;
                    }
                    catch (Exception ex)
                    {
                        logger.warn("Failed to rollback transaction", ex);
                    }
                }
                
            } // end while state
        }
        catch (SocketException ex)
        {

            // DEBUG

            if (logger.isErrorEnabled() && hasDebug(DBG_STATE))
                logger.error("Socket closed by remote client");
        }
        catch (Exception ex)
        {

            // Output the exception details

            if (isShutdown() == false)
            {
                logger.debug(ex);
            }
        }
        finally
        {
            // If there is an active transaction then roll it back
            
            if ( tx != null)
            {
                try
                {
                    tx.rollback();
                    tx = null;
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to rollback transaction", ex);
                }
            }                
        }

        // Cleanup the session, make sure all resources are released

        closeSession();

        // Debug

        if (hasDebug(DBG_STATE))
            logger.debug("Server session closed");
    }
}