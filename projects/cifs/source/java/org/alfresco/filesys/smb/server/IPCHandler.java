package org.alfresco.filesys.smb.server;

import java.io.IOException;

import org.alfresco.filesys.server.filesys.NetworkFile;
import org.alfresco.filesys.server.filesys.TooManyFilesException;
import org.alfresco.filesys.server.filesys.TreeConnection;
import org.alfresco.filesys.smb.PacketType;
import org.alfresco.filesys.smb.SMBStatus;
import org.alfresco.filesys.smb.TransactionNames;
import org.alfresco.filesys.smb.dcerpc.DCEPipeType;
import org.alfresco.filesys.smb.dcerpc.server.DCEPipeFile;
import org.alfresco.filesys.smb.dcerpc.server.DCEPipeHandler;
import org.alfresco.filesys.util.DataBuffer;
import org.alfresco.filesys.util.DataPacker;
import org.apache.log4j.Logger;

/**
 * <p>
 * The IPCHandler class processes requests made on the IPC$ remote admin pipe. The code is shared
 * amongst different SMB protocol handlers.
 */
class IPCHandler
{

    // Debug logging

    private static final Logger logger = Logger.getLogger("org.alfresco.smb.protocol");

    /**
     * Process a request made on the IPC$ remote admin named pipe.
     * 
     * @param sess SMBSrvSession
     * @param outPkt SMBSrvPacket
     * @exception java.io.IOException If an I/O error occurs
     * @exception SMBSrvException If an SMB protocol error occurs
     */
    public static void processIPCRequest(SMBSrvSession sess, SMBSrvPacket outPkt) throws java.io.IOException,
            SMBSrvException
    {

        // Get the received packet from the session and verify that the connection is valid

        SMBSrvPacket smbPkt = sess.getReceivePacket();

        // Get the tree id from the received packet and validate that it is a valid
        // connection id.

        int treeId = smbPkt.getTreeId();
        TreeConnection conn = sess.findConnection(treeId);

        if (conn == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.DOSInvalidDrive, SMBStatus.ErrDos);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ Request [" + treeId + "] - cmd = " + smbPkt.getPacketTypeString());

        // Determine the SMB command

        switch (smbPkt.getCommand())
        {

        // Open file request

        case PacketType.OpenAndX:
        case PacketType.OpenFile:
            procIPCFileOpen(sess, smbPkt, outPkt);
            break;

        // Read file request

        case PacketType.ReadFile:
            procIPCFileRead(sess, smbPkt, outPkt);
            break;

        // Read AndX file request

        case PacketType.ReadAndX:
            procIPCFileReadAndX(sess, smbPkt, outPkt);
            break;

        // Write file request

        case PacketType.WriteFile:
            procIPCFileWrite(sess, smbPkt, outPkt);
            break;

        // Write AndX file request

        case PacketType.WriteAndX:
            procIPCFileWriteAndX(sess, smbPkt, outPkt);
            break;

        // Close file request

        case PacketType.CloseFile:
            procIPCFileClose(sess, smbPkt, outPkt);
            break;

        // NT create andX request

        case PacketType.NTCreateAndX:
            procNTCreateAndX(sess, smbPkt, outPkt);
            break;

        // Default, respond with an unsupported function error.

        default:
            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            break;
        }
    }

    /**
     * Process an IPC$ transaction request.
     * 
     * @param tbuf SrvTransactBuffer
     * @param sess SMBSrvSession
     * @param outPkt SMBSrvPacket
     */
    protected static void procTransaction(SrvTransactBuffer tbuf, SMBSrvSession sess, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ Transaction  pipe=" + tbuf.getName() + ", subCmd="
                    + NamedPipeTransaction.getSubCommand(tbuf.getFunction()));

        // Call the required transaction handler

        if (tbuf.getName().compareTo(TransactionNames.PipeLanman) == 0)
        {

            // Call the \PIPE\LANMAN transaction handler to process the request

            if (PipeLanmanHandler.processRequest(tbuf, sess, outPkt))
                return;
        }

        // Process the pipe command

