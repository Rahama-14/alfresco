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
package org.alfresco.email.server.impl.subetha;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.email.EmailMessagePart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Concrete representation of an email message as implemented for the SubEtha mail server.
 * 
 * @since 2.2
 */
public class SubethaEmailMessage implements EmailMessage
{
    private static final String ERR_FAILED_TO_CREATE_MIME_MESSAGE = "email.server.err.failed_to_create_mime_message";
    private static final String ERR_EXTRACTING_FROM_ADDRESS = "email.server.err.extracting_from_address";
    private static final String ERR_NO_FROM_ADDRESS = "email.server.err.no_from_address";
    private static final String ERR_EXTRACTING_TO_ADDRESS = "email.server.err.extracting_to_address";
    private static final String ERR_NO_TO_ADDRESS = "email.server.err.no_to_address";
    private static final String ERR_EXTRACTING_SUBJECT = "email.server.err.extracting_subject";
    private static final String ERR_EXTRACTING_SENT_DATE = "email.server.err.extracting_sent_date";
    private static final String ERR_PARSE_MESSAGE = "email.server.err.parse_message";
    
    private static final long serialVersionUID = -3735187524926395261L;

    private static final Log log = LogFactory.getLog(SubethaEmailMessage.class);

    private static final String MIME_PLAIN_TEXT = "text/plain";
    private static final String MIME_HTML_TEXT = "text/html";
    private static final String MIME_XML_TEXT = "text/xml";
    private static final String MIME_APPLICATION = "application/*";
    private static final String MIME_IMAGE = "image/*";
    private static final String MIME_MULTIPART = "multipart/*";
    private static final String MIME_RFC822 = "message/rfc822";
    private static final String FILENAME_ATTACHMENT_PREFIX = "Attachment";

    private String from;
    private String to;
    private String subject;
    private Date sentDate;
    private EmailMessagePart body;
    private EmailMessagePart[] attachments;
    transient private int bodyNumber = 0;
    transient private int attachmentNumber = 0;
    transient private List<EmailMessagePart> attachmentList = new LinkedList<EmailMessagePart>();

    protected SubethaEmailMessage()
    {
        super();
    }

    public SubethaEmailMessage(MimeMessage mimeMessage)
    {
        processMimeMessage(mimeMessage);
    }

    public SubethaEmailMessage(String from, String to, InputStream dataInputStream)
    {
        this.to = to;
        this.from = from;

        MimeMessage mimeMessage = null;
        try
        {
            mimeMessage = new MimeMessage(Session.getDefaultInstance(System.getProperties()), dataInputStream);
        }
        catch (MessagingException e)
        {
            throw new EmailMessageException(ERR_FAILED_TO_CREATE_MIME_MESSAGE, e.getMessage());
        }

        processMimeMessage(mimeMessage);
    }

    private void processMimeMessage(MimeMessage mimeMessage)
    {
        if (from == null)
        {
            Address[] addresses = null;
            try
            {
                addresses = mimeMessage.getFrom();
            }
            catch (MessagingException e)
            {
                throw new EmailMessageException(ERR_EXTRACTING_FROM_ADDRESS, e.getMessage());
            }
            if (addresses == null || addresses.length == 0)
            {
                throw new EmailMessageException(ERR_NO_FROM_ADDRESS);
            }
            from = addresses[0].toString();
        }

        if (to == null)
        {
            Address[] addresses = null;
            try
            {
                addresses = mimeMessage.getAllRecipients();
            }
            catch (MessagingException e)
            {
                throw new EmailMessageException(ERR_EXTRACTING_TO_ADDRESS, e.getMessage());
            }
            if (addresses == null || addresses.length == 0)
            {
                throw new EmailMessageException(ERR_NO_TO_ADDRESS);
            }
            to = addresses[0].toString();
        }

        try
        {
            subject = mimeMessage.getSubject();
        }
        catch (MessagingException e)
        {
            throw new EmailMessageException(ERR_EXTRACTING_SUBJECT, e.getMessage());
        }
        if (subject == null)
        {
            subject = ""; // Just anti-null stub :)
        }

        try
        {
            sentDate = mimeMessage.getSentDate();
        }
        catch (MessagingException e)
        {
            throw new EmailMessageException(ERR_EXTRACTING_SENT_DATE, e.getMessage());
        }
        if (sentDate == null)
        {
            sentDate = new Date(); // Just anti-null stub :)
        }

        parseMesagePart(mimeMessage);
        attachments = new EmailMessagePart[attachmentList.size()];
        attachmentList.toArray(attachments);
        attachmentList = null;
    }

