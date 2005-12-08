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
package org.alfresco.config.evaluator;

/**
 * Evaluator that tests whether an object is equal to a string
 * 
 * @author gavinc
 */
public class StringEvaluator implements Evaluator
{
    /**
     * Tests whether the given object is equal to the string given in the condition
     * 
     * @see org.alfresco.config.evaluator.Evaluator#applies(java.lang.Object, java.lang.String)
     */
    public boolean applies(Object obj, String condition)
    {
        boolean result = false;

        if (obj instanceof String)
        {
            result = obj.toString().equals(condition);
        }

        return result;
    }

}
