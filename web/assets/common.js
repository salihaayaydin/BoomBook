/* ============================================================
   ORTAK YARDIMCI FONKSIYONLAR
   - Gercek oturum durumu: sayfa yuklendiginde /api/auth/ben cagrilir,
     navbar'daki kullanici alani ve "Admin Paneli" linki buna gore
     guncellenir (bkz. oturumDurumunuHazirla()).
   - Sepet yonetimi ARTIK SUNUCU TARAFINDA (sepet tablosu, /api/sepet
     uc noktasi). Oturum kapatilip acildiginda sepet korunur. Sepeti
     kullanmak icin giris yapmis olmak gerekir; giris yoksa fonksiyonlar
     kullaniciyi login.jsp'ye yonlendirir.
   - Kategori / Yazar / Yayinevi dropdown menulerinin doldurulmasi.
   ============================================================ */

const EK = (() => {

    let oturumKullanicisiCache = null;

    /* ============================================================
       MISAFIR (GIRIS YAPILMAMIS) SEPETI
       Sunucu sepeti oturum gerektirdigi icin, giris yapmamis bir
       kullanici "sepete ekle" dedigi anda 401 alinir. Kullaniciyi
       zorla login sayfasina atmak/engellemek yerine, urunu tarayicida
       (localStorage) tutariz; boylece gezinmeye, sepeti gormeye ve
       adet degistirmeye devam edebilir. Giris/kayit basarili oldugu
       AN bu liste sunucu sepetiyle birlestirilir (bkz. misafirSepetiBirlestir,
       auth.js icinde girisForm/kayitForm submit sonrasi cagrilir).
       ============================================================ */
    const MISAFIR_SEPET_ANAHTARI = 'bb-misafir-sepet';

    function misafirSepetiOku() {
        try {
            const ham = localStorage.getItem(MISAFIR_SEPET_ANAHTARI);
            const liste = ham ? JSON.parse(ham) : [];
            return Array.isArray(liste) ? liste : [];
        } catch (err) {
            return [];
        }
    }

    function misafirSepetiYaz(liste) {
        try {
            localStorage.setItem(MISAFIR_SEPET_ANAHTARI, JSON.stringify(liste));
        } catch (err) {
            // localStorage dolu/erisilemez (orn. gizli sekme kisitlamasi) olabilir; sessizce gec.
        }
    }

    /** kitap: kitapKarti/detay sayfasindan gelen tam kitap nesnesi (kitapId, kitapAdi, fiyat, indirimliFiyat, kapakResmiUrl, stokMiktari...).
     *  Not: misafir sepeti icin stok siniri, kitap objesindeki (sayfa yuklenirken alinmis) stokMiktari
     *  uzerinden en iyi cabayla uygulanir; asil/kesin kontrol her zaman giristen sonra sunucuda
     *  (SepetDAO.ekle) ve nihayetinde siparis aninda tekrar yapilir. */
    function misafirSepeteEkle(kitap) {
        const liste = misafirSepetiOku();
        const stokLimiti = Number.isFinite(kitap.stokMiktari) ? kitap.stokMiktari : Infinity;
        const mevcut = liste.find(k => k.kitapId === kitap.kitapId);
        let stoklaSinirli = false;
        if (mevcut) {
            const istenen = mevcut.adet + 1;
            mevcut.adet = Math.min(istenen, stokLimiti);
            stoklaSinirli = mevcut.adet < istenen;
        } else {
            if (stokLimiti <= 0) {
                return { liste, stoklaSinirli: true, eklenemedi: true };
            }
            liste.push({
                kitapId: kitap.kitapId,
                kitapAdi: kitap.kitapAdi,
                kapakResmiUrl: kitap.kapakResmiUrl || null,
                fiyat: kitap.fiyat,
                indirimliFiyat: kitap.indirimliFiyat != null ? kitap.indirimliFiyat : null,
                birimFiyat: kitap.indirimliFiyat != null ? kitap.indirimliFiyat : kitap.fiyat,
                stokMiktari: kitap.stokMiktari,
                adet: 1
            });
        }
        misafirSepetiYaz(liste);
        return { liste, stoklaSinirli, eklenemedi: false };
    }

    function misafirSepettenCikar(kitapId) {
        const liste = misafirSepetiOku().filter(k => k.kitapId !== kitapId);
        misafirSepetiYaz(liste);
        return liste;
    }

    function misafirAdetGuncelle(kitapId, adet) {
        const liste = misafirSepetiOku();
        const kalem = liste.find(k => k.kitapId === kitapId);
        let stoklaSinirli = false;
        if (kalem) {
            const stokLimiti = Number.isFinite(kalem.stokMiktari) ? kalem.stokMiktari : Infinity;
            const istenen = Math.max(1, adet);
            kalem.adet = Math.min(istenen, stokLimiti);
            stoklaSinirli = kalem.adet < istenen;
        }
        misafirSepetiYaz(liste);
        return { liste, stoklaSinirli };
    }

    function misafirSepetiTemizle() {
        misafirSepetiYaz([]);
    }

    /** Giris/kayit basarili olur olmaz cagrilir: misafir sepetindeki her urunu
     *  gercek sunucu sepetine (api/sepet POST) tek tek ekler, sonra localStorage'i temizler. */
    async function misafirSepetiBirlestir() {
        const liste = misafirSepetiOku();
        if (liste.length === 0) return;
        for (const kalem of liste) {
            try {
                await fetch('api/sepet', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ kitapId: kalem.kitapId, adet: kalem.adet })
                });
            } catch (err) {
                // Bir urun eklenemezse digerlerine devam et; kullaniciyi akistan koparmayalim.
            }
        }
        misafirSepetiTemizle();
    }

    /** Sunucudaki (giris yapilmissa) veya misafir (localStorage) sepetini getirir. */
    async function getSepet() {
        try {
            const res = await fetch('api/sepet');
            if (res.status === 401) {
                return misafirSepetiOku();
            }
            if (!res.ok) {
                throw new Error('Sunucu hatasi: ' + res.status);
            }
            return await res.json();
        } catch (err) {
            return misafirSepetiOku();
        }
    }

    /** Sepete bir kitap ekler (adet 1 artirir). Giris yoksa misafir sepetine (localStorage) ekler. */
    async function sepeteEkle(kitap) {
        const res = await fetch('api/sepet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kitapId: kitap.kitapId, adet: 1 })
        });
        if (res.status === 401) {
            const misafirSonuc = misafirSepeteEkle(kitap);
            if (misafirSonuc.eklenemedi) {
                toast('Üzgünüz, bu ürün şu anda stokta yok.', 'error');
                return false;
            }
            await updateCartBadge();
            if (misafirSonuc.stoklaSinirli) {
                toast('Sepete eklendi. Stok sınırı nedeniyle bu üründen en fazla ' + kitap.stokMiktari + ' adet alabilirsiniz.', 'dark');
            }
            return true;
        }
        const sonuc = await res.json();
        if (res.ok && sonuc.basarili) {
            await updateCartBadge();
            if (sonuc.stoklaSinirli) toast(sonuc.mesaj, 'dark');
            return true;
        }
        toast(sonuc.mesaj || 'Sepete eklenemedi.', 'error');
        return false;
    }

    async function sepettenCikar(kitapId) {
        const res = await fetch('api/sepet?kitapId=' + encodeURIComponent(kitapId), { method: 'DELETE' });
        if (res.status === 401) {
            misafirSepettenCikar(kitapId);
            await updateCartBadge();
            return true;
        }
        await updateCartBadge();
        return res.ok;
    }

    /** Adedi MUTLAK deger olarak gunceller (1'in altina dusemez, cagiran taraf kontrol eder). */
    async function adetGuncelle(kitapId, adet) {
        const res = await fetch('api/sepet', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kitapId, adet: Math.max(1, adet) })
        });
        if (res.status === 401) {
            const misafirSonuc = misafirAdetGuncelle(kitapId, adet);
            await updateCartBadge();
            if (misafirSonuc.stoklaSinirli) toast('Stok sınırı nedeniyle adet güncellendi.', 'dark');
            return true;
        }
        const sonuc = await res.json().catch(() => null);
        await updateCartBadge();
        if (sonuc && sonuc.stoklaSinirli) toast(sonuc.mesaj, 'dark');
        return res.ok;
    }

    async function sepetiTemizle() {
        const res = await fetch('api/sepet?hepsi=true', { method: 'DELETE' });
        if (res.status === 401) {
            misafirSepetiTemizle();
        }
        await updateCartBadge();
    }

    function sepetToplam(sepet) {
        return sepet.reduce((toplam, k) => toplam + (k.birimFiyat * k.adet), 0);
    }

    async function updateCartBadge() {
        const sepet = await getSepet();
        const adet = sepet.reduce((t, k) => t + k.adet, 0);
        document.querySelectorAll('.ek-cart-count').forEach(el => {
            el.textContent = adet;
            el.style.display = adet > 0 ? 'flex' : 'none';
        });
        return sepet;
    }

    /* ---- Favoriler (kalp ikonu) - sepet ile ayni mantik, /api/favori ---- */

    /** Sadece favori kitapId'lerini (Set) getirir; giris yoksa bos Set doner. */
    async function getFavoriIdler() {
        try {
            const res = await fetch('api/favori?sadeceId=true');
            if (res.status === 401) return new Set();
            if (!res.ok) throw new Error('Sunucu hatasi: ' + res.status);
            const dizi = await res.json();
            return new Set(dizi);
        } catch (err) {
            return new Set();
        }
    }

    async function getFavoriler() {
        try {
            const res = await fetch('api/favori');
            if (res.status === 401) return [];
            if (!res.ok) throw new Error('Sunucu hatasi: ' + res.status);
            return await res.json();
        } catch (err) {
            return [];
        }
    }

    async function favoriyeEkle(kitapId) {
        const res = await fetch('api/favori', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kitapId })
        });
        if (res.status === 401) {
            girisGerekliGoster('Bu kitabı favorilerinize eklemek için giriş yapmanız veya kayıt olmanız gerekiyor.');
            return false;
        }
        await updateFavBadge();
        return res.ok;
    }

    async function favoridenCikar(kitapId) {
        const res = await fetch('api/favori?kitapId=' + encodeURIComponent(kitapId), { method: 'DELETE' });
        await updateFavBadge();
        return res.ok;
    }

    async function updateFavBadge() {
        const idler = await getFavoriIdler();
        document.querySelectorAll('.ek-fav-count').forEach(el => {
            el.textContent = idler.size;
            el.style.display = idler.size > 0 ? 'flex' : 'none';
        });
        return idler;
    }

    function toast(mesaj, tur = 'success') {
        let container = document.getElementById('ekToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'ekToastContainer';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            container.style.zIndex = 1080;
            document.body.appendChild(container);
        }
        const id = 'toast' + Date.now();
        const bg = tur === 'success' ? 'text-bg-success' : (tur === 'error' ? 'text-bg-danger' : 'text-bg-dark');
        container.insertAdjacentHTML('beforeend', `
            <div id="${id}" class="toast ${bg}" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${mesaj}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>`);
        const el = document.getElementById(id);
        const t = new bootstrap.Toast(el, { delay: 2800 });
        t.show();
        el.addEventListener('hidden.bs.toast', () => el.remove());
    }

    /**
     * Giris gerektiren bir islem (sepete ekle, favorile vb.) giris yapilmadan
     * denendiginde kullaniciyi ZORLA yonlendirmek yerine ona secim sunan
     * bir Bootstrap modal gosterir: "Giris Yap" ya da "Vazgec".
     */
    function girisGerekliGoster(mesaj) {
        let modalEl = document.getElementById('ekGirisGerekliModal');
        if (!modalEl) {
            modalEl = document.createElement('div');
            modalEl.className = 'modal fade';
            modalEl.id = 'ekGirisGerekliModal';
            modalEl.tabIndex = -1;
            modalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Giriş Yapmalısınız</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body" id="ekGirisGerekliMesaj"></div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Vazgeç, Gezinmeye Devam Et</button>
                            <a class="btn btn-primary" id="ekGirisGerekliLink" href="#">Giriş Yap / Kayıt Ol</a>
                        </div>
                    </div>
                </div>`;
            document.body.appendChild(modalEl);
        }
        document.getElementById('ekGirisGerekliMesaj').textContent = mesaj;
        document.getElementById('ekGirisGerekliLink').href =
            'login.jsp?sonraki=' + encodeURIComponent(window.location.pathname + window.location.search);

        if (window.bootstrap && window.bootstrap.Modal) {
            window.bootstrap.Modal.getOrCreateInstance(modalEl).show();
        } else {
            toast(mesaj, 'error');
        }
    }

    function fiyatFormat(deger) {
        return Number(deger).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TL';
    }

    /**
     * Kullanicidan/veritabanindan gelen serbest metni innerHTML icine
     * basmadan once HTML olarak yorumlanmasini engeller (XSS korumasi).
     * Kullanici tarafindan girilebilen HER metin (yorum, ad soyad, aciklama vb.)
     * innerHTML'e yazilirken MUTLAKA bu fonksiyondan gecirilmelidir.
     */
    function escapeHtml(deger) {
        if (deger === null || deger === undefined) return '';
        const div = document.createElement('div');
        div.textContent = String(deger);
        return div.innerHTML;
    }

    /** Navbar'daki Kategoriler / Yazarlar / Yayinevleri dropdown'larini API'den doldurur. */
    async function filtreMenuleriniDoldur() {
        const eslesmeler = [
            { url: 'api/kategoriler', menuId: 'kategoriMenu', idAlan: 'kategoriId', adAlan: 'kategoriAdi', param: 'kategoriId' },
            { url: 'api/yazarlar', menuId: 'yazarMenu', idAlan: 'yazarId', adAlan: 'yazarAdi', param: 'yazarId' },
            { url: 'api/yayinevleri', menuId: 'yayineviMenu', idAlan: 'yayineviId', adAlan: 'yayineviAdi', param: 'yayineviId' }
        ];

        for (const e of eslesmeler) {
            const menu = document.getElementById(e.menuId);
            if (!menu) continue;
            try {
                const res = await fetch(e.url);
                const liste = await res.json();
                if (!Array.isArray(liste) || liste.length === 0) {
                    menu.innerHTML = '<div class="bb-mega-menu-empty">Kayıt bulunamadı</div>';
                    continue;
                }
                menu.innerHTML = liste.map(item => `
                    <a href="katalog.html?${e.param}=${item[e.idAlan]}">${item[e.adAlan]}</a>`).join('');
            } catch (err) {
                menu.innerHTML = '<div class="bb-mega-menu-empty">Yüklenemedi</div>';
            }
        }
    }

    /** Sayfa acilirken /api/auth/ben ile oturumu sorgular, navbar'i gunceller. */
    async function oturumDurumunuHazirla() {
        const alan = document.getElementById('ekKullaniciAlani');
        const adminNavItem = document.getElementById('ekAdminNavItem');
        const profilMenu = document.getElementById('ekProfilMenu'); // opsiyonel: yeni BOOMBOOK header'i

        try {
            const res = await fetch('api/auth/ben');
            if (res.status === 401) {
                oturumKullanicisiCache = null;
                if (alan) alan.innerHTML = girisLinkiHtml();
                if (adminNavItem) adminNavItem.classList.add('d-none');
                if (profilMenu) profilMenu.innerHTML = profilMenuMisafirHtml();
                return;
            }
            const kullanici = await res.json();
            oturumKullanicisiCache = kullanici;
            if (alan) alan.innerHTML = kullaniciChipHtml(kullanici);
            if (adminNavItem) adminNavItem.classList.toggle('d-none', kullanici.rol !== 'Admin');
            if (profilMenu) profilMenu.innerHTML = profilMenuKullaniciHtml(kullanici);

            document.querySelectorAll('.ek-cikis-btn').forEach(btn => {
                btn.addEventListener('click', async () => {
                    await fetch('api/auth/cikis', { method: 'POST' });
                    window.location.href = 'index.html';
                });
            });
        } catch (err) {
            // Sunucuya erisilemedi; giris linkini goster, sayfayi bozma.
            if (alan) alan.innerHTML = girisLinkiHtml();
            if (adminNavItem) adminNavItem.classList.add('d-none');
            if (profilMenu) profilMenu.innerHTML = profilMenuMisafirHtml();
        }
    }

    function profilMenuMisafirHtml() {
        return `
            <li><h6 class="dropdown-header">Hesabım</h6></li>
            <li><a class="dropdown-item" href="login.jsp">Giriş Yap</a></li>
            <li><a class="dropdown-item" href="login.jsp?kayit=1">Kayıt Ol</a></li>`;
    }

    function profilMenuKullaniciHtml(kullanici) {
        const adminSatiri = kullanici.rol === 'Admin'
            ? '<li><a class="dropdown-item" href="admin.jsp">⚙ Admin Paneli</a></li><li><hr class="dropdown-divider"></li>'
            : '';
        return `
            <li><h6 class="dropdown-header">Merhaba, ${escapeHtml(kullanici.adSoyad.split(' ')[0])}</h6></li>
            <li><a class="dropdown-item" href="sepet.html">🛒 Sepetim</a></li>
            <li><a class="dropdown-item" href="siparislerim.html">📦 Siparişlerim</a></li>
            <li><a class="dropdown-item" href="kutuphanem.html">📚 Kütüphanem</a></li>
            <li><hr class="dropdown-divider"></li>
            ${adminSatiri}
            <li><button type="button" class="dropdown-item ek-cikis-btn">↩ Çıkış Yap</button></li>`;
    }

    function girisLinkiHtml() {
        return `<a href="login.jsp" class="btn btn-sm btn-ek-gold">Giriş Yap</a>`;
    }

    function kullaniciChipHtml(kullanici) {
        const rolRozet = kullanici.rol === 'Admin'
            ? '<span class="badge text-bg-warning ek-role-badge">Admin</span>'
            : '';
        return `
            <div class="ek-user-chip">
                <span class="ek-user-ad">${escapeHtml(kullanici.adSoyad)}</span>
                ${rolRozet}
                <button type="button" class="ek-link-btn ek-cikis-btn" id="ekCikisBtn">Çıkış Yap</button>
            </div>`;
    }

    /** Giris yapmis kullaniciyi (sifre_hash icermez) doner; henuz sorgulanmadiysa null olabilir. */
    function oturumKullanicisi() {
        return oturumKullanicisiCache;
    }

    document.addEventListener('DOMContentLoaded', () => {
        updateCartBadge();
        updateFavBadge();
        filtreMenuleriniDoldur();
        oturumDurumunuHazirla();
        urlHatalariniGoster();
    });

    /** Sunucudan sessiz yonlendirmelerle gelen hata kodlarini (orn. ?hata=admin_yetkisi_yok) yakalayip toast gosterir. */
    function urlHatalariniGoster() {
        const params = new URLSearchParams(window.location.search);
        const hataKodu = params.get('hata');
        if (!hataKodu) return;

        const mesajlar = {
            admin_yetkisi_yok: 'Bu sayfaya erişmek için Admin yetkisine sahip olmanız gerekiyor.'
        };
        toast(mesajlar[hataKodu] || 'Bu işlem için yetkiniz yok.', 'error');

        // URL'yi temizle (yenilemede tekrar tekrar toast cikmasin)
        params.delete('hata');
        const yeniUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '');
        window.history.replaceState({}, '', yeniUrl);
    }

    return {
        oturumKullanicisi,
        getSepet, sepeteEkle, sepettenCikar, adetGuncelle, sepetiTemizle, sepetToplam,
        updateCartBadge, toast, fiyatFormat, escapeHtml,
        getFavoriIdler, getFavoriler, favoriyeEkle, favoridenCikar, updateFavBadge,
        misafirSepetiOku, misafirSepetiBirlestir
    };
})();
