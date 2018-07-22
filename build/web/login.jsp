<%-- 
    Document   : index
    Created on : May 24, 2016, 12:21:05 PM
    Author     : Muhammad Wannous
--%>

<%@page import="ccDocStrg.Defs"%>
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
            <%=(session.getAttribute(Defs.SESSION_MESSAGE_STRING) == null ? "" : 
                    session.getAttribute(Defs.SESSION_MESSAGE_STRING))%>
        </p>
        <hr>
        <br><br>
        <form method="post" action="validate">
            <table width="40%" border="0">
                <tr>
                    <td colspan="2" align="center"><b>Login</b><br></td>
                </tr>
                <tr>
                    <td align="right"><b>Username:</b></td>
                    <td align="left"><input type="text" id="userName" name="userName"></td>
                </tr>
                <tr>
                    <td align="right"><b>Password:</b></td>
                    <td align="left"><input type="password" id="passWord" name="passWord"></td>
                </tr>
                <tr><td></td><td></td></tr>
                <tr>
                <tr>
                    <td colspan="2" align="center"><br><input type="submit" value="Login"></td>
                </tr>
            </table>
        </form>
        <hr>
        <br>New user? Register <a href="register.jsp">here.</a>
    </body>
</html>
