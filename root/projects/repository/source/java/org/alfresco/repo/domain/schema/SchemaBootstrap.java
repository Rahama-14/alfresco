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
package org.alfresco.repo.domain.schema;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.admin.patch.impl.SchemaUpgradeScriptPatch;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.hibernate.dialect.AlfrescoSQLServerDialect;
import org.alfresco.repo.domain.hibernate.dialect.AlfrescoSybaseAnywhereDialect;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.AbstractLifecycleBean;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.LogUtil;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.UserSuppliedConnectionProvider;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.MySQLInnoDBDialect;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.ActionQueue;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * Bootstraps the schema and schema update.  The schema is considered missing if the applied patch table
 * is not present, and the schema is considered empty if the applied patch table is empty.
 * 
 * @author Derek Hulley
 */
public class SchemaBootstrap extends AbstractLifecycleBean
{
    /** The placeholder for the configured <code>Dialect</code> class name: <b>${db.script.dialect}</b> */
    private static final String PLACEHOLDER_SCRIPT_DIALECT = "\\$\\{db\\.script\\.dialect\\}";

    private static final String MSG_DIALECT_USED = "schema.update.msg.dialect_used";
    private static final String MSG_BYPASSING_SCHEMA_UPDATE = "schema.update.msg.bypassing";
    private static final String MSG_NO_CHANGES = "schema.update.msg.no_changes";
    private static final String MSG_ALL_STATEMENTS = "schema.update.msg.all_statements";
    private static final String MSG_EXECUTING_GENERATED_SCRIPT = "schema.update.msg.executing_generated_script";
    private static final String MSG_EXECUTING_COPIED_SCRIPT = "schema.update.msg.executing_copied_script";
    private static final String MSG_EXECUTING_STATEMENT = "schema.update.msg.executing_statement";
    private static final String MSG_OPTIONAL_STATEMENT_FAILED = "schema.update.msg.optional_statement_failed";
    private static final String WARN_DIALECT_UNSUPPORTED = "schema.update.warn.dialect_unsupported";
    private static final String WARN_DIALECT_HSQL = "schema.update.warn.dialect_hsql";
    private static final String WARN_DIALECT_DERBY = "schema.update.warn.dialect_derby";
    private static final String ERR_MULTIPLE_SCHEMAS = "schema.update.err.found_multiple";
    private static final String ERR_PREVIOUS_FAILED_BOOTSTRAP = "schema.update.err.previous_failed";
    private static final String ERR_STATEMENT_FAILED = "schema.update.err.statement_failed";
    private static final String ERR_UPDATE_FAILED = "schema.update.err.update_failed";
    private static final String ERR_VALIDATION_FAILED = "schema.update.err.validation_failed";
    private static final String ERR_SCRIPT_NOT_RUN = "schema.update.err.update_script_not_run";
    private static final String ERR_SCRIPT_NOT_FOUND = "schema.update.err.script_not_found";
    private static final String ERR_STATEMENT_TERMINATOR = "schema.update.err.statement_terminator";
    
    public static final int DEFAULT_LOCK_RETRY_COUNT = 24;
    public static final int DEFAULT_LOCK_RETRY_WAIT_SECONDS = 5;
    
    public static final int DEFAULT_MAX_STRING_LENGTH = 1024;
    private static volatile int maxStringLength = DEFAULT_MAX_STRING_LENGTH;
    
    
   
   

    /**
     * @see PropertyValue#DEFAULT_MAX_STRING_LENGTH
     */
    public static final void setMaxStringLength(int length)
    {
        if (length < 1024)
        {
            throw new AlfrescoRuntimeException("The maximum string length must >= 1024 characters.");
        }
        SchemaBootstrap.maxStringLength = length;
    }

    /**
     * @return      Returns the maximum number of characters that a string field can be
     */
    public static final int getMaxStringLength()
    {
        return SchemaBootstrap.maxStringLength;
    }
    
    private static Log logger = LogFactory.getLog(SchemaBootstrap.class);
    
    private LocalSessionFactoryBean localSessionFactory;
    private String schemaOuputFilename;
    private boolean updateSchema;
    private List<String> postCreateScriptUrls;
    private List<SchemaUpgradeScriptPatch> validateUpdateScriptPatches;
    private List<SchemaUpgradeScriptPatch> preUpdateScriptPatches;
    private List<SchemaUpgradeScriptPatch> postUpdateScriptPatches;
    private int schemaUpdateLockRetryCount = DEFAULT_LOCK_RETRY_COUNT;
    private int schemaUpdateLockRetryWaitSeconds = DEFAULT_LOCK_RETRY_WAIT_SECONDS;
    private int maximumStringLength;
    
    private ThreadLocal<StringBuilder> executedStatementsThreadLocal = new ThreadLocal<StringBuilder>();

