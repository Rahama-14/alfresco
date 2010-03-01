/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.repo.transfer;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.transfer.manifest.TransferManifestProcessor;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.transfer.TransferReceiver;

/**
 * @author brian
 * 
 */
public class DefaultManifestProcessorFactoryImpl implements ManifestProcessorFactory
{
    private NodeService nodeService;
    private ContentService contentService;
    private CorrespondingNodeResolverFactory nodeResolverFactory;

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.transfer.ManifestProcessorFactory#getPrimaryCommitProcessor()
     */
    public List<TransferManifestProcessor> getCommitProcessors(TransferReceiver receiver, String transferId)
    {
        List<TransferManifestProcessor> processors = new ArrayList<TransferManifestProcessor>();
        CorrespondingNodeResolver nodeResolver = nodeResolverFactory.getResolver();
        
        RepoPrimaryManifestProcessorImpl primaryProcessor = new RepoPrimaryManifestProcessorImpl(receiver, transferId);
        primaryProcessor.setContentService(contentService);
        primaryProcessor.setNodeResolver(nodeResolver);
        primaryProcessor.setNodeService(nodeService);
        processors.add(primaryProcessor);
        
        RepoSecondaryManifestProcessorImpl secondaryProcessor = new RepoSecondaryManifestProcessorImpl(receiver, transferId);
        secondaryProcessor.setNodeResolver(nodeResolver);
        secondaryProcessor.setNodeService(nodeService);
        processors.add(secondaryProcessor);
        
        return processors;
    }

    /**
     * @param nodeService the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param contentService the contentService to set
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param nodeResolverFactory the nodeResolverFactory to set
     */
    public void setNodeResolverFactory(CorrespondingNodeResolverFactory nodeResolverFactory)
    {
        this.nodeResolverFactory = nodeResolverFactory;
    }

}
