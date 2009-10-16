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
package org.alfresco.filesys.repo;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.jlan.server.filesys.AccessDeniedException;
import org.alfresco.jlan.server.filesys.DiskFullException;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.smb.SeekType;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.usage.ContentQuotaException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of the <tt>NetworkFile</tt> for direct interaction
 * with the channel repository.
 * <p>
 * This provides the interaction with the Alfresco Content Model file/folder structure.
 * 
 * @author Derek Hulley
 */
public class ContentNetworkFile extends NodeRefNetworkFile
{
    private static final Log logger = LogFactory.getLog(ContentNetworkFile.class);
    
    // Services
    
    private NodeService nodeService;
    private ContentService contentService;
    private MimetypeService mimetypeService;
    
    // File channel to file content
    
    private FileChannel channel;
    
    // File content
    
    private ContentAccessor content;
    private ContentReader preUpdateContentReader;
    
    // Indicate if file has been written to or truncated/resized
    
    private boolean modified;
    
    // Flag to indicate if the file channel is writable
    
    private boolean writableChannel;

    /**
     * Helper method to create a {@link NetworkFile network file} given a node reference.
     */
    public static ContentNetworkFile createFile(
            NodeService nodeService,
            ContentService contentService,
            MimetypeService mimetypeService,
            CifsHelper cifsHelper,
            NodeRef nodeRef,
            FileOpenParams params)
    {
        String path = params.getPath();
        
        // Check write access
        // TODO: Check access writes and compare to write requirements
        
        // Create the file
        
        ContentNetworkFile netFile = null;
        
        if ( isMSOfficeSpecialFile(path)) {
        	
        	// Create a file for special processing
        	
        	netFile = new MSOfficeContentNetworkFile( nodeService, contentService, mimetypeService, nodeRef, path);
        }
        else {
        	
        	// Create a normal content file
        
        	netFile = new ContentNetworkFile(nodeService, contentService, mimetypeService, nodeRef, path);
        }
        
        // Set relevant parameters
        
        if (params.isReadOnlyAccess())
        {
            netFile.setGrantedAccess(NetworkFile.READONLY);
        }
        else
        {
            netFile.setGrantedAccess(NetworkFile.READWRITE);
        }
        
        // Check the type
        
        FileInfo fileInfo;
        try
        {
            fileInfo = cifsHelper.getFileInformation(nodeRef, "");
        }
        catch (FileNotFoundException e)
        {
            throw new AlfrescoRuntimeException("File not found when creating network file: " + nodeRef, e);
        }
        
        if (fileInfo.isDirectory())
        {
            netFile.setAttributes(FileAttribute.Directory);
        }
        else
        {
            // Set the current size
            
            netFile.setFileSize(fileInfo.getSize());
        }
        
        // Set the file timestamps
        
        if ( fileInfo.hasCreationDateTime())
            netFile.setCreationDate( fileInfo.getCreationDateTime());
        
        if ( fileInfo.hasModifyDateTime())
            netFile.setModifyDate(fileInfo.getModifyDateTime());
        
        if ( fileInfo.hasAccessDateTime())
            netFile.setAccessDate(fileInfo.getAccessDateTime());
        
        // Set the file attributes
        
        netFile.setAttributes(fileInfo.getFileAttributes());

        // If the file is read-only then only allow read access
        
        if ( netFile.isReadOnly())
            netFile.setGrantedAccess(NetworkFile.READONLY);
        
        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Create file node=" + nodeRef + ", param=" + params + ", netfile=" + netFile);

        // Return the network file
        
        return netFile;
    }

    /**
     * Class constructor
     * 
     * @param transactionService TransactionService
     * @param nodeService NodeService
     * @param contentService ContentService
     * @param nodeRef NodeRef
     * @param name String
     */
    protected ContentNetworkFile(
            NodeService nodeService,
            ContentService contentService,
            MimetypeService mimetypeService,
            NodeRef nodeRef,
            String name)
    {
        super(name, nodeRef);
        setFullName(name);
        this.nodeService = nodeService;
        this.contentService = contentService;
        this.mimetypeService = mimetypeService;
    }

    /**
     * Return the file details as a string
     * 
     * @return String
     */
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        
        str.append( "[");
        str.append(getFullName());
        str.append(",");
        str.append( getNodeRef().getId());
        str.append( ",channel=");
        str.append( channel);
        if ( channel != null)
        	str.append( writableChannel ? "(Write)" : "(Read)");
        if ( modified)
        	str.append( ",modified");
        str.append( "]");