    private void parseMesagePart(Part messagePart)
    {
        try
        {
            if (messagePart.isMimeType(MIME_PLAIN_TEXT) || messagePart.isMimeType(MIME_HTML_TEXT))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Text or HTML part was found. ContentType: " + messagePart.getContentType());
                }
                addBody(messagePart);
            }
            else if (messagePart.isMimeType(MIME_XML_TEXT))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("XML part was found.");
                }
                addAttachment(messagePart);
            }
            else if (messagePart.isMimeType(MIME_APPLICATION))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Application part was found.");
                }
                addAttachment(messagePart);
            }
            else if (messagePart.isMimeType(MIME_IMAGE))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Image part was found.");
                }
                addAttachment(messagePart);
            }
            else if (messagePart.isMimeType(MIME_MULTIPART))
            {
                // if multipart, this method will be called recursively
                // for each of its parts
                Multipart mp = (Multipart) messagePart.getContent();
                int count = mp.getCount();

                if (log.isDebugEnabled())
                {
                    log.debug("MULTIPART with " + count + " part(s) found. Processin each part...");
                }
                for (int i = 0; i < count; i++)
                {
                    parseMesagePart(mp.getBodyPart(i));
                }

                if (log.isDebugEnabled())
                {
                    log.debug("MULTIPART processed.");
                }

            }
            else if (messagePart.isMimeType(MIME_RFC822))
            {
                // if rfc822, call this method with its content as the part
                if (log.isDebugEnabled())
                {
                    log.debug("MIME_RFC822 part found. Processing inside part...");
                }

                parseMesagePart((Part) messagePart.getContent());

                if (log.isDebugEnabled())
                {
                    log.debug("MIME_RFC822 processed.");
                }

            }
            else
            {
                // if all else fails, put this in the attachments map.
                // Actually we don't know what it is.
                if (log.isDebugEnabled())
                {
                    log.debug("Unrecognized part was found. Put it into attachments.");
                }
                addAttachment(messagePart);
            }
        }
        catch (IOException e)
        {
            throw new EmailMessageException(ERR_PARSE_MESSAGE, e.getMessage());
        }
        catch (MessagingException e)
        {
            throw new EmailMessageException(ERR_PARSE_MESSAGE, e.getMessage());
        }
    }

    private void addBody(Part messagePart) throws MessagingException
    {
        if (body != null)
        {
            if (!MIME_PLAIN_TEXT.equals(body.getContentType()) && messagePart.isMimeType(MIME_PLAIN_TEXT))
            {
                attachmentList.add(body);
                body = new SubethaEmailMessagePart(messagePart);
                if (log.isDebugEnabled())
                {
                    log.debug("Body has been changed to the new one.");
                }
            }
            else
            {
                attachmentList.add(new SubethaEmailMessagePart(messagePart, getPartFileName(getSubject() + " (part " + ++bodyNumber + ")", messagePart)));
                if (log.isInfoEnabled())
                {
                    log.info(String.format("Attachment \"%s\" has been added.", attachmentList.get(attachmentList.size() - 1).getFileName()));
                }
            }
        }
        else
        {
            body = new SubethaEmailMessagePart(messagePart, getPartFileName(getSubject() + " (part " + ++bodyNumber + ")", messagePart));
            if (log.isDebugEnabled())
            {
                log.debug("Boby has been added.");
            }
        }

    }

    /**
     * Method adds a message part to the attachments list
     * 
     * @param messagePart A part of message
     * @throws EmailMessageException
     * @throws MessagingException
     */
    private void addAttachment(Part messagePart) throws MessagingException
    {
        String fileName = getPartFileName(FILENAME_ATTACHMENT_PREFIX + ++attachmentNumber, messagePart);
        attachmentList.add(new SubethaEmailMessagePart(messagePart, fileName));
        if (log.isDebugEnabled())
        {
            log.debug("Attachment added: " + fileName);
        }
    }

    /**
     * Method extracts file name from a message part for saving its as aa attachment. If the file name can't be extracted, it will be generated based on defaultPrefix parameter.
     * 
     * @param defaultPrefix This prefix fill be used for generating file name.
     * @param messagePart A part of message
     * @return File name.
     * @throws MessagingException
     */
    private String getPartFileName(String defaultPrefix, Part messagePart) throws MessagingException
    {
        String fileName = messagePart.getFileName();
        if (fileName != null)
        {
            try
            {
                fileName = MimeUtility.decodeText(fileName);
            }
            catch (UnsupportedEncodingException ex)
            {
                // Nothing to do :)
            }
        }
        else
        {
            fileName = defaultPrefix;
            if (messagePart.isMimeType(MIME_PLAIN_TEXT))
                fileName += ".txt";
            else if (messagePart.isMimeType(MIME_HTML_TEXT))
                fileName += ".html";
            else if (messagePart.isMimeType(MIME_XML_TEXT))
                fileName += ".xml";
            else if (messagePart.isMimeType(MIME_IMAGE))
                fileName += ".gif";
        }
        return fileName;
    }

    public void setRmiRegistry(String rmiRegistryHost, int rmiRegistryPort)
    {
        if (body instanceof SubethaEmailMessagePart) 
        {
            ((SubethaEmailMessagePart) body).setRmiRegistry(rmiRegistryHost, rmiRegistryPort);
        }
        
        for (EmailMessagePart attachment : attachments)
        {
            if (attachment instanceof SubethaEmailMessagePart) {
                ((SubethaEmailMessagePart) attachment).setRmiRegistry(rmiRegistryHost, rmiRegistryPort);
            }
        }
    }
    
    
    public String getFrom()
    {
        return from;
    }

    public String getTo()
    {
        return to;
    }

    public Date getSentDate()
    {
        return sentDate;
    }

    public String getSubject()
    {
        return subject;
    }

    public EmailMessagePart getBody()
    {
        return body;
    }

    public EmailMessagePart[] getAttachments()
    {
        return attachments;
    }

}
