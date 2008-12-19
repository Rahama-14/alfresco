package org.alfresco.repo.node.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.alfresco.error.StackTraceUtil;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for Node cleaners.  This class ensures calls through
 * after having created a read-write transaction that is authenticated
 * as system.
 * 
 * @author Derek Hulley
 * @since 2.2 SP2
 */
public abstract class AbstractNodeCleanupWorker implements NodeCleanupWorker
{
    protected final Log logger;
    private final ReentrantLock cleanupLock;
    
    private NodeCleanupRegistry registry;
    protected TransactionService transactionService;
    protected DbNodeServiceImpl dbNodeService;
    protected NodeDaoService nodeDaoService;
    
    public AbstractNodeCleanupWorker()
    {
        logger = LogFactory.getLog(this.getClass());
        cleanupLock = new ReentrantLock();
    }
    
    public void setRegistry(NodeCleanupRegistry registry)
    {
        this.registry = registry;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setDbNodeService(DbNodeServiceImpl dbNodeService)
    {
        this.dbNodeService = dbNodeService;
    }

    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    public void register()
    {
        PropertyCheck.mandatory(this, "registry", registry);
        PropertyCheck.mandatory(this, "transactionService", transactionService);
        PropertyCheck.mandatory(this, "dbNodeService", dbNodeService);
        PropertyCheck.mandatory(this, "nodeDaoService", nodeDaoService);

        registry.register(this);
    }

    /**
     * Calls {@link #doCleanInternal()} in a System-user authenticated read-write transaction.
     * This method is non-blocking but passes all second and subsequent concurrent invocations
     * straight through.
     */
    public List<String> doClean()
    {
        /** Prevent multiple executions of the implementation method */
        boolean locked = cleanupLock.tryLock();
        if (locked)
        {
            try
            {
                return doCleanWithTxn();
            }
            catch (Throwable e)
            {
                if (logger.isDebugEnabled())
                {
                    StringBuilder sb = new StringBuilder(1024);
                    StackTraceUtil.buildStackTrace(
                            "Node cleanup failed: " +
                            "   Worker: " + this.getClass().getName() + "\n" +
                            "   Error:  ",
                            e.getStackTrace(),
                            sb,
                            Integer.MAX_VALUE);
                    logger.debug(sb.toString());
                }
                StringBuilder sb = new StringBuilder(1024);
                StackTraceUtil.buildStackTrace(
                    "Node cleanup failed: " +
                    "   Worker: " + this.getClass().getName() + "\n" +
                    "   Error:  ",
                    e.getStackTrace(),
                    sb,
                    20);
                return Collections.singletonList(sb.toString());
            }
            finally
            {
                cleanupLock.unlock();
            }
        }
        else
        {
            return Collections.emptyList();
        }
    }
    
    private List<String> doCleanWithTxn()
    {
        final RetryingTransactionCallback<List<String>> doCleanCallback = new RetryingTransactionCallback<List<String>>()
        {
            public List<String> execute() throws Throwable
            {
                return doCleanInternal();
            }
        };
        final RunAsWork<List<String>> doCleanRunAs = new RunAsWork<List<String>>()
        {
            public List<String> doWork() throws Exception
            {
                return transactionService.getRetryingTransactionHelper().doInTransaction(doCleanCallback, false, true);
            }
        };
        return AuthenticationUtil.runAs(doCleanRunAs, AuthenticationUtil.getSystemUserName());
    }
    
    /**
     * Do the actual cleanup.  Any errors are handled by this base class.
     * 
     * @return      Returns the cleanup messages.
     */
    protected abstract List<String> doCleanInternal() throws Throwable;
}