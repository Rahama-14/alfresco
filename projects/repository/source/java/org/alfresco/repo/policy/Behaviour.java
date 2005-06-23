/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.policy;


/**
 * A Behaviour represents an encapsulated piece of logic (system or business)
 * that may be bound to a Policy.  The logic may be expressed in any
 * language (java, script etc).  
 *
 * Once bound to a Policy, the behaviour must be able to provide the interface
 * declared by that policy.
 * 
 * @author David Caruana
 */
public interface Behaviour
{
    /**
     * Gets the requested policy interface onto the behaviour 
     * 
     * @param policy  the policy interface class
     * @return  the policy interface
     */
    public <T> T getInterface(Class<T> policy);
    
    /**
     * Disable the behaviour (for this thread only)
     */
    public void disable();

    /**
     * Enable the behaviour (for this thread only)
     */
    public void enable();

    /**
     * @return  is the behaviour enabled (for this thread only)
     */
    public boolean isEnabled();
    
}
