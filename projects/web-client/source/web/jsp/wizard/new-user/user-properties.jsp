<%--
  Copyright (C) 2005 Alfresco, Inc.

  Licensed under the GNU Lesser General Public License as
  published by the Free Software Foundation; either version
  2.1 of the License, or (at your option) any later version.
  You may obtain a copy of the License at

    http://www.gnu.org/licenses/lgpl.txt

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  either express or implied. See the License for the specific
  language governing permissions and limitations under the
  License.
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="32kb" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page>

<script language="JavaScript1.2">
   function updateButtonState()
   {
      if (document.getElementById("user-props:userName").value.length == 0)
      {
         document.getElementById("user-props:finish-button").disabled = true;
         document.getElementById("user-props:next-button").disabled = true;
      }
      else
      {
         document.getElementById("user-props:finish-button").disabled = false;
         document.getElementById("user-props:next-button").disabled = false;
      }
   }
</script>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages" var="msg"/>
   
   <%-- set the form name here --%>
   <h:form id="user-props">
   
   <%-- Main outer table --%>
   <table cellspacing="0" cellpadding="2">
      
      <%-- Title bar --%>
      <tr>
         <td colspan="2">
            <%@ include file="../../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign="top">
         <%-- Shelf --%>
         <td>
            <%@ include file="../../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width="100%">
            <table cellspacing="0" cellpadding="0" width="100%">
               <%-- Breadcrumb --%>
               <%@ include file="../../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width="4"></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr valign="top">
                           <td width="32">
                              <h:graphicImage id="wizard-logo" url="/images/icons/new_user_large.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"/><h:outputText value='#{NavigationBean.nodeProperties.name}' /></div>
                              <div class="mainTitle"><h:outputText value="#{NewUserWizard.wizardTitle}" /></div>
                              <div class="mainSubText"><h:outputText value="#{NewUserWizard.wizardDescription}" /></div>
                           </td>
                        </tr>
                     </table>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- separator row with gradient shadow --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_7.gif" width="4" height="9"></td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_9.gif" width="4" height="9"></td>
               </tr>
               
               <%-- Details --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width="4"></td>
                  <td>
                     <table cellspacing="0" cellpadding="3" border="0" width="100%">
                        <tr>
                           <td width="20%" valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <h:outputText styleClass="mainSubTitle" value="Steps"/><br>
                              <a:modeList itemSpacing="3" iconColumnWidth="2" selectedStyleClass="statusListHighlight"
                                    value="2" disabled="true">
                                 <a:listItem value="1" label="1. Person Properties" />
                                 <a:listItem value="2" label="2. User Properties" />
                                 <a:listItem value="3" label="3. Summary" />
                              </a:modeList>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "blue"); %>
                           </td>
                           
                           <td width="100%" valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "white", "white"); %>
                              <table cellpadding="2" cellspacing="2" border="0" width="100%">
                                 <tr>
                                    <td class="mainSubTitle"><h:outputText value="#{NewUserWizard.stepTitle}" /></td>
                                 </tr>
                                 <tr>
                                    <td class="mainSubText"><h:outputText value="#{NewUserWizard.stepDescription}" /></td>
                                 </tr>
                                 
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading">User Properties</td>
                                 </tr>
                                 <tr>
                                    <td>User Name:</td>
                                    <td>
                                       <h:inputText id="userName" value="#{NewUserWizard.userName}" size="35" maxlength="1024" validator="#{LoginBean.validateUsername}" onkeyup="updateButtonState();" disabled="#{NewUserWizard.editMode}" />&nbsp;*
                                       &nbsp;<h:message id="errors1" for="userName" style="color:red" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td>User Password:</td>
                                    <td>
                                       <h:inputSecret id="password" value="#{NewUserWizard.password}" size="35" maxlength="1024" validator="#{LoginBean.validatePassword}" onkeyup="updateButtonState();" disabled="#{NewUserWizard.editMode}" redisplay="true" />&nbsp;*
                                       &nbsp;<h:message id="errors2" for="password" style="color:red" />
                                    </td>
                                 </tr>
                                 
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading">Home Space</td>
                                 </tr>
                                 <tr>
                                    <td>Home Space Location:</td>
                                    <td>
                                       <r:spaceSelector label="Click here to select the Home Space location" value="#{NewUserWizard.homeSpaceLocation}" initialSelection="#{NavigationBean.currentNodeId}" style="border: 1px dashed #cccccc; padding: 2px;"/>
                                    </td>
                                 </tr>
                                 <tr>
                                    <td>Home Space Name:</td>
                                    <td>
                                       <h:inputText id="homeSpaceName" value="#{NewUserWizard.homeSpaceName}" size="35" maxlength="1024" onkeyup="updateButtonState();" />
                                    </td>
                                 </tr>
                                 
                                 <tr><td class="paddingRow"></td></tr>
                                 <tr>
                                    <td><h:outputText value="#{NewUserWizard.stepInstructions}" /></td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "white"); %>
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="Next" id="next-button" action="#{NewUserWizard.next}" styleClass="wizardButton" disabled="true" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="Back" action="#{NewUserWizard.back}" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="Finish" id="finish-button" action="#{NewUserWizard.finish}" styleClass="wizardButton" disabled="true" />
                                    </td>
                                 </tr>
                                 <tr><td class="wizardButtonSpacing"></td></tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="Cancel" action="#{NewUserWizard.cancel}" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "blue"); %>
                           </td>
                        </tr>
                     </table>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- separator row with bottom panel graphics --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_7.gif" width="4" height="4"></td>
                  <td width="100%" align="center" style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_9.gif" width="4" height="4"></td>
               </tr>
               
            </table>
          </td>
       </tr>
    </table>
    
    </h:form>
    
</f:view>

<script>
   updateButtonState();
</script>

</r:page>