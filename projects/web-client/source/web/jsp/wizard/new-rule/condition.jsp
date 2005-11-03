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

<r:page titleId="title_new_rule_condition">

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="new-rule-condition">
   
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
                              <h:graphicImage id="wizard-logo" url="/images/icons/new_rule_large.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"><h:outputText value='#{NavigationBean.nodeProperties["name"]}' /></div>
                              <div class="mainTitle"><h:outputText value="#{NewRuleWizard.wizardTitle}" /></div>
                              <div class="mainSubText"><h:outputText value="#{NewRuleWizard.wizardDescription}" /></div>
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
                              <h:outputText styleClass="mainSubTitle" value="#{msg.steps}"/><br>
                              <a:modeList itemSpacing="3" iconColumnWidth="2" selectedStyleClass="statusListHighlight" 
                                          value="2" disabled="true">
                                 <a:listItem value="1" label="1. #{msg.details}" />
                                 <a:listItem value="2" label="2. #{msg.conditions}" />
                                 <a:listItem value="3" label="3. #{msg.actions}" />
                                 <a:listItem value="4" label="4. #{msg.summary}" />
                              </a:modeList>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "blue"); %>
                           </td>
                           
                           <td width="100%" valign="top">
                           
                              <a:errors message="#{msg.error_wizard}" styleClass="errorMessage" />
                           
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "white", "white"); %>
                              <table cellpadding="2" cellspacing="2" border="0" width="100%">
                                 <tr>
                                    <td colspan="2" class="mainSubTitle"><h:outputText value="#{NewRuleWizard.stepTitle}" /></td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td>1.</td>
                                    <td><h:outputText value="#{msg.select_condition}"/></td>
                                 </tr>
                                 <tr>
                                    <td>&nbsp;</td>
                                    <td width="98%">
                                       <h:selectOneMenu value="#{NewRuleWizard.condition}" 
                                          onchange="javascript:itemSelected(this);">
                                          <f:selectItems value="#{NewRuleWizard.conditions}" />
                                       </h:selectOneMenu>
                                    </td>
                                 </tr>
                                 <%--
                                 <tr><td class="paddingRow"></td></tr>
                                 <tr>
                                    <td valign="top">&nbsp;</td>
                                    <td>
                                       <div>
                                          <a:dynamicDescription selected="#{NewRuleWizard.condition}">
                                             <a:descriptions value="#{NewRuleWizard.conditionDescriptions}" />
                                          </a:dynamicDescription>
                                       </div>
                                    </td>
                                 </tr>
                                 --%>
                                 <tr><td class="paddingRow"></td></tr>
                                 <tr>
                                    <td>2.</td>
                                    <td><h:outputText value="#{msg.click_set_and_add}" id="instruction-text"/></td>
                                 </tr>
                                 <tr>
                                    <td>&nbsp;</td>
                                    <td><h:commandButton id="set-add-button" value="#{msg.set_and_add_button}" 
                                                         action="#{NewRuleWizard.promptForConditionValues}"
                                                         disabled="true"/></td>
                                 </tr>
                                 <tr><td class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan='2'><h:outputText value="#{msg.selected_conditions}"/></td>
                                 </tr>
                                 <tr>
                                    <td colspan='2'>
                                       <h:dataTable value="#{NewRuleWizard.allConditionsDataModel}" var="row"
                                                    rowClasses="selectedItemsRow,selectedItemsRowAlt"
                                                    styleClass="selectedItems" headerClass="selectedItemsHeader"
                                                    cellspacing="0" cellpadding="4" 
                                                    rendered="#{NewRuleWizard.allConditionsDataModel.rowCount != 0}">
                                          <h:column>
                                             <f:facet name="header">
                                                <h:outputText value="#{msg.summary}" />
                                             </f:facet>
                                             <h:outputText value="#{row.conditionSummary}"/>
                                             <h:outputText value="&nbsp;&nbsp;" escape="false"/>
                                          </h:column>
                                          <h:column>
                                             <a:actionLink action="#{NewRuleWizard.removeCondition}" image="/images/icons/delete.gif"
                                                           value="#{msg.remove}" showLink="false"/>
                                             <h:outputText value="&nbsp;" escape="false"/>
                                             <a:actionLink action="#{NewRuleWizard.editCondition}" image="/images/icons/edit_icon.gif"
                                                           value="#{msg.change}" showLink="false" 
                                                           rendered='#{row.conditionName != "no-condition"}'/>
                                          </h:column>
                                       </h:dataTable>
                                       <a:panel id="no-items" rendered="#{NewRuleWizard.allConditionsDataModel.rowCount == 0}">
                                          <table cellspacing='0' cellpadding='2' border='0' class='selectedItems'>
                                             <tr>
                                                <td colspan='2' class='selectedItemsHeader'><h:outputText id="no-items-name" value="#{msg.summary}" /></td>
                                             </tr>
                                             <tr>
                                                <td class='selectedItemsRow'><h:outputText id="no-items-msg" value="#{msg.no_selected_items}" /></td>
                                             </tr>
                                          </table>
                                       </a:panel>
                                    </td>
                                 </tr>
                                 <tr><td class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2"><h:outputText value="#{NewRuleWizard.stepInstructions}" /></td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "white"); %>
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.next_button}" action="#{NewRuleWizard.next}" styleClass="wizardButton" 
                                                        disabled="#{NewRuleWizard.allConditionsDataModel.rowCount == 0}" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.back_button}" action="#{NewRuleWizard.back}" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.finish_button}" action="#{NewRuleWizard.finish}" styleClass="wizardButton" 
                                                        disabled="true"/>
                                    </td>
                                 </tr>
                                 <tr><td class="wizardButtonSpacing"></td></tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.cancel_button}" action="#{NewRuleWizard.cancel}" styleClass="wizardButton" />
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
    
    <script language="JavaScript1.2">
      function itemSelected(inputField)
      {
         if (inputField.selectedIndex == 0)
         {
            document.getElementById("new-rule-condition:set-add-button").disabled = true;
         }
         else
         {
            document.getElementById("new-rule-condition:set-add-button").disabled = false;
         }
         
         // also check to see if the 'no-condition' option has been selected, if it has, change
         // the explanation text and the button label
         var short_text = "<a:outputText value='#{msg.click_add}'/>";
         var long_text = "<a:outputText value='#{msg.click_set_and_add}'/>";
         var short_label = "<a:outputText value='#{msg.add_to_list_button}'/>";
         var long_label = "<a:outputText value='#{msg.set_and_add_button}'/>";
         
         if (inputField.value == "no-condition")
         {
            document.getElementById("new-rule-condition:set-add-button").value = short_label;
            document.getElementById("new-rule-condition:instruction-text").innerHTML = short_text;
         }
         else
         {
            document.getElementById("new-rule-condition:set-add-button").value = long_label;
            document.getElementById("new-rule-condition:instruction-text").innerHTML = long_text;
         }
      }
    </script>
    
   </h:form>
    
</f:view>

</r:page>