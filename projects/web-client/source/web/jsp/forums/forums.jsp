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

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<r:page titleId="title_forums">

<f:view>
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="browse-forums">
   
   <%-- Main outer table --%>
   <table cellspacing=0 cellpadding=2>

      <%-- Title bar --%>
      <tr>
         <td colspan=2>
            <%@ include file="../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign=top>
         <%-- Shelf --%>
         <td>
            <%@ include file="../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width=100%>
            <table cellspacing=0 cellpadding=0 width=100%>
               <%-- Breadcrumb --%>
               <%@ include file="../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width=4></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing=4 cellpadding=0 width=100%>
                        <tr valign=top>
 
                           <%-- actions for forums --%>
                           <a:panel id="forums-actions">
                              <td width=32>
                                 <h:graphicImage id="space-logo" url="/images/icons/forums_large.gif" width="32" height="32" />
                              </td>
                              <td>
                                 <%-- Summary --%>
                                 <div class="mainSubTitle"><h:outputText value="#{msg.forums}" id="msg1" /></div>
                                 <div class="mainTitle"><h:outputText value="#{NavigationBean.nodeProperties.name}" id="msg2" /></div>
                                 <div class="mainSubText"><h:outputText value="#{msg.forums_info}" id="msg3" /></div>
                                 <div class="mainSubText"><h:outputText value="#{NavigationBean.nodeProperties.description}" id="msg4" /></div>
                              </td>
                              <td bgcolor="#465F7D" width=1></td>
                              <td width=110 style="padding-left:2px">
                                 <%-- Current object actions --%>
                                 <h:outputText style="padding-left:20px" styleClass="mainSubTitle" value="#{msg.actions}" id="msg5" /><br>
                                 <r:permissionEvaluator value="#{NavigationBean.currentNode}" allow="CreateChildren" id="eval1">
                                    <a:actionLink value="#{msg.create_forum}" image="/images/icons/create_forum.gif" padding="4" action="createForum" actionListener="#{CreateForumDialog.startWizard}" id="link1" />
                                 </r:permissionEvaluator>
                                 <a:actionLink value="#{msg.view_details}" image="/images/icons/View_details.gif" padding="4" action="showForumsDetails" actionListener="#{BrowseBean.setupSpaceAction}" id="link2">
                                    <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param1" />
                                 </a:actionLink>
                                 <r:permissionEvaluator value="#{NavigationBean.currentNode}" allow="Delete" id="eval2">
                                    <a:actionLink value="#{msg.delete_forums}" image="/images/icons/delete.gif" padding="4" action="deleteForums" actionListener="#{BrowseBean.setupDeleteAction}" id="link3">
                                       <f:param name="id" value="#{NavigationBean.currentNodeId}" id="param2" />
                                    </a:actionLink>
                                 </r:permissionEvaluator>
                              </td>
                           </a:panel>
                                                      
                           <td bgcolor="#465F7D" width=1></td>
                           <td width=110>
                              <%-- View mode settings --%>
                              <h:outputText style="padding-left:26px" styleClass="mainSubTitle" value="#{msg.view}"/><br>
                              <a:modeList itemSpacing="3" iconColumnWidth="20" selectedStyleClass="statusListHighlight" selectedImage="/images/icons/Details.gif"
                                    value="#{ForumsBean.forumsViewMode}" actionListener="#{ForumsBean.forumsViewModeChanged}">
                                 <a:listItem value="details" label="#{msg.details_view}" />
                                 <a:listItem value="icons" label="#{msg.view_icon}" />
                                 <a:listItem value="list" label="#{msg.view_browse}" />
                              </a:modeList>
                           </td>
                        </tr>
                     </table>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_6.gif)" width=4></td>
               </tr>
               
               <%-- separator row with gradient shadow --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_7.gif" width=4 height=9></td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_9.gif" width=4 height=9></td>
               </tr>
               
               <%-- Details - Forums --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <div style="padding:4px">
                     
                     <a:panel id="forums-panel" border="white" bgcolor="white" titleBorder="blue" titleBgcolor="#D3E6FE" styleClass="mainSubTitle" label="#{msg.browse_forums}">
                     
                     <%-- Forums List --%>
                     <a:richList id="forumsList" binding="#{ForumsBean.forumsRichList}" viewMode="#{ForumsBean.forumsViewMode}" pageSize="#{ForumsBean.forumsPageSize}"
                           styleClass="recordSet" headerStyleClass="recordSetHeader" rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                           value="#{ForumsBean.forums}" var="r">
                        
                        <%-- component to display if the list is empty --%>
                        <f:facet name="empty">
                           <h:outputFormat value="#{msg.no_forums}" escape="false" />
                        </f:facet>
                        
                        <%-- Primary column for details view mode --%>
                        <a:column primary="true" width="200" style="padding:2px;text-align:left" rendered="#{ForumsBean.forumsViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="#{msg.name}" value="name" mode="case-insensitive" styleClass="header"/>
                           </f:facet>
                           <f:facet name="small-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/space_small.gif" actionListener="#{BrowseBean.clickSpace}" action="showTopics" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for icons view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left;vertical-align:top;" rendered="#{ForumsBean.forumsViewMode == 'icons'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/#{r.icon}.gif" actionListener="#{BrowseBean.clickSpace}" action="showTopics" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="header">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Primary column for list view mode --%>
                        <a:column primary="true" style="padding:2px;text-align:left" rendered="#{ForumsBean.forumsViewMode == 'list'}">
                           <f:facet name="large-icon">
                              <a:actionLink value="#{r.name}" image="/images/icons/#{r.icon}.gif" actionListener="#{BrowseBean.clickSpace}" action="showTopics" showLink="false">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </f:facet>
                           <a:actionLink value="#{r.name}" actionListener="#{BrowseBean.clickSpace}" styleClass="title">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <%-- Description column for all view modes --%>
                        <a:column style="text-align:left">
                           <f:facet name="header">
                              <a:sortLink label="#{msg.description}" value="description" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.description}" />
                        </a:column>
                        
                        <%-- Path column for search mode in details view mode --%>
                        <a:column style="text-align:left" rendered="#{NavigationBean.searchContext != null && ForumsBean.forumsViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="#{msg.path}" value="displayPath" styleClass="header"/>
                           </f:facet>
                           <r:nodePath value="#{r.path}" actionListener="#{BrowseBean.clickSpacePath}" />
                        </a:column>
                        
                        <%-- Created Date column for details view mode --%>
                        <a:column style="text-align:left" rendered="#{ForumsBean.forumsViewMode == 'details'}">
                           <f:facet name="header">
                              <a:sortLink label="#{msg.created}" value="created" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.created}">
                              <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Modified Date column for details/icons view modes --%>
                        <a:column style="text-align:left" rendered="#{ForumsBean.forumsViewMode == 'details' || ForumsBean.forumsViewMode == 'icons'}">
                           <f:facet name="header">
                              <a:sortLink label="#{msg.modified}" value="modified" styleClass="header"/>
                           </f:facet>
                           <h:outputText value="#{r.modified}">
                              <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                           </h:outputText>
                        </a:column>
                        
                        <%-- Node Descendants links for list view mode --%>
                        <a:column style="text-align:left" rendered="#{ForumsBean.forumsViewMode == 'list'}">
                           <r:nodeDescendants value="#{r.nodeRef}" styleClass="header" actionListener="#{BrowseBean.clickDescendantSpace}" />
                        </a:column>
                        
                        <%-- Actions column --%>
                        <a:column actions="true" style="text-align:left">
                           <f:facet name="header">
                              <h:outputText value="#{msg.actions}"/>
                           </f:facet>
                           <r:permissionEvaluator value="#{r}" allow="Delete">
                              <a:actionLink value="#{msg.delete_forum}" image="/images/icons/delete.gif" showLink="false" styleClass="inlineAction" action="deleteForum" actionListener="#{BrowseBean.setupDeleteAction}">
                                 <f:param name="id" value="#{r.id}" />
                              </a:actionLink>
                           </r:permissionEvaluator>
                           <a:actionLink value="#{msg.view_details}" image="/images/icons/View_details.gif" showLink="false" styleClass="inlineAction" action="showForumDetails" actionListener="#{BrowseBean.setupSpaceAction}">
                              <f:param name="id" value="#{r.id}" />
                           </a:actionLink>
                        </a:column>
                        
                        <a:dataPager styleClass="pager" />
                     </a:richList>
                     
                     </a:panel>
                     
                     </div>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- Error Messages --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <%-- messages tag to show messages not handled by other specific message tags --%>
                     <h:messages globalOnly="true" styleClass="errorMessage" layout="table" />
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- separator row with bottom panel graphics --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_7.gif" width=4 height=4></td>
                  <td width=100% align=center style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_9.gif" width=4 height=4></td>
               </tr>
               
            </table>
          </td>
       </tr>
    </table>
    
    </h:form>
    
</f:view>

</r:page>
