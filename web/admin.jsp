<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="tr" data-tema="aydinlik">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Paneli | BOOMBOOK</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="assets/boombook.css" rel="stylesheet">
</head>
<body class="bb-body">

<!-- ================= SADE UST BAR (tam magaza header'i icin index.html) ================= -->
<header class="bb-header">
    <div class="bb-container bb-header-top">
        <a href="index.html" class="bb-logo"><img src="assets/img/logo.png" alt="BoomBook logo" class="bb-logo-img"> <span class="badge text-bg-secondary" style="font-size:0.6rem;vertical-align:middle">ADMIN</span></a>
        <div class="flex-grow-1"></div>
        <div class="bb-header-actions">
            <button type="button" class="bb-icon-btn" id="bbTemaBtn" title="Karanlık Mod">🌙</button>
            <a href="index.html" class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink)">← Siteye Dön</a>
            <div class="dropdown">
                <button type="button" class="bb-icon-btn" data-bs-toggle="dropdown" aria-expanded="false" title="Hesabım">👤</button>
                <ul class="dropdown-menu dropdown-menu-end" id="ekProfilMenu" style="min-width:220px">
                    <li><span class="dropdown-item-text text-muted small">Yükleniyor...</span></li>
                </ul>
            </div>
        </div>
    </div>
</header>

