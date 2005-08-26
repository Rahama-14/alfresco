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

<%@ page buffer="32kb" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages" var="msg"/>
   <f:loadBundle basename="alfresco.version" var="version"/>
   
   <h:form id="system-information-form">
   
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
                              <h:graphicImage id="wizard-logo" url="/images/icons/file_large.gif" />
                           </td>
                           <td>
                              <div class="mainTitle">System Information</div>
                              <div class="mainSubTitle">Current User: <h:outputText value="#{NavigationBean.currentUser.userName}" /></div>
                              <div class="mainSubText">Version: <h:outputText value="#{version.major}.#{version.minor}.#{version.revision}" /></div>
                           </td>
                           <!--
                           <td bgcolor="#465F7D" width=1></td>
                           <td width="125" style="padding-left:2px">
                              <%-- Current object actions --%>
                              <h:outputText style="padding-left:20px" styleClass="mainSubTitle" value="#{msg.actions}" /><br>
                              <a:actionLink value="#{msg.reset_config}" actionListener="#{AdminBean.resetConfigService}" 
                                            image="/images/icons/View_details.gif" padding="4" />
                           </td>
                           -->
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
                                    <td colspan="2">
                                       <a:panel label="HTTP Application State" id="http-application-state" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:httpApplicationState id="has" />
                                       </a:panel>
                                       <br/>
                                       <a:panel label="HTTP Session State" id="http-session-state" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:httpSessionState id="hss" />
                                       </a:panel>
                                       <br/>
                                       <a:panel label="HTTP Request State" id="http-request-state" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:httpRequestState id="hrs" />
                                       </a:panel>
                                       <br/>
                                       <a:panel label="HTTP Request Parameters" id="http-request-params" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:httpRequestParams id="hrp" />
                                       </a:panel>
                                       <br/>
                                       <a:panel label="HTTP Request Headers" id="http-request-headers" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:httpRequestHeaders id="hrh" />
                                       </a:panel>
                                       <br/>
                                       <a:panel label="System Properties" id="system-props" border="white" bgcolor="white" 
                                                titleBorder="blue" titleBgcolor="#D3E6FE" progressive="true" styleClass="mainSubTitle"
                                                expanded="false">
                                       	<a:systemProperties id="sp" />
                                       </a:panel>
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
                                       <h:commandButton value="Close" action="browse" styleClass="wizardButton" />
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
               
               <%-- Error Messages --%>
               <tr valign="top">
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width="4"></td>
                  <td>
                     <%-- messages tag to show messages not handled by other specific message tags --%>
                     <h:messages globalOnly="true" styleClass="errorMessage" layout="table" />
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