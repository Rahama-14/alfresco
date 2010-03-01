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
package org.alfresco.repo.search.impl.lucene;

import org.alfresco.service.cmr.repository.StoreRef;

public class ADMLuceneUnIndexedIndexAndSearcherFactory extends ADMLuceneIndexerAndSearcherFactory
{

    @Override
    protected LuceneIndexer createIndexer(StoreRef storeRef, String deltaId)
    {
        ADMLuceneNoActionIndexerImpl indexer = ADMLuceneIndexerImpl.getNoActionIndexer(storeRef, deltaId, this);
        indexer.setNodeService(nodeService);
        indexer.setTenantService(tenantService);
        indexer.setDictionaryService(dictionaryService);
        // indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setFullTextSearchIndexer(fullTextSearchIndexer);
        indexer.setContentService(contentService);
        indexer.setMaxAtomicTransformationTime(getMaxTransformationTime());
        return indexer;
    }
}