<div class="bb-admin-layout">
    <!-- ================= SOL SIDEBAR ================= -->
    <aside class="bb-admin-sidebar">
        <div class="bb-admin-nav-item bb-active" data-tab="dashboard">📊 Dashboard</div>
        <div class="bb-admin-nav-item" data-tab="duyurular">📢 Duyurular</div>
        <div class="bb-admin-nav-item" data-tab="kitaplar">📚 Kitaplar</div>
        <div class="bb-admin-nav-item" data-tab="yazarlar">✍️ Yazarlar</div>
        <div class="bb-admin-nav-item" data-tab="kategoriler">🗂️ Kategoriler</div>
        <div class="bb-admin-nav-item" data-tab="yayinevleri">🏢 Yayınevleri</div>
        <div class="bb-admin-nav-item" data-tab="siparisler">📦 Siparişler</div>
        <div class="bb-admin-nav-item" data-tab="kullanicilar">👥 Kullanıcılar</div>
        <div class="bb-admin-nav-item" data-tab="araclar">⚙️ Stok &amp; İndirim Araçları</div>
    </aside>

    <!-- ================= ICERIK ================= -->
    <main class="bb-admin-content">

        <!-- ========== DASHBOARD ========== -->
        <section class="bb-admin-tab bb-active" id="tab-dashboard">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Dashboard</h2>
                <div class="d-flex align-items-center gap-2">
                    <span class="bb-live-dot" title="Bu sekme açıkken veriler otomatik yenilenir"></span>
                    <span class="text-muted small" id="bbDashGuncellendi">—</span>
                    <button class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink);padding:5px 14px;font-size:0.78rem" id="bbDashYenileBtn">Yenile</button>
                </div>
            </div>

            <div class="bb-stat-grid" id="bbIstatKartlar">
                <div class="bb-stat-card bb-stat-card-satis"><div class="bb-stat-icon">💰</div><div><div class="bb-stat-label">Toplam Satış</div><div class="bb-stat-value">—</div></div></div>
                <div class="bb-stat-card bb-stat-card-siparis"><div class="bb-stat-icon">📦</div><div><div class="bb-stat-label">Tamamlanan Sipariş</div><div class="bb-stat-value">—</div></div></div>
                <div class="bb-stat-card bb-stat-card-kullanici"><div class="bb-stat-icon">👥</div><div><div class="bb-stat-label">Toplam Kullanıcı</div><div class="bb-stat-value">—</div></div></div>
                <div class="bb-stat-card bb-stat-card-kitap"><div class="bb-stat-icon">📚</div><div><div class="bb-stat-label">Toplam Kitap</div><div class="bb-stat-value">—</div></div></div>
            </div>

            <div class="row g-3">
                <div class="col-lg-6">
                    <div class="bb-admin-card">
                        <div class="bb-admin-card-header">En Çok Satan 5 Kitap</div>
                        <div class="bb-admin-card-body">
                            <table class="bb-admin-table">
                                <thead><tr><th>Kitap</th><th>Satış Adedi</th></tr></thead>
                                <tbody id="bbEnCokSatanGovde"><tr><td colspan="2" class="text-muted">Yükleniyor...</td></tr></tbody>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="col-lg-6">
                    <div class="bb-admin-card">
                        <div class="bb-admin-card-header">Aylık Gelir (Son 12 Ay)</div>
                        <div class="bb-admin-card-body">
                            <canvas id="bbGelirGrafik" height="220"></canvas>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- ========== DUYURULAR (Hero Slider + Kampanya Kartlari Yonetimi) ========== -->
        <section class="bb-admin-tab" id="tab-duyurular">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Duyurular &amp; Kampanya Kartları</h2>
                <button class="bb-btn bb-btn-navy" id="bbDuyuruEkleBtn">+ Yeni Duyuru</button>
            </div>
            <p class="text-muted small">
                <strong>Hero Slider</strong>: ana sayfanın en üstündeki büyük banner. <strong>Kampanya Kartı</strong>: ana sayfada "SEPETTE %50" gibi görünen küçük kutular.
                İkisi de "Aktif" işaretliyken <strong>Sıra</strong> değerine göre gösterilir.
            </p>
            <div class="bb-pill-filter mb-3" id="bbDuyuruFiltre">
                <button class="bb-pill bb-active" data-filtre="hepsi">Tümü</button>
                <button class="bb-pill" data-filtre="slider">🖼️ Hero Slider</button>
                <button class="bb-pill" data-filtre="kart">🏷️ Kampanya Kartları</button>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <table class="bb-admin-table">
                        <thead><tr><th>Resim</th><th>Tür</th><th>Sıra</th><th>Başlık</th><th>Buton</th><th>Durum</th><th></th></tr></thead>
                        <tbody id="bbDuyuruGovde"><tr><td colspan="7" class="text-muted">Yükleniyor...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </section>

        <!-- ========== KITAPLAR ========== -->
        <section class="bb-admin-tab" id="tab-kitaplar">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Kitaplar</h2>
                <div class="d-flex gap-2 flex-wrap">
                    <input type="text" class="bb-filtre-ara-input" style="width:200px" id="bbKitapAra" placeholder="Kitap ara...">
                    <select class="form-select form-select-sm" style="width:170px" id="bbKitapKategoriFiltre">
                        <option value="">Tüm Kategoriler</option>
                    </select>
                    <select class="form-select form-select-sm" style="width:170px" id="bbKitapSiralaSelect">
                        <option value="enYeni">En Yeni</option>
                        <option value="stokAzalan">Stok (Azdan Çoğa)</option>
                        <option value="fiyatArtan">Fiyat (Artan)</option>
                        <option value="fiyatAzalan">Fiyat (Azalan)</option>
                        <option value="cokSatan">Çok Satanlar</option>
                        <option value="puanAzalan">Puana Göre</option>
                    </select>
                    <button class="bb-btn bb-btn-navy" id="bbKitapEkleBtn">+ Yeni Kitap</button>
                </div>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <div class="table-responsive">
                        <table class="bb-admin-table">
                            <thead><tr><th></th><th>Kitap Adı</th><th>Yazar</th><th>Yayınevi</th><th>Fiyat</th><th>Stok</th><th></th></tr></thead>
                            <tbody id="bbKitapGovde"><tr><td colspan="7" class="text-muted">Yükleniyor...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="bb-sayfalama" id="bbKitapSayfalama"></div>
        </section>

        <!-- ========== YAZARLAR ========== -->
        <section class="bb-admin-tab" id="tab-yazarlar">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Yazarlar</h2>
                <button class="bb-btn bb-btn-navy" id="bbYazarEkleBtn">+ Yeni Yazar</button>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <table class="bb-admin-table">
                        <thead><tr><th>Yazar Adı</th><th>Biyografi</th><th></th></tr></thead>
                        <tbody id="bbYazarGovde"><tr><td colspan="3" class="text-muted">Yükleniyor...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </section>

        <!-- ========== KATEGORILER ========== -->
        <section class="bb-admin-tab" id="tab-kategoriler">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Kategoriler</h2>
                <button class="bb-btn bb-btn-navy" id="bbKategoriEkleBtn">+ Yeni Kategori</button>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <table class="bb-admin-table">
                        <thead><tr><th>Kategori Adı</th><th></th></tr></thead>
                        <tbody id="bbKategoriGovde"><tr><td colspan="2" class="text-muted">Yükleniyor...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </section>

        <!-- ========== YAYINEVLERI ========== -->
        <section class="bb-admin-tab" id="tab-yayinevleri">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Yayınevleri</h2>
                <button class="bb-btn bb-btn-navy" id="bbYayineviEkleBtn">+ Yeni Yayınevi</button>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <table class="bb-admin-table">
                        <thead><tr><th>Yayınevi Adı</th><th>Son İndirim Oranı</th><th></th></tr></thead>
                        <tbody id="bbYayineviGovde"><tr><td colspan="3" class="text-muted">Yükleniyor...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </section>

        <!-- ========== SIPARISLER ========== -->
        <section class="bb-admin-tab" id="tab-siparisler">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Siparişler</h2>
                <div class="d-flex align-items-center gap-2">
                    <span class="bb-live-dot" title="Bu sekme açıkken veriler otomatik yenilenir"></span>
                    <span class="text-muted small" id="bbSiparisGuncellendi">—</span>
                    <button class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink);padding:5px 14px;font-size:0.78rem" id="bbSiparisYenileBtn">Yenile</button>
                </div>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <div class="table-responsive">
                        <table class="bb-admin-table">
                            <thead><tr><th>Sipariş No</th><th>Müşteri</th><th>Tarih</th><th>Toplam</th><th>Durum</th></tr></thead>
                            <tbody id="bbSiparisGovde"><tr><td colspan="5" class="text-muted">Yükleniyor...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

        <!-- ========== KULLANICILAR ========== -->
        <section class="bb-admin-tab" id="tab-kullanicilar">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Kullanıcılar</h2>
                <div class="d-flex gap-2 flex-wrap">
                    <input type="text" class="bb-filtre-ara-input" style="width:220px" id="bbKullaniciAra" placeholder="İsim veya e-posta ara...">
                    <select class="form-select form-select-sm" style="width:170px" id="bbKullaniciSiralaSelect">
                        <option value="yeni">Kayıt (Yeniden Eskiye)</option>
                        <option value="eski">Kayıt (Eskiden Yeniye)</option>
                        <option value="adAsc">İsme Göre (A-Z)</option>
                    </select>
                </div>
            </div>
            <div class="bb-admin-card">
                <div class="bb-admin-card-body p-0">
                    <div class="table-responsive">
                        <table class="bb-admin-table">
                            <thead><tr><th>Ad Soyad</th><th>Email</th><th>Rol</th><th>Durum</th><th>Kayıt Tarihi</th><th></th></tr></thead>
                            <tbody id="bbKullaniciGovde"><tr><td colspan="6" class="text-muted">Yükleniyor...</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="bb-sayfalama" id="bbKullaniciSayfalama"></div>
        </section>

        <!-- ========== STOK & INDIRIM ARACLARI (mevcut ozellik, tasima) ========== -->
        <section class="bb-admin-tab" id="tab-araclar">
            <div class="bb-admin-tab-header">
                <h2 class="bb-admin-tab-title">Stok &amp; İndirim Araçları</h2>
            </div>

            <div class="row g-4 mb-4">
                <div class="col-lg-6">
                    <div class="bb-admin-card h-100">
                        <div class="bb-admin-card-header">Yayınevine Göre Toplu İndirim</div>
                        <div class="bb-admin-card-body">
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
                                    <button type="submit" class="bb-btn bb-btn-navy w-100">İndirimi Uygula</button>
                                </div>
                            </form>
                            <div id="ekIndirimSonuc" class="mt-3"></div>
                        </div>
                    </div>
                </div>

                <div class="col-lg-6">
                    <div class="bb-admin-card h-100">
                        <div class="bb-admin-card-header">Otomatik Kritik Stok İndirimi</div>
                        <div class="bb-admin-card-body">
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
                                    <button type="submit" class="bb-btn bb-btn-navy w-100">Kritik Stok Kuralını Uygula</button>
                                </div>
                            </form>
                            <div id="ekKritikSonuc" class="mt-3"></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="bb-admin-card">
                <div class="bb-admin-card-header">
                    <span>Stok Takip Paneli</span>
                    <button class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink);padding:5px 14px;font-size:0.78rem" id="ekStokYenileBtn">Yenile</button>
                </div>
                <div class="bb-admin-card-body">
                    <div id="ekStokYukleniyor" class="text-center py-4">
                        <div class="spinner-border" style="color:var(--bb-navy)" role="status"></div>
                    </div>
                    <div class="table-responsive d-none" id="ekStokTabloWrapper">
                        <table class="bb-admin-table">
                            <thead>
                                <tr><th>Kitap</th><th>Yayınevi</th><th>Fiyat</th><th>İndirimli Fiyat</th><th>Stok Miktarı</th></tr>
                            </thead>
                            <tbody id="ekStokGovde"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>

    </main>
