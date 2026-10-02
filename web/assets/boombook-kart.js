/* ============================================================
   BOOMBOOK — Kitap Karti / Yardimci Fonksiyonlar
   boombook-anasayfa.js VE kitap-detay.js tarafindan ortak kullanilir.
   window.BB.kitapKartOlustur / yildizHtml / indirimYuzdesi
   / carouselOkHazirla fonksiyonlarini disariya acar.
   ============================================================ */

window.BB = window.BB || {};

(() => {
    const PLACEHOLDER_ICON = '📖';

    /**
     * GERCEK degerlendirme ortalamasini yildiz HTML'ine cevirir.
     * puan null/undefined ise (henuz hic degerlendirme yoksa) "Henuz
     * degerlendirme yok" metni gosterilir -- sahte/rastgele puan UYDURULMAZ.
     */
    function yildizHtml(puan, sayim) {
        if (puan === null || puan === undefined) {
            return '<span style="color:var(--bb-muted)">Henüz değerlendirme yok</span>';
        }
        let html = '';
        for (let i = 1; i <= 5; i++) {
            html += i <= Math.round(puan) ? '★' : '<span class="bb-star-empty">★</span>';
        }
        const sayimMetni = sayim != null ? ` (${sayim})` : ` (${puan.toFixed(1)})`;
        return `${html} <span style="color:var(--bb-muted)">${sayimMetni}</span>`;
    }

    function indirimYuzdesi(k) {
        if (!k.indirimliFiyat) return null;
        return Math.round((1 - (k.indirimliFiyat / k.fiyat)) * 100);
    }

    function kitapKartOlustur(k) {
        const stokYok = k.stokMiktari <= 0;
        const indirimYuzde = indirimYuzdesi(k);
        const favoride = window.BB.favoriSet.has(k.kitapId);
        const detayUrl = 'kitap-detay.html?id=' + k.kitapId;
        const kitapAdiGuvenli = EK.escapeHtml(k.kitapAdi);
        const yazarAdiGuvenli = EK.escapeHtml(k.yazarAdi || 'Bilinmeyen Yazar');

        const kart = document.createElement('div');
        kart.className = 'bb-book-card';
        // NOT: Detay sayfasina gecis GERCEK <a href> etiketleriyle yapiliyor
        // (JS calismasa veya bir onceki adim hata verse bile linkler calisir).
        // Favori/hizli-sepet butonlari ise anchor'larin DISINDA, ayri
        // konumlandirilmis elemanlar (nested <a><button> gecersiz HTML olurdu).
        kart.innerHTML = `
            <a href="${detayUrl}" class="bb-book-cover-wrap" aria-label="${kitapAdiGuvenli} detayına git">
                ${k.kapakResmiUrl
                    ? `<img src="${k.kapakResmiUrl}" alt="${kitapAdiGuvenli}" loading="lazy"
                           onerror="this.style.display='none'; this.insertAdjacentHTML('afterend', '<div style=\\'display:flex;align-items:center;justify-content:center;height:100%;font-size:2.2rem;color:rgba(255,255,255,.6)\\'>${PLACEHOLDER_ICON}</div>')">`
                    : `<div style="display:flex;align-items:center;justify-content:center;height:100%;font-size:2.2rem;color:rgba(255,255,255,.6)">${PLACEHOLDER_ICON}</div>`}
                ${indirimYuzde ? `<span class="bb-discount-badge">%${indirimYuzde}</span>` : ''}
            </a>
            <div class="bb-book-hover-actions">
                <button type="button" class="bb-book-mini-btn bb-fav-btn ${favoride ? 'bb-fav-active' : ''}" title="Favorile" data-kitap-id="${k.kitapId}">
                    ${favoride ? '♥' : '♡'}
                </button>
                <button type="button" class="bb-book-mini-btn bb-hizli-sepet-btn" title="Hızlı Sepete Ekle" data-kitap-id="${k.kitapId}" ${stokYok ? 'disabled' : ''}>
                    🛒
                </button>
            </div>
            <a href="${detayUrl}" class="bb-book-body">
                <h3 class="bb-book-title">${kitapAdiGuvenli}</h3>
                <div class="bb-book-author">${yazarAdiGuvenli}</div>
                <div class="bb-book-rating">${yildizHtml(k.ortalamaPuan, k.degerlendirmeSayisi)}</div>
                <div class="bb-book-price-row">
                    ${stokYok
                        ? '<span class="bb-price-outofstock">Stokta Yok</span>'
                        : (k.indirimliFiyat
                            ? `<span class="bb-price-old">${EK.fiyatFormat(k.fiyat)}</span><span class="bb-price-current">${EK.fiyatFormat(k.indirimliFiyat)}</span>`
                            : `<span class="bb-price-current">${EK.fiyatFormat(k.fiyat)}</span>`)}
                </div>
            </a>
            <div style="padding:0 12px 12px">
                <button type="button" class="bb-book-add-btn bb-sepete-ekle-btn" data-kitap-id="${k.kitapId}" ${stokYok ? 'disabled' : ''}>
                    🛒 ${stokYok ? 'Stokta Yok' : 'Sepete Ekle'}
                </button>
            </div>`;

        kart.querySelector('.bb-fav-btn').addEventListener('click', async (ev) => {
            ev.stopPropagation();
            const btn = ev.currentTarget;
            const simdiFavoriMi = window.BB.favoriSet.has(k.kitapId);
            if (simdiFavoriMi) {
                const ok = await EK.favoridenCikar(k.kitapId);
                if (ok) { window.BB.favoriSet.delete(k.kitapId); btn.classList.remove('bb-fav-active'); btn.textContent = '♡'; }
            } else {
                const ok = await EK.favoriyeEkle(k.kitapId);
                if (ok) { window.BB.favoriSet.add(k.kitapId); btn.classList.add('bb-fav-active'); btn.textContent = '♥'; EK.toast(`"${k.kitapAdi}" favorilere eklendi.`, 'success'); }
            }
        });

        kart.querySelector('.bb-hizli-sepet-btn').addEventListener('click', async (ev) => {
            ev.stopPropagation();
            const btn = ev.currentTarget;
            btn.disabled = true;
            const ok = await EK.sepeteEkle(k);
            if (ok) {
                EK.toast(`"${k.kitapAdi}" sepete eklendi.`, 'success');
                window.BB.sepetPopoverGuncelle();
            }
            btn.disabled = k.stokMiktari <= 0;
        });

        const eklemeBtn = kart.querySelector('.bb-sepete-ekle-btn');
        if (eklemeBtn) {
            eklemeBtn.addEventListener('click', async (ev) => {
                ev.stopPropagation();
                const btn = ev.currentTarget;
                btn.disabled = true;
                const ok = await EK.sepeteEkle(k);
                if (ok) {
                    EK.toast(`"${k.kitapAdi}" sepete eklendi.`, 'success');
                    window.BB.sepetPopoverGuncelle();
                }
                btn.disabled = k.stokMiktari <= 0;
            });
        }

        return kart;
    }

    function carouselOkHazirla(trackId, prevId, nextId) {
        const track = document.getElementById(trackId);
        const prev = document.getElementById(prevId);
        const next = document.getElementById(nextId);
        if (!track || !prev || !next) return;
        const kaydirmaMiktari = () => Math.min(track.clientWidth * 0.8, 500);
        prev.addEventListener('click', () => track.scrollBy({ left: -kaydirmaMiktari(), behavior: 'smooth' }));
        next.addEventListener('click', () => track.scrollBy({ left: kaydirmaMiktari(), behavior: 'smooth' }));
    }

    window.BB.yildizHtml = yildizHtml;
    window.BB.indirimYuzdesi = indirimYuzdesi;
    window.BB.kitapKartOlustur = kitapKartOlustur;
    window.BB.carouselOkHazirla = carouselOkHazirla;
})();
