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
package org.alfresco.service.namespace;

import java.util.Collection;



/**
 * Namespace Service.
 * 
 * The Namespace Service provides access to and definition of namespace
 * URIs and Prefixes. 
 * 
 * @author David Caruana
 */
public interface NamespaceService extends NamespacePrefixResolver
{
    /** Default Namespace URI */
    public static final String DEFAULT_URI = "";
    
    /** Default Namespace Prefix */
    public static final String DEFAULT_PREFIX = "";

    /** Default Alfresco URI */
    public static final String ALFRESCO_URI = "http://www.alfresco.org";
    
    /** Default Alfresco Prefix */
    public static final String ALFRESCO_PREFIX = "alf";
    
    /** Dictionary Model URI */
    public static final String DICTIONARY_MODEL_1_0_URI = "http://www.alfresco.org/model/dictionary/1.0";
    
    /** Dictionary Model Prefix */
    public static final String DICTIONARY_MODEL_PREFIX = "d";

    /** System Model URI */
    public static final String SYSTEM_MODEL_1_0_URI = "http://www.alfresco.org/model/system/1.0";

    /** System Model Prefix */
    public static final String SYSTEM_MODEL_PREFIX = "sys";

    /** Content Model URI */
    public static final String CONTENT_MODEL_1_0_URI = "http://www.alfresco.org/model/content/1.0";

    /** Content Model Prefix */
    public static final String CONTENT_MODEL_PREFIX = "cm";

    /** Application Model URI */
    public static final String APP_MODEL_1_0_URI = "http://www.alfresco.org/model/application/1.0";

    /** Application Model Prefix */
    public static final String APP_MODEL_PREFIX = "app";

    /** Alfresco View Namespace URI */
    public static final String REPOSITORY_VIEW_1_0_URI = "http://www.alfresco.org/view/repository/1.0";
    
    /** Alfresco View Namespace Prefix */
    public static final String REPOSITORY_VIEW_PREFIX = "view";
    
    
    /**
     * Gets all registered Namespace URIs
     * 
     * @return collection of all registered namespace URIs
     */
    public Collection<String> getURIs();
    
}