    public SchemaBootstrap()
    {
        postCreateScriptUrls = new ArrayList<String>(1);
        validateUpdateScriptPatches = new ArrayList<SchemaUpgradeScriptPatch>(4);
        preUpdateScriptPatches = new ArrayList<SchemaUpgradeScriptPatch>(4);
        postUpdateScriptPatches = new ArrayList<SchemaUpgradeScriptPatch>(4);
        maximumStringLength = -1;
    }
    
    public void setLocalSessionFactory(LocalSessionFactoryBean localSessionFactory)
    {
        this.localSessionFactory = localSessionFactory;
    }
    
    public LocalSessionFactoryBean getLocalSessionFactory()
    {
        return localSessionFactory;
    }

    /**
     * Set this to output the full database creation script
     * 
     * @param schemaOuputFilename the name of a file to dump the schema to, or null to ignore
     */
    public void setSchemaOuputFilename(String schemaOuputFilename)
    {
        this.schemaOuputFilename = schemaOuputFilename;
    }

    /**
     * Set whether to modify the schema or not.  Either way, the schema will be validated.
     * 
     * @param updateSchema true to update and validate the schema, otherwise false to just
     *      validate the schema.  Default is <b>true</b>.
     */
    public void setUpdateSchema(boolean updateSchema)
    {
        this.updateSchema = updateSchema;
    }

    /**
     * Set the scripts that must be executed after the schema has been created.
     * 
     * @param postCreateScriptUrls file URLs
     * 
     * @see #PLACEHOLDER_SCRIPT_DIALECT
     */
    public void setPostCreateScriptUrls(List<String> postUpdateScriptUrls)
    {
        this.postCreateScriptUrls = postUpdateScriptUrls;
    }

    /**
     * Set the schema script patches that must have been applied.  These will not be
     * applied to the database.  These can be used where the script <u>cannot</u> be
     * applied automatically or where a particular upgrade path is no longer supported.
     * For example, at version 3.0, the upgrade scripts for version 1.4 may be considered
     * unsupported - this doesn't prevent the manual application of the scripts, though.
     * 
     * @param scriptPatches a list of schema patches to check
     */
    public void setValidateUpdateScriptPatches(List<SchemaUpgradeScriptPatch> scriptPatches)
    {
        this.validateUpdateScriptPatches = scriptPatches;
    }

    /**
     * Set the schema script patches that may be applied prior to the auto-update process.
     * 
     * @param scriptPatches a list of schema patches to check
     */
    public void setPreUpdateScriptPatches(List<SchemaUpgradeScriptPatch> scriptPatches)
    {
        this.preUpdateScriptPatches = scriptPatches;
    }

    /**
     * Set the schema script patches that may be applied after the auto-update process.
     * 
     * @param postUpdateScriptPatches a list of schema patches to check
     */
    public void setPostUpdateScriptPatches(List<SchemaUpgradeScriptPatch> scriptPatches)
    {
        this.postUpdateScriptPatches = scriptPatches;
    }

    /**
     * Set the number times that the DB must be checked for the presence of the table
     * indicating that a schema change is in progress.
     * 
     * @param schemaUpdateLockRetryCount        the number of times to retry (default 24)
     */
    public void setSchemaUpdateLockRetryCount(int schemaUpdateLockRetryCount)
    {
        this.schemaUpdateLockRetryCount = schemaUpdateLockRetryCount;
    }

    /**
     * Set the wait time (seconds) between checks for the schema update lock.
     * 
     * @param schemaUpdateLockRetryWaitSeconds  the number of seconds between checks (default 5 seconds)
     */
    public void setSchemaUpdateLockRetryWaitSeconds(int schemaUpdateLockRetryWaitSeconds)
    {
        this.schemaUpdateLockRetryWaitSeconds = schemaUpdateLockRetryWaitSeconds;
    }

    /**
     * Optionally override the system's default maximum string length.  Some databases have
     * limitations on how long the <b>string_value</b> columns can be while other do not.
     * When a <tt>String<tt> value exceeds the maximum size it is persisted in the
     * <b>serializable_value</b> column instead.  Some databases have limitation on the size
     * of the serializable columns as well, but usually support much more.
     * <p>
     * The system - as of V2.1.2 - will attempt to adjust the maximum string length size
     * automatically and therefore this method is not normally required.  But it is possible
     * to manually override the value if, for example, the system doesn't guess the correct
     * maximum length or if the dialect is not explicitly catered for.
     * <p>
     * All negative or zero values are ignored and the system defaults to its best guess based
     * on the dialect being used.
     * 
     * @param maximumStringLength       the maximum length of the <b>string_value</b> columns
     */
    public void setMaximumStringLength(int maximumStringLength)
    {
        if (maximumStringLength > 0)
        {
            this.maximumStringLength = maximumStringLength;
        }
    }

