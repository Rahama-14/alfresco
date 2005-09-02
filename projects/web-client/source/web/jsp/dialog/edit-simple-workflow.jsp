<%--
  Copyright (C) 2005 Alfresco, Inc.
 
  Licensed under the Mozilla Public License version 1.1 
  with a permitted attribution clause. You may obtain a
  copy of the License at
 
    http://www.alfresco.org/legal/license.txt
 
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

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page titleId="title_edit_simple_workflow">

<script language="JavaScript1.2">
   
   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("edit-simple-workflow:approve-step-name").focus();
      checkButtonState();
   }
   
   function checkButtonState()
   {
      if (document.getElementById("edit-simple-workflow:approve-step-name").value.length == 0 ||
          document.getElementById("edit-simple-workflow:client-approve-folder").value.length == 0 ||
          rejectValid() == false)
      {
         document.getElementById("edit-simple-workflow:ok-button").disabled = true;
      }
      else
      {
         document.getElementById("edit-simple-workflow:ok-button").disabled = false;
      }
   }
   
   function rejectValid()
   {
      var result = true;
      
      if (document.forms['edit-simple-workflow']['edit-simple-workflow:reject-step-present'][0].checked && 
          (document.getElementById("edit-simple-workflow:reject-step-name").value.length == 0 ||
           document.getElementById("edit-simple-workflow:client-reject-folder").value.length == 0))
      {
         result = false;
      }
      
      return result;
   }
</script>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="edit-simple-workflow">
   
   <%-- add the approve and reject folder ids as hidden fields --%>
   <h:inputHidden id="client-approve-folder" value="#{DocumentDetailsBean.workflowProperties.approveFolder}" />
   <h:inputHidden id="client-reject-folder" value="#{DocumentDetailsBean.workflowProperties.rejectFolder}" />
   
   <%-- Main outer table --%>
   <table cellspacing="0" cellpadding="2">
      
      <%-- Title bar --%>
      <tr>
         <td colspan="2">
            <%@ include file="../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign="top">
         <%-- Shelf --%>
         <td>
            <%@ include file="../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width="100%">
            <table cellspacing="0" cellpadding="0" width="100%">
               <%-- Breadcrumb --%>
               <%@ include file="../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width="4"></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr valign="top">
                           <td width="32">
                              <h:graphicImage id="wizard-logo" url="/images/icons/edit_large.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"><h:outputText value="#{NavigationBean.nodeProperties.name}" /></div>
                              <div class="mainTitle"><h:outputText value="#{msg.modify_workflow_props}" /></div>
                              <div class="mainSubText"><h:outputText value="#{msg.editworkflow_description}" /></div>
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
                           <td width="100%" valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "white", "white"); %>
                              <table cellpadding="2" cellspacing="2" border="0" width="100%">
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading"><h:outputText value="#{msg.approve_flow}" /></td>
                                 </tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.name_approve_step}" />:</nobr></td>
                                    <td width="90%">
                                       <h:inputText id="approve-step-name" value="#{DocumentDetailsBean.workflowProperties.approveStepName}" 
                                                    onkeyup="javascript:checkButtonState();" />
                                    </td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr><td colspan="2"><h:outputText value="#{msg.choose_copy_move_location}" /></td>
                                 <tr>
                                    <td colspan="2">
                                       <table cellpadding="2" cellspacing="2" border="0">
                                          <tr>
                                             <td valign="top">
                                                <h:selectOneRadio value="#{DocumentDetailsBean.workflowProperties.approveAction}">
                                                   <f:selectItem itemValue="move" itemLabel="Move" />
                                                   <f:selectItem itemValue="copy" itemLabel="Copy" />
                                                </h:selectOneRadio>
                                             </td>
                                             <td style="padding-left:6px;"></td>
                                             <td valign="top" style="padding-top:10px;"><h:outputText value="#{msg.to}" />:</td>
                                             <td style="padding-left:6px;"></td>
                                             <td style="padding-top:6px;">
                                                <r:spaceSelector label="#{msg.select_destination_prompt}"
                                                        value="#{DocumentDetailsBean.workflowProperties.approveFolder}"
                                                        initialSelection="#{NavigationBean.currentNodeId}"
                                                        style="border: 1px dashed #cccccc; padding: 6px;"/>
                                             </td>
                                          </tr>
                                       </table>
                                    </td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading"><h:outputText value="#{msg.reject_flow}" /></td>
                                 </tr>
                                 <tr>
                                    <td colspan="2"><h:outputText value="#{msg.select_reject_step}" /></td>
                                 </tr>
                                 <tr>
                                    <td>
                                       <h:selectOneRadio id="reject-step-present" value="#{DocumentDetailsBean.workflowProperties.rejectStepPresent}"
                                                         onclick="javascript:checkButtonState();">
                                          <f:selectItem itemValue="yes" itemLabel="#{msg.yes}" />
                                          <f:selectItem itemValue="no" itemLabel="#{msg.no}" />
                                       </h:selectOneRadio>
                                    </td>
                                 </tr>
                                 <tr>
                                    <td colspan="2">
                                       <table cellpadding="0" cellspacing="0" border="0">
                                          <tr>
                                             <td style="padding-left:24px;"></td>
                                             <td>
                                                <table cellpadding="2" cellspacing="2" border="0">
                                                   <tr>
                                                      <td><nobr>
                                                         <h:outputText value="#{msg.name_reject_step}" />:&nbsp;
                                                         <h:inputText id="reject-step-name" value="#{DocumentDetailsBean.workflowProperties.rejectStepName}" 
                                                                      onkeyup="javascript:checkButtonState();" />
                                                      </nobr></td>
                                                   </tr>
                                                   <tr><td class="paddingRow"></td></tr>
                                                   <tr><td><h:outputText value="#{msg.choose_copy_move_location}" /></td>
                                                   <tr>
                                                      <td>
                                                         <table cellpadding="2" cellspacing="2" border="0">
                                                            <tr>
                                                               <td valign="top">
                                                                  <h:selectOneRadio value="#{DocumentDetailsBean.workflowProperties.rejectAction}">
                                                                     <f:selectItem itemValue="move" itemLabel="#{msg.move}" />
                                                                     <f:selectItem itemValue="copy" itemLabel="#{msg.copy}" />
                                                                  </h:selectOneRadio>
                                                               </td>
                                                               <td style="padding-left:6px;"></td>
                                                               <td valign="top" style="padding-top:10px;"><h:outputText value="#{msg.to}" />:</td>
                                                               <td style="padding-left:6px;"></td>
                                                               <td style="padding-top:6px;">
                                                                  <r:spaceSelector label="#{msg.select_destination_prompt}" 
                                                                          value="#{DocumentDetailsBean.workflowProperties.rejectFolder}" 
                                                                          initialSelection="#{NavigationBean.currentNodeId}"
                                                                          style="border: 1px dashed #cccccc; padding: 6px;"/>
                                                               </td>
                                                            </tr>
                                                         </table>
                                                      </td>
                                                   </tr>
                                                </table>
                                             </td>
                                          </tr>
                                       </table>
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "white"); %>
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton id="ok-button" value="#{msg.ok}" action="#{DocumentDetailsBean.saveWorkflow}" 
                                                        styleClass="wizardButton" disabled="true" />
                                    </td>
                                 </tr>
                                 <tr><td class="wizardButtonSpacing"></td></tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.cancel}" action="cancel" styleClass="wizardButton" />
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

</r:page>