<%--
  Copyright (C) 2005 Alfresco, Inc.
 
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ taglib uri="/WEB-INF/wcm.tld" prefix="wcm" %>

<script type="text/javascript">
function _xforms_getSubmitButtons()
{
  return [ document.getElementById("wizard:next-button"),
           document.getElementById("wizard:finish-button") ];
}
function _xforms_getSaveDraftButtons()
{
  return [ document.getElementById("wizard:back-button") ];
}
</script>
<wcm:formProcessor id="form-data-renderer"
		   formProcessorSession="#{WizardManager.bean.formProcessorSession}" 
		   formInstanceData="#{WizardManager.bean.instanceDataDocument}" 
		   formInstanceDataName="#{WizardManager.bean.fileName}" 
		   form="#{WizardManager.bean.form}"/>
