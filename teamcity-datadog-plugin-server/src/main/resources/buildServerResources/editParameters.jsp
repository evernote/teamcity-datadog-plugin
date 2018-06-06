<%--suppress XmlPathReference --%>
<%@ page
    import="static com.evernote.teamcity.datadog.DataDogBuildFeatureParameters.DATADOG_AGENT_ADDRESS_AND_PORT" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<tr>
  <th>DataDog Agent<l:star/></th>
  <td>
    <props:textProperty name="<%=DATADOG_AGENT_ADDRESS_AND_PORT%>"
                        className="longField"/>
    <span class="smallNote">Specify DataDog Agent hostname and port</span>
  </td>
</tr>
