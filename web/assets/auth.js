/* ============================================================
   login.jsp icin giris/kayit form mantigi.
   Basarili giris/kayit sonrasi:
     - URL'de ?sonraki=... varsa o sayfaya (ornegin admin.jsp) yonlendirir
     - yoksa index.html'e doner
   ============================================================ */
(() => {
    const uyariKutusu = document.getElementById('ekAuthUyari');
    const tabGiris = document.getElementById('tabGiris');
    const tabKayit = document.getElementById('tabKayit');
    const girisForm = document.getElementById('girisForm');
    const kayitForm = document.getElementById('kayitForm');

    function sekmeAc(hedefId) {
        [girisForm, kayitForm].forEach(f => f.classList.remove('active'));
        [tabGiris, tabKayit].forEach(t => t.classList.remove('active'));
        document.getElementById(hedefId).classList.add('active');
        document.getElementById(hedefId === 'girisForm' ? 'tabGiris' : 'tabKayit').classList.add('active');
        uyariGizle();
    }

    tabGiris.addEventListener('click', () => sekmeAc('girisForm'));
    tabKayit.addEventListener('click', () => sekmeAc('kayitForm'));

    // Eger login.jsp'ye AuthFilter tarafindan yonlendirildiysek (admin.jsp korumasi),
    // kullaniciya neden burada oldugunu belirtelim.
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

    function butonYukleniyor(btn, yukleniyorMu) {
        btn.disabled = yukleniyorMu;
        btn.dataset.orijinal = btn.dataset.orijinal || btn.textContent;
        btn.textContent = yukleniyorMu ? 'Lütfen bekleyin...' : btn.dataset.orijinal;
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
            yonlendir();
        } catch (err) {
            uyariGoster('Sunucuya bağlanılamadı, lütfen tekrar deneyin.');
        } finally {
            butonYukleniyor(btn, false);
        }
    });
})();
