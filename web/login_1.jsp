<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giriş Yap / Kayıt Ol | E-Kitap</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="assets/style.css" rel="stylesheet">
</head>
<body>

<!-- NAVBAR (basit; tam navbar index.html'de) -->
<nav class="navbar navbar-expand-lg ek-navbar sticky-top">
    <div class="container">
        <a class="navbar-brand ek-brand" href="index.html">📚 E-Kitap</a>
    </div>
</nav>

<main class="container">
    <div class="ek-auth-wrap">
        <div class="ek-auth-card">

            <div class="ek-auth-tabs">
                <button type="button" class="ek-auth-tab active" data-hedef="girisForm" id="tabGiris">Giriş Yap</button>
                <button type="button" class="ek-auth-tab" data-hedef="kayitForm" id="tabKayit">Kayıt Ol</button>
            </div>

            <div id="ekAuthUyari" class="alert alert-danger d-none" role="alert"></div>

            <!-- GIRIS FORMU -->
            <form id="girisForm" class="ek-auth-form active">
                <div class="mb-3">
                    <label class="form-label">Email</label>
                    <input type="email" class="form-control" id="girisEmail" required autocomplete="email">
                </div>
                <div class="mb-3">
                    <label class="form-label">Şifre</label>
                    <input type="password" class="form-control" id="girisSifre" required autocomplete="current-password">
                </div>
                <button type="submit" class="btn btn-ek-gold w-100" id="girisBtn">Giriş Yap</button>
            </form>

            <!-- KAYIT FORMU -->
            <form id="kayitForm" class="ek-auth-form">
                <div class="mb-3">
                    <label class="form-label">Ad Soyad</label>
                    <input type="text" class="form-control" id="kayitAdSoyad" required autocomplete="name">
                </div>
                <div class="mb-3">
                    <label class="form-label">Email</label>
                    <input type="email" class="form-control" id="kayitEmail" required autocomplete="email">
                </div>
                <div class="mb-3">
                    <label class="form-label">Şifre</label>
                    <input type="password" class="form-control" id="kayitSifre" required minlength="6" autocomplete="new-password">
                    <div class="form-text">En az 6 karakter.</div>
                </div>
                <div class="mb-3">
                    <label class="form-label">Şifre (Tekrar)</label>
                    <input type="password" class="form-control" id="kayitSifreTekrar" required minlength="6" autocomplete="new-password">
                </div>
                <button type="submit" class="btn btn-ek-gold w-100" id="kayitBtn">Kayıt Ol</button>
            </form>

        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="assets/auth.js"></script>
</body>
</html>