        switch (tbuf.getFunction())
        {

        // Set named pipe handle state

        case NamedPipeTransaction.SetNmPHandState:
            procSetNamedPipeHandleState(sess, tbuf, outPkt);
            break;

        // Named pipe transation request, pass the request to the DCE/RPC handler

        case NamedPipeTransaction.TransactNmPipe:
            DCERPCHandler.processDCERPCRequest(sess, tbuf, outPkt);
            break;

        // Unknown command

        default:
            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            break;
        }
    }

    /**
     * Process a special IPC$ file open request.
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileOpen(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Get the data bytes position and length

        int dataPos = rxPkt.getByteOffset();
        int dataLen = rxPkt.getByteCount();
        byte[] buf = rxPkt.getBuffer();

        // Extract the filename string

        String fileName = DataPacker.getString(buf, dataPos, dataLen);

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ Open file = " + fileName);

        // Check if the requested IPC$ file is valid

        int pipeType = DCEPipeType.getNameAsType(fileName);
        if (pipeType == -1)
        {
            sess.sendErrorResponseSMB(SMBStatus.DOSFileNotFound, SMBStatus.ErrDos);
            return;
        }

        // Get the tree connection details

        int treeId = rxPkt.getTreeId();
        TreeConnection conn = sess.findConnection(treeId);

        if (conn == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.SRVInvalidTID, SMBStatus.ErrSrv);
            return;
        }

        // Create a network file for the special pipe

        DCEPipeFile pipeFile = new DCEPipeFile(pipeType);
        pipeFile.setGrantedAccess(NetworkFile.READWRITE);

        // Add the file to the list of open files for this tree connection

        int fid = -1;

        try
        {
            fid = conn.addFile(pipeFile, sess);
        }
        catch (TooManyFilesException ex)
        {

            // Too many files are open on this connection, cannot open any more files.

            sess.sendErrorResponseSMB(SMBStatus.DOSTooManyOpenFiles, SMBStatus.ErrDos);
            return;
        }

        // Build the open file response

        outPkt.setParameterCount(15);

        outPkt.setAndXCommand(0xFF);
        outPkt.setParameter(1, 0); // AndX offset

        outPkt.setParameter(2, fid);
        outPkt.setParameter(3, 0); // file attributes
        outPkt.setParameter(4, 0); // last write time
        outPkt.setParameter(5, 0); // last write date
        outPkt.setParameterLong(6, 0); // file size
        outPkt.setParameter(8, 0);
        outPkt.setParameter(9, 0);
        outPkt.setParameter(10, 0); // named pipe state
        outPkt.setParameter(11, 0);
        outPkt.setParameter(12, 0); // server FID (long)
        outPkt.setParameter(13, 0);
        outPkt.setParameter(14, 0);

        outPkt.setByteCount(0);

        // Send the response packet

        sess.sendResponseSMB(outPkt);
    }

    /**
     * Process an IPC pipe file read request
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileRead(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Check if the received packet is a valid read file request

        if (rxPkt.checkPacketIsValid(5, 0) == false)
        {

            // Invalid request

            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ File Read");

        // Pass the read request the DCE/RPC handler

        DCERPCHandler.processDCERPCRead(sess, rxPkt, outPkt);
    }

    /**
     * Process an IPC pipe file read andX request
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileReadAndX(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Check if the received packet is a valid read andX file request

        if (rxPkt.checkPacketIsValid(10, 0) == false)
        {

            // Invalid request

            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ File Read AndX");

        // Pass the read request the DCE/RPC handler

        DCERPCHandler.processDCERPCRead(sess, rxPkt, outPkt);
    }

    /**
     * Process an IPC pipe file write request
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileWrite(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Check if the received packet is a valid write file request

        if (rxPkt.checkPacketIsValid(5, 0) == false)
        {

            // Invalid request

            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ File Write");

        // Pass the write request the DCE/RPC handler

        DCERPCHandler.processDCERPCRequest(sess, rxPkt, outPkt);
    }

    /**
     * Process an IPC pipe file write andX request
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileWriteAndX(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Check if the received packet is a valid write andX request

        if (rxPkt.checkPacketIsValid(12, 0) == false)
        {

            // Invalid request

            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ File Write AndX");

        // Pass the write request the DCE/RPC handler

        DCERPCHandler.processDCERPCRequest(sess, rxPkt, outPkt);
    }

    /**
     * Process a special IPC$ file close request.
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procIPCFileClose(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Check that the received packet looks like a valid file close request

        if (rxPkt.checkPacketIsValid(3, 0) == false)
        {
            sess.sendErrorResponseSMB(SMBStatus.SRVUnrecognizedCommand, SMBStatus.ErrSrv);
            return;
        }

        // Get the tree id from the received packet and validate that it is a valid
        // connection id.

        int treeId = rxPkt.getTreeId();
        TreeConnection conn = sess.findConnection(treeId);

        if (conn == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.DOSInvalidDrive, SMBStatus.ErrDos);
            return;
        }

        // Get the file id from the request

        int fid = rxPkt.getParameter(0);
        DCEPipeFile netFile = (DCEPipeFile) conn.findFile(fid);

        if (netFile == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.DOSInvalidHandle, SMBStatus.ErrDos);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("IPC$ File close [" + treeId + "] fid=" + fid);

        // Remove the file from the connections list of open files

        conn.removeFile(fid, sess);

        // Build the close file response

        outPkt.setParameterCount(0);
        outPkt.setByteCount(0);

        // Send the response packet

        sess.sendResponseSMB(outPkt);
    }

    /**
     * Process a set named pipe handle state request
     * 
     * @param sess SMBSrvSession
     * @param tbuf SrvTransactBuffer
     * @param outPkt SMBSrvPacket
     */
    protected static void procSetNamedPipeHandleState(SMBSrvSession sess, SrvTransactBuffer tbuf, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Get the request parameters

        DataBuffer setupBuf = tbuf.getSetupBuffer();
        setupBuf.skipBytes(2);
        int fid = setupBuf.getShort();

        DataBuffer paramBuf = tbuf.getParameterBuffer();
        int state = paramBuf.getShort();

        // Get the connection for the request

        TreeConnection conn = sess.findConnection(tbuf.getTreeId());

        // Get the IPC pipe file for the specified file id

        DCEPipeFile netFile = (DCEPipeFile) conn.findFile(fid);
        if (netFile == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.DOSInvalidHandle, SMBStatus.ErrDos);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("  SetNmPHandState pipe=" + netFile.getName() + ", fid=" + fid + ", state=0x"
                    + Integer.toHexString(state));

        // Store the named pipe state

        netFile.setPipeState(state);

        // Setup the response packet

        SMBSrvTransPacket.initTransactReply(outPkt, 0, 0, 0, 0);

        // Send the response packet

        sess.sendResponseSMB(outPkt);
    }

    /**
     * Process an NT create andX request
     * 
     * @param sess SMBSrvSession
     * @param rxPkt SMBSrvPacket
     * @param outPkt SMBSrvPacket
     */
    protected static void procNTCreateAndX(SMBSrvSession sess, SMBSrvPacket rxPkt, SMBSrvPacket outPkt)
            throws IOException, SMBSrvException
    {

        // Get the tree id from the received packet and validate that it is a valid
        // connection id.

        int treeId = rxPkt.getTreeId();
        TreeConnection conn = sess.findConnection(treeId);

        if (conn == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.NTInvalidParameter, SMBStatus.NTErr);
            return;
        }

        // Extract the NT create andX parameters

        NTParameterPacker prms = new NTParameterPacker(rxPkt.getBuffer(), SMBSrvPacket.PARAMWORDS + 5);

        int nameLen = prms.unpackWord();
        int flags = prms.unpackInt();
        int rootFID = prms.unpackInt();
        int accessMask = prms.unpackInt();
        long allocSize = prms.unpackLong();
        int attrib = prms.unpackInt();
        int shrAccess = prms.unpackInt();
        int createDisp = prms.unpackInt();
        int createOptn = prms.unpackInt();
        int impersonLev = prms.unpackInt();
        int secFlags = prms.unpackByte();

        // Extract the filename string

        int pos = DataPacker.wordAlign(rxPkt.getByteOffset());
        String fileName = DataPacker.getUnicodeString(rxPkt.getBuffer(), pos, nameLen);
        if (fileName == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.NTInvalidParameter, SMBStatus.NTErr);
            return;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_IPC))
            logger.debug("NT Create AndX [" + treeId + "] name=" + fileName + ", flags=0x"
                    + Integer.toHexString(flags) + ", attr=0x" + Integer.toHexString(attrib) + ", allocSize="
                    + allocSize);

        // Check if the pipe name is a short or long name

        if (fileName.startsWith("\\PIPE") == false)
            fileName = "\\PIPE" + fileName;

        // Check if the requested IPC$ file is valid

        int pipeType = DCEPipeType.getNameAsType(fileName);
        if (pipeType == -1)
        {
            sess.sendErrorResponseSMB(SMBStatus.NTObjectNotFound, SMBStatus.NTErr);
            return;
        }

        // Check if there is a handler for the pipe file

        if (DCEPipeHandler.getHandlerForType(pipeType) == null)
        {
            sess.sendErrorResponseSMB(SMBStatus.NTAccessDenied, SMBStatus.NTErr);
            return;
        }

        // Create a network file for the special pipe

        DCEPipeFile pipeFile = new DCEPipeFile(pipeType);
        pipeFile.setGrantedAccess(NetworkFile.READWRITE);

        // Add the file to the list of open files for this tree connection

        int fid = -1;

        try
        {
            fid = conn.addFile(pipeFile, sess);
        }
        catch (TooManyFilesException ex)
        {

            // Too many files are open on this connection, cannot open any more files.

            sess.sendErrorResponseSMB(SMBStatus.Win32InvalidHandle, SMBStatus.NTErr);
            return;
        }

        // Build the NT create andX response

        outPkt.setParameterCount(34);

        prms.reset(outPkt.getBuffer(), SMBSrvPacket.PARAMWORDS + 4);

        prms.packByte(0);
        prms.packWord(fid);
        prms.packInt(0x0001); // File existed and was opened

        prms.packLong(0); // Creation time
        prms.packLong(0); // Last access time
        prms.packLong(0); // Last write time
        prms.packLong(0); // Change time

        prms.packInt(0x0080); // File attributes
        prms.packLong(4096); // Allocation size
        prms.packLong(0); // End of file
        prms.packWord(2); // File type - named pipe, message mode
        prms.packByte(0xFF); // Pipe instancing count
        prms.packByte(0x05); // IPC state bits

        prms.packByte(0); // directory flag

        outPkt.setByteCount(0);

        outPkt.setAndXCommand(0xFF);
        outPkt.setParameter(1, outPkt.getLength()); // AndX offset

        // Send the response packet

        sess.sendResponseSMB(outPkt);
    }
}