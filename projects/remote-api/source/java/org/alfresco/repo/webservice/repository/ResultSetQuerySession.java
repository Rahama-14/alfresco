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
package org.alfresco.repo.webservice.repository;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.webservice.Utils;
import org.alfresco.repo.webservice.types.NamedValue;
import org.alfresco.repo.webservice.types.Query;
import org.alfresco.repo.webservice.types.ResultSetRowNode;
import org.alfresco.repo.webservice.types.Store;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of a QuerySession that retrieves results from a repository ResultSet
 * 
 * @author gavinc
 */
public class ResultSetQuerySession extends AbstractQuerySession
{
   private transient static Log logger = LogFactory.getLog(ResultSetQuerySession.class);
   
   private Store store;
   private Query query;
   private boolean includeMetaData;
   
   /**
    * Constructs a ResultSetQuerySession
    * 
    * @param batchSize The batch size to use for this session
    * @param store The repository store to query against
    * @param query The query to execute
    * @param includeMetaData Whether to include metadata in the query results
    */
   public ResultSetQuerySession(int batchSize, Store store, Query query, boolean includeMetaData)
   {
      super(batchSize);
      
      this.store = store;
      this.query = query;
      this.includeMetaData = includeMetaData;
   }
   
   /**
    * @see org.alfresco.repo.webservice.repository.QuerySession#getNextResultsBatch()
    */
   public QueryResult getNextResultsBatch(SearchService searchService, NodeService nodeService)
   {
      QueryResult queryResult = null;
      
      if (this.position != -1)
      {
         if (logger.isDebugEnabled())
            logger.debug("Before getNextResultsBatch: " + toString());
         
         // handle the special search string of * meaning, get everything
         String statement = query.getStatement();
         if (statement.equals("*"))
         {
            statement = "ISNODE:*";
         }
         
         ResultSet searchResults = searchService.query(Utils.convertToStoreRef(this.store), 
               this.query.getLanguage().getValue(), statement);
         
         int totalRows = searchResults.length();
         int lastRow = calculateLastRowIndex(totalRows);
         int currentBatchSize = lastRow - this.position;
         
         if (logger.isDebugEnabled())
            logger.debug("Total rows = " + totalRows + ", current batch size = " + currentBatchSize);
         
         org.alfresco.repo.webservice.types.ResultSet batchResults = new org.alfresco.repo.webservice.types.ResultSet();      
         org.alfresco.repo.webservice.types.ResultSetRow[] rows = new org.alfresco.repo.webservice.types.ResultSetRow[currentBatchSize];
         
         int arrPos = 0;
         for (int x = this.position; x < lastRow; x++)
         {
            ResultSetRow origRow = searchResults.getRow(x);
            NodeRef nodeRef = origRow.getNodeRef();
            ResultSetRowNode rowNode = new ResultSetRowNode(nodeRef.getId(), nodeService.getType(nodeRef).toString(), null);
            
            // get the data for the row and build up the columns structure
            Map<Path, Serializable> values = origRow.getValues();
            NamedValue[] columns = new NamedValue[values.size()];
            int col = 0;
            for (Path path : values.keySet())
            {
               String value = null;
               Serializable valueObj = values.get(path);
               if (valueObj != null)
               {
                  value = valueObj.toString();
               }
               columns[col] = new NamedValue(path.toString(), value);
               col++;
            }
            
            org.alfresco.repo.webservice.types.ResultSetRow row = new org.alfresco.repo.webservice.types.ResultSetRow();
            row.setColumns(columns);
            row.setScore(origRow.getScore());
            row.setRowIndex(x);
            row.setNode(rowNode);
            
            // add the row to the overall results
            rows[arrPos] = row;
            arrPos++;
         }
         
         // TODO: build up the meta data data structure if we've been asked to
         
         // add the rows to the result set and set the total row count
         batchResults.setRows(rows);
         batchResults.setTotalRowCount(totalRows);
         
         queryResult = new QueryResult(getId(), batchResults);
         
         // move the position on
         updatePosition(totalRows, queryResult);
         
         if (logger.isDebugEnabled())
            logger.debug("After getNextResultsBatch: " + toString());
      }
      
      return queryResult;
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder(super.toString());
      builder.append(" (id=").append(getId());
      builder.append(" batchSize=").append(this.batchSize);
      builder.append(" position=").append(this.position);
      builder.append(" store=").append(this.store.getScheme().getValue()).append(":").append(this.store.getAddress());
      builder.append(" language=").append(this.query.getLanguage().getValue());
      builder.append(" statement=").append(this.query.getStatement());
      builder.append(")");
      return builder.toString();
   }
}