</div>

<!-- ================= MODAL: KITAP EKLE/DUZENLE ================= -->
<div class="modal fade" id="bbKitapModal" tabindex="-1">
    <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="bbKitapModalBaslik">Yeni Kitap</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <form id="bbKitapForm">
                    <input type="hidden" id="bbKitapId">
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label">Kitap Adı *</label>
                            <input type="text" class="form-control" id="bbKitapAdi" required>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Yazar</label>
                            <select class="form-select" id="bbKitapYazar"><option value="">— Seçiniz —</option></select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Kategori</label>
                            <select class="form-select" id="bbKitapKategori"><option value="">— Seçiniz —</option></select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Yayınevi</label>
                            <select class="form-select" id="bbKitapYayinevi"><option value="">— Seçiniz —</option></select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Fiyat (TL) *</label>
                            <input type="number" class="form-control" id="bbKitapFiyat" min="0" step="0.01" required>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">İndirimli Fiyat</label>
                            <input type="number" class="form-control" id="bbKitapIndirimliFiyat" min="0" step="0.01">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Stok Miktarı *</label>
                            <input type="number" class="form-control" id="bbKitapStok" min="0" required>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Sayfa Sayısı</label>
                            <input type="number" class="form-control" id="bbKitapSayfaSayisi" min="0">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Dosya Formatı</label>
                            <select class="form-select" id="bbKitapDosyaFormati">
                                <option value="PDF">PDF</option>
                                <option value="EPUB">EPUB</option>
                                <option value="MOBI">MOBI</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Yayın Tarihi</label>
                            <input type="date" class="form-control" id="bbKitapYayinTarihi">
                        </div>
                        <div class="col-12">
                            <label class="form-label">Açıklama</label>
                            <textarea class="form-control" id="bbKitapAciklama" rows="3"></textarea>
                        </div>
                    </div>
                </form>

                <hr class="my-3">

                <h6>Kapak Resmi</h6>
                <div class="d-flex align-items-center gap-3 mb-3">
                    <img id="bbKitapKapakOnizleme" src="" alt="" style="width:60px;height:90px;object-fit:cover;border-radius:6px;background:var(--bb-plum);display:none">
                    <input type="file" class="form-control" id="bbKitapKapakInput" accept=".jpg,.jpeg,.png,.webp">
                </div>
                <h6>E-Kitap Dosyası (PDF / EPUB / MOBI)</h6>
                <div class="d-flex align-items-center gap-3">
                    <input type="file" class="form-control" id="bbKitapDosyaInput" accept=".pdf,.epub,.mobi">
                </div>
                <div class="form-text mt-1">Dosyaları seçip aşağıdan <strong>Kaydet</strong>'e basmanız yeterli — ayrıca yüklemenize gerek yok.</div>
                <div id="bbKitapYuklemeSonuc" class="mt-2 small"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Vazgeç</button>
                <button type="button" class="bb-btn bb-btn-navy" id="bbKitapKaydetBtn">Kaydet</button>
            </div>
        </div>
    </div>
