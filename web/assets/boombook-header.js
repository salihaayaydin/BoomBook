/* ============================================================
   BOOMBOOK — Ortak Header Mantigi
   Bu dosya index.html VE kitap-detay.html gibi BOOMBOOK temali her
   sayfada ortak olan header davranislarini icerir: tema (karanlik
   mod), mobil hamburger, canli arama, sepet (mini popover + tam
   offcanvas), favoriler (offcanvas), profil menusu tetikleyicisi.

   Sayfaya OZEL kisimlar (hero slider, cok satanlar/yayinevi
   carousel'leri, kampanya kartlari) burada DEGIL, boombook-anasayfa.js
   icinde; kitap karti/yildiz/indirim yardimcilari ise boombook-kart.js
   icindedir. Boylece bu dosya, olmayan elementlere erisip hata
   FIRLATMADAN her BOOMBOOK sayfasinda guvenle calisir.

   Disariya window.BB namespace'i ile bazi fonksiyonlari acar:
     BB.favoriSet        -> giris yapilmissa favori kitapId'leri (Set)
     BB.favorilerHazir    -> favoriSet doldugunda cozulen Promise
     BB.sepetPopoverGuncelle() -> sepet degistiginde tekrar cagrilmali
   ============================================================ */

window.BB = window.BB || {};

