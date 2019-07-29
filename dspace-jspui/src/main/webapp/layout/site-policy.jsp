<%@ page contentType="text/html;charset=UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>


<!-- This modal shows the site policy if requested or not already accepted by the current user -->
<%
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    boolean sitePolicyAccepted = true;

	String sitePolicyURL = StringUtils.isNotEmpty(ConfigurationManager.getProperty("dspace.site.policy.url")) ? ConfigurationManager.getProperty("dspace.site.policy.url") : request.getContextPath() + "/sitePolicy";
	String sandboxOptions = StringUtils.isNotEmpty(ConfigurationManager.getProperty("dspace.site.policy.sandbox.options")) ? ConfigurationManager.getProperty("dspace.site.policy.sandbox.options") : "";

    if (user != null)
    {
		sitePolicyAccepted = request.getAttribute("site.policy.accepted") != null ? (boolean) request.getAttribute("site.policy.accepted") : true;
    }
%>

<!--
Automatically show site policy after login
if not already accepted and allow the current
user to accept the site policy.
-->
<% if (!sitePolicyAccepted) { %>
<script>
	var acceptPolicy = function() {
		var policyReadChecked = j('#policy-read-checkbox:checkbox:checked').length > 0;
		j.ajax({
            url : "<%= request.getContextPath() %>/sitePolicy",
            type: "POST",
            data: { "policyReadChecked": policyReadChecked },
            success: function(data) {
				return true;
            },
            error: function(err) {
            	alert("There was a problem saving your decision :/");
            	return false;
            }
        });
	};
	j(window).load(function() {
		j('#sitePolicyModal').modal('show');
	});
</script>
<% } %>

<!-- Modal -->
<div id="sitePolicyModal" class="modal fade" role="dialog" <% if (!sitePolicyAccepted) { %> data-keyboard="false" data-backdrop="static" <% } %> >
	<!-- Modal content-->
	<div class="modal-content">
		<div class="modal-header">
			<!-- If site policy has been already accepted by the current user, he is allowed to dismiss the modal  -->
			<% if (sitePolicyAccepted) { %>
				<button type="button" class="close" data-dismiss="modal">&times;</button>
			<% } %>
		  		<h4 style="color:black" class="modal-title"><fmt:message key="jsp.sitepolicy.header"/></h4>
		</div>
		<div class="modal-body">
			<iframe sandbox="<%= sandboxOptions %>" src="<%= sitePolicyURL %>" frameborder="0" width="100%" height="750"></iframe>
		</div>
		<div class="modal-footer">
			<!-- If site policy has not been accepted by the current user, he can accept or decline the policy -->
			<% if (!sitePolicyAccepted) { %>
				<input id="policy-read-checkbox" type="checkbox" name="accepted" value="accepted" onchange="j('#policy-accept-button').prop('disabled', function(i, v) { return !v; });"> <fmt:message key="jsp.sitepolicy.checkbox"/></input>	
				<input id="policy-accept-button" disabled type="button" onclick="return acceptPolicy();" data-dismiss="modal" class="btn btn-default" value="<fmt:message key='jsp.sitepolicy.accept'/>">
				<a id="policy-decline-button" href="<%= request.getContextPath() %>/logout" class="btn btn-danger"><fmt:message key="jsp.sitepolicy.decline"/></a>
			<% } %>
			<a id="open-policy-external" href="<%= request.getContextPath() %>/sitePolicy" target="_blank" class="btn btn-default pull-left"><fmt:message key="jsp.sitepolicy.openinnewtab"/></a>
		</div>
	</div>
</div>
<!--  -->
