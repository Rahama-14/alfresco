<%--
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="64kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="javax.faces.context.FacesContext" %>

<r:page titleId="title_file_details">

<f:view>
   <%
      FacesContext fc = FacesContext.getCurrentInstance();

      // set locale for JSF framework usage
      fc.getViewRoot().setLocale(Application.getLanguage(fc));
   %>

   <%-- load your custom properties file --%>
   <f:loadBundle basename="alfresco.extension.webclient" var="customMsg"/>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>

   <h:form acceptcharset="UTF-8" id="document-details">

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
                  <td bgcolor="#dfe6ed">

                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr>
                           <td width="32">
                              <img src="<%=request.getContextPath()%>/images/icons/details_large.gif" width=32 height=32>
                           </td>
                           <td>
                              <div class="mainTitle">
                                 <h:outputText value="#{msg.details_of}" /> '<h:outputText value="#{DocumentDetailsBean.name}" />'<r:lockIcon value="#{DocumentDetailsBean.document.nodeRef}" align="absmiddle" />
                              </div>
                              <div class="mainSubText">
                                 <h:outputText value="#{msg.location}" />: <r:nodePath value="#{DocumentDetailsBean.document.nodeRef}" breadcrumb="true" actionListener="#{BrowseBean.clickSpacePath}" />
                              </div>
                              <a:panel id="working-copy" rendered="#{DocumentDetailsBean.locked && DocumentDetailsBean.workingCopyDocument != null}">
                                 <div class="mainSubText">
                                    <h:outputText id="out-workingcopy" value="#{msg.working_copy_document}" />:
                                    <a:actionLink id="act-details" rendered="#{DocumentDetailsBean.workingCopyDocument != null}" value="#{DocumentDetailsBean.workingCopyDocument.properties.name}" actionListener="#{BrowseBean.setupContentAction}" action="showDocDetails">
                                       <f:param name="id" value="#{DocumentDetailsBean.workingCopyDocument.id}" />
                                    </a:actionLink>
                                 </div>
                              </a:panel>
                              <div class="mainSubText"><h:outputText id="doc-details" value="#{msg.documentdetails_description}" /></div>
                           </td>

                           <%-- Navigation --%>
                           <td align=right>
                              <a:actionLink id="act-prev" value="#{msg.previous_item}" image="/images/icons/nav_prev.gif" showLink="false" actionListener="#{DocumentDetailsBean.previousItem}" action="showDocDetails">
                                 <f:param name="id" value="#{DocumentDetailsBean.id}" />
                              </a:actionLink>
                              <img src="<%=request.getContextPath()%>/images/icons/nav_file.gif" width=24 height=24 align=absmiddle>
                              <a:actionLink id="act-next" value="#{msg.next_item}" image="/images/icons/nav_next.gif" showLink="false" actionListener="#{DocumentDetailsBean.nextItem}" action="showDocDetails">
                                 <f:param name="id" value="#{DocumentDetailsBean.id}" />
                              </a:actionLink>
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

               <%-- Error Messages --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <%-- messages tag to show messages not handled by other specific message tags --%>
                     <a:errors message="" infoClass="statusWarningText" errorClass="statusErrorText" />
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>

               <%-- Details --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width="4"></td>
                  <td>
                     <table cellspacing="0" cellpadding="3" border="0" width="100%">
                        <tr>
                           <td width="100%" valign="top">
                              <%-- wrapper comment used by the panel to add additional component facets --%>
                              <h:panelGroup id="dashboard-panel-facets">
                                 <f:facet name="title">
                                    <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write" id="evalChange">
                                       <a:actionLink id="actModify" value="#{msg.modify}" action="dialog:applyTemplate" showLink="false" image="/images/icons/preview.gif" style="padding-right:8px" />
                                       <a:actionLink id="actRemove" value="#{msg.remove}" actionListener="#{DocumentDetailsBean.removeTemplate}" showLink="false" image="/images/icons/delete.gif" />
                                    </r:permissionEvaluator>
                                 </f:facet>
                              </h:panelGroup>
                              <a:panel label="#{msg.custom_view}" id="dashboard-panel" progressive="true" facetsId="dashboard-panel-facets"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       expanded='#{DocumentDetailsBean.panels["dashboard-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <table width=100% cellspacing=0 cellpadding=0 border=0>
                                    <tr>
                                       <td align=left>
                                          <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write" id="evalApply">
                                             <a:actionLink id="actDashboard" value="#{msg.apply_template}" rendered="#{!DocumentDetailsBean.hasCustomView}" action="dialog:applyTemplate" />
                                          </r:permissionEvaluator>
                                          <a:panel id="template-panel" rendered="#{DocumentDetailsBean.hasCustomView}">
                                             <div style="padding:4px;border: 1px dashed #cccccc">
                                                <r:webScript id="webscript" scriptUrl="#{DocumentDetailsBean.webscriptUrl}" context="#{DocumentDetailsBean.document.nodeRef}" rendered="#{DocumentDetailsBean.hasWebscriptView}" />
                                                <r:template id="dashboard" template="#{DocumentDetailsBean.templateRef}" model="#{DocumentDetailsBean.templateModel}" rendered="#{!DocumentDetailsBean.hasWebscriptView && DocumentDetailsBean.hasTemplateView}" />
                                             </div>
                                          </a:panel>
                                       </td>
                                    </tr>
                                 </table>
                              </a:panel>

                              <div style="padding:4px"></div>

                              <a:panel label="#{msg.view_links}" id="preview-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       expanded='#{DocumentDetailsBean.panels["preview-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <table width="100%" cellspacing="2" cellpadding="2" border="0" align="center">
                                    <tr>
                                       <td>
                                          <a:actionLink value="#{msg.view_in_browser}" href="#{DocumentDetailsBean.browserUrl}" target="new" id="link1" />
                                       </td>
                                       <td>
                                          <a:actionLink value="#{msg.view_in_webdav}" href="#{DocumentDetailsBean.webdavUrl}" target="new" id="link2" />
                                       </td>
                                       <td>
                                          <a:actionLink value="#{msg.view_in_cifs}" href="#{DocumentDetailsBean.cifsPath}" target="new" id="link3" />
                                       </td>
                                    </tr>
                                    <tr>
                                       <td>
                                          <a:actionLink value="#{msg.download_content}" href="#{DocumentDetailsBean.downloadUrl}" target="new" id="link4" />
                                       </td>
                                       <td>
                                          <a href='<%=request.getContextPath()%><a:outputText value="#{DocumentDetailsBean.bookmarkUrl}" id="out1" />' onclick="return false;"><a:outputText value="#{msg.details_page_bookmark}" id="out2" /></a>
                                       </td>
                                       <td>
                                          <a href='<a:outputText value="#{DocumentDetailsBean.nodeRefUrl}" id="out3" />' onclick="return false;"><a:outputText value="#{msg.noderef_link}" id="out4" /></a>
                                       </td>
                                    </tr>
                                 </table>
                              </a:panel>

                              <div style="padding:4px"></div>

                              <h:panelGroup id="props-panel-facets">
                                 <f:facet name="title">
                                    <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write">
                                       <a:actionLink id="titleLink1" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif"
                                                     action="dialog:editContentProperties" actionListener="#{DocumentDetailsBean.setupEditContext}" />
                                    </r:permissionEvaluator>
                                 </f:facet>
                              </h:panelGroup>
                              <a:panel label="#{customMsg.three_column_properties}" id="properties-panel" facetsId="props-panel-facets" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       rendered="#{DocumentDetailsBean.locked == false}"
                                       expanded='#{DocumentDetailsBean.panels["properties-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <table cellspacing="0" cellpadding="0" border="0" width="100%">
                                    <tr>
                                       <td width=80 align=center>
                                          <%-- icon image for the doc --%>
                                          <table cellspacing=0 cellpadding=0 border=0>
                                             <tr>
                                                <td>
                                                   <div style="border: thin solid #CCCCCC; padding:4px">
                                                      <a:actionLink id="doc-logo1" value="#{DocumentDetailsBean.name}" href="#{DocumentDetailsBean.url}" target="new" image="#{DocumentDetailsBean.document.properties.fileType32}" showLink="false" />
                                                   </div>
                                                </td>
                                                <td><img src="<%=request.getContextPath()%>/images/parts/rightSideShadow42.gif" width=6 height=42></td>
                                             </tr>
                                             <tr>
                                                <td colspan=2><img src="<%=request.getContextPath()%>/images/parts/bottomShadow42.gif" width=48 height=5></td>
                                             </tr>
                                          </table>
                                       </td>
                                       <td>
                                          <%-- properties for the doc --%>
                                          <r:propertySheetGrid id="document-props" value="#{DocumentDetailsBean.document}" var="documentProps"
                                                      columns="3" mode="view" labelStyleClass="propertiesLabel"
                                                      externalConfig="true" />
                                          <h:outputText id="no-inline-msg" value="<br/>#{msg.not_inline_editable}<br/><br/>"
                                               rendered="#{DocumentDetailsBean.inlineEditable == false}" escape="false" />
                                          <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write" id="eval_inline">
                                             <a:actionLink id="make-inline" value="#{msg.allow_inline_editing}"
                                                  action="#{DocumentDetailsBean.applyInlineEditable}"
                                                  rendered="#{DocumentDetailsBean.inlineEditable == false}" />
                                          </r:permissionEvaluator>
                                          <h:message id="msg1" for="document-props" styleClass="statusMessage" />
                                       </td>
                                    </tr>
                                 </table>
                              </a:panel>
                              <a:panel label="#{msg.properties}" id="properties-panel-locked" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" rendered="#{DocumentDetailsBean.locked}"
                                       expanded='#{DocumentDetailsBean.panels["properties-panel-locked"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <table cellspacing="0" cellpadding="0" border="0" width="100%">
                                    <tr>
                                       <td width=80 align=center>
                                          <%-- icon image for the doc --%>
                                          <table cellspacing=0 cellpadding=0 border=0>
                                             <tr>
                                                <td>
                                                   <div style="border: thin solid #CCCCCC; padding:4px">
                                                      <a:actionLink id="doc-logo2" value="#{DocumentDetailsBean.name}" href="#{DocumentDetailsBean.url}" target="new" image="#{DocumentDetailsBean.document.properties.fileType32}" showLink="false" />
                                                   </div>
                                                </td>
                                                <td><img src="<%=request.getContextPath()%>/images/parts/rightSideShadow42.gif" width=6 height=42></td>
                                             </tr>
                                             <tr>
                                                <td colspan=2><img src="<%=request.getContextPath()%>/images/parts/bottomShadow42.gif" width=48 height=5></td>
                                             </tr>
                                          </table>
                                       </td>
                                       <td>
                                          <r:propertySheetGrid id="document-props-locked" value="#{DocumentDetailsBean.document}" var="documentProps"
                                                      columns="1" mode="view" labelStyleClass="propertiesLabel" externalConfig="true" />
                                          <h:outputText id="no-inline-msg2" value="<br/>#{msg.not_inline_editable}<br/>"
                                               rendered="#{DocumentDetailsBean.inlineEditable == false}" escape="false" />
                                          <h:message id="msg2" for="document-props-locked" styleClass="statusMessage" />
                                       </td>
                                    </tr>
                                 </table>
                              </a:panel>

                              <div style="padding:4px"></div>

                              <%-- Multilingual properties --%>
                              <h:panelGroup id="ml-props-panel-facets">
                                 <f:facet name="title">
                                    <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write">
                                       <a:actionLink id="titleLinkMl" value="#{msg.modify}" showLink="false" image="/images/icons/edit_properties.gif"
                                                     action="dialog:editMlContainer" />
                                    </r:permissionEvaluator>
                                  </f:facet>
                              </h:panelGroup>

                              <%-- Panel if the node has the multilingual aspect--%>
                              <a:panel label="#{msg.ml_content_info}" facetsId="ml-props-panel-facets" id="ml-info-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" rendered="#{DocumentDetailsBean.multilingual}"
                                       expanded='#{DocumentDetailsBean.panels["ml-info-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">

                                 <%-- properties for Ml container --%>
                                 <div style="padding:4px"></div>
                                 <h:outputText value="#{msg.properties}" styleClass="nodeWorkflowInfoTitle" style="padding:20px;"/>
                                 <div style="padding:4px"></div>
   
                                 <r:propertySheetGrid id="ml-container-props-sheet" value="#{DocumentDetailsBean.documentMlContainer}"
                                          var="mlContainerProps" columns="1" labelStyleClass="propertiesLabel"
                                          externalConfig="true" cellpadding="2" cellspacing="2" mode="view"/>
   
                                 <div style="padding:8px"></div>
   
                                 <a:panel label="#{msg.related_translations}" id="related-translation-panel" progressive="true"
                                          expanded='#{DocumentDetailsBean.panels["related-translation-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}" styleClass="nodeWorkflowInfoTitle">
   
                                    <div style="padding:4px"></div>
                                    <%-- list of translations --%>
                                    <a:richList id="TranslationList" viewMode="details" value="#{DocumentDetailsBean.translations}"
                                               var="r" styleClass="recordSet" headerStyleClass="recordSetHeader"
                                               rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                                               pageSize="10" initialSortColumn="Name" initialSortDescending="false">

                                       <%-- Name and icon columns --%>
                                       <a:column id="col21" primary="true" width="300" style="text-align:left">
                                          <f:facet name="small-icon">
                                             <h:graphicImage url="/images/filetypes/_default.gif" width="16" height="16"/>
                                          </f:facet>
                                          <f:facet name="header">
                                             <a:sortLink label="#{msg.name}" value="Name" mode="case-insensitive" styleClass="header"/>
                                          </f:facet>
                                          <a:actionLink id="view-name" value="#{r.name}" href="#{r.url}" target="new" />
                                       </a:column>
   
                                       <%-- Language columns --%>
                                       <a:column id="col22" width="50" style="text-align:left">
                                          <f:facet name="header">
                                             <a:sortLink label="#{msg.language}" value="language" mode="case-insensitive" styleClass="header"/>
                                          </f:facet>
                                          <a:actionLink id="view-language" value="#{r.language}" href="#{r.url}" target="new" />
                                       </a:column>
   
                                       <%-- view actions --%>
                                       <a:column id="col25" style="text-align: left">
                                          <f:facet name="header">
                                             <h:outputText value="#{msg.actions}"/>
                                          </f:facet>
                                          <a:actionLink id="view-link" value="#{msg.view}" href="#{r.url}" target="new" />
                                       </a:column>
   
                                       <a:dataPager styleClass="pager" />
                                    </a:richList>
                                 </a:panel>
                               
                                 <%-- Actions - Add Translation, Add Translation with Content --%>
                                 <div style="padding:16px">
                                    <a:actionLink id="act-add-trans-with-c"    value="#{msg.add_translation}"    action="addTranslation" actionListener="#{AddTranslationDialog.start}" showLink="true" image="/images/icons/add_tranlsation.gif"  />
                                    <span style="padding-left:16px">
                                       <a:actionLink id="act-add-trans-without-c" value="#{msg.add_translation_wc}" action="dialog:addTranslationWithoutContent" showLink="true" image="/images/icons/add_tranlsation_wc.gif" />
                                    </span>
                                 </div>
                              </a:panel>

                              <%-- Panel if the node has not the multilingual aspect--%>
                              <a:panel label="#{msg.ml_content_info}" id="no-ml-info-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       rendered="#{DocumentDetailsBean.multilingual == false}"
                                       expanded='#{DocumentDetailsBean.panels["ml-info-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <h:outputText id="no-ml-msg" value="#{msg.not_multilingual}" />
                                 <%-- Action - Add Translation --%>
                                 <div style="padding:16px"><a:actionLink id="act-make-multilingual" value="#{msg.make_multilingual}" action="dialog:makeMultilingual" showLink="true" image="/images/icons/make_ml.gif" /></div>
                              </a:panel>

                              <div style="padding:4px"></div>

                              <h:panelGroup id="workflow-panel-facets">
                                 <f:facet name="title">
                                    <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write">
                                       <a:actionLink id="titleLink2" value="#{msg.title_edit_simple_workflow}" showLink="false"
                                                     image="/images/icons/Change_details.gif" action="editSimpleWorkflow"
                                                     rendered="#{DocumentDetailsBean.approveStepName != null}" />
                                    </r:permissionEvaluator>
                                 </f:facet>
                              </h:panelGroup>
                              <a:panel label="#{msg.workflows}" id="workflow-panel" facetsId="workflow-panel-facets" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       expanded='#{DocumentDetailsBean.panels["workflow-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <r:nodeWorkflowInfo id="workflow-info" value="#{DocumentDetailsBean.document}" />
                              </a:panel>

                              <div style="padding:4px"></div>

                              <h:panelGroup id="category-panel-facets">
                                 <f:facet name="title">
                                    <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write">
                                       <a:actionLink id="titleLink3" value="#{msg.change_category}" showLink="false" image="/images/icons/Change_details.gif"
                                             action="editCategories" actionListener="#{DocumentDetailsBean.setupCategoriesForEdit}" />
                                    </r:permissionEvaluator>
                                 </f:facet>
                              </h:panelGroup>
                              <a:panel label="#{msg.category}" id="category-panel" facetsId="category-panel-facets" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" rendered="#{DocumentDetailsBean.categorised}"
                                       expanded='#{DocumentDetailsBean.panels["category-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <h:outputText id="category-overview" value="#{DocumentDetailsBean.categoriesOverviewHTML}"
                                               escape="false" />
                              </a:panel>
                              <a:panel label="#{msg.category}" id="no-category-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       rendered="#{DocumentDetailsBean.categorised == false}"
                                       expanded='#{DocumentDetailsBean.panels["category-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <h:outputText id="no-category-msg" value="#{msg.not_in_category}<br/><br/>"
                                               escape="false"/>
                                 <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write" id="eval_cat">
                                    <a:actionLink id="make-classifiable" value="#{msg.allow_categorization}"
                                                  action="#{DocumentDetailsBean.applyClassifiable}"
                                                  rendered="#{DocumentDetailsBean.locked == false}" />
                                 </r:permissionEvaluator>
                              </a:panel>

                              <div style="padding:4px"></div>

                              <a:panel label="#{msg.version_history}" id="version-history-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" rendered="#{DocumentDetailsBean.versionable}"
                                       expanded='#{DocumentDetailsBean.panels["version-history-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">

                                 <a:richList id="versionHistoryList" viewMode="details" value="#{DocumentDetailsBean.versionHistory}"
                                             var="r" styleClass="recordSet" headerStyleClass="recordSetHeader"
                                             rowStyleClass="recordSetRow" altRowStyleClass="recordSetRowAlt" width="100%"
                                             pageSize="10" initialSortColumn="versionLabel" initialSortDescending="false">

                                    <%-- Primary column for details view mode --%>
                                    <a:column id="col1" primary="true" width="100" style="padding:2px;text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="#{msg.version}" value="versionLabel" mode="case-insensitive" styleClass="header"/>
                                       </f:facet>
                                       <a:actionLink id="label" value="#{r.versionLabel}" href="#{r.url}" target="new" />
                                    </a:column>

                                    <%-- Version notes columns --%>
                                    <a:column id="col2" width="200" style="text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="#{msg.notes}" value="notes" styleClass="header"/>
                                       </f:facet>
                                       <h:outputText id="notes" value="#{r.notes}" />
                                    </a:column>

                                    <%-- Description columns --%>
                                    <a:column id="col3" style="text-align:left">
                                       <f:facet name="header">
                                          <a:sortLink label="#{msg.author}" value="author" styleClass="header"/>
                                       </f:facet>
                                       <h:outputText id="author" value="#{r.author}" />
                                    </a:column>

                                    <%-- Created Date column for details view mode --%>
                                    <a:column id="col4" style="text-align:left; white-space:nowrap">
                                       <f:facet name="header">
                                          <a:sortLink label="#{msg.date}" value="versionDate" styleClass="header"/>
                                       </f:facet>
                                       <h:outputText id="date" value="#{r.versionDate}">
                                          <a:convertXMLDate type="both" pattern="#{msg.date_time_pattern}" />
                                       </h:outputText>
                                    </a:column>

                                    <%-- view the contents of the specific version --%>
                                    <a:column id="col5" style="text-align: left">
                                       <f:facet name="header">
                                          <h:outputText value="#{msg.actions}"/>
                                       </f:facet>
                                       <a:actionLink id="view-link" value="#{msg.view}" href="#{r.url}" target="new" />
                                    </a:column>

                                    <a:dataPager styleClass="pager" />
                                 </a:richList>
                              </a:panel>
                              <a:panel label="#{msg.version_history}" id="no-version-history-panel" progressive="true"
                                       border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white"
                                       rendered="#{DocumentDetailsBean.versionable == false}"
                                       expanded='#{DocumentDetailsBean.panels["version-history-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <h:outputText id="no-history-msg" value="#{msg.not_versioned}<br/><br/>" escape="false" />
                                 <r:permissionEvaluator value="#{DocumentDetailsBean.document}" allow="Write" id="eval_ver">
                                    <a:actionLink id="make-versionable" value="#{msg.allow_versioning}"
                                                  action="#{DocumentDetailsBean.applyVersionable}"
                                                  rendered="#{DocumentDetailsBean.locked == false}" />
                                 </r:permissionEvaluator>
                              </a:panel>
                           </td>

                           <td valign="top">

                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "greyround", "#F5F5F5"); %>
                              <table cellpadding="1" cellspacing="1" border="0" width="100%">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton id="close-btn" value="#{msg.close}" action="dialog:close" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "greyround"); %>

                              <div style="padding:4px"></div>

                              <%-- Document Actions --%>
                              <a:panel label="#{msg.actions}" id="actions-panel" border="white" bgcolor="white" titleBorder="lbgrey" expandedTitleBorder="dotted" titleBgcolor="white" style="text-align:center"
                                    progressive="true" expanded='#{DocumentDetailsBean.panels["actions-panel"]}' expandedActionListener="#{DocumentDetailsBean.expandPanel}">
                                 <r:actions id="actions_doc" value="doc_details_actions" context="#{DocumentDetailsBean.document}" verticalSpacing="3" style="white-space:nowrap" />
                              </a:panel>
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