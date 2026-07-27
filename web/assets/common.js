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

    /** Sunucudaki sepeti getirir. Giris yapilmamissa bos dizi doner (sayfa akisini bozmamak icin). */
    async function getSepet() {
        try {
            const res = await fetch('api/sepet');
            if (res.status === 401) {
                return [];
            }
            if (!res.ok) {
                throw new Error('Sunucu hatasi: ' + res.status);
            }
            return await res.json();
        } catch (err) {
            return [];
        }
    }

    /** Sepete bir kitap ekler (adet 1 artirir). Giris yoksa login.jsp'ye yonlendirir. */
    async function sepeteEkle(kitap) {
        const res = await fetch('api/sepet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kitapId: kitap.kitapId, adet: 1 })
        });
        if (res.status === 401) {
            toast('Sepete eklemek icin once giris yapmalisiniz.', 'error');
            setTimeout(() => { window.location.href = 'login.jsp?sonraki=' + encodeURIComponent(window.location.pathname); }, 900);
            return false;
        }
        const sonuc = await res.json();
        if (res.ok && sonuc.basarili) {
            await updateCartBadge();
            return true;
        }
        toast(sonuc.mesaj || 'Sepete eklenemedi.', 'error');
        return false;
    }

    async function sepettenCikar(kitapId) {
        const res = await fetch('api/sepet?kitapId=' + encodeURIComponent(kitapId), { method: 'DELETE' });
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
        await updateCartBadge();
        return res.ok;
    }

    async function sepetiTemizle() {
        await fetch('api/sepet?hepsi=true', { method: 'DELETE' });
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

    function fiyatFormat(deger) {
        return Number(deger).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TL';
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
                    menu.innerHTML = '<li><span class="dropdown-item-text text-muted">Kayit bulunamadi</span></li>';
                    continue;
                }
                menu.innerHTML = liste.map(item => `
                    <li><a class="dropdown-item" href="index.html?${e.param}=${item[e.idAlan]}">
                        ${item[e.adAlan]}
                    </a></li>`).join('');
            } catch (err) {
                menu.innerHTML = '<li><span class="dropdown-item-text text-danger">Yuklenemedi</span></li>';
            }
        }
    }

    /** Sayfa acilirken /api/auth/ben ile oturumu sorgular, navbar'i gunceller. */
    async function oturumDurumunuHazirla() {
        const alan = document.getElementById('ekKullaniciAlani');
        const adminNavItem = document.getElementById('ekAdminNavItem');

        try {
            const res = await fetch('api/auth/ben');
            if (res.status === 401) {
                oturumKullanicisiCache = null;
                if (alan) alan.innerHTML = girisLinkiHtml();
                if (adminNavItem) adminNavItem.classList.add('d-none');
                return;
            }
            const kullanici = await res.json();
            oturumKullanicisiCache = kullanici;
            if (alan) alan.innerHTML = kullaniciChipHtml(kullanici);
            if (adminNavItem) adminNavItem.classList.toggle('d-none', kullanici.rol !== 'Admin');

            const cikisBtn = document.getElementById('ekCikisBtn');
            if (cikisBtn) {
                cikisBtn.addEventListener('click', async () => {
                    await fetch('api/auth/cikis', { method: 'POST' });
                    window.location.href = 'index.html';
                });
            }
        } catch (err) {
            // Sunucuya erisilemedi; giris linkini goster, sayfayi bozma.
            if (alan) alan.innerHTML = girisLinkiHtml();
            if (adminNavItem) adminNavItem.classList.add('d-none');
        }
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
                <span class="ek-user-ad">${kullanici.adSoyad}</span>
                ${rolRozet}
                <button type="button" class="ek-link-btn" id="ekCikisBtn">Çıkış Yap</button>
            </div>`;
    }

    /** Giris yapmis kullaniciyi (sifre_hash icermez) doner; henuz sorgulanmadiysa null olabilir. */
    function oturumKullanicisi() {
        return oturumKullanicisiCache;
    }

    document.addEventListener('DOMContentLoaded', () => {
        updateCartBadge();
        filtreMenuleriniDoldur();
        oturumDurumunuHazirla();
    });

    return {
        oturumKullanicisi,
        getSepet, sepeteEkle, sepettenCikar, adetGuncelle, sepetiTemizle, sepetToplam,
        updateCartBadge, toast, fiyatFormat
    };
})();