(() => {
    window.BB.favoriSet = new Set();

    /* ---------------- TEMA (Karanlik Mod) ---------------- */
    function temaHazirla() {
        const kayitli = localStorage.getItem('bb-tema') || 'aydinlik';
        document.documentElement.setAttribute('data-tema', kayitli);
        const btn = document.getElementById('bbTemaBtn');
        if (btn) btn.textContent = kayitli === 'karanlik' ? '☀️' : '🌙';

        if (btn) {
            btn.addEventListener('click', () => {
                const yeni = document.documentElement.getAttribute('data-tema') === 'karanlik' ? 'aydinlik' : 'karanlik';
                document.documentElement.setAttribute('data-tema', yeni);
                localStorage.setItem('bb-tema', yeni);
                btn.textContent = yeni === 'karanlik' ? '☀️' : '🌙';
            });
        }
    }

    /* ---------------- MOBIL HAMBURGER ---------------- */
    function mobilMenuHazirla() {
        const hamburger = document.getElementById('bbHamburger');
        const nav = document.getElementById('bbNav');
        if (!hamburger || !nav) return;

        hamburger.addEventListener('click', () => nav.classList.toggle('bb-mobile-open'));

        nav.querySelectorAll('.bb-nav-item').forEach(item => {
            const link = item.querySelector('.bb-nav-link');
            const mega = item.querySelector('.bb-mega-menu');
            if (!link || !mega) return;
            link.addEventListener('click', (ev) => {
                if (window.innerWidth > 768) return;
                ev.preventDefault();
                item.classList.toggle('bb-mobile-expanded');
            });
        });
    }

    /* ---------------- ARAMA (sunucu taraflı, canli oneri) ---------------- */
    function aramaHazirla() {
        const input = document.getElementById('bbAramaInput');
        const clearBtn = document.getElementById('bbAramaTemizle');
        const sonucKutu = document.getElementById('bbAramaSonuclar');
        if (!input) return;

        let zamanlayici = null;

        input.addEventListener('input', () => {
            clearBtn.style.display = input.value ? 'block' : 'none';
            clearTimeout(zamanlayici);
            zamanlayici = setTimeout(() => aramaSonuclariGetir(input.value.trim()), 250);
        });

        clearBtn.addEventListener('click', () => {
            input.value = '';
            clearBtn.style.display = 'none';
            sonucKutu.classList.remove('bb-open');
            input.focus();
        });

        document.addEventListener('click', (ev) => {
            if (!ev.target.closest('.bb-search-wrap')) {
                sonucKutu.classList.remove('bb-open');
            }
        });

        input.addEventListener('keydown', (ev) => {
            if (ev.key === 'Enter' && input.value.trim()) {
                window.location.href = 'arama-sonuclari.html?q=' + encodeURIComponent(input.value.trim());
            }
        });

        async function aramaSonuclariGetir(sorgu) {
            if (!sorgu) { sonucKutu.classList.remove('bb-open'); return; }

            try {
                const res = await fetch('api/kitaplar?q=' + encodeURIComponent(sorgu));
                const eslesenler = (await res.json()).slice(0, 6);

                if (eslesenler.length === 0) {
                    sonucKutu.innerHTML = `<div class="bb-search-empty">"${sorgu}" için sonuç bulunamadı.</div>`;
                } else {
                    sonucKutu.innerHTML = eslesenler.map(k => `
                        <div class="bb-search-result-item" data-kitap-id="${k.kitapId}">
                            ${k.kapakResmiUrl ? `<img src="${k.kapakResmiUrl}" alt="">` : `<div style="width:34px;height:50px;background:var(--bb-plum);border-radius:4px;flex-shrink:0"></div>`}
                            <div>
                                <div class="bb-search-result-title">${k.kitapAdi}</div>
                                <div class="bb-search-result-meta">${k.yazarAdi || ''}</div>
                            </div>
                        </div>`).join('') +
                        `<div class="bb-search-result-item" style="justify-content:center;font-weight:600;color:var(--bb-navy)" id="bbAramaTumSonuclar">
                            "${sorgu}" için tüm sonuçları gör →
                        </div>`;

                    sonucKutu.querySelectorAll('.bb-search-result-item[data-kitap-id]').forEach(el => {
                        el.addEventListener('click', () => {
                            window.location.href = 'kitap-detay.html?id=' + el.dataset.kitapId;
                        });
                    });
                    const tumSonuclar = document.getElementById('bbAramaTumSonuclar');
                    if (tumSonuclar) {
                        tumSonuclar.addEventListener('click', () => {
                            window.location.href = 'arama-sonuclari.html?q=' + encodeURIComponent(sorgu);
                        });
                    }
                }
                sonucKutu.classList.add('bb-open');
            } catch (err) {
                sonucKutu.innerHTML = `<div class="bb-search-empty">Arama yapılamadı.</div>`;
                sonucKutu.classList.add('bb-open');
            }
        }
    }

    /* ---------------- MINI SEPET POPOVER + OFFCANVAS ---------------- */
    async function sepetPopoverGuncelle() {
        const icerik = document.getElementById('bbCartPopoverIcerik');
        const toplamEl = document.getElementById('bbCartPopoverToplam');
        if (!icerik) return;

        const sepet = await EK.getSepet();
        if (sepet.length === 0) {
            icerik.innerHTML = '<div class="bb-cart-popover-empty">Sepetiniz boş.</div>';
            toplamEl.style.display = 'none';
            return;
        }
        toplamEl.style.display = 'flex';
        icerik.innerHTML = sepet.slice(0, 3).map(k => `
            <div class="bb-cart-popover-item">
                ${k.kapakResmiUrl ? `<img src="${k.kapakResmiUrl}" alt="">` : `<div style="width:30px;height:44px;background:var(--bb-plum);border-radius:3px"></div>`}
                <div class="flex-grow-1">${k.kitapAdi} <span class="text-muted">× ${k.adet}</span></div>
            </div>`).join('') + (sepet.length > 3 ? `<div class="text-muted small mt-1">+ ${sepet.length - 3} ürün daha</div>` : '');

        const toplam = EK.sepetToplam(sepet);
        toplamEl.querySelector('.bb-cart-popover-toplam-deger').textContent = EK.fiyatFormat(toplam);
    }

    /* ---------------- FAVORI OFFCANVAS ---------------- */
    function favoriOffcanvasHazirla() {
        const offcanvasEl = document.getElementById('bbFavoriOffcanvas');
        if (!offcanvasEl) return;
        offcanvasEl.addEventListener('show.bs.offcanvas', favoriOffcanvasRenderla);
    }

    async function favoriOffcanvasRenderla() {
        const liste = document.getElementById('bbFavoriListe');
        const bos = document.getElementById('bbFavoriBos');
        const favoriler = await EK.getFavoriler();

        if (favoriler.length === 0) {
            bos.classList.remove('d-none');
            liste.innerHTML = '';
            return;
        }
        bos.classList.add('d-none');
        liste.innerHTML = favoriler.map(k => `
            <div class="d-flex align-items-center gap-2 py-2 border-bottom" style="border-color:var(--bb-border) !important">
                ${k.kapakResmiUrl ? `<img src="${k.kapakResmiUrl}" style="width:44px;height:66px;object-fit:cover" class="rounded">` : `<div style="width:44px;height:66px;background:var(--bb-plum);border-radius:4px"></div>`}
                <div class="flex-grow-1">
                    <div class="small fw-semibold">${k.kitapAdi}</div>
                    <div class="small text-muted">${EK.fiyatFormat(k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat)}</div>
                </div>
                <button class="btn btn-sm bb-btn bb-btn-navy bb-favori-sepete-ekle" data-kitap-id="${k.kitapId}" title="Sepete Ekle">🛒</button>
                <button class="btn btn-sm btn-outline-danger bb-favori-kaldir" data-kitap-id="${k.kitapId}">&times;</button>
            </div>`).join('');

        liste.querySelectorAll('.bb-favori-sepete-ekle').forEach(btn => btn.addEventListener('click', async () => {
            const kitapId = parseInt(btn.dataset.kitapId, 10);
            const kitap = favoriler.find(k => k.kitapId === kitapId);
            const ok = await EK.sepeteEkle(kitap);
            if (ok) { EK.toast('Sepete eklendi.', 'success'); sepetPopoverGuncelle(); }
        }));
        liste.querySelectorAll('.bb-favori-kaldir').forEach(btn => btn.addEventListener('click', async () => {
            const kitapId = parseInt(btn.dataset.kitapId, 10);
            await EK.favoridenCikar(kitapId);
            window.BB.favoriSet.delete(kitapId);
            favoriOffcanvasRenderla();
        }));
    }

    /* dısarıya ac: baska scriptler sepet degistiginde popover'i yenilemek icin cagirir */
    window.BB.sepetPopoverGuncelle = sepetPopoverGuncelle;

    /* ---------------- BASLAT ---------------- */
    window.BB.favorilerHazir = (async () => {
        window.BB.favoriSet = await EK.getFavoriIdler();
    })();

    document.addEventListener('DOMContentLoaded', () => {
        // Her adim ayri try/catch icinde: biri hata verirse digerleri
        // (arama, sepet, favoriler, tema...) yine de calismaya devam etsin.
        [temaHazirla, mobilMenuHazirla, aramaHazirla, favoriOffcanvasHazirla].forEach(fn => {
            try { fn(); } catch (err) { console.error('BOOMBOOK header baslatma hatasi:', err); }
        });

        const cartWrap = document.querySelector('.bb-cart-wrap');
        if (cartWrap) cartWrap.addEventListener('mouseenter', sepetPopoverGuncelle);

        sepetPopoverGuncelle();
    });
})();
