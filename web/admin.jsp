<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Paneli | E-Kitap</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="assets/style.css" rel="stylesheet">
</head>
<body>

<nav class="navbar navbar-expand-lg ek-navbar sticky-top">
    <div class="container">
        <a class="navbar-brand ek-brand" href="index.html">📚 E-Kitap</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#ekNavContent">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="ekNavContent">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item"><a class="nav-link" href="index.html">Ana Sayfa</a></li>
                <li class="nav-item"><a class="nav-link" href="siparislerim.html">Siparişlerim</a></li>
                <li class="nav-item"><a class="nav-link active" href="admin.jsp">Admin Paneli</a></li>
            </ul>
        </div>
    </div>
</nav>

<header class="ek-hero">
    <div class="container">
        <h1>Admin Paneli</h1>
        <p>Stok durumunu izleyin, yayınevi bazlı toplu indirim uygulayın ve kritik stok kuralını çalıştırın.</p>
    </div>
</header>

<main class="container my-4">

    <div class="row g-4 mb-4">
        <!-- Yayinevine gore toplu indirim -->
        <div class="col-lg-6">
            <div class="card ek-admin-card h-100">
                <div class="card-header">Yayınevine Göre Toplu İndirim</div>
                <div class="card-body">
                    <p class="text-muted small">Seçilen yayınevine ait tüm kitapların indirimli fiyatını yeniden hesaplar.</p>
                    <form id="ekIndirimForm" class="row g-3">
                        <div class="col-12">
                            <label class="form-label">Yayınevi</label>
                            <select class="form-select" id="ekIndirimYayinevi" required>
                                <option value="">Yükleniyor...</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label">İndirim Oranı (%)</label>
                            <input type="number" class="form-control" id="ekIndirimOran" min="0" max="100" step="1" value="15" required>
                        </div>
                        <div class="col-12">
                            <button type="submit" class="btn btn-ek-gold w-100">İndirimi Uygula</button>
                        </div>
                    </form>
                    <div id="ekIndirimSonuc" class="mt-3"></div>
                </div>
            </div>
        </div>

        <!-- Kritik stok kurali -->
        <div class="col-lg-6">
            <div class="card ek-admin-card h-100">
                <div class="card-header">Otomatik Kritik Stok İndirimi</div>
                <div class="card-body">
                    <p class="text-muted small">Stoğu belirlenen eşiğin altına düşen ve henüz indirimi olmayan kitaplara otomatik indirim uygular.</p>
                    <form id="ekKritikForm" class="row g-3">
                        <div class="col-6">
                            <label class="form-label">Kritik Stok Eşiği</label>
                            <input type="number" class="form-control" id="ekKritikEsik" min="1" value="5" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label">İndirim Oranı (%)</label>
                            <input type="number" class="form-control" id="ekKritikOran" min="1" max="100" value="15" required>
                        </div>
                        <div class="col-12">
                            <button type="submit" class="btn btn-ek-gold w-100">Kritik Stok Kuralını Uygula</button>
                        </div>
                    </form>
                    <div id="ekKritikSonuc" class="mt-3"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Stok takip tablosu -->
    <div class="card ek-admin-card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span>Stok Takip Paneli</span>
            <button class="btn btn-sm btn-outline-light" id="ekStokYenileBtn">Yenile</button>
        </div>
        <div class="card-body">
            <div id="ekStokYukleniyor" class="text-center py-4">
                <div class="spinner-border" role="status"></div>
            </div>
            <div class="table-responsive d-none" id="ekStokTabloWrapper">
                <table class="table table-hover align-middle ek-table">
                    <thead>
                        <tr>
                            <th>Kitap</th>
                            <th>Yayınevi</th>
                            <th>Fiyat</th>
                            <th>İndirimli Fiyat</th>
                            <th>Stok Miktarı</th>
                        </tr>
                    </thead>
                    <tbody id="ekStokGovde"></tbody>
                </table>
            </div>
        </div>
    </div>

</main>

<footer class="ek-footer text-center">
    <div class="container">
        <small>&copy; 2026 E-Kitap Dijital Kitapevi &middot; Admin Paneli</small>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="assets/common.js"></script>
<script src="assets/admin.js"></script>
</body>
</html>
