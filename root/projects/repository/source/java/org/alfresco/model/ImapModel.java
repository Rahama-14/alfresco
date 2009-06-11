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
package org.alfresco.model;

import org.alfresco.service.namespace.QName;

/**
 * IMAP Model Constants
 * 
 * @author Mike Shavnev
 */
public interface ImapModel
{
    static final String IMAP_MODEL_1_0_URI = "http://www.alfresco.org/model/imap/1.0";

    static final QName ASPECT_IMAP_FOLDER_NONSUBSCRIBED = QName.createQName(IMAP_MODEL_1_0_URI, "nonSubscribed");
    static final QName ASPECT_IMAP_FOLDER_NONSELECTABLE = QName.createQName(IMAP_MODEL_1_0_URI, "nonselectable");

    static final QName ASPECT_IMAP_CONTENT = QName.createQName(IMAP_MODEL_1_0_URI, "imapContent");
    static final QName PROP_MESSAGE_FROM = QName.createQName(IMAP_MODEL_1_0_URI, "messageFrom");
    static final QName PROP_MESSAGE_TO = QName.createQName(IMAP_MODEL_1_0_URI, "messageTo");
    static final QName PROP_MESSAGE_CC = QName.createQName(IMAP_MODEL_1_0_URI, "messageCc");
    static final QName PROP_MESSAGE_SUBJECT = QName.createQName(IMAP_MODEL_1_0_URI, "messageSubject");
    static final QName PROP_MESSAGE_ID = QName.createQName(IMAP_MODEL_1_0_URI, "messageId");
    static final QName PROP_THREAD_INDEX = QName.createQName(IMAP_MODEL_1_0_URI, "threadIndex");
    static final QName ASSOC_IMAP_ATTACHMENT = QName.createQName(IMAP_MODEL_1_0_URI, "attachment");
    static final QName ASSOC_IMAP_ATTACHMENTS_FOLDER = QName.createQName(IMAP_MODEL_1_0_URI, "attachmentsFolder");

    static final QName ASPECT_FLAGGABLE = QName.createQName(IMAP_MODEL_1_0_URI, "flaggable");
    static final QName PROP_FLAG_ANSWERED = QName.createQName(IMAP_MODEL_1_0_URI, "flagAnswered");
    static final QName PROP_FLAG_DELETED = QName.createQName(IMAP_MODEL_1_0_URI, "flagDeleted");
    static final QName PROP_FLAG_DRAFT = QName.createQName(IMAP_MODEL_1_0_URI, "flagDraft");
    static final QName PROP_FLAG_SEEN = QName.createQName(IMAP_MODEL_1_0_URI, "flagSeen");
    static final QName PROP_FLAG_RECENT = QName.createQName(IMAP_MODEL_1_0_URI, "flagRecent");
    static final QName PROP_FLAG_FLAGGED = QName.createQName(IMAP_MODEL_1_0_URI, "flagFlagged");

    static final QName TYPE_IMAP_BODY = QName.createQName(IMAP_MODEL_1_0_URI, "imapBody");

    static final QName TYPE_IMAP_ATTACH = QName.createQName(IMAP_MODEL_1_0_URI, "imapAttach");
    static final QName PROP_ATTACH_ID = QName.createQName(IMAP_MODEL_1_0_URI, "attachID");

}