</div>

<!-- ================= MODAL: DUYURU EKLE/DUZENLE ================= -->
<div class="modal fade" id="bbDuyuruModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="bbDuyuruModalBaslik">Yeni Duyuru</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="bbDuyuruId">
                <div class="mb-3">
                    <label class="form-label">Tür *</label>
                    <select class="form-select" id="bbDuyuruTur">
                        <option value="slider">🖼️ Hero Slider (ana sayfa büyük banner)</option>
                        <option value="kart">🏷️ Kampanya Kartı (küçük kutu, örn. "Sepette %50")</option>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label">Başlık *</label>
                    <input type="text" class="form-control" id="bbDuyuruBaslik" required maxlength="150">
                </div>
                <div class="mb-3">
                    <label class="form-label">Açıklama</label>
                    <input type="text" class="form-control" id="bbDuyuruAciklama" maxlength="300">
                </div>
                <div class="row g-3 mb-3">
                    <div class="col-6">
                        <label class="form-label">Buton Metni</label>
                        <input type="text" class="form-control" id="bbDuyuruButonMetni" value="Keşfet" maxlength="50">
                    </div>
                    <div class="col-6">
                        <label class="form-label">Buton Linki</label>
                        <input type="text" class="form-control" id="bbDuyuruButonLink" value="katalog.html">
                    </div>
                </div>

                <hr class="my-3">
                <h6>Kampanya / Banner Resmi <span class="text-danger small fw-normal">*</span></h6>
                <div class="d-flex align-items-center gap-3 mb-1">
                    <img id="bbDuyuruResimOnizleme" src="" alt="" style="width:110px;height:60px;object-fit:cover;border-radius:8px;background:var(--bb-plum);display:none">
                    <input type="file" class="form-control" id="bbDuyuruResimInput" accept=".jpg,.jpeg,.png,.webp">
                </div>
                <div class="form-text mb-1">Resmi seçip aşağıdan <strong>Kaydet</strong>'e basmanız yeterli — ayrıca yüklemenize gerek yok.</div>
                <div id="bbDuyuruResimSonuc" class="small"></div>

                <div class="row g-3 mt-1">
                    <div class="col-6">
                        <label class="form-label">Sıra</label>
                        <input type="number" class="form-control" id="bbDuyuruSira" value="0" min="0">
                    </div>
                    <div class="col-6 d-flex align-items-end">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="bbDuyuruAktif" checked>
                            <label class="form-check-label" for="bbDuyuruAktif">Aktif (ana sayfada göster)</label>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Vazgeç</button>
                <button type="button" class="bb-btn bb-btn-navy" id="bbDuyuruKaydetBtn">Kaydet</button>
            </div>
        </div>
    </div>
</div>

<!-- ================= MODAL: YAZAR / KATEGORI / YAYINEVI EKLE-DUZENLE (ortak, basit) ================= -->
<div class="modal fade" id="bbBasitModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="bbBasitModalBaslik">Ekle</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="bbBasitId">
                <input type="hidden" id="bbBasitTur">
                <div class="mb-3">
                    <label class="form-label" id="bbBasitAdLabel">Ad</label>
                    <input type="text" class="form-control" id="bbBasitAd" required>
                </div>
                <div class="mb-3 d-none" id="bbBasitBiyografiWrap">
                    <label class="form-label">Biyografi</label>
                    <textarea class="form-control" id="bbBasitBiyografi" rows="3"></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Vazgeç</button>
                <button type="button" class="bb-btn bb-btn-navy" id="bbBasitKaydetBtn">Kaydet</button>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
<script src="assets/common.js"></script>
<script src="assets/boombook-header.js"></script>
<script src="assets/admin.js"></script>
</body>
</html>
