<%-- 
    Document   : restore
    Created on : Jul 30, 2018, 1:56:05 PM
    Author     : mariusngaboyamahina
--%>
<%@page import="com.google.appengine.api.datastore.Query.FilterPredicate"%>
<%@page import="com.google.appengine.api.datastore.Query.FilterOperator"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="com.google.appengine.api.datastore.FetchOptions"%>
<%@page import="com.google.appengine.api.datastore.Query"%>
<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="com.google.appengine.api.datastore.DatastoreServiceFactory"%>
<%@page import="com.google.appengine.api.datastore.DatastoreService"%>
<%@page import="ccDocStrg.Defs"%>
<%@page import="ccDocStrg.User"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Cloud Storage</title>
    </head>
    <body>
        <h1>Cloud Storage</h1>
        <h2>A Cloud-based application for storing files.</h2>
        <p align="right">
        </p>
        <hr>
        <br><br>
        <table border="1">
            <%
                User currentUser = (User) session.getAttribute(Defs.SESSION_USER_STRING);
                if (currentUser != null) {
                    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
                    
                    //Jonathan: set a filter (condition) on the userName
                    Query.Filter propertyFilter = new FilterPredicate(Defs.ENTITY_PROPERTY_UPLOADER_STRING, FilterOperator.EQUAL, currentUser.getUserName());
                    Query fileQuery = new Query(Defs.DATASTORE_KIND_FILES_BACKUP_STRING).setFilter(propertyFilter);
                    List<Entity> files = datastore.prepare(fileQuery).asList(FetchOptions.Builder.withDefaults());
                    if (!files.isEmpty()) {
                        Iterator<Entity> allFiles = files.iterator();
                        //jonathan
                        Iterator<Entity> times = files.iterator();

            %>
            <tr>
                <td><b>File Name</b></td>
                <td><b>Deleted Time</b></td>
                <td><b>Function to Recover</b></td>
                <td><b>Function to Remove Permanently </b></td>
            </tr>
            <%  while (allFiles.hasNext()) {
                    String fileName = (String) allFiles.next().getProperty(Defs.ENTITY_PROPERTY_FILENAME_STRING);
                    String time = (String) times.next().getProperty(Defs.ENTITY_PROPERTY_DELETED_TIME_STRING);
            %>
            <tr>
                <td><%=fileName%></td>
                <td><%=time%></td>
                <td><a href='restore?fileName=<%=fileName%>'>Restore</a></td>
                <td><a href='clean?fileName=<%=fileName%>'>Clean</a></td>
            </tr>
            <%
                    }
                }
            %>
        </table>
        <br>
        <hr>
        <footer>
            <a href="list.jsp">Home</a> | 
            <%              } else {
                    session.setAttribute(Defs.SESSION_MESSAGE_STRING, "Please login firt!");
                    response.sendRedirect(Defs.LOGIN_PAGE_STRING);
                }
            %>
    </body>
</html>