        return str.toString();
    }
    
    /**
     * @return Returns true if the channel should be writable
     * 
     * @see NetworkFile#getGrantedAccess()
     * @see NetworkFile#READONLY
     * @see NetworkFile#WRITEONLY
     * @see NetworkFile#READWRITE
     */
    private boolean isWritable()
    {
        // Check that we are allowed to write
    	
        int access = getGrantedAccess();
        return (access == NetworkFile.READWRITE || access == NetworkFile.WRITEONLY);
    }

    /**
     * Determine if the file content data has been opened
     * 
     * @return boolean
     */
    public final boolean hasContent()
    {
        return content != null ? true : false;
    }
    
    /**
     * Opens the channel for reading or writing depending on the access mode.
     * <p>
     * If the channel is already open, it is left.
     * 
     * @param write true if the channel must be writable
     * @param trunc true if the writable channel does not require the previous content data
     * @throws AccessDeniedException if this network file is read only
     * @throws AlfrescoRuntimeException if this network file represents a directory
     *
     * @see NetworkFile#getGrantedAccess()
     * @see NetworkFile#READONLY
     * @see NetworkFile#WRITEONLY
     * @see NetworkFile#READWRITE
     */
    private void openContent(boolean write, boolean trunc)
    	throws AccessDeniedException, AlfrescoRuntimeException
    {
    	// Check if the file is a directory
    	
        if (isDirectory())
        {
            throw new AlfrescoRuntimeException("Unable to open channel for a directory network file: " + this);
        }
        
        // Check if write access is required and the current channel is read-only
        
        else if ( write && writableChannel == false && channel != null)
        {
            // Close the existing read-only channel
            
            try
            {
                channel.close();
                channel = null;
            }
            catch (IOException ex)
            {
                logger.error("Error closing read-only channel", ex);
            }
            
            // Debug
            
            if ( logger.isDebugEnabled())
                logger.debug("Switching to writable channel for " + getName());
        }
        else if (channel != null)
        {
            // Already have channel open
        	
            return;
        }
        
        // We need to create the channel
        
        if (write && !isWritable())
        {
            throw new AccessDeniedException("The network file was created for read-only: " + this);
        }

        content = null;
        preUpdateContentReader = null;
        if (write)
        {
        	// Get a writeable channel to the content, along with the original content
        	
            content = contentService.getWriter( getNodeRef(), ContentModel.PROP_CONTENT, false);
            
            // Keep the original content for later comparison
            
            preUpdateContentReader = contentService.getReader( getNodeRef(), ContentModel.PROP_CONTENT);
            
            // Indicate that we have a writable channel to the file
            
            writableChannel = true;
            
            // Get the writable channel, do not copy existing content data if the file is to be truncated
            
            channel = ((ContentWriter) content).getFileChannel( trunc);
        }
        else
        {
        	// Get a read-only channel to the content
        	
            content = contentService.getReader( getNodeRef(), ContentModel.PROP_CONTENT);
            
            // Ensure that the content we are going to read is valid
            
            content = FileContentReader.getSafeContentReader(
                    (ContentReader) content,
                    I18NUtil.getMessage(FileContentReader.MSG_MISSING_CONTENT),
                    getNodeRef(), content);
            
            // Indicate that we only have a read-only channel to the data
            
            writableChannel = false;
            
            // Get the read-only channel
            
            channel = ((ContentReader) content).getFileChannel();
        }
    }

    /**
     * Close the file
     * 
     * @exception IOException
     */
    public void closeFile()
    	throws IOException
    {
    	// Check if this is a directory
    	
        if (isDirectory())
        {
        	// Nothing to do
        	
            return;
        }
        else if (channel == null)
        {
        	// File was not read/written so channel was not opened
        	
            return;
        }
        
        // Check if the file has been modified
        
        if (modified)
        {
            // Take a guess at the mimetype
            channel.position(0);
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            ContentCharsetFinder charsetFinder = mimetypeService.getContentCharsetFinder();
            Charset charset = charsetFinder.getCharset(is, content.getMimetype());
            content.setEncoding(charset.name());
            
            // Close the channel
        	
            channel.close();
            channel = null;
            
            // Update node properties, but only if the binary has changed (ETHREEOH-1861)
            
            ContentReader postUpdateContentReader = ((ContentWriter) content).getReader();
            boolean contentChanged = !AbstractContentReader.compareContentReaders(preUpdateContentReader, postUpdateContentReader);
            
            if (contentChanged)
            {
                ContentData contentData = content.getContentData();
                try
                {
                    nodeService.setProperty( getNodeRef(), ContentModel.PROP_CONTENT, contentData);
                }
                catch (ContentQuotaException qe)
                {
                    throw new DiskFullException(qe.getMessage());
                }
            }
        }
        else
        {
            // Close it - it was not modified
        	
            channel.close();
            channel = null;
        }
    }

    /**
     * Truncate or extend the file to the specified length
     * 
     * @param size long
     * @exception IOException
     */
    public void truncateFile(long size)
    	throws IOException
    {
    	try {
        	// If the content data channel has not been opened yet and the requested size is zero
            // then this is an open for overwrite so the existing content data is not copied
            
            if ( hasContent() == false && size == 0L)
            {
                // Open content for overwrite, no need to copy existing content data
                
                openContent(true, true);
            }
            else
            {
                // Normal open for write
                
                openContent(true, false);

                // Truncate or extend the channel
                
                channel.truncate(size);
            }
    	}
    	catch ( ContentIOException ex) {
    		
    		// DEBUG
    		
    		if ( logger.isDebugEnabled())
    			logger.debug("Error opening file " + getFullName() + " for write", ex);
    		
    		// Convert to a file server I/O error
    		
    		throw new DiskFullException("Failed to open " + getFullName() + " for write");
    	}
        
        // Set modification flag
        
        modified = true;
        
        // Set the new file size
        
        setFileSize( size);
        
        // Update the modification date/time
        
        if ( getFileState() != null)
        	getFileState().updateModifyDateTime();
        
        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Truncate file=" + this + ", size=" + size);
    }

    /**
     * Write a block of data to the file.
     * 
     * @param buf byte[]
     * @param len int
     * @param pos int
     * @param fileOff long
     * @exception IOException
     */
    public void writeFile(byte[] buffer, int length, int position, long fileOffset)
    	throws IOException
    {
    	try {
	        // Open the channel for writing
	        
	        openContent(true, false);
    	}
    	catch ( ContentIOException ex) {
    		
    		// DEBUG
    		
    		if ( logger.isDebugEnabled())
    			logger.debug("Error opening file " + getFullName() + " for write", ex);
    		
    		// Convert to a file server I/O error
    		
    		throw new DiskFullException("Failed to open " + getFullName() + " for write");
    	}
        
        // Write to the channel
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, position, length);
        int count = channel.write(byteBuffer, fileOffset);
        
        // Set modification flag
        
        modified = true;
        incrementWriteCount();

        // Update the current file size
        
        setFileSize(channel.size());
        
        // Update the modification date/time
        
        if ( getFileState() != null)
        	getFileState().updateModifyDateTime();

        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Write file=" + this + ", size=" + count);
    }

    /**
     * Read from the file.
     * 
     * @param buf byte[]
     * @param len int
     * @param pos int
     * @param fileOff long
     * @return Length of data read.
     * @exception IOException
     */
    public int readFile(byte[] buffer, int length, int position, long fileOffset)
    	throws IOException
    {
        // Open the channel for reading
        
        openContent(false, false);
        
        // Read from the channel
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, position, length);
        int count = channel.read(byteBuffer, fileOffset);
        if (count < 0)
        {
            count = 0;  // doesn't obey the same rules, i.e. just returns the bytes read
        }
        
        // Update the access date/time
        
        if ( getFileState() != null)
        	getFileState().updateAccessDateTime();

        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Read file=" + this + " read=" + count);
        
        // Return the actual count of bytes read
        
        return count;
    }
    
    /**
     * Open the file
     * 
     * @param createFlag boolean
     * @exception IOException
     */
    @Override
    public void openFile(boolean createFlag)
    	throws IOException
    {
    	// Wait for read/write before opening the content channel
    }

    /**
     * Seek to a new position in the file
     * 
     * @param pos long
     * @param typ int
     * @return long
     */
    @Override
    public long seekFile(long pos, int typ)
    	throws IOException
    {
        //  Open the file, if not already open

        openContent( false, false);

        //  Check if the current file position is the required file position

        long curPos = channel.position();
        
        switch (typ) {

          //  From start of file

          case SeekType.StartOfFile :
            if (curPos != pos)
              channel.position( pos);
            break;

            //  From current position

          case SeekType.CurrentPos :
            channel.position( curPos + pos);
            break;

            //  From end of file

          case SeekType.EndOfFile :
            {
              long newPos = channel.size() + pos;
              channel.position(newPos);
            }
            break;
        }

        // Update the access date/time
        
        if ( getFileState() != null)
        	getFileState().updateAccessDateTime();

        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Seek file=" + this + ", pos=" + pos + ", type=" + typ);

        //  Return the new file position

        return channel.position();
    }

    /**
     * Flush and buffered data for this file
     * 
     * @exception IOException
     */
    @Override
    public void flushFile()
    	throws IOException
    {
        // Open the channel for writing
    	
        openContent(true, false);
        
        // Flush the channel - metadata flushing is not important
        
        channel.force(false);
        
        // Update the access date/time
        
        if ( getFileState() != null)
        	getFileState().updateAccessDateTime();

        // DEBUG
        
        if (logger.isDebugEnabled())
            logger.debug("Flush file=" + this);
    }
    
    /**
     * Check if the file is an MS Office document type that needs special processing
     * 
     * @param path String
     * @return boolean
     */
    private static final boolean isMSOfficeSpecialFile(String path) {
    	
    	// Check if the file extension indicates a problem MS Office format

    	path = path.toLowerCase();
    	
    	if ( path.endsWith( ".xls"))
    		return true;
    	return false;
    }
}
