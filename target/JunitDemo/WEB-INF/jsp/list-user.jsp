<%--
  Created by IntelliJ IDEA.
  User: Ashish.Modh
  Date: 7/12/2022
  Time: 7:40 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <title> Users List </title>
</head>
<body>
<h1><U>User List</U></h1>
<table border="2" width="70%" cellpadding="2">
    <tr>
        <th><b>ID:</b></th>
        <th>Name:</th>
        <th>Gender:</th>
        <th>Address:</th>
        <th>Edit :</th>
        <th>Delete :</th>
    </tr>
    <c:forEach items="${list}" var="user">
        <tr>
            <td>${user.id}</td>
            <td>${user.name}</td>
            <td>${user.gender}</td>
            <td>${user.address}</td>
            <td><a href="editUser/${user.id}">Edit</a></td>
            <td><a href="deleteuser/${user.id}">Delete</a></td>
        </tr>
    </c:forEach>
</table>

</body>
</html>
