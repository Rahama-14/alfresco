/*-----------------------------------------------------------------------------
*  Copyright 2006 Alfresco Inc.
*  
*  Licensed under the Mozilla Public License version 1.1
*  with a permitted attribution clause. You may obtain a
*  copy of the License at:
*  
*      http://www.alfresco.org/legal/license.txt
*  
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
*  either express or implied. See the License for the specific
*  language governing permissions and limitations under the
*  License.
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    VirtServerRegistryMBean.java
*----------------------------------------------------------------------------*/

package org.alfresco.mbeans;

public interface VirtServerRegistryMBean
{
    public void initialize();

    // public void   setVirtServerJmxUrl(String virtServerJmxUrl);
    public String    getVirtServerJmxUrl();

    public Integer getVirtServerHttpPort();
    public String  getVirtServerFQDN();

    public void registerVirtServerInfo( String  virtServerJmxUrl,
                                        String  virtServerFQDN,
                                        Integer virtServerHttpPort
                                      );  


    /**  Sets password file used to access virt server */
    public void   setPasswordFile(String path);

    /**  Gets password file used to access virt server */
    public String getPasswordFile();
    
    /**  Sets access "role" file used by virt server */
    public void   setAccessFile(String path);

    /**  Gets  access "role" file used by virt server */
    public String getAccessFile();


    /** 
    *  Notifies remote listener that a AVM-based webapp has been updated;
    *  an "update" is any change to (or creation of) contents within
    *  WEB-INF/classes  WEB-INF/lib, WEB-INF/web.xml of a webapp.
    *
    * @param version      The version of the webapp being updated.
    *                     Typically, this is set to -1, which corresponds
    *                     to the very latest version ("HEAD").
    *                     If versinon != -1, you might want to consider
    *                     setting the 'isRecursive' parameter to false.
    *                     <p>
    *
    * @param pathToWebapp The full AVM path to the webapp being updated.
    *                     For example:  repoName:/www/avm_webapps/your_webapp
    *                     <p>
    *
    * @param isRecursive  When true, update all webapps that depend on this one.
    *                     For example, an author's webapps share jar/class files
    *                     with the master version in staging; thus, the author's
    *                     webapp "depends" on the webapp in staging.   Similarly,
    *                     webapps in an author's preview area depend on the ones
    *                     in the "main" layer of the author's sandbox.   
    *                     You might wish to set this parameter to 'false' if 
    *                     the goal is to bring a non-HEAD version of a staging 
    *                     area online, without forcing the virtualization server 
    *                     to load all the author sandboxes for this archived 
    *                     version as well.
    */
    public boolean 
    updateAllWebapps(int version, String pathToWebapp, boolean isRecursive);

    /**
    *  Notifies remote listener that a AVM-based webapp has been removed.
    *
    * @param version      The version of the webapp being removed.
    *                     Typically, this is set to -1, which corresponds
    *                     to the very latest version ("HEAD").
    *                     If versinon != -1, you might want to consider
    *                     setting the 'isRecursive' parameter to false.
    *                     <p>
    *
    * @param pathToWebapp The full AVM path to the webapp being removed.
    *                     For example:  repoName:/www/avm_webapps/your_webapp
    *                     <p>
    *
    * @param isRecursive  When true, remove all webapps that depend on this one.
    *                     For example, an author's webapps share jar/class files
    *                     with the master version in staging; thus, the author's
    *                     webapp "depends" on the webapp in staging.   Similarly,
    *                     webapps in an author's preview area depend on the ones
    *                     in the "main" layer of the author's sandbox.   
    *                     You might wish to set this parameter to 'false' if 
    *                     the goal is to bring a non-HEAD version of a staging 
    *                     area online, without forcing the virtualization server 
    *                     to load all the author sandboxes for this archived 
    *                     version as well.
    */
    public boolean 
    removeAllWebapps(int version, String pathToWebapp, boolean isRecursive );
}