    /**
     * Get the limit for the hibernate executions queue
     * @return
     */
    public int getHibernateMaxExecutions()
    {
        return ActionQueue.getMAX_EXECUTIONS_SIZE();
    }

    /**
     * Set the limit for the hibernate executions queue
     * Less than zero always uses event amalgamation 
     * @param hibernateMaxExecutions
     */
    public void setHibernateMaxExecutions(int hibernateMaxExecutions)
    {
        ActionQueue.setMAX_EXECUTIONS_SIZE(hibernateMaxExecutions);
    }
    
    /**
     * Helper method to generate a schema creation SQL script from the given Hibernate
     * configuration.
     */
    private static void dumpSchemaCreate(Configuration cfg, File schemaOutputFile)
    {
        // if the file exists, delete it
        if (schemaOutputFile.exists())
        {
            schemaOutputFile.delete();
        }
        SchemaExport schemaExport = new SchemaExport(cfg)
                .setFormat(true)
                .setHaltOnError(true)
                .setOutputFile(schemaOutputFile.getAbsolutePath())
                .setDelimiter(";");
        schemaExport.execute(false, false, false, true);
    }
    
    private SessionFactory getSessionFactory()
    {
        return (SessionFactory) localSessionFactory.getObject();
    }
    
    private static class NoSchemaException extends Exception
    {
        private static final long serialVersionUID = 5574280159910824660L;
    }
    
    /**
     * Count applied patches.  This fails if multiple applied patch tables are found,
     * which normally indicates that the schema view needs to be limited.
     * 
     * @param cfg           The Hibernate config
     * @param connection    a valid database connection
     * @return Returns the number of applied patches
     * @throws NoSchemaException if the table of applied patches can't be found
     */
    private int countAppliedPatches(Configuration cfg, Connection connection) throws Exception
    {
        String defaultSchema = cfg.getProperty("hibernate.default_schema");
        if (defaultSchema != null && defaultSchema.length() == 0)
        {
            defaultSchema = null;
        }
        String defaultCatalog = cfg.getProperty("hibernate.default_catalog");
        if (defaultCatalog != null && defaultCatalog.length() == 0)
        {
            defaultCatalog = null;
        }
        DatabaseMetaData dbMetadata = connection.getMetaData();
        
        ResultSet tableRs = dbMetadata.getTables(defaultCatalog, defaultSchema, "%", null);
        boolean newPatchTable = false;
        boolean oldPatchTable = false;
        try
        {
            boolean multipleSchemas = false;
            while (tableRs.next())
            {
                String tableName = tableRs.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase("applied_patch"))
                {
                    if (oldPatchTable || newPatchTable)
                    {
                        // Found earlier
                        multipleSchemas = true;
                    }
                    oldPatchTable = true;
                }
                else if (tableName.equalsIgnoreCase("alf_applied_patch"))
                {
                    if (oldPatchTable || newPatchTable)
                    {
                        // Found earlier
                        multipleSchemas = true;
                    }
                    newPatchTable = true;
                }
            }
            // We go through all the tables so that multiple visible schemas are detected
            if (multipleSchemas)
            {
                throw new AlfrescoRuntimeException(ERR_MULTIPLE_SCHEMAS);
            }
        }
        finally
        {
            try { tableRs.close(); } catch (Throwable e) {e.printStackTrace(); }
        }
        
