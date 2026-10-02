<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="tr" data-tema="aydinlik">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giriş Yap / Kayıt Ol | BOOMBOOK</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,wght@0,600;0,700;1,600;1,700&family=Public+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="assets/boombook.css" rel="stylesheet">
</head>
<body class="bb-body">

<!-- Sade header (tam header icin index.html) -->
<header class="bb-header">
    <div class="bb-container bb-header-top">
        <a href="index.html" class="bb-logo"><img src="assets/img/logo.png" alt="BoomBook logo" class="bb-logo-img"></a>
    </div>
</header>

<main class="bb-auth-wrap">
    <div class="bb-auth-card">

        <div class="bb-auth-icon">👤</div>
        <div class="bb-auth-heading">Hesabım</div>

        <div class="bb-auth-tabs">
            <button type="button" class="bb-auth-tab bb-active" data-hedef="girisForm" id="tabGiris">Giriş Yap</button>
            <button type="button" class="bb-auth-tab" data-hedef="kayitForm" id="tabKayit">Kayıt Ol</button>
        </div>

        <div id="ekAuthUyari" class="alert alert-danger d-none" role="alert"></div>

        <!-- GIRIS FORMU -->
        <form id="girisForm" class="bb-auth-form bb-active">
            <div class="mb-3">
                <label class="form-label">Email</label>
                <input type="email" class="form-control" id="girisEmail" required autocomplete="email">
            </div>
            <div class="mb-2">
                <label class="form-label">Şifre</label>
                <div class="bb-password-wrap">
                    <input type="password" class="form-control" id="girisSifre" required autocomplete="current-password">
                    <button type="button" class="bb-password-toggle" data-target="girisSifre" title="Şifreyi göster" aria-label="Şifreyi göster">👁</button>
                </div>
            </div>
            <div class="d-flex align-items-center justify-content-between mb-3">
                <label class="bb-auth-remember">
                    <input type="checkbox" id="girisBeniHatirla"> Beni Hatırla
                </label>
                <a href="#" id="sifremiUnuttumLink" class="bb-link-small" data-bs-toggle="modal" data-bs-target="#sifremiUnuttumModal">Şifremi unuttum</a>
            </div>
            <button type="submit" class="bb-btn bb-btn-red w-100" id="girisBtn">Giriş Yap</button>

            <div class="bb-auth-divider">veya</div>
            <div class="bb-auth-social">
                <button type="button" class="bb-social-btn" title="Facebook ile giriş" aria-label="Facebook ile giriş">📘</button>
                <button type="button" class="bb-social-btn" title="Google ile giriş" aria-label="Google ile giriş">🔴</button>
                <button type="button" class="bb-social-btn" title="Apple ile giriş" aria-label="Apple ile giriş">🍎</button>
            </div>
        </form>

        <!-- KAYIT FORMU -->
        <form id="kayitForm" class="bb-auth-form">
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
                <div class="bb-password-wrap">
                    <input type="password" class="form-control" id="kayitSifre" required minlength="6" autocomplete="new-password">
                    <button type="button" class="bb-password-toggle" data-target="kayitSifre" title="Şifreyi göster" aria-label="Şifreyi göster">👁</button>
                </div>
                <div class="form-text">En az 6 karakter.</div>
            </div>
            <div class="mb-3">
                <label class="form-label">Şifre (Tekrar)</label>
                <div class="bb-password-wrap">
                    <input type="password" class="form-control" id="kayitSifreTekrar" required minlength="6" autocomplete="new-password">
                    <button type="button" class="bb-password-toggle" data-target="kayitSifreTekrar" title="Şifreyi göster" aria-label="Şifreyi göster">👁</button>
                </div>
            </div>
            <label class="bb-auth-consent">
                <input type="checkbox" required>
                <span><a href="#" data-bs-toggle="modal" data-bs-target="#ticariIletiModal">Ticari Elektronik İleti Onayı</a>'nı okudum, onaylıyorum.</span>
            </label>
            <label class="bb-auth-consent">
                <input type="checkbox" required>
                <span><a href="#" data-bs-toggle="modal" data-bs-target="#kvkkModal">KVKK Metni</a>'ni okudum ve kabul ediyorum.</span>
            </label>
            <button type="submit" class="bb-btn bb-btn-red w-100 mt-2" id="kayitBtn">Kayıt Ol</button>
        </form>

    </div>
</main>

