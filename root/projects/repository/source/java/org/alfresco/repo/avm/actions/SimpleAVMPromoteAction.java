/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.avm.actions;

import java.util.List;

import org.alfresco.config.JNDIConstants;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncException;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.surf.util.Pair;

/**
 * An ActionExecuter that promotes content from one store to another.
 * The NodeRef argument is in the source AVMStore. The 'target-store'
 * mandatory argument is the name of the destination store.
 * @author britt
 * 
 * @deprecated
 */
public class SimpleAVMPromoteAction extends ActionExecuterAbstractBase
{
    public static final String NAME = "simple-avm-promote";
    public static final String PARAM_TARGET_STORE = "target-store";

    /**
     * The AVMSyncService instance.
     */
    private AVMSyncService fAVMSyncService;
    
    /**
     * Default constructor.
     */
    public SimpleAVMPromoteAction()
    {
        super();
    }
    
    /**
     * Set the AVMSyncService instance.
     * @param avmSyncService
     */
    public void setAvmSyncService(AVMSyncService avmSyncService)
    {
        fAVMSyncService = avmSyncService;
    }
    
    /**
     * Do a promotion of an asset from one sandbox to another.
     * Takes a mandatory parameter 'target-store' which is the name of the
     * target AVMStore.
     * @param action The source of parameters.
     * @param actionedUponNodeRef The source AVM NodeRef.
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        String targetStoreName = (String)action.getParameterValue(PARAM_TARGET_STORE);
        // Crack the NodeRef.
        Pair<Integer, String> avmVersionPath = AVMNodeConverter.ToAVMVersionPath(actionedUponNodeRef);
        int version = avmVersionPath.getFirst();
        String path = avmVersionPath.getSecond();
        // Get store name and path parts.
        String [] storePath = path.split(":");
        if (storePath.length != 2)
        {
            throw new AVMSyncException("Malformed source path: " + path);
        }
        // Compute the corresponding target path.
        String targetPath = targetStoreName + ":" + storePath[1];
        // Find the differences.
        List<AVMDifference> diffs = 
            fAVMSyncService.compare(version, path, -1, targetPath, null);
        // TODO fix update comments at some point.
        // Do the promote.
        fAVMSyncService.update(diffs, null, true, true, false, false, null, null);
        // Flatten the source on top of the destination.
        fAVMSyncService.flatten(storePath[0]    + ":/" + JNDIConstants.DIR_DEFAULT_WWW,
                                targetStoreName + ":/" + JNDIConstants.DIR_DEFAULT_WWW);
    }

    /**
     * Define needed parameters.
     * @param paramList The List of ParameterDefinitions to add to.
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        paramList.add(new ParameterDefinitionImpl(PARAM_TARGET_STORE,
                                                  DataTypeDefinition.TEXT,
                                                  true,
                                                  getParamDisplayLabel(PARAM_TARGET_STORE)));
    }
}