        if (newPatchTable)
        {
            Statement stmt = connection.createStatement();
            try
            {
                ResultSet rs = stmt.executeQuery("select count(id) from alf_applied_patch");
                rs.next();
                int count = rs.getInt(1);
                return count;
            }
            catch (SQLException e)
            {
                // This should work at least and is probably an indication of the user viewing multiple schemas
                throw new AlfrescoRuntimeException(ERR_MULTIPLE_SCHEMAS);
            }
            finally
            {
                try { stmt.close(); } catch (Throwable e) {}
            }
        }
        else if (oldPatchTable)
        {
            // found the old style table name
            Statement stmt = connection.createStatement();
            try
            {
                ResultSet rs = stmt.executeQuery("select count(id) from applied_patch");
                rs.next();
                int count = rs.getInt(1);
                return count;
            }
            finally
            {
                try { stmt.close(); } catch (Throwable e) {}
            }
        }
        else
        {
            // The applied patches table is not present
            throw new NoSchemaException();
        }
    }
    
    /**
     * @return  Returns the name of the applied patch table, or <tt>null</tt> if the table doesn't exist
     */
    private String getAppliedPatchTableName(Connection connection) throws Exception
    {
        Statement stmt = connection.createStatement();
        try
        {
            stmt.executeQuery("select * from alf_applied_patch");
            return "alf_applied_patch";
        }
        catch (Throwable e)
        {
            // we'll try another table name
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
        // for pre-1.4 databases, the table was named differently
        stmt = connection.createStatement();
        try
        {
            stmt.executeQuery("select * from applied_patch");
            return "applied_patch";
        }
        catch (Throwable e)
        {
            // It is not there
            return null;
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
    }
    
    /**
     * @return Returns the number of applied patches
     */
    private boolean didPatchSucceed(Connection connection, String patchId) throws Exception
    {
        String patchTableName = getAppliedPatchTableName(connection);
        if (patchTableName == null)
        {
            // Table doesn't exist, yet
            return false;
        }
        Statement stmt = connection.createStatement();
        try
        {
            ResultSet rs = stmt.executeQuery("select succeeded from " + patchTableName + " where id = '" + patchId + "'");
            if (!rs.next())
            {
                return false;
            }
            boolean succeeded = rs.getBoolean(1);
            return succeeded;
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
    }
    
    private static class LockFailedException extends Exception
    {
        private static final long serialVersionUID = -6676398230191205456L;
    }
    
    
    /**
     * Records that the bootstrap process has started
     */
    private synchronized void setBootstrapStarted(Connection connection) throws Exception
    {
        // Create the marker table
        Statement stmt = connection.createStatement();
        try
        {
            stmt.executeUpdate("create table alf_bootstrap_lock (charval CHAR(1) NOT NULL)");
            // Success
            return;
        }
        catch (Throwable e)
        {
            // We throw a well-known exception to be handled by retrying code if required
            throw new LockFailedException();
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
    }
    
    /**
     * Records that the bootstrap process has finished
     */
    private void setBootstrapCompleted(Connection connection) throws Exception
    {
        // Create the marker table
        Statement stmt = connection.createStatement();
        try
        {
            stmt.executeUpdate("drop table alf_bootstrap_lock");
        }
        catch (Throwable e)
        {
            // Table exists
            throw AlfrescoRuntimeException.create(ERR_PREVIOUS_FAILED_BOOTSTRAP);
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
    }
    
    /**
     * Builds the schema from scratch or applies the necessary patches to the schema.
     */
    private boolean updateSchema(Configuration cfg, Session session, Connection connection) throws Exception
    {
        boolean create = false;
        try
        {
            countAppliedPatches(cfg, connection);
        }
        catch (NoSchemaException e)
        {
            create = true;
        }
        // Get the dialect
        final Dialect dialect = Dialect.getDialect(cfg.getProperties());
        String dialectStr = dialect.getClass().getName();

        if (create)
        {
            // the applied patch table is missing - we assume that all other tables are missing
            // perform a full update using Hibernate-generated statements
            File tempFile = TempFileProvider.createTempFile("AlfrescoSchemaCreate-" + dialectStr + "-", ".sql");
            SchemaBootstrap.dumpSchemaCreate(cfg, tempFile);
            executeScriptFile(cfg, connection, tempFile, null);
            // execute post-create scripts (not patches)
            for (String scriptUrl : this.postCreateScriptUrls)
            {
                executeScriptUrl(cfg, connection, scriptUrl);
            }
        }
        else
        {
            // Check for scripts that must have been run
            checkSchemaPatchScripts(cfg, session, connection, validateUpdateScriptPatches, false);
            // Execute any pre-auto-update scripts
            checkSchemaPatchScripts(cfg, session, connection, preUpdateScriptPatches, true);
            
            // Build and execute changes generated by Hibernate
            File tempFile = null;
            Writer writer = null;
            try
            {
                DatabaseMetadata metadata = new DatabaseMetadata(connection, dialect);
                String[] sqls = cfg.generateSchemaUpdateScript(dialect, metadata);
                if (sqls.length > 0)
                {
                    tempFile = TempFileProvider.createTempFile("AlfrescoSchemaUpdate-" + dialectStr + "-", ".sql");
                    writer = new BufferedWriter(new FileWriter(tempFile));
                    for (String sql : sqls)
                    {
                        writer.append(sql);
                        writer.append(";\n");
                    }
                }
            }
            finally
            {
                if (writer != null)
                {
                    try {writer.close();} catch (Throwable e) {}
                }
            }
            // execute if there were changes raised by Hibernate
            if (tempFile != null)
            {
                executeScriptFile(cfg, connection, tempFile, null);
            }
            
            // Execute any post-auto-update scripts
            checkSchemaPatchScripts(cfg, session, connection, postUpdateScriptPatches, true);
        }
        
        return create;
    }
    
    /**
     * Check that the necessary scripts have been executed against the database
     */
    private void checkSchemaPatchScripts(
            Configuration cfg,
            Session session,
            Connection connection,
            List<SchemaUpgradeScriptPatch> scriptPatches,
            boolean apply) throws Exception
    {
        // first check if there have been any applied patches
        int appliedPatchCount = countAppliedPatches(cfg, connection);
        if (appliedPatchCount == 0)
        {
            // This is a new schema, so upgrade scripts are irrelevant
            // and patches will not have been applied yet
            return;
        }
        
        for (SchemaUpgradeScriptPatch patch : scriptPatches)
        {
            final String patchId = patch.getId();
            final String scriptUrl = patch.getScriptUrl();

            // check if the script was successfully executed
            boolean wasSuccessfullyApplied = didPatchSucceed(connection, patchId);
            if (wasSuccessfullyApplied)
            {
                // Either the patch was executed before or the system was bootstrapped
                // with the patch bean present.
                continue;
            }
            else if (!apply)
            {
                // the script was not run and may not be run automatically
                throw AlfrescoRuntimeException.create(ERR_SCRIPT_NOT_RUN, scriptUrl);
            }
            // it wasn't run and it can be run now
            executeScriptUrl(cfg, connection, scriptUrl);
        }
    }
    
    private void executeScriptUrl(Configuration cfg, Connection connection, String scriptUrl) throws Exception
    {
        Dialect dialect = Dialect.getDialect(cfg.getProperties());
        String dialectStr = dialect.getClass().getName();
        InputStream scriptInputStream = getScriptInputStream(dialect.getClass(), scriptUrl);
        // check that it exists
        if (scriptInputStream == null)
        {
            throw AlfrescoRuntimeException.create(ERR_SCRIPT_NOT_FOUND, scriptUrl);
        }
        // write the script to a temp location for future and failure reference
        File tempFile = null;
        try
        {
            tempFile = TempFileProvider.createTempFile("AlfrescoSchemaUpdate-" + dialectStr + "-", ".sql");
            ContentWriter writer = new FileContentWriter(tempFile);
            writer.putContent(scriptInputStream);
        }
        finally
        {
            try { scriptInputStream.close(); } catch (Throwable e) {}  // usually a duplicate close
        }
        // now execute it
        String dialectScriptUrl = scriptUrl.replaceAll(PLACEHOLDER_SCRIPT_DIALECT, dialect.getClass().getName());
        // Replace the script placeholders
        executeScriptFile(cfg, connection, tempFile, dialectScriptUrl);
    }
    
    /**
     * Replaces the dialect placeholder in the script URL and attempts to find a file for
     * it.  If not found, the dialect hierarchy will be walked until a compatible script is
     * found.  This makes it possible to have scripts that are generic to all dialects.
     * 
     * @return Returns an input stream onto the script, otherwise null
     */
    private InputStream getScriptInputStream(Class dialectClazz, String scriptUrl) throws Exception
    {
        // replace the dialect placeholder
        String dialectScriptUrl = scriptUrl.replaceAll(PLACEHOLDER_SCRIPT_DIALECT, dialectClazz.getName());
        // get a handle on the resource
        ResourcePatternResolver rpr = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
        Resource resource = rpr.getResource(dialectScriptUrl);
        if (!resource.exists())
        {
            // it wasn't found.  Get the superclass of the dialect and try again
            Class superClazz = dialectClazz.getSuperclass();
            if (Dialect.class.isAssignableFrom(superClazz))
            {
                // we still have a Dialect - try again
                return getScriptInputStream(superClazz, scriptUrl);
            }
            else
            {
                // we have exhausted all options
                return null;
            }
        }
        else
        {
            // we have a handle to it
            return resource.getInputStream();
        }
    }
    
    /**
     * @param cfg           the Hibernate configuration
     * @param connection    the DB connection to use
     * @param scriptFile    the file containing the statements
     * @param scriptUrl     the URL of the script to report.  If this is null, the script
     *                      is assumed to have been auto-generated.
     */
    private void executeScriptFile(
            Configuration cfg,
            Connection connection,
            File scriptFile,
            String scriptUrl) throws Exception
    {
        StringBuilder executedStatements = executedStatementsThreadLocal.get();
        if (executedStatements == null)
        {
            // There is no lock at this stage.  This process can fall out if the lock can't be applied.
            setBootstrapStarted(connection);
            executedStatements = new StringBuilder(8094);
            executedStatementsThreadLocal.set(executedStatements);
        }
        
        if (scriptUrl == null)
        {
            LogUtil.info(logger, MSG_EXECUTING_GENERATED_SCRIPT, scriptFile);
        }
        else
        {
            LogUtil.info(logger, MSG_EXECUTING_COPIED_SCRIPT, scriptFile, scriptUrl);
        }
        
        InputStream scriptInputStream = new FileInputStream(scriptFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(scriptInputStream, "UTF-8"));
        try
        {
            int line = 0;
            // loop through all statements
            StringBuilder sb = new StringBuilder(1024);
            while(true)
            {
                String sql = reader.readLine();
                line++;
                
                if (sql == null)
                {
                    // nothing left in the file
                    break;
                }
                
                // trim it
                sql = sql.trim();
                if (sql.length() == 0 ||
                    sql.startsWith( "--" ) ||
                    sql.startsWith( "//" ) ||
                    sql.startsWith( "/*" ) )
                {
                    if (sb.length() > 0)
                    {
                        // we have an unterminated statement
                        throw AlfrescoRuntimeException.create(ERR_STATEMENT_TERMINATOR, (line - 1), scriptUrl);
                    }
                    // there has not been anything to execute - it's just a comment line
                    continue;
                }
                // have we reached the end of a statement?
                boolean execute = false;
                boolean optional = false;
                if (sql.endsWith(";"))
                {
                    sql = sql.substring(0, sql.length() - 1);
                    execute = true;
                    optional = false;
                }
                else if (sql.endsWith("(optional)") || sql.endsWith("(OPTIONAL)"))
                {
                    // Get the end of statement
                    int endIndex = sql.lastIndexOf(';');
                    if (endIndex > 0)
                    {
                        sql = sql.substring(0, endIndex);
                        execute = true;
                        optional = true;
                    }
                    else
                    {
                        // Ends with "(optional)" but there is no semi-colon.
                        // Just take it at face value and probably fail.
                    }
                }
                // append to the statement being built up
                sb.append(" ").append(sql);
                // execute, if required
                if (execute)
                {
                    sql = sb.toString();
                    executeStatement(connection, sql, optional, line, scriptFile);
                    sb = new StringBuilder(1024);
                }
            }
        }
        finally
        {
            try { reader.close(); } catch (Throwable e) {}
            try { scriptInputStream.close(); } catch (Throwable e) {}
        }
    }
    
    /**
     * Execute the given SQL statement, absorbing exceptions that we expect during
     * schema creation or upgrade.
     */
    private void executeStatement(Connection connection, String sql, boolean optional, int line, File file) throws Exception
    {
        StringBuilder executedStatements = executedStatementsThreadLocal.get();
        if (executedStatements == null)
        {
            throw new IllegalArgumentException("The executedStatementsThreadLocal must be populated");
        }

        Statement stmt = connection.createStatement();
        try
        {
            if (logger.isDebugEnabled())
            {
                LogUtil.debug(logger, MSG_EXECUTING_STATEMENT, sql);
            }
            stmt.execute(sql);
            // Record the statement
            executedStatements.append(sql).append(";\n");
        }
        catch (SQLException e)
        {
            if (optional)
            {
                // it was marked as optional, so we just ignore it
                LogUtil.debug(logger, MSG_OPTIONAL_STATEMENT_FAILED, sql, e.getMessage(), file.getAbsolutePath(), line);
            }
            else
            {
                LogUtil.error(logger, ERR_STATEMENT_FAILED, sql, e.getMessage(), file.getAbsolutePath(), line);
                throw e;
            }
        }
        finally
        {
            try { stmt.close(); } catch (Throwable e) {}
        }
    }
    
    /**
     * Performs dialect-specific checking.  This includes checking for InnoDB, dumping the dialect being used
     * as well as setting any runtime, dialect-specific properties.
     */
    private void checkDialect(Dialect dialect)
    {
        Class dialectClazz = dialect.getClass();
        LogUtil.info(logger, MSG_DIALECT_USED, dialectClazz.getName());
        if (dialectClazz.equals(MySQLDialect.class) || dialectClazz.equals(MySQL5Dialect.class))
        {
            LogUtil.warn(logger, WARN_DIALECT_UNSUPPORTED, dialectClazz.getName());
        }
        if (dialectClazz.equals(HSQLDialect.class))
        {
            LogUtil.info(logger, WARN_DIALECT_HSQL);
        }
        if (dialectClazz.equals(DerbyDialect.class))
        {
            LogUtil.info(logger, WARN_DIALECT_DERBY);
        }
        
        int maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        // Adjust the maximum allowable String length according to the dialect
        if (dialect instanceof AlfrescoSQLServerDialect)
        {
            // string_value nvarchar(1024) null,
            // serializable_value image null,
            maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        }
        else if (dialect instanceof AlfrescoSybaseAnywhereDialect)
        {
            // string_value text null,
            // serializable_value varbinary(8192) null,
            maxStringLength = Integer.MAX_VALUE;
        }
        else if (dialect instanceof DB2Dialect)
        {
            // string_value varchar(1024),
            // serializable_value varchar(8192) for bit data,
            maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        }
        else if (dialect instanceof HSQLDialect)
        {
            // string_value varchar(1024),
            // serializable_value varbinary(8192),
            maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        }
        else if (dialect instanceof MySQLInnoDBDialect)
        {
            // string_value text,
            // serializable_value blob,
            maxStringLength = Integer.MAX_VALUE;
        }
        else if (dialect instanceof Oracle9Dialect)
        {
            // string_value varchar2(1024 char),
            // serializable_value long raw,
            maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        }
        else if (dialect instanceof PostgreSQLDialect)
        {
            // string_value varchar(1024),
            // serializable_value bytea,
            maxStringLength = SchemaBootstrap.DEFAULT_MAX_STRING_LENGTH;
        }
        SchemaBootstrap.setMaxStringLength(maxStringLength);
        
        // Now override the maximum string length if it was set directly
        if (maximumStringLength > 0)
        {
            SchemaBootstrap.setMaxStringLength(maximumStringLength);
        }
    }

    @Override
    protected synchronized void onBootstrap(ApplicationEvent event)
    {
        // do everything in a transaction
        Session session = getSessionFactory().openSession();
        try
        {
            // make sure that we AUTO-COMMIT
            Connection connection = session.connection();
            connection.setAutoCommit(true);
            
            Configuration cfg = localSessionFactory.getConfiguration();
            
            // Check and dump the dialect being used
            Dialect dialect = Dialect.getDialect(cfg.getProperties());
            checkDialect(dialect);
            
            // Ensure that our static connection provider is used
            String defaultConnectionProviderFactoryClass = cfg.getProperty(Environment.CONNECTION_PROVIDER);
            cfg.setProperty(Environment.CONNECTION_PROVIDER, SchemaBootstrapConnectionProvider.class.getName());
            SchemaBootstrapConnectionProvider.setBootstrapConnection(connection);
            
            // Update the schema, if required.
            if (updateSchema)
            {
                // Retries are required here as the DB lock will be applied lazily upon first statement execution.
                // So if the schema is up to date (no statements executed) then the LockFailException cannot be
                // thrown.  If it is thrown, the the update needs to be rerun as it will probably generate no SQL
                // statements the second time around.
                boolean updatedSchema = false;
                boolean createdSchema = false;
                
                for (int i = 0; i < schemaUpdateLockRetryCount; i++)
                {
                    try
                    {
                        createdSchema = updateSchema(cfg, session, connection);
                        updatedSchema = true;
                        break;
                    }
                    catch (LockFailedException e)
                    {
                        try { this.wait(schemaUpdateLockRetryWaitSeconds * 1000L); } catch (InterruptedException ee) {}
                    }
                }
                
                if (!updatedSchema)
                {
                    // The retries were exceeded
                    throw new AlfrescoRuntimeException(ERR_PREVIOUS_FAILED_BOOTSTRAP);
                }
                
                // Copy the executed statements to the output file
                File schemaOutputFile = null;
                if (schemaOuputFilename != null)
                {
                    schemaOutputFile = new File(schemaOuputFilename);
                }
                else
                {
                    schemaOutputFile = TempFileProvider.createTempFile("AlfrescoSchemaUpdate-All_Statements-", ".sql");
                }
                StringBuilder executedStatements = executedStatementsThreadLocal.get();
                if (executedStatements == null)
                {
                    LogUtil.info(logger, MSG_NO_CHANGES);
                }
                else
                {
                    FileContentWriter writer = new FileContentWriter(schemaOutputFile);
                    writer.setEncoding("UTF-8");
                    String executedStatementsStr = executedStatements.toString();
                    writer.putContent(executedStatementsStr);
                    LogUtil.info(logger, MSG_ALL_STATEMENTS, schemaOutputFile.getPath());
                }
                
                if (! createdSchema)
                {
                    // verify that all patches have been applied correctly 
                    checkSchemaPatchScripts(cfg, session, connection, validateUpdateScriptPatches, false);  // check scripts
                    checkSchemaPatchScripts(cfg, session, connection, preUpdateScriptPatches, false);       // check scripts
                    checkSchemaPatchScripts(cfg, session, connection, postUpdateScriptPatches, false);      // check scripts
                }
                
                if (executedStatements != null)
                {
                    // Remove the flag indicating a running bootstrap
                    setBootstrapCompleted(connection);
                }
            }
            else
            {
                LogUtil.info(logger, MSG_BYPASSING_SCHEMA_UPDATE);
            }

            // Reset the configuration
            cfg.setProperty(Environment.CONNECTION_PROVIDER, defaultConnectionProviderFactoryClass);
            
            // all done successfully
        }
        catch (Throwable e)
        {
            LogUtil.error(logger, e, ERR_UPDATE_FAILED);
            if (updateSchema)
            {
                throw new AlfrescoRuntimeException(ERR_UPDATE_FAILED, e);
            }
            else
            {
                throw new AlfrescoRuntimeException(ERR_VALIDATION_FAILED, e);
            }
        }
        finally
        {
            // Remove the connection reference from the threadlocal boostrap
            SchemaBootstrapConnectionProvider.setBootstrapConnection(null);
            
        }
    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        // NOOP
    }
    
    /**
     * This is a workaround for the odd Spring-Hibernate interaction during configuration.
     * The Hibernate code assumes that schema scripts will be generated during context
     * initialization.  We want to do it afterwards and have a little more control.  Hence this class.
     * <p>
     * The connection that is used will not be closed or manipulated in any way.  This class
     * merely serves to give the connection to Hibernate.
     * 
     * @author Derek Hulley
     */
    public static class SchemaBootstrapConnectionProvider extends UserSuppliedConnectionProvider
    {
        private static ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<Connection>();
        
        public SchemaBootstrapConnectionProvider()
        {
        }
        
        /**
         * Set the connection for Hibernate to use for schema generation.
         */
        public static void setBootstrapConnection(Connection connection)
        {
            threadLocalConnection.set(connection);
        }

        /**
         * Unsets the connection.
         */
        @Override
        public void close()
        {
            // Leave the connection well alone, just remove it
            threadLocalConnection.set(null);
        }

        /**
         * Does nothing.  The connection was given by a 3rd party and they can close it.
         */
        @Override
        public void closeConnection(Connection conn)
        {
        }

        /**
         * Does nothing.
         */
        @Override
        public void configure(Properties props) throws HibernateException
        {
        }

        /**
         * @see #setBootstrapConnection(Connection)
         */
        @Override
        public Connection getConnection()
        {
            return threadLocalConnection.get();
        }

        @Override
        public boolean supportsAggressiveRelease()
        {
            return false;
        }
    }
    
    private static final String DIR_SCHEMAS = "schemas";
    /**
     * Dump a set of creation files for all known Hibernate dialects.  These will be
     * dumped into the default temporary location in a subdirectory named <b>schemas</b>.
     */
    public static void main(String[] args)
    {
        int exitCode = 0;
        try
        {
            exitCode = dumpDialects(args);
        }
        catch (Throwable e)
        {
            LogUtil.error(logger, e, "SchemaBootstrap script dump failed");
            exitCode = 1;
        }
        // We can exit
        System.exit(exitCode);
    }
    
    private static int dumpDialects(String[] dialectClassNames)
    {
        if (dialectClassNames.length == 0)
        {
            System.out.println(
                    "\n" +
                    "   ERROR: A list of fully qualified class names is required");
            return 1;
        }
        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
        SchemaBootstrap schemaBootstrap = (SchemaBootstrap) ctx.getBean("schemaBootstrap");
        LocalSessionFactoryBean localSessionFactoryBean = schemaBootstrap.getLocalSessionFactory();
        Configuration configuration = localSessionFactoryBean.getConfiguration();
        
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        DescriptorService descriptorService = serviceRegistry.getDescriptorService();
        Descriptor descriptor = descriptorService.getServerDescriptor();
        
        File tempDir = TempFileProvider.getTempDir();
        File schemasDir = new File(tempDir, DIR_SCHEMAS);
        if (!schemasDir.exists())
        {
            schemasDir.mkdir();
        }
        File dumpDir = new File(schemasDir, descriptor.getVersion());
        if (!dumpDir.exists())
        {
            dumpDir.mkdir();
        }
        for (String dialectClassName : dialectClassNames)
        {
            Class dialectClazz = null;
            try
            {
                dialectClazz = Class.forName(dialectClassName);
            }
            catch (ClassNotFoundException e)
            {
                System.out.println(
                        "\n" +
                        "   ERROR: Class not found: " + dialectClassName);
                continue;
            }
            if (!Dialect.class.isAssignableFrom(dialectClazz))
            {
                System.out.println(
                        "\n" +
                        "   ERROR: The class name is not a valid dialect: " + dialectClassName);
                continue;
            }
            dumpDialectScript(configuration, dialectClazz, dumpDir);
        }
        // Done
        return 0;
    }
    
    private static void dumpDialectScript(Configuration configuration, Class dialectClazz, File directory)
    {
        // Set the dialect
        configuration.setProperty("hibernate.dialect", dialectClazz.getName());
        
        // First dump the dialect's schema
        String filename = "default-schema-create-" + dialectClazz.getName() + ".sql";
        File dumpFile = new File(directory, filename);
        
        // Write the file
        SchemaBootstrap.dumpSchemaCreate(configuration, dumpFile);
    }
}
