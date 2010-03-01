/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.imap;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;

/**
 * Implementation of greenmail MailFolder. It represents an Alfresco content folder and handles
 * appendMessage, copyMessage, expunge (delete), getMessages, getMessage and so requests.
 * 
 * @author Ivan Rybnikov
 */
public abstract class AbstractImapFolder implements MailFolder
{
    private List<FolderListener> listeners = new LinkedList<FolderListener>();

    protected ServiceRegistry serviceRegistry;
    protected static int MAX_RETRIES = 1;

    
    public AbstractImapFolder(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * Method that checks mandatory parameter.
     * @param The parameter instance to check.
     * @param The name of the parameter.
     */
    protected void checkParameter(Object parameter, String name)
    {
        if (parameter == null)
        {
            throw new IllegalArgumentException(name + " parameter is null.");
        }
    }


    /**
     * Appends message to the folder.
     * 
     * @param message - message.
     * @param flags - message flags.
     * @param internalDate - not used. Current date used instead.
     * @return
     */
    public long appendMessage(final MimeMessage message, final Flags flags, final Date internalDate) throws FolderException
    {
        if (isReadOnly())
        {
            throw new FolderException("Can't append message - Permission denied");
        }

        CommandCallback<Long> command = new CommandCallback<Long>()
        {
            public Long command() throws Throwable
            {
                return appendMessageInternal(message, flags, internalDate);
            }
        };
        return command.runFeedback();
     }


    /**
     * Copies message with the given UID to the specified {@link MailFolder}.
     * 
     * @param uid - UID of the message
     * @param toFolder - reference to the destination folder.
     */
    public void copyMessage(final long uid, final MailFolder toFolder) throws FolderException
    {
        AbstractImapFolder toImapMailFolder = (AbstractImapFolder) toFolder;

        if (toImapMailFolder.isReadOnly())
        {
            throw new FolderException("Can't create folder - Permission denied");
        }

        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                copyMessageInternal(uid, toFolder);
                return null;
            }
        };
        command.runFeedback();
    }

    /**
     * Marks all messages in the folder as deleted using {@link Flags.Flag#DELETED} flag.
     */
    public void deleteAllMessages() throws FolderException
    {
        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                deleteAllMessagesInternal();
                return null;
            }
        };
        command.runFeedback();
    }

    

    /**
     * Deletes messages marked with {@link Flags.Flag#DELETED}. Note that this message deletes all messages with this flag.
     */
    public void expunge() throws FolderException
    {
        if (isReadOnly())
        {
            throw new FolderException("Can't expunge - Permission denied");
        }
        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                expungeInternal();
                return null;
            }
        };
        command.runFeedback();
    }

    
    /**
     * Returns the number of the first unseen message.
     * 
     * @return Number of the first unseen message.
     */
    public int getFirstUnseen()
    {
        return getFirstUnseenInternal();
    }

    
    /**
     * Returns full name of the folder with namespace and full path delimited with the hierarchy delimiter
     * (see {@link AlfrescoImapConst#HIERARCHY_DELIMITER})
     * <p/> E.g.: <br/>
     * #mail.admin."Repository_archive.Data Dictionary.Space Templates.Software Engineering Project" <br/>
     * This is required by GreenMail implementation.
     */
    public String getFullName()
    {
        CommandCallback<String> command = new CommandCallback<String>()
        {
            public String command() throws Throwable
            {
                return getFullNameInternal();
            }
        };
        return command.run();
    }
    
    
    /**
     * Returns message by its UID.
     * 
     * @param uid - UID of the message.
     * @return message.
     */
    public SimpleStoredMessage getMessage(final long uid)
    {
        CommandCallback<SimpleStoredMessage> command = new CommandCallback<SimpleStoredMessage>()
        {
            public SimpleStoredMessage command() throws Throwable
            {
                return getMessageInternal(uid);
            }
        };
        return command.run();
    }
    
    /**
     * Returns count of the messages in the folder.
     * 
     * @return Count of the messages.
     */
    public int getMessageCount()
    {
        CommandCallback<Integer> command = new CommandCallback<Integer>()
        {
            public Integer command() throws Throwable
            {
                return getMessageCountInternal();
            }
        };
        return command.run();
    }
   
    /**
     * Returns list of all messages in the folder.
     * 
     * @return list of {@link SimpleStoredMessage} objects.
     */
    public List<SimpleStoredMessage> getMessages()
    {
        CommandCallback<List<SimpleStoredMessage>> command = new CommandCallback<List<SimpleStoredMessage>>()
        {
            public List<SimpleStoredMessage> command() throws Throwable
            {
                return getMessagesInternal();
            }
        };
        return command.run();
    }

    /**
     * Returns list of messages by filter.
     * 
     * @param msgRangeFilter - {@link MsgRangeFilter} object representing filter.
     * @return list of filtered messages.
     */
    public List<SimpleStoredMessage> getMessages(final MsgRangeFilter msgRangeFilter)
    {
        CommandCallback <List<SimpleStoredMessage>> command = new CommandCallback <List<SimpleStoredMessage>>()
        {
            public List<SimpleStoredMessage> command() throws Throwable
            {
                return getMessagesInternal(msgRangeFilter);
            }
        };
        return command.run();
    }

    /**
     * Returns message sequence number in the folder by its UID.
     * 
     * @param uid - message UID.
     * @return message sequence number.
     * @throws FolderException if no message with given UID.
     */
    public int getMsn(final long uid) throws FolderException
    {
        CommandCallback<Integer> command = new CommandCallback<Integer>()
        {
            public Integer command() throws Throwable
            {
                return getMsnInternal(uid);
            }
        };
        return command.runFeedback(true);
    }

    /**
     * Returns folder name.
     * 
     * @return folder name.
     */
    public String getName()
    {
        CommandCallback<String> command = new CommandCallback<String>()
        {
            public String command() throws Throwable
            {
                return getNameInternal();
            }
        };
        return command.run();
    }

    /**
     * Returns UIDs of all messages in the folder.
     * 
     * @return UIDS of the messages.
     */
    public long[] getMessageUids()
    {
        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                return getMessageUidsInternal();
            }
        };
        return (long[])command.run();
    }

    /**
     * Returns the list of messages that have no {@link Flags.Flag#DELETED} flag set for current user.
     * 
     * @return the list of non-deleted messages.
     */
    public List<SimpleStoredMessage> getNonDeletedMessages()
    {
        CommandCallback <List<SimpleStoredMessage>> command = new CommandCallback<List<SimpleStoredMessage>>()
        {
            public List<SimpleStoredMessage> command() throws Throwable
            {
                return getNonDeletedMessagesInternal();
            }
        };
        List<SimpleStoredMessage> result = (List<SimpleStoredMessage>)command.run();
        return result;
    }

    /**
     * Returns permanent flags.
     * 
     * @return {@link Flags} object containing flags.
     */
    public Flags getPermanentFlags()
    {
        CommandCallback<Flags> command = new CommandCallback<Flags>()
        {
            public Flags command() throws Throwable
            {
                return getPermanentFlagsInternal();
            }
        };
        return command.run(true);
    }

    /**
     * Returns count of messages with {@link Flags.Flag#RECENT} flag. If {@code reset} parameter is {@code true} -
     * removes {@link Flags.Flag#RECENT} flag from the message for current user.
     * 
     * @param reset - if true the {@link Flags.Flag#RECENT} will be deleted for current user if exists.
     * @return returns count of recent messages.
     */
    public int getRecentCount(final boolean reset)
    {
        CommandCallback<Integer> command = new CommandCallback<Integer>()
        {
            public Integer command() throws Throwable
            {
                return getRecentCountInternal(reset);
            }
        };
        return command.run(true);
    }

    /**
     * Returns UIDNEXT value of the folder.
     * 
     * @return UIDNEXT value.
     */
    public long getUidNext()
    {
        CommandCallback<Long> command = new CommandCallback<Long>()
        {
            public Long command() throws Throwable
            {
                return getUidNextInternal();
            }
        };
        return command.run(true);
    }

    /**
     * Returns UIDVALIDITY value of the folder.
     * 
     * @return UIDVALIDITY value.
     */
    public long getUidValidity()
    {
        CommandCallback<Long> command = new CommandCallback<Long>()
        {
            public Long command() throws Throwable
            {
                return getUidValidityInternal();
            }
        };
        return command.run(true);
    }

    /**
     * Returns count of the messages with {@link Flags.Flag#SEEN} in the folder for the current user.
     * 
     * @return Count of the unseen messages for current user.
     */
    public int getUnseenCount()
    {
        CommandCallback<Integer> command = new CommandCallback<Integer>()
        {
            public Integer command() throws Throwable
            {
                return getUnseenCountInternal();
            }
        };
        return command.run();
    }

    /**
     * Whether the folder is selectable.
     * 
     * @return {@code boolean}.
     */
    public boolean isSelectable()
    {
        CommandCallback<Boolean> command = new CommandCallback<Boolean>()
        {
            public Boolean command() throws Throwable
            {
                return isSelectableInternal();
            }
        };
        return command.run(true);
    }

    /**
     * Replaces flags for the message with the given UID. If {@code addUid} is set to
     * {@code true} {@link FolderListener} objects defined for this folder will be notified.
     * {@code silentListener} can be provided - this listener wouldn't be notified.
     * 
     * @param flags - new flags.
     * @param uid - message UID.
     * @param silentListener - listener that shouldn't be notified.
     * @param addUid - defines whether or not listeners be notified.
     */
    public void replaceFlags(final Flags flags, final long uid, final FolderListener silentListener, final boolean addUid) throws FolderException
    {
        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                replaceFlagsInternal(flags, uid, silentListener, addUid);
                return null;
            }
        };
        command.runFeedback();
    }

    
    /**
     * Simply returns UIDs of all messages in the folder.
     * 
     * @param searchTerm - not used
     * @return UIDs of the messages
     */
    public long[] search(SearchTerm searchTerm)
    {
        return getMessageUids();
    }

    /**
     * Sets flags for the message with the given UID. If {@code addUid} is set to {@code true}
     * {@link FolderListener} objects defined for this folder will be notified.
     * {@code silentListener} can be provided - this listener wouldn't be notified.
     * 
     * @param flags - new flags.
     * @param value - flags value.
     * @param uid - message UID.
     * @param silentListener - listener that shouldn't be notified.
     * @param addUid - defines whether or not listeners be notified.
     */
    public void setFlags(
            final Flags flags,
            final boolean value,
            final long uid,
            final FolderListener silentListener,
            final boolean addUid)
            throws FolderException
    {
        CommandCallback<Object> command = new CommandCallback<Object>()
        {
            public Object command() throws Throwable
            {
                setFlagsInternal(flags, value, uid, silentListener, addUid);
                return null;
            }
        };
        command.runFeedback();
    }


    
    /**
     * Not supported. Added to implement {@link MailFolder#store(MovingMessage)}.
     */
    public void store(MovingMessage mail) throws Exception
    {
        throw new UnsupportedOperationException("Method store(MovingMessage) is not suppoted.");
    }

    /**
     * Not supported. Added to implement {@link MailFolder#store(MimeMessage)}.
     */
    public void store(MimeMessage message) throws Exception
    {
        throw new UnsupportedOperationException("Method store(MimeMessage) is not suppoted.");
    }

    
    /**
     * Adds {@link FolderListener} to the folder.
     * 
     * @param listener - new listener.
     */
    public void addListener(FolderListener listener)
    {
        listeners.add(listener);
    }

    
    
    /**
     * Removes {@link FolderListener} from the folder.
     * 
     * @param listener - Listener to remove.
     */
    public void removeListener(FolderListener listener)
    {
        listeners.remove(listener);
    }
    
    /**
     * Method is called before the deletion of the folder. Notifies {@link FolderListener} objects with
     * {@link FolderListener#mailboxDeleted()} method calls.
     */
    public void signalDeletion()
    {
        synchronized (listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                FolderListener listener = (FolderListener) listeners.get(i);
                listener.mailboxDeleted();
            }
        }
    }
    
    
    protected void notifyFlagUpdate(int msn, Flags flags, Long uidNotification, FolderListener silentListener)
    {
        synchronized (listeners)
        {
            for (FolderListener listener : listeners)
            {
                if (listener == silentListener)
                {
                    continue;
                }

                listener.flagsUpdated(msn, flags, uidNotification);
            }
        }
    }

    
    protected abstract boolean isReadOnly();

    protected abstract long appendMessageInternal(MimeMessage message, Flags flags, Date internalDate) throws Exception;

    protected abstract void copyMessageInternal(long uid, MailFolder toFolder) throws Exception;
    
    protected abstract void deleteAllMessagesInternal() throws Exception;

    protected abstract void expungeInternal() throws Exception;

    protected abstract int getFirstUnseenInternal();

    protected abstract String getFullNameInternal() throws Exception;
    
    protected abstract SimpleStoredMessage getMessageInternal(long uid) throws Exception;

    protected abstract int getMessageCountInternal();

    protected abstract List<SimpleStoredMessage> getMessagesInternal();
    
    protected abstract List<SimpleStoredMessage> getMessagesInternal(MsgRangeFilter msgRangeFilter);

    protected abstract int getMsnInternal(long uid) throws Exception;
   
    protected abstract String getNameInternal();
    
    protected abstract long[] getMessageUidsInternal();

    protected abstract List<SimpleStoredMessage> getNonDeletedMessagesInternal();

    protected abstract Flags getPermanentFlagsInternal();

    protected abstract int getRecentCountInternal(boolean reset);
    
    protected abstract long getUidNextInternal();

    protected abstract long getUidValidityInternal();
      
    protected abstract int getUnseenCountInternal();
  
    protected abstract boolean isSelectableInternal();
    
    protected abstract void replaceFlagsInternal(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws Exception;

    protected abstract void setFlagsInternal(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws Exception;

    protected abstract class CommandCallback<T>
    {
        public abstract T command() throws Throwable;

        public T runFeedback() throws FolderException
        {
            return this.runFeedback(false);
        }

        public T runFeedback(boolean readOnly) throws FolderException
        {
            try
            {
                RetryingTransactionHelper txHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
                txHelper.setMaxRetries(MAX_RETRIES);
                txHelper.setReadOnly(readOnly);
                T result = txHelper.doInTransaction(
                        new RetryingTransactionCallback<T>()
                        {
                            public T execute() throws Throwable
                            {
                                return command();
                            }
                        }, readOnly);
                return result;
            }
            catch (Exception e)
            {
                Throwable cause = e.getCause();
                String message;
                if (cause != null)
                {
                    message = cause.getMessage();
                }
                else
                {
                    message = e.getMessage();
                }
                throw new FolderException(message);
            }
        }

        public T run()
        {
            return this.run(false);
        }
        
        public T run(boolean readOnly)
        {
            RetryingTransactionHelper txHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
            txHelper.setMaxRetries(MAX_RETRIES);
            txHelper.setReadOnly(readOnly);
            T result = txHelper.doInTransaction(
                    new RetryingTransactionCallback<T>()
                    {
                        public T execute() throws Throwable
                        {
                            return command();
                        }
                    }, readOnly);
            return result;
        }
    }
}