<!-- ================= TICARI ELEKTRONIK ILETI ONAYI MODAL ================= -->
<div class="modal fade" id="ticariIletiModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-scrollable modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Ticari Elektronik İleti Onayı</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Kapat"></button>
            </div>
            <div class="modal-body bb-legal-text">
                <p>İşbu metin, 6563 sayılı Elektronik Ticaretin Düzenlenmesi Hakkında Kanun ve ilgili mevzuat uyarınca, BOOMBOOK tarafından tarafınıza kampanya, duyuru, indirim ve yeni ürün bilgilendirmesi içeren ticari elektronik ileti gönderilebilmesi amacıyla açık rızanızın alınması için hazırlanmıştır.</p>
                <p>Onay vermeniz halinde e-posta, SMS ve/veya arama yoluyla tarafınıza pazarlama ve tanıtım amaçlı iletiler gönderilebilir. Bu onayı istediğiniz zaman, hiçbir gerekçe göstermeksizin ve ücretsiz olarak "Hesabım &gt; Bildirim Tercihleri" bölümünden veya bize ulaşarak geri çekebilirsiniz.</p>
                <p>Onayınızın geri çekilmesi, üyeliğinizin devamına veya BOOMBOOK üzerinden alışveriş yapmanıza herhangi bir şekilde engel teşkil etmez.</p>
                <p>Tarafınıza gönderilecek iletiler yalnızca BOOMBOOK'a ait ürün, kampanya ve hizmetlerle sınırlı olacak; iletişim bilgileriniz onayınız olmaksızın üçüncü taraflarla paylaşılmayacaktır.</p>
                <p>Kişisel verileriniz, 6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında işlenmekte olup detaylı bilgiye KVKK Metni üzerinden ulaşabilirsiniz.</p>
                <p class="text-muted small mb-0">Bu metin örnek/şablon içerik olarak hazırlanmıştır; yayına almadan önce hukuk danışmanınıza onaylatmanız önerilir.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="bb-btn bb-btn-navy" data-bs-dismiss="modal">Kapat</button>
            </div>
        </div>
    </div>
</div>

<!-- ================= KVKK METNI MODAL ================= -->
<div class="modal fade" id="kvkkModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-scrollable modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Kişisel Verilerin Korunması (KVKK) Aydınlatma Metni</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Kapat"></button>
            </div>
            <div class="modal-body bb-legal-text">
                <p><strong>1. Veri Sorumlusu</strong><br>BOOMBOOK olarak, 6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca veri sorumlusu sıfatıyla, kişisel verilerinizi aşağıda açıklanan kapsamda işlemekteyiz.</p>
                <p><strong>2. İşlenen Kişisel Veriler</strong><br>Ad-soyad, e-posta adresi, telefon numarası, teslimat/fatura adresi, sipariş ve ödeme bilgileri, site kullanım ve tercih verileri işlenmektedir.</p>
                <p><strong>3. İşleme Amaçları</strong><br>Üyelik işlemlerinin yürütülmesi, sipariş ve teslimat süreçlerinin gerçekleştirilmesi, müşteri destek hizmetlerinin sağlanması, yasal yükümlülüklerin yerine getirilmesi ve onay vermeniz halinde pazarlama iletişimi amaçlarıyla işlenmektedir.</p>
                <p><strong>4. Verilerin Aktarımı</strong><br>Kişisel verileriniz; kargo/lojistik iş ortaklarımız, ödeme kuruluşları ve yasal olarak yetkili kamu kurum/kuruluşları ile mevzuatın izin verdiği ölçüde paylaşılabilir.</p>
                <p><strong>5. Haklarınız</strong><br>KVKK'nın 11. maddesi uyarınca; verilerinizin işlenip işlenmediğini öğrenme, işlenmişse buna ilişkin bilgi talep etme, işlenme amacını öğrenme, yurt içinde/dışında aktarıldığı üçüncü kişileri bilme, eksik/yanlış işlenmişse düzeltilmesini isteme, silinmesini/yok edilmesini talep etme haklarına sahipsiniz.</p>
                <p><strong>6. Başvuru</strong><br>Haklarınızı kullanmak için destek@boombook.com adresi üzerinden bizimle iletişime geçebilirsiniz.</p>
                <p class="text-muted small mb-0">Bu metin örnek/şablon içerik olarak hazırlanmıştır; yayına almadan önce hukuk danışmanınıza onaylatmanız önerilir.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="bb-btn bb-btn-navy" data-bs-dismiss="modal">Kapat</button>
            </div>
        </div>
    </div>
</div>

<!-- ================= SIFREMI UNUTTUM MODAL ================= -->
<div class="modal fade" id="sifremiUnuttumModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Şifremi Unuttum</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Kapat"></button>
            </div>
            <form id="sifremiUnuttumForm">
                <div class="modal-body">
                    <p class="text-muted small">Kayıtlı e-posta adresinizi girin; hesabınız sistemde varsa şifre sıfırlama bağlantısı gönderilecektir.</p>
                    <div class="mb-2">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" id="sifremiUnuttumEmail" required autocomplete="email">
                    </div>
                    <div id="sifremiUnuttumMesaj" class="alert d-none small mb-0" role="alert"></div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Vazgeç</button>
                    <button type="submit" class="bb-btn bb-btn-navy" id="sifremiUnuttumBtn">Bağlantı Gönder</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="assets/common.js"></script>
<script src="assets/auth.js"></script>
</body>
</html>
