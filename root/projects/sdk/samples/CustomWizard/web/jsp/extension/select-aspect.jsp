<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
            
<h:outputText value="#{msg.aspect}: " />
<h:selectOneMenu value="#{WizardManager.bean.aspect}">
   <f:selectItems value="#{WizardManager.bean.aspects}" />
</h:selectOneMenu>