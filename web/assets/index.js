(() => {

    const grid = document.getElementById('ekKitapGrid');
    const yukleniyor = document.getElementById('ekYukleniyor');
    const bosMesaj = document.getElementById('ekBosMesaj');
    const baslik = document.getElementById('ekListeBaslik');
    const aktifFiltreEl = document.getElementById('ekAktifFiltre');

    const PLACEHOLDER = 'https://placehold.co/300x450/ece3cd/16241f?text=Kapak+Yok';

    function urlParametreleriniOku() {
        const p = new URLSearchParams(window.location.search);
        return {
            kategoriId: p.get('kategoriId'),
            yazarId: p.get('yazarId'),
            yayineviId: p.get('yayineviId')
        };
    }

    function kitapKarti(k) {
        const indirimde = k.indirimliFiyat != null && k.indirimliFiyat < k.fiyat;
        const fiyatHtml = indirimde
            ? `<span class="ek-price-old">${EK.fiyatFormat(k.fiyat)}</span> <span class="ek-price-new">${EK.fiyatFormat(k.indirimliFiyat)}</span>`
            : `<span class="ek-price">${EK.fiyatFormat(k.fiyat)}</span>`;

        const stokYok = k.stokMiktari <= 0;
        const stokBadge = stokYok
            ? `<span class="badge ek-stock-badge-low">Stokta yok</span>`
            : (k.stokMiktari < 5
                ? `<span class="badge ek-stock-badge-low">Son ${k.stokMiktari} adet</span>`
                : `<span class="badge ek-stock-badge-ok">Stokta</span>`);

        const kapak = k.kapakResmiUrl && k.kapakResmiUrl.trim() !== '' ? k.kapakResmiUrl : PLACEHOLDER;
        const aciklama = (k.aciklama || 'Bu kitap için açıklama girilmemiş.');
        const kisaAciklama = aciklama.length > 90 ? aciklama.slice(0, 90) + '…' : aciklama;

        const col = document.createElement('div');
        col.className = 'col-6 col-md-4 col-lg-3';
        col.innerHTML = `
            <div class="card ek-book-card">
                <img src="${kapak}" class="ek-book-cover" alt="${k.kitapAdi}"
                     onerror="this.onerror=null;this.src='${PLACEHOLDER}'">
                <div class="card-body d-flex flex-column">
                    <div class="ek-book-meta mb-1">${k.yayineviAdi || 'Bilinmeyen Yayınevi'}</div>
                    <h3 class="ek-book-title">${k.kitapAdi}</h3>
                    <div class="ek-book-meta mb-2">${k.yazarAdi || 'Bilinmeyen Yazar'}</div>
                    <p class="small text-muted flex-grow-1">${kisaAciklama}</p>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div>${fiyatHtml}</div>
                        ${stokBadge}
                    </div>
                    <button class="btn btn-ek-gold w-100 ek-sepete-ekle" ${stokYok ? 'disabled' : ''}>
                        ${stokYok ? 'Stokta Yok' : 'Sepete Ekle'}
                    </button>
                </div>
            </div>`;

        col.querySelector('.ek-sepete-ekle').addEventListener('click', async (ev) => {
            const btn = ev.currentTarget;
            btn.disabled = true;
            const basarili = await EK.sepeteEkle(k);
            if (basarili) {
                EK.toast(`"${k.kitapAdi}" sepete eklendi.`, 'success');
                sepetiRenderla();
            }
            btn.disabled = stokYok;
        });

        return col;
    }

    async function kitaplariYukle() {
        yukleniyor.classList.remove('d-none');
        bosMesaj.classList.add('d-none');
        grid.innerHTML = '';

        const { kategoriId, yazarId, yayineviId } = urlParametreleriniOku();
        const qs = new URLSearchParams();
        if (kategoriId) qs.set('kategoriId', kategoriId);
        if (yazarId) qs.set('yazarId', yazarId);
        if (yayineviId) qs.set('yayineviId', yayineviId);

        aktifFiltreEl.innerHTML = '';
        if (kategoriId || yazarId || yayineviId) {
            baslik.textContent = 'Filtrelenmiş Kitaplar';
            aktifFiltreEl.innerHTML = `
                <span class="ek-filter-active">
                    Filtre uygulandı
                    <button type="button" onclick="window.location='index.html'" title="Filtreyi temizle">&times;</button>
                </span>`;
        } else {
            baslik.textContent = 'Tüm Kitaplar';
        }

        try {
            const res = await fetch('api/kitaplar' + (qs.toString() ? '?' + qs.toString() : ''));
            if (!res.ok) throw new Error('Sunucu hatası: ' + res.status);
            const kitaplar = await res.json();

            yukleniyor.classList.add('d-none');

            if (!Array.isArray(kitaplar) || kitaplar.length === 0) {
                bosMesaj.classList.remove('d-none');
                return;
            }
            kitaplar.forEach(k => grid.appendChild(kitapKarti(k)));
        } catch (err) {
            yukleniyor.classList.add('d-none');
            grid.innerHTML = `<div class="col-12"><div class="alert alert-danger">Kitaplar yüklenemedi: ${err.message}</div></div>`;
        }
    }

    async function sepetiRenderla() {
        const liste = document.getElementById('ekSepetListe');
        const bos = document.getElementById('ekSepetBos');
        const toplamEl = document.getElementById('ekSepetToplam');
        const girisUyari = document.getElementById('ekSepetGirisUyari');

        // Once giris durumunu kontrol et: EK.oturumKullanicisi() sayfa yuklendiginde
        // oturumDurumunuHazirla() tarafindan doldurulmus olabilir; henuz hazir
        // degilse dogrudan sepeti sorgulamak da 401 durumunda bos dizi dondurur.
        const sepet = await EK.getSepet();

        if (sepet.length === 0) {
            bos.classList.remove('d-none');
            liste.innerHTML = '';
            toplamEl.textContent = EK.fiyatFormat(0);
            return;
        }
        bos.classList.add('d-none');
        if (girisUyari) girisUyari.classList.add('d-none');

        liste.innerHTML = sepet.map(k => {
            const birimFiyat = k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat;
            return `
            <div class="d-flex align-items-center gap-2 py-2 border-bottom">
                <img src="${k.kapakResmiUrl || 'https://placehold.co/60x90/ece3cd/16241f?text=--'}"
                     style="width:44px;height:66px;object-fit:cover" class="rounded"
                     onerror="this.onerror=null;this.src='https://placehold.co/60x90/ece3cd/16241f?text=--'">
                <div class="flex-grow-1">
                    <div class="small fw-semibold">${k.kitapAdi}</div>
                    <div class="small text-muted">${EK.fiyatFormat(birimFiyat)} / adet</div>
                    <div class="input-group input-group-sm mt-1" style="max-width:110px">
                        <button class="btn btn-outline-secondary ek-azalt" type="button" data-kitap-id="${k.kitapId}" data-adet="${k.adet}">-</button>
                        <input type="text" class="form-control text-center ek-adet" value="${k.adet}" readonly>
                        <button class="btn btn-outline-secondary ek-artir" type="button" data-kitap-id="${k.kitapId}" data-adet="${k.adet}">+</button>
                    </div>
                </div>
                <button class="btn btn-sm btn-outline-danger ek-kaldir" data-kitap-id="${k.kitapId}" title="Kaldır">&times;</button>
            </div>`;
        }).join('');

        liste.querySelectorAll('.ek-azalt').forEach(btn => btn.addEventListener('click', async () => {
            const adet = parseInt(btn.dataset.adet, 10);
            await EK.adetGuncelle(parseInt(btn.dataset.kitapId, 10), adet - 1 <= 0 ? 1 : adet - 1);
            sepetiRenderla();
        }));
        liste.querySelectorAll('.ek-artir').forEach(btn => btn.addEventListener('click', async () => {
            const adet = parseInt(btn.dataset.adet, 10);
            await EK.adetGuncelle(parseInt(btn.dataset.kitapId, 10), adet + 1);
            sepetiRenderla();
        }));
        liste.querySelectorAll('.ek-kaldir').forEach(btn => btn.addEventListener('click', async () => {
            await EK.sepettenCikar(parseInt(btn.dataset.kitapId, 10));
            sepetiRenderla();
        }));

        toplamEl.textContent = EK.fiyatFormat(EK.sepetToplam(sepet));
    }

    async function siparisiTamamla() {
        const btn = document.getElementById('ekSiparisTamamlaBtn');
        btn.disabled = true;
        btn.textContent = 'İşleniyor...';

        try {
            const res = await fetch('api/siparis', { method: 'POST' });

            if (res.status === 401) {
                window.location.href = 'login.jsp?sonraki=index.html';
                return;
            }

            const sonuc = await res.json();

            if (res.ok && sonuc.basarili) {
                EK.toast('Siparişiniz oluşturuldu! Sipariş No: ' + sonuc.siparisId, 'success');
                sepetiRenderla();
                kitaplariYukle();
            } else {
                EK.toast(sonuc.mesaj || 'Sipariş oluşturulamadı.', 'error');
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Siparişi Tamamla';
        }
    }

    document.getElementById('ekSiparisTamamlaBtn').addEventListener('click', siparisiTamamla);

    document.addEventListener('DOMContentLoaded', () => {
        kitaplariYukle();
        sepetiRenderla();

        const offcanvas = document.getElementById('ekSepetOffcanvas');
        if (offcanvas) {
            offcanvas.addEventListener('show.bs.offcanvas', sepetiRenderla);
        }
    });
})();
