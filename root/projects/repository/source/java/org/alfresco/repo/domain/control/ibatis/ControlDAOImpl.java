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
package org.alfresco.repo.domain.control.ibatis;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.alfresco.repo.domain.control.AbstractControlDAOImpl;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * iBatis-specific, DB-agnostic implementation for connection controlling DAO.
 * 
 * @author Derek Hulley
 * @since 3.2SP1
 */
public class ControlDAOImpl extends AbstractControlDAOImpl
{
    /**
     * The iBatis-specific template for convenient statement execution.
     */
    protected SqlMapClientTemplate template;
    
    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate)
    {
        this.template = sqlMapClientTemplate;
    }

    public void startBatch()
    {
        /*
         * The 'transactions' here are just iBatis internal markers and
         * don't have any effect other than to let iBatis know that a batch
         * is possible.
         */
        SqlMapClient sqlMapClient = template.getSqlMapClient();
        try
        {
            sqlMapClient.startTransaction();
            sqlMapClient.startBatch();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to start DAO batch.", e);
        }
    }

    public void executeBatch()
    {
        /*
         * The 'transactions' here are just iBatis internal markers and
         * don't have any effect other than to let iBatis know that a batch
         * is possible.
         */
        SqlMapClient sqlMapClient = template.getSqlMapClient();
        try
        {
            sqlMapClient.executeBatch();
            sqlMapClient.commitTransaction();
            sqlMapClient.endTransaction();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to start DAO batch.", e);
        }
    }
    
    /**
     * PostgreSQL-specific implementation for control DAO.
     * 
     * @author Derek Hulley
     * @since 3.2SP1
     */
    public static class PostgreSQL extends ControlDAOImpl
    {
        /**
         * Calls through to the {@link Connection#setSavepoint(String) current connection}.
         */
        @Override
        public Savepoint createSavepoint(final String savepoint)
        {
            try
            {
                Connection connection = DataSourceUtils.getConnection(template.getDataSource());
                return connection.setSavepoint(savepoint);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Failed to create SAVEPOINT: " + savepoint, e);
            }
        }
        /**
         * Calls through to the {@link Connection#setSavepoint(String) current connection}.
         */
        @Override
        public void rollbackToSavepoint(Savepoint savepoint)
        {
            try
            {
                Connection connection = DataSourceUtils.getConnection(template.getDataSource());
                connection.rollback(savepoint);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Failed to create SAVEPOINT: " + savepoint, e);
            }
        }
        @Override
        public void releaseSavepoint(Savepoint savepoint)
        {
            try
            {
                Connection connection = DataSourceUtils.getConnection(template.getDataSource());
                connection.releaseSavepoint(savepoint);
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Failed to create SAVEPOINT: " + savepoint, e);
            }
        }
    }
}
