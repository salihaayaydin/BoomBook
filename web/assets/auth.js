
(() => {
    const uyariKutusu = document.getElementById('ekAuthUyari');
    const tabGiris = document.getElementById('tabGiris');
    const tabKayit = document.getElementById('tabKayit');
    const girisForm = document.getElementById('girisForm');
    const kayitForm = document.getElementById('kayitForm');

    function sekmeAc(hedefId) {
        [girisForm, kayitForm].forEach(f => f.classList.remove('bb-active'));
        [tabGiris, tabKayit].forEach(t => t.classList.remove('bb-active'));
        document.getElementById(hedefId).classList.add('bb-active');
        document.getElementById(hedefId === 'girisForm' ? 'tabGiris' : 'tabKayit').classList.add('bb-active');
        uyariGizle();
    }

    tabGiris.addEventListener('click', () => sekmeAc('girisForm'));
    tabKayit.addEventListener('click', () => sekmeAc('kayitForm'));

    const ilkParams = new URLSearchParams(window.location.search);
    if (ilkParams.get('kayit') === '1') {
        sekmeAc('kayitForm');
    }

    const params = new URLSearchParams(window.location.search);
    const sonraki = params.get('sonraki');
    if (sonraki) {
        uyariGoster('Devam etmek icin once giris yapmalisiniz.', 'alert-warning');
    }
    
    function uyariGoster(mesaj, sinif = 'alert-danger') {
        uyariKutusu.textContent = mesaj;
        uyariKutusu.className = 'alert ' + sinif;
        uyariKutusu.classList.remove('d-none');
    }

    function uyariGizle() {
        uyariKutusu.classList.add('d-none');
    }

    function yonlendir() {
        window.location.href = sonraki ? sonraki : 'index.html';
    }

    // Zaten giris yapilmissa (orn. yeni sekmede login.jsp'yi tekrar actiysa)
    // formu gostermeden dogrudan yonlendir.
    (async () => {
        try {
            const res = await fetch('api/auth/ben');
            if (res.ok) yonlendir();
        } catch (err) {
            // sunucuya erisilemedi, formu normal sekilde goster
        }
    })();

    function butonYukleniyor(btn, yukleniyorMu) {
        btn.disabled = yukleniyorMu;
        btn.dataset.orijinal = btn.dataset.orijinal || btn.textContent;
        btn.textContent = yukleniyorMu ? 'Lütfen bekleyin...' : btn.dataset.orijinal;
    }

    /* ---------------- SIFRE GOSTER / GIZLE ---------------- */
    document.querySelectorAll('.bb-password-toggle').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;
            const gosteriliyorMu = input.type === 'text';
            input.type = gosteriliyorMu ? 'password' : 'text';
            const yeniEtiket = gosteriliyorMu ? 'Şifreyi göster' : 'Şifreyi gizle';
            btn.textContent = gosteriliyorMu ? '👁' : '🙈';
            btn.title = yeniEtiket;
            btn.setAttribute('aria-label', yeniEtiket);
        });
    });

    /* ---------------- SIFREMI UNUTTUM ----------------
       NOT: Bu formu gonderdiginde 'api/auth/sifremi-unuttum' adinda bir
       uc nokta cagrilir. Bu proje icinde sadece frontend/JSP dosyalari
       bulundugundan (Java kaynak kodu yok) bu uc nokta suanda backend'de
       MEVCUT DEGIL - eklenmesi gerekiyor. UI bilerek "kayitliysa gonderildi"
       tarzi notr bir mesaj gosterir (e-postanin sistemde olup olmadigini
       disariya sizdirmamak icin), backend eklendiginde ekstra degisiklik
       gerekmeden calisacak sekilde yazildi. */
    const sifremiUnuttumForm = document.getElementById('sifremiUnuttumForm');
    if (sifremiUnuttumForm) {
        sifremiUnuttumForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const mesajKutusu = document.getElementById('sifremiUnuttumMesaj');
            const btn = document.getElementById('sifremiUnuttumBtn');
            const email = document.getElementById('sifremiUnuttumEmail').value.trim();
            btn.disabled = true;
            btn.textContent = 'Gönderiliyor...';
            try {
                await fetch('api/auth/sifremi-unuttum', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email })
                });
            } catch (err) {
                // Ag/backend hatasi olsa bile asagida ayni notr mesaj gosterilir.
            } finally {
                mesajKutusu.className = 'alert alert-info small mb-0';
                mesajKutusu.textContent = 'Bu e-posta adresi sistemde kayıtlıysa, şifre sıfırlama bağlantısı gönderildi.';
                mesajKutusu.classList.remove('d-none');
                btn.disabled = false;
                btn.textContent = 'Bağlantı Gönder';
            }
        });
    }

    girisForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        uyariGizle();
        const btn = document.getElementById('girisBtn');
        butonYukleniyor(btn, true);
        try {
            const res = await fetch('api/auth/giris', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: document.getElementById('girisEmail').value.trim(),
                    sifre: document.getElementById('girisSifre').value
                })
            });
            const veri = await res.json();
            if (!res.ok || veri.basarili === false) {
                uyariGoster(veri.mesaj || 'Giriş başarısız.');
                return;
            }
            // Giris yapmadan once sepete eklenen (misafir) urunler varsa,
            // artik gercek oturuma sahip oldugumuz icin sunucu sepetine tasi.
            if (window.EK && typeof EK.misafirSepetiBirlestir === 'function') {
                await EK.misafirSepetiBirlestir();
            }
            yonlendir();
        } catch (err) {
            uyariGoster('Sunucuya bağlanılamadı, lütfen tekrar deneyin.');
        } finally {
            butonYukleniyor(btn, false);
        }
    });

    kayitForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        uyariGizle();
        const sifre = document.getElementById('kayitSifre').value;
        const sifreTekrar = document.getElementById('kayitSifreTekrar').value;
        if (sifre !== sifreTekrar) {
            uyariGoster('Şifreler eşleşmiyor.');
            return;
        }
        const btn = document.getElementById('kayitBtn');
        butonYukleniyor(btn, true);
        try {
            const res = await fetch('api/auth/kayit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    adSoyad: document.getElementById('kayitAdSoyad').value.trim(),
                    email: document.getElementById('kayitEmail').value.trim(),
                    sifre: sifre,
                    sifreTekrar: sifreTekrar
                })
            });
            const veri = await res.json();
            if (!res.ok || veri.basarili === false) {
                uyariGoster(veri.mesaj || 'Kayıt başarısız.');
                return;
            }
            if (window.EK && typeof EK.misafirSepetiBirlestir === 'function') {
                await EK.misafirSepetiBirlestir();
            }
            yonlendir();
        } catch (err) {
            uyariGoster('Sunucuya bağlanılamadı, lütfen tekrar deneyin.');
        } finally {
            butonYukleniyor(btn, false);
        }
    });
})();
