<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Giriş Yap</title>
</head>
<body>
    <h2>Kullanıcı Girişi</h2>

    <%-- Hatalı giriş uyarı mesajı --%>
    <%
        String hata = (String) request.getAttribute("hataMesaji");
        if (hata != null) {
    %>
        <p style="color: red; font-weight: bold;"><%= hata %></p>
    <%
        }
    %>

    <form action="LoginServlet" method="POST">
        <p>Email: <br><input type="email" name="email" required></p>
        <p>Şifre: <br><input type="password" name="sifre" required></p>
        <button type="submit">Giriş Yap</button>
    </form>
</body>
</html>