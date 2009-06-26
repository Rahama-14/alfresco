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
package org.alfresco.repo.imap;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.util.SharedByteArrayInputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;

/**
 * Extended MimeMessage to represent a content stored in the Alfresco repository.
 * 
 * @author Arseny Kovalchuk
 */
public class ImapModelMessage extends AbstractMimeMessage
{
    /**
     * Constructs {@link ImapModelMessage} object.
     * 
     * @param fileInfo - reference to the {@link FileInfo} object representing the message.
     * @param imapHelper - reference to the {@link ImapHelper} object.
     * @param generateBody - if {@code true} message body will be generated.
     * 
     * @throws MessagingException if generation of the body fails.
     */
    public ImapModelMessage(FileInfo fileInfo, ServiceRegistry serviceRegistry, boolean generateBody) throws MessagingException
    {
        super(fileInfo, serviceRegistry, generateBody);
    }

    @Override
    public void buildMessageInternal() throws MessagingException
    {
        if (generateBody != false)
        {
            setMessageHeaders();
            buildImapMessage();
        }
    }

    /**
     * This method builds MimeMessage based on either ImapModel or ContentModel type.
     * 
     * @param fileInfo - Source file information {@link FileInfo}
     * @throws MessagingException
     */
    private void buildImapMessage() throws MessagingException
    {
        modified = false;
        saved = false;
        buildRFC822Message();
        saved = true;
    }

    private void buildRFC822Message() throws MessagingException
    {
        ContentService contentService = serviceRegistry.getContentService();
        ContentReader reader = contentService.getReader(messageFileInfo.getNodeRef(), ContentModel.PROP_CONTENT);
        try
        {
            InputStream is = reader.getContentInputStream();
            this.parse(is);
            is.close();
            is = null;
        }
        catch (ContentIOException e)
        {
            //logger.error(e);
            throw new MessagingException("The error occured during message creation from content stream.", e);
        }
        catch (IOException e)
        {
            //logger.error(e);
            throw new MessagingException("The error occured during message creation from content stream.", e);
        }
    }

    @Override
    protected InputStream getContentStream() throws MessagingException
    {
        try
        {
            if (this.contentStream == null)
            {
                if (content != null)
                {
                    return new SharedByteArrayInputStream(content);
                }
                else
                {
                    throw new MessagingException("No content");
                }
            }
            return this.contentStream;
        }
        catch (Exception e)
        {
            throw new MessagingException(e.getMessage(),e);
        }
    }

    /*
    protected void parse(InputStream inputstream) throws MessagingException
    {
        headers = createInternetHeaders(inputstream);
        contentStream = inputstream;
    }
    */
    
}
