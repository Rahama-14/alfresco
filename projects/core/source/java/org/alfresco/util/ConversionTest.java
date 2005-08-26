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
package org.alfresco.util;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Unit test to make sure the Conversion utility is working correctly
 * 
 * @author gavinc
 */
public class ConversionTest extends BaseTest
{
   private static Log logger = LogFactory.getLog(ConversionTest.class);
   
   public void testDateConversion() throws Exception
   {
      Date now = new Date();
      String dateBefore = now.toString();
      logger.info("date before = " + dateBefore);
      
      String formattedDate = Conversion.dateToXmlDate(now);
      logger.info("formattedDate = " + formattedDate);
      
      Date dateAfterXml = Conversion.dateFromXmlDate(formattedDate);
      String dateAfter = dateAfterXml.toString();
      logger.info("date after = " + dateAfter);
      
      assertEquals("The date before and after conversion should be the same", dateBefore, dateAfter);
   }
}
