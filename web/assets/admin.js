/* ============================================================
   BOOMBOOK — Admin Paneli JS
   Sekmeler: Dashboard, Kitaplar, Yazarlar, Kategoriler, Yayinevleri,
   Siparisler, Kullanicilar, Stok & Indirim Araclari.
   ============================================================ */

(() => {
    let yazarlarCache = [];
    let kategorilerCache = [];
    let yayinevleriCache = [];
    let gelirGrafik = null;

    /* ================= CANLI YENILEME (POLLING) =================
       Dashboard ve Siparisler sekmeleri, ekranda acikken belirli araliklarla
       kendini otomatik yeniler (yeni siparisleri / guncel istatistikleri
       sayfa yenilemeden gormek icin). Sekmeden ayrilinca zamanlayici durur,
       boylece gereksiz istek atilmaz. */
    const CANLI_YENILEME_ARALIGI_MS = 20000;
    let canliZamanlayici = null;

    function saatDamgasi() {
        return 'Son güncelleme: ' + new Date().toLocaleTimeString('tr-TR');
    }

    function canliYenilemeyiDurdur() {
        if (canliZamanlayici) {
            clearInterval(canliZamanlayici);
            canliZamanlayici = null;
        }
    }

    function canliYenilemeyiBaslat(fonksiyon) {
        canliYenilemeyiDurdur();
        canliZamanlayici = setInterval(fonksiyon, CANLI_YENILEME_ARALIGI_MS);
    }

    /* ================= SEKME GECISI ================= */
    function sekmeleriHazirla() {
        const navItems = document.querySelectorAll('.bb-admin-nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => {
                navItems.forEach(n => n.classList.remove('bb-active'));
                item.classList.add('bb-active');
                document.querySelectorAll('.bb-admin-tab').forEach(t => t.classList.remove('bb-active'));
                document.getElementById('tab-' + item.dataset.tab).classList.add('bb-active');

                canliYenilemeyiDurdur();

                if (item.dataset.tab === 'dashboard') {
                    dashboardYukle();
                    canliYenilemeyiBaslat(dashboardYukle);
                }
                if (item.dataset.tab === 'duyurular') duyurulariYukle();
                if (item.dataset.tab === 'kitaplar') kitaplariYukle();
                if (item.dataset.tab === 'yazarlar') yazarlariYukle();
                if (item.dataset.tab === 'kategoriler') kategorileriYukle();
                if (item.dataset.tab === 'yayinevleri') yayinevleriniYukle();
                if (item.dataset.tab === 'siparisler') {
                    siparisleriYukle();
                    canliYenilemeyiBaslat(siparisleriYukle);
                }
                if (item.dataset.tab === 'kullanicilar') kullanicilariYukle();
            });
        });
    }

    /* ================= YARDIMCI ================= */
    function hataGoster(hedefId, mesaj) {
        const el = document.getElementById(hedefId);
        if (el) el.innerHTML = `<tr><td colspan="10" class="text-danger text-center py-3">${mesaj}</td></tr>`;
    }

    async function ortakSecimleriYukle() {
        try {
            const [yazRes, katRes, yayRes] = await Promise.all([
                fetch('api/yazarlar'), fetch('api/kategoriler'), fetch('api/yayinevleri')
            ]);
            yazarlarCache = await yazRes.json();
            kategorilerCache = await katRes.json();
            yayinevleriCache = await yayRes.json();

            const doldur = (selectId, liste, idAlan, adAlan) => {
                const sel = document.getElementById(selectId);
                const mevcutDeger = sel.value;
                sel.innerHTML = '<option value="">— Seçiniz —</option>' +
                    liste.map(item => `<option value="${item[idAlan]}">${item[adAlan]}</option>`).join('');
                if (mevcutDeger) sel.value = mevcutDeger;
            };
            doldur('bbKitapYazar', yazarlarCache, 'yazarId', 'yazarAdi');
            doldur('bbKitapKategori', kategorilerCache, 'kategoriId', 'kategoriAdi');
            doldur('bbKitapYayinevi', yayinevleriCache, 'yayineviId', 'yayineviAdi');
        } catch (err) {
            EK.toast('Seçim listeleri yüklenemedi: ' + err.message, 'error');
        }
    }

    /* Kitaplar tablosunun ustundeki "Tum Kategoriler" filtre dropdown'unu doldurur.
       ortakSecimleriYukle() zaten kategorilerCache'i doldurdugu icin ek bir istek atmiyor. */
    async function kitapKategoriFiltresiniDoldur() {
        if (kategorilerCache.length === 0) {
            try {
                kategorilerCache = await (await fetch('api/kategoriler')).json();
            } catch (err) {
                return; // sessizce vazgec, filtre "Tum Kategoriler" olarak kalir
            }
        }
        const sel = document.getElementById('bbKitapKategoriFiltre');
        sel.innerHTML = '<option value="">Tüm Kategoriler</option>' +
            kategorilerCache.map(k => `<option value="${k.kategoriId}">${k.kategoriAdi}</option>`).join('');
    }

    /* ================= DASHBOARD ================= */
    async function dashboardYukle() {
        try {
            const res = await fetch('api/admin/istatistik');
            const ist = await res.json();

            const kartlar = document.querySelectorAll('#bbIstatKartlar .bb-stat-value');
            kartlar[0].textContent = EK.fiyatFormat(ist.toplamSatisTutari);
            kartlar[1].textContent = ist.toplamTamamlananSiparis;
            kartlar[2].textContent = ist.toplamKullanici;
            kartlar[3].textContent = ist.toplamKitap;

            const govde = document.getElementById('bbEnCokSatanGovde');
            if (!ist.enCokSatanlar || ist.enCokSatanlar.length === 0) {
                govde.innerHTML = '<tr><td colspan="2" class="text-muted text-center">Henüz tamamlanmış satış yok.</td></tr>';
            } else {
                govde.innerHTML = ist.enCokSatanlar.map(k => `<tr><td>${k.kitapAdi}</td><td>${k.satisAdedi}</td></tr>`).join('');
            }

            const etiketler = (ist.aylikGelir || []).map(a => a.ay);
            const degerler = (ist.aylikGelir || []).map(a => a.tutar);
            const ctx = document.getElementById('bbGelirGrafik');
            if (gelirGrafik) gelirGrafik.destroy();
            gelirGrafik = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: etiketler.length ? etiketler : ['Veri yok'],
                    datasets: [{
                        label: 'Gelir (TL)',
                        data: degerler.length ? degerler : [0],
                        backgroundColor: '#2b2a6b',
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });

            const damga = document.getElementById('bbDashGuncellendi');
            if (damga) damga.textContent = saatDamgasi();
        } catch (err) {
            EK.toast('İstatistikler yüklenemedi: ' + err.message, 'error');
        }
    }

    /* ================= DUYURULAR (Hero Slider) ================= */
    let duyuruTumListe = [];
    let duyuruAktifFiltre = 'hepsi';

    const DUYURU_TUR_ETIKET = { slider: '🖼️ Slider', kart: '🏷️ Kart' };

    async function duyurulariYukle() {
        try {
            const res = await fetch('api/admin/duyurular');
            const liste = await res.json();
            duyuruTumListe = Array.isArray(liste) ? liste : [];
            duyuruTablosunuCiz();
        } catch (err) {
            hataGoster('bbDuyuruGovde', 'Duyurular yüklenemedi: ' + err.message);
        }
    }

    function duyuruTablosunuCiz() {
        const govde = document.getElementById('bbDuyuruGovde');
        const liste = duyuruAktifFiltre === 'hepsi'
            ? duyuruTumListe
            : duyuruTumListe.filter(d => (d.tur || 'slider') === duyuruAktifFiltre);

        if (liste.length === 0) {
            govde.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-3">Bu türde henüz duyuru/kampanya kartı eklenmedi.</td></tr>';
            return;
        }
        govde.innerHTML = liste.map(d => `
            <tr>
                <td>${d.resimUrl
                    ? `<img src="${d.resimUrl}" class="bb-admin-thumb" style="width:56px;height:32px">`
                    : `<div class="bb-admin-thumb" style="width:56px;height:32px;background:linear-gradient(120deg,#2b2a6b,#3a1f35)" title="Resim yüklenmedi"></div>`}</td>
                <td><span class="bb-tur-badge">${DUYURU_TUR_ETIKET[d.tur] || DUYURU_TUR_ETIKET.slider}</span></td>
                <td>${d.sira}</td>
                <td>${EK.escapeHtml(d.baslik)}</td>
                <td class="text-muted small">${EK.escapeHtml(d.butonMetni)} → ${EK.escapeHtml(d.butonLink)}</td>
                <td><span class="bb-status-badge ${d.aktif ? 'bb-status-aktif' : 'bb-status-pasif'}">${d.aktif ? 'Aktif' : 'Pasif'}</span></td>
                <td>
                    <button class="bb-icon-action bb-duyuru-duzenle" data-id="${d.duyuruId}" title="Düzenle">✏️</button>
                    <button class="bb-icon-action bb-danger bb-duyuru-sil" data-id="${d.duyuruId}" data-baslik="${EK.escapeHtml(d.baslik)}" title="Sil">🗑️</button>
                </td>
            </tr>`).join('');

        govde.querySelectorAll('.bb-duyuru-duzenle').forEach(btn =>
            btn.addEventListener('click', () => duyuruModalAc(parseInt(btn.dataset.id, 10), duyuruTumListe)));
        govde.querySelectorAll('.bb-duyuru-sil').forEach(btn =>
            btn.addEventListener('click', () => duyuruSil(parseInt(btn.dataset.id, 10), btn.dataset.baslik)));
    }

    function duyuruModalAc(id, liste) {
        document.getElementById('bbDuyuruId').value = id || '';
        document.getElementById('bbDuyuruModalBaslik').textContent = id ? 'Duyuruyu Düzenle' : 'Yeni Duyuru';

        const kayit = id ? liste.find(d => d.duyuruId === id) : null;
        document.getElementById('bbDuyuruTur').value = kayit ? (kayit.tur || 'slider')
            : (duyuruAktifFiltre === 'kart' ? 'kart' : 'slider');
        document.getElementById('bbDuyuruBaslik').value = kayit ? kayit.baslik : '';
        document.getElementById('bbDuyuruAciklama').value = kayit ? (kayit.aciklama || '') : '';
        document.getElementById('bbDuyuruButonMetni').value = kayit ? kayit.butonMetni : 'Keşfet';
        document.getElementById('bbDuyuruButonLink').value = kayit ? kayit.butonLink : 'katalog.html';
        document.getElementById('bbDuyuruSira').value = kayit ? kayit.sira : 0;
        document.getElementById('bbDuyuruAktif').checked = kayit ? kayit.aktif : true;

        document.getElementById('bbDuyuruResimSonuc').textContent = '';
        document.getElementById('bbDuyuruResimInput').value = '';
        const onizleme = document.getElementById('bbDuyuruResimOnizleme');
        if (kayit && kayit.resimUrl) {
            onizleme.src = kayit.resimUrl;
            onizleme.style.display = 'block';
        } else {
            onizleme.src = '';
            onizleme.style.display = 'none';
        }

        new bootstrap.Modal(document.getElementById('bbDuyuruModal')).show();
    }

    async function duyuruKaydet() {
        const baslik = document.getElementById('bbDuyuruBaslik').value.trim();
        if (!baslik) { EK.toast('Başlık zorunludur.', 'error'); return; }

        const dosya = document.getElementById('bbDuyuruResimInput').files[0];
        if (dosya) {
            const uzanti = dosyaUzantisi(dosya.name);
            if (!IZINLI_KAPAK_UZANTI.includes(uzanti)) {
                EK.toast('Geçersiz resim türü. İzin verilenler: ' + IZINLI_KAPAK_UZANTI.join(', '), 'error');
                return;
            }
            if (dosya.size > MAKS_KAPAK_MB * 1024 * 1024) {
                EK.toast('Resim çok büyük (maksimum ' + MAKS_KAPAK_MB + ' MB).', 'error');
                return;
            }
        }

        const id = document.getElementById('bbDuyuruId').value;
        const govde = {
            tur: document.getElementById('bbDuyuruTur').value,
            baslik,
            aciklama: document.getElementById('bbDuyuruAciklama').value.trim(),
            butonMetni: document.getElementById('bbDuyuruButonMetni').value.trim() || 'Keşfet',
            butonLink: document.getElementById('bbDuyuruButonLink').value.trim() || 'katalog.html',
            sira: parseInt(document.getElementById('bbDuyuruSira').value, 10) || 0,
            aktif: document.getElementById('bbDuyuruAktif').checked
        };

        const kaydetBtn = document.getElementById('bbDuyuruKaydetBtn');
        kaydetBtn.disabled = true;
        kaydetBtn.textContent = dosya ? 'Kaydediliyor + resim yükleniyor…' : 'Kaydediliyor…';

        try {
            const url = id ? 'api/admin/duyurular?id=' + id : 'api/admin/duyurular';
            const method = id ? 'PUT' : 'POST';
            const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(govde) });
            const sonuc = await res.json();
            if (!res.ok) { EK.toast(sonuc.mesaj || 'Kaydedilemedi.', 'error'); return; }

            const kaydedilenId = id || sonuc.duyuruId || sonuc.id;

            // Resim secildiyse, kayit basarili olur olmaz OTOMATIK yukle - ayri bir
            // "Yukle" tiklamasina gerek yok, tek "Kaydet" tiklamasi yeterli.
            if (dosya) {
                const yuklemeSonucu = await duyuruResmininiYukle(kaydedilenId, dosya);
                if (!yuklemeSonucu) {
                    EK.toast('Duyuru kaydedildi ama resim yüklenirken sorun oldu, tekrar dener misiniz?', 'error');
                    duyurulariYukle();
                    return;
                }
            }

            EK.toast('Duyuru ' + (id ? 'güncellendi.' : 'eklendi.') + (dosya ? ' Resim de yüklendi.' : ''), 'success');
            bootstrap.Modal.getInstance(document.getElementById('bbDuyuruModal'))?.hide();
            duyurulariYukle();
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        } finally {
            kaydetBtn.disabled = false;
            kaydetBtn.textContent = 'Kaydet';
        }
    }

    /** Verilen duyuruId'ye resmi yukler. duyuruKaydet() tarafindan otomatik cagirilir.
     *  Basarili olursa true, olmazsa hata toast'i gosterip false doner. */
    async function duyuruResmininiYukle(duyuruId, dosya) {
        const formData = new FormData();
        formData.append('duyuruId', duyuruId);
        formData.append('tur', 'duyuru');
        formData.append('dosya', dosya);
        try {
            const res = await fetch('api/admin/yukle', { method: 'POST', body: formData });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) return true;
            EK.toast(sonuc.mesaj || 'Resim yüklenemedi.', 'error');
            return false;
        } catch (err) {
            EK.toast('Resim yükleme bağlantı hatası: ' + err.message, 'error');
            return false;
        }
    }

    async function duyuruSil(id, baslik) {
        if (!confirm(`"${baslik}" duyurusunu silmek istediğinize emin misiniz?`)) return;
        try {
            const res = await fetch('api/admin/duyurular?id=' + id, { method: 'DELETE' });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) { EK.toast('Duyuru silindi.', 'success'); duyurulariYukle(); }
            else EK.toast(sonuc.mesaj || 'Silinemedi.', 'error');
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    /* ================= KITAPLAR ================= */
    let kitapAramaZamanlayici = null;
    let kullaniciAramaZamanlayici = null;

    let kitapSayfaDurumu = { sayfa: 1, sayfaBoyutu: 20, sirala: 'enYeni', kategoriId: '', q: '' };

    async function kitaplariYukle() {
        const govde = document.getElementById('bbKitapGovde');
        kitapSayfaDurumu.q = document.getElementById('bbKitapAra').value.trim();
        try {
            const p = new URLSearchParams({
                sayfa: kitapSayfaDurumu.sayfa,
                sayfaBoyutu: kitapSayfaDurumu.sayfaBoyutu,
                sirala: kitapSayfaDurumu.sirala
            });
            if (kitapSayfaDurumu.q) p.set('q', kitapSayfaDurumu.q);
            if (kitapSayfaDurumu.kategoriId) p.set('kategoriId', kitapSayfaDurumu.kategoriId);

            const res = await fetch('api/admin/kitaplar?' + p.toString());
            const sonuc = await res.json();
            const liste = sonuc.kitaplar || [];

            if (liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="7" class="text-muted text-center py-3">Kitap bulunamadı.</td></tr>';
                document.getElementById('bbKitapSayfalama').innerHTML = '';
                return;
            }

            govde.innerHTML = liste.map(k => `
                <tr>
                    <td>${k.kapakResmiUrl ? `<img src="${k.kapakResmiUrl}" class="bb-admin-thumb">` : `<div class="bb-admin-thumb"></div>`}</td>
                    <td>${k.kitapAdi}</td>
                    <td>${k.yazarAdi || '-'}</td>
                    <td>${k.yayineviAdi || '-'}</td>
                    <td>${EK.fiyatFormat(k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat)}</td>
                    <td>${k.stokMiktari <= 3 ? `<span class="bb-status-badge bb-status-basarisiz">${k.stokMiktari}</span>` : k.stokMiktari}</td>
                    <td>
                        <button class="bb-icon-action bb-kitap-duzenle" data-id="${k.kitapId}" title="Düzenle">✏️</button>
                        <button class="bb-icon-action bb-danger bb-kitap-sil" data-id="${k.kitapId}" data-ad="${k.kitapAdi}" title="Sil">🗑️</button>
                    </td>
                </tr>`).join('');

            govde.querySelectorAll('.bb-kitap-duzenle').forEach(btn =>
                btn.addEventListener('click', () => kitapModalAc(parseInt(btn.dataset.id, 10))));
            govde.querySelectorAll('.bb-kitap-sil').forEach(btn =>
                btn.addEventListener('click', () => kitapSil(parseInt(btn.dataset.id, 10), btn.dataset.ad)));

            sayfalamaCiz('bbKitapSayfalama', sonuc.sayfa, sonuc.toplamSayfa, sonuc.toplamKayit, (yeniSayfa) => {
                kitapSayfaDurumu.sayfa = yeniSayfa;
                kitaplariYukle();
            });
        } catch (err) {
            hataGoster('bbKitapGovde', 'Kitaplar yüklenemedi: ' + err.message);
        }
    }

    /* Genel amacli sayfalama cizici: onceki/sonraki + sayfa numaralari + "X-Y / Z kayit". */
    function sayfalamaCiz(containerId, mevcutSayfa, toplamSayfa, toplamKayit, tiklandiginda) {
        const el = document.getElementById(containerId);
        if (!el) return;
        if (!toplamSayfa || toplamSayfa <= 1) {
            el.innerHTML = toplamKayit ? `<span class="text-muted small">${toplamKayit} kayıt</span>` : '';
            return;
        }

        const sayfaNumaralari = [];
        const baslangic = Math.max(1, mevcutSayfa - 2);
        const bitis = Math.min(toplamSayfa, baslangic + 4);
        for (let i = baslangic; i <= bitis; i++) sayfaNumaralari.push(i);

        el.innerHTML = `
            <span class="text-muted small me-2">Toplam ${toplamKayit} kayıt · Sayfa ${mevcutSayfa}/${toplamSayfa}</span>
            <button class="bb-sayfa-btn" data-sayfa="${mevcutSayfa - 1}" ${mevcutSayfa <= 1 ? 'disabled' : ''}>‹</button>
            ${sayfaNumaralari.map(n => `<button class="bb-sayfa-btn ${n === mevcutSayfa ? 'bb-active' : ''}" data-sayfa="${n}">${n}</button>`).join('')}
            <button class="bb-sayfa-btn" data-sayfa="${mevcutSayfa + 1}" ${mevcutSayfa >= toplamSayfa ? 'disabled' : ''}>›</button>
        `;
        el.querySelectorAll('.bb-sayfa-btn:not([disabled])').forEach(btn =>
            btn.addEventListener('click', () => tiklandiginda(parseInt(btn.dataset.sayfa, 10))));
    }

    async function kitapSil(id, ad) {
        if (!confirm(`"${ad}" kitabını silmek istediğinize emin misiniz?`)) return;
        try {
            const res = await fetch('api/admin/kitaplar?id=' + id, { method: 'DELETE' });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                EK.toast('Kitap silindi.', 'success');
                kitaplariYukle();
            } else {
                EK.toast(sonuc.mesaj || 'Silinemedi.', 'error');
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    async function kitapModalAc(id) {
        await ortakSecimleriYukle();
        const form = document.getElementById('bbKitapForm');
        form.reset();
        document.getElementById('bbKitapId').value = '';
        document.getElementById('bbKitapYuklemeSonuc').innerHTML = '';
        document.getElementById('bbKitapKapakInput').value = '';
        document.getElementById('bbKitapDosyaInput').value = '';
        document.getElementById('bbKitapKapakOnizleme').style.display = 'none';

        if (id) {
            document.getElementById('bbKitapModalBaslik').textContent = 'Kitabı Düzenle';
            try {
                const res = await fetch('api/admin/kitaplar?id=' + id);
                const k = await res.json();
                document.getElementById('bbKitapId').value = k.kitapId;
                document.getElementById('bbKitapAdi').value = k.kitapAdi || '';
                document.getElementById('bbKitapYazar').value = k.yazarId || '';
                document.getElementById('bbKitapKategori').value = k.kategoriId || '';
                document.getElementById('bbKitapYayinevi').value = k.yayineviId || '';
                document.getElementById('bbKitapFiyat').value = k.fiyat;
                document.getElementById('bbKitapIndirimliFiyat').value = k.indirimliFiyat ?? '';
                document.getElementById('bbKitapStok').value = k.stokMiktari;
                document.getElementById('bbKitapSayfaSayisi').value = k.sayfaSayisi || '';
                document.getElementById('bbKitapDosyaFormati').value = k.dosyaFormati || 'PDF';
                document.getElementById('bbKitapYayinTarihi').value = k.yayinTarihi || '';
                document.getElementById('bbKitapAciklama').value = k.aciklama || '';

                if (k.kapakResmiUrl) {
                    const img = document.getElementById('bbKitapKapakOnizleme');
                    img.src = k.kapakResmiUrl;
                    img.style.display = 'block';
                }
            } catch (err) {
                EK.toast('Kitap bilgisi alınamadı: ' + err.message, 'error');
                return;
            }
        } else {
            document.getElementById('bbKitapModalBaslik').textContent = 'Yeni Kitap';
        }

        new bootstrap.Modal(document.getElementById('bbKitapModal')).show();
    }

    function kitapFormdanGovdeOlustur() {
        return {
            kitapAdi: document.getElementById('bbKitapAdi').value.trim(),
            yazarId: document.getElementById('bbKitapYazar').value ? parseInt(document.getElementById('bbKitapYazar').value, 10) : null,
            kategoriId: document.getElementById('bbKitapKategori').value ? parseInt(document.getElementById('bbKitapKategori').value, 10) : null,
            yayineviId: document.getElementById('bbKitapYayinevi').value ? parseInt(document.getElementById('bbKitapYayinevi').value, 10) : null,
            fiyat: parseFloat(document.getElementById('bbKitapFiyat').value),
            indirimliFiyat: document.getElementById('bbKitapIndirimliFiyat').value ? parseFloat(document.getElementById('bbKitapIndirimliFiyat').value) : null,
            stokMiktari: parseInt(document.getElementById('bbKitapStok').value, 10),
            sayfaSayisi: document.getElementById('bbKitapSayfaSayisi').value ? parseInt(document.getElementById('bbKitapSayfaSayisi').value, 10) : 0,
            dosyaFormati: document.getElementById('bbKitapDosyaFormati').value,
            yayinTarihi: document.getElementById('bbKitapYayinTarihi').value || null,
            aciklama: document.getElementById('bbKitapAciklama').value.trim()
        };
    }

    async function kitapKaydet() {
        const form = document.getElementById('bbKitapForm');
        if (!form.reportValidity()) return;

        const id = document.getElementById('bbKitapId').value;
        const govde = kitapFormdanGovdeOlustur();
        const kapakDosyasi = document.getElementById('bbKitapKapakInput').files[0];
        const ekitapDosyasi = document.getElementById('bbKitapDosyaInput').files[0];
        const btn = document.getElementById('bbKitapKaydetBtn');
        const sonucEl = document.getElementById('bbKitapYuklemeSonuc');
        sonucEl.innerHTML = '';
        btn.disabled = true;
        btn.textContent = (kapakDosyasi || ekitapDosyasi) ? 'Kaydediliyor + dosyalar yükleniyor…' : 'Kaydediliyor…';

        try {
            const url = id ? 'api/admin/kitaplar?id=' + id : 'api/admin/kitaplar';
            const method = id ? 'PUT' : 'POST';
            const res = await fetch(url, {
                method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(govde)
            });
            const sonuc = await res.json();

            if (!res.ok) {
                EK.toast(sonuc.mesaj || 'Kaydedilemedi.', 'error');
                return;
            }

            const kaydedilenId = id || sonuc.kitapId;

            // Kapak/e-kitap dosyasi secildiyse, kayit basarili olur olmaz OTOMATIK
            // yukle - ayri "Yukle" tiklamalarina gerek yok, tek "Kaydet" yeterli.
            let dosyaHatasi = false;
            if (kapakDosyasi) {
                const basarili = await kitapDosyaYukle(kaydedilenId, 'kapak', kapakDosyasi);
                if (!basarili) dosyaHatasi = true;
            }
            if (ekitapDosyasi) {
                const basarili = await kitapDosyaYukle(kaydedilenId, 'dosya', ekitapDosyasi);
                if (!basarili) dosyaHatasi = true;
            }

            if (dosyaHatasi) {
                EK.toast('Kitap kaydedildi ama bazı dosyalar yüklenemedi, kitabı tekrar açıp deneyin.', 'error');
                kitaplariYukle();
                return;
            }

            EK.toast('Kitap ' + (id ? 'güncellendi.' : 'eklendi.') + ((kapakDosyasi || ekitapDosyasi) ? ' Dosyalar da yüklendi.' : ''), 'success');
            bootstrap.Modal.getInstance(document.getElementById('bbKitapModal'))?.hide();
            kitaplariYukle();
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Kaydet';
        }
    }

    const IZINLI_KAPAK_UZANTI = ['jpg', 'jpeg', 'png', 'webp'];
    const IZINLI_EKITAP_UZANTI = ['pdf', 'epub', 'mobi'];
    const MAKS_KAPAK_MB = 8;
    const MAKS_EKITAP_MB = 50;

    function dosyaUzantisi(ad) {
        const parca = (ad || '').split('.');
        return parca.length > 1 ? parca.pop().toLowerCase() : '';
    }

    function dosyaSeciminiOnizle(inputId, onizlemeImgId) {
        const input = document.getElementById(inputId);
        const img = document.getElementById(onizlemeImgId);
        if (!input || !img) return;
        input.addEventListener('change', () => {
            const dosya = input.files[0];
            if (!dosya) return;
            img.src = URL.createObjectURL(dosya);
            img.style.display = 'block';
        });
    }

    /** Verilen kitapId icin kapak/dosya yukler. kitapKaydet() tarafindan otomatik cagirilir.
     *  Basarili olursa true, olmazsa hata toast'i/mesaji gosterip false doner. */
    async function kitapDosyaYukle(kitapId, tur, dosya) {
        const sonucEl = document.getElementById('bbKitapYuklemeSonuc');
        const uzanti = dosyaUzantisi(dosya.name);
        const izinliListe = tur === 'kapak' ? IZINLI_KAPAK_UZANTI : IZINLI_EKITAP_UZANTI;
        const maksMb = tur === 'kapak' ? MAKS_KAPAK_MB : MAKS_EKITAP_MB;

        if (!izinliListe.includes(uzanti)) {
            EK.toast(`Geçersiz ${tur === 'kapak' ? 'kapak resmi' : 'e-kitap dosyası'} türü. İzin verilenler: ${izinliListe.join(', ')}`, 'error');
            return false;
        }
        if (dosya.size > maksMb * 1024 * 1024) {
            EK.toast(`Dosya çok büyük (maksimum ${maksMb} MB, seçilen: ${(dosya.size / 1024 / 1024).toFixed(1)} MB).`, 'error');
            return false;
        }

        const formData = new FormData();
        formData.append('kitapId', kitapId);
        formData.append('tur', tur);
        formData.append('dosya', dosya);

        try {
            const res = await fetch('api/admin/yukle', { method: 'POST', body: formData });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                if (sonucEl) sonucEl.innerHTML += `<div class="text-success">✓ ${sonuc.mesaj}</div>`;
                return true;
            }
            EK.toast(sonuc.mesaj || 'Yüklenemedi.', 'error');
            return false;
        } catch (err) {
            EK.toast('Dosya yükleme bağlantı hatası: ' + err.message, 'error');
            return false;
        }
    }

    /* ================= YAZARLAR / KATEGORILER / YAYINEVLERI (ortak basit CRUD) ================= */
    async function yazarlariYukle() {
        try {
            const res = await fetch('api/admin/yazarlar');
            const liste = await res.json();
            const govde = document.getElementById('bbYazarGovde');
            if (liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="3" class="text-muted text-center py-3">Yazar bulunamadı.</td></tr>';
                return;
            }
            govde.innerHTML = liste.map(y => `
                <tr>
                    <td>${y.yazarAdi}</td>
                    <td class="text-muted small">${(y.biyografi || '').slice(0, 80)}${(y.biyografi || '').length > 80 ? '…' : ''}</td>
                    <td>
                        <button class="bb-icon-action bb-basit-duzenle" data-tur="yazar" data-id="${y.yazarId}" title="Düzenle">✏️</button>
                        <button class="bb-icon-action bb-danger bb-basit-sil" data-tur="yazar" data-id="${y.yazarId}" data-ad="${y.yazarAdi}" title="Sil">🗑️</button>
                    </td>
                </tr>`).join('');
            basitAksiyonlariBagla();
        } catch (err) {
            hataGoster('bbYazarGovde', 'Yazarlar yüklenemedi: ' + err.message);
        }
    }

    async function kategorileriYukle() {
        try {
            const res = await fetch('api/admin/kategoriler');
            const liste = await res.json();
            const govde = document.getElementById('bbKategoriGovde');
            if (liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="2" class="text-muted text-center py-3">Kategori bulunamadı.</td></tr>';
                return;
            }
            govde.innerHTML = liste.map(k => `
                <tr>
                    <td>${k.kategoriAdi}</td>
                    <td>
                        <button class="bb-icon-action bb-basit-duzenle" data-tur="kategori" data-id="${k.kategoriId}" title="Düzenle">✏️</button>
                        <button class="bb-icon-action bb-danger bb-basit-sil" data-tur="kategori" data-id="${k.kategoriId}" data-ad="${k.kategoriAdi}" title="Sil">🗑️</button>
                    </td>
                </tr>`).join('');
            basitAksiyonlariBagla();
        } catch (err) {
            hataGoster('bbKategoriGovde', 'Kategoriler yüklenemedi: ' + err.message);
        }
    }

    async function yayinevleriniYukle() {
        try {
            const res = await fetch('api/admin/yayinevleri');
            const liste = await res.json();
            const govde = document.getElementById('bbYayineviGovde');
            if (liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="3" class="text-muted text-center py-3">Yayınevi bulunamadı.</td></tr>';
                return;
            }
            govde.innerHTML = liste.map(y => `
                <tr>
                    <td>${y.yayineviAdi}</td>
                    <td>${y.sonIndirimOrani != null ? '%' + y.sonIndirimOrani : '-'}</td>
                    <td>
                        <button class="bb-icon-action bb-basit-duzenle" data-tur="yayinevi" data-id="${y.yayineviId}" title="Düzenle">✏️</button>
                        <button class="bb-icon-action bb-danger bb-basit-sil" data-tur="yayinevi" data-id="${y.yayineviId}" data-ad="${y.yayineviAdi}" title="Sil">🗑️</button>
                    </td>
                </tr>`).join('');
            basitAksiyonlariBagla();
        } catch (err) {
            hataGoster('bbYayineviGovde', 'Yayınevleri yüklenemedi: ' + err.message);
        }
    }

    function turBilgisi(tur) {
        return {
            yazar: { yol: 'yazarlar', adAlan: 'yazarAdi', idAlan: 'yazarId', baslik: 'Yazar', biyografiVar: true },
            kategori: { yol: 'kategoriler', adAlan: 'kategoriAdi', idAlan: 'kategoriId', baslik: 'Kategori', biyografiVar: false },
            yayinevi: { yol: 'yayinevleri', adAlan: 'yayineviAdi', idAlan: 'yayineviId', baslik: 'Yayınevi', biyografiVar: false }
        }[tur];
    }

    function basitAksiyonlariBagla() {
        document.querySelectorAll('.bb-basit-duzenle').forEach(btn => {
            btn.onclick = () => basitModalAc(btn.dataset.tur, parseInt(btn.dataset.id, 10));
        });
        document.querySelectorAll('.bb-basit-sil').forEach(btn => {
            btn.onclick = () => basitSil(btn.dataset.tur, parseInt(btn.dataset.id, 10), btn.dataset.ad);
        });
    }

    async function basitModalAc(tur, id) {
        const bilgi = turBilgisi(tur);
        document.getElementById('bbBasitTur').value = tur;
        document.getElementById('bbBasitId').value = id || '';
        document.getElementById('bbBasitAdLabel').textContent = bilgi.baslik + ' Adı';
        document.getElementById('bbBasitAd').value = '';
        document.getElementById('bbBasitBiyografi').value = '';
        document.getElementById('bbBasitBiyografiWrap').classList.toggle('d-none', !bilgi.biyografiVar);
        document.getElementById('bbBasitModalBaslik').textContent = id ? bilgi.baslik + ' Düzenle' : 'Yeni ' + bilgi.baslik;

        if (id) {
            const kaynakListe = tur === 'yazar' ? yazarlarCache : tur === 'kategori' ? kategorilerCache : yayinevleriCache;
            const kayit = kaynakListe.find(item => item[bilgi.idAlan] === id);
            if (kayit) {
                document.getElementById('bbBasitAd').value = kayit[bilgi.adAlan] || '';
                if (bilgi.biyografiVar) document.getElementById('bbBasitBiyografi').value = kayit.biyografi || '';
            }
        }

        new bootstrap.Modal(document.getElementById('bbBasitModal')).show();
    }

    async function basitSil(tur, id, ad) {
        const bilgi = turBilgisi(tur);
        if (!confirm(`"${ad}" ${bilgi.baslik.toLowerCase()}sini silmek istediğinize emin misiniz?\n(Bu ${bilgi.baslik.toLowerCase()}ye bağlı kitaplar silinmez, sadece bağlantıları kaldırılır.)`)) return;
        try {
            const res = await fetch(`api/admin/${bilgi.yol}?id=${id}`, { method: 'DELETE' });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                EK.toast(sonuc.mesaj, 'success');
                yenidenYukle(tur);
            } else {
                EK.toast(sonuc.mesaj || 'Silinemedi.', 'error');
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    function yenidenYukle(tur) {
        if (tur === 'yazar') yazarlariYukle();
        if (tur === 'kategori') kategorileriYukle();
        if (tur === 'yayinevi') yayinevleriniYukle();
    }

    async function basitKaydet() {
        const tur = document.getElementById('bbBasitTur').value;
        const bilgi = turBilgisi(tur);
        const id = document.getElementById('bbBasitId').value;
        const ad = document.getElementById('bbBasitAd').value.trim();
        const biyografi = document.getElementById('bbBasitBiyografi').value.trim();

        if (!ad) { EK.toast(bilgi.baslik + ' adı zorunludur.', 'error'); return; }

        const govde = { [bilgi.adAlan]: ad };
        if (bilgi.biyografiVar) govde.biyografi = biyografi;

        try {
            const url = id ? `api/admin/${bilgi.yol}?id=${id}` : `api/admin/${bilgi.yol}`;
            const method = id ? 'PUT' : 'POST';
            const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(govde) });
            const sonuc = await res.json();
            if (!res.ok) { EK.toast(sonuc.mesaj || 'Kaydedilemedi.', 'error'); return; }

            EK.toast(bilgi.baslik + (id ? ' güncellendi.' : ' eklendi.'), 'success');
            bootstrap.Modal.getInstance(document.getElementById('bbBasitModal'))?.hide();
            yenidenYukle(tur);
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    /* ================= SIPARISLER ================= */
    function tarihFormat(iso) {
        if (!iso) return '-';
        const d = new Date(iso.replace(' ', 'T'));
        return isNaN(d) ? iso : d.toLocaleString('tr-TR');
    }

    async function siparisleriYukle() {
        try {
            const res = await fetch('api/admin/siparisler');
            const liste = await res.json();
            const govde = document.getElementById('bbSiparisGovde');
            const damga = document.getElementById('bbSiparisGuncellendi');
            if (liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="5" class="text-muted text-center py-3">Sipariş bulunamadı.</td></tr>';
                if (damga) damga.textContent = saatDamgasi();
                return;
            }
            govde.innerHTML = liste.map(s => `
                <tr>
                    <td>
                        <button type="button" class="bb-icon-action bb-siparis-detay-ac" data-id="${s.siparisId}" title="Kalemleri Göster">▸</button>
                        #${s.siparisId}
                    </td>
                    <td>${EK.escapeHtml(s.kullaniciAdSoyad)}<div class="text-muted small">${EK.escapeHtml(s.kullaniciEmail)}</div></td>
                    <td>${tarihFormat(s.siparisTarihi)}</td>
                    <td>${EK.fiyatFormat(s.toplamTutar)}${s.kargoUcreti > 0 ? `<div class="text-muted small">(kargo: ${EK.fiyatFormat(s.kargoUcreti)})</div>` : ''}</td>
                    <td>
                        <select class="form-select form-select-sm bb-siparis-durum" data-id="${s.siparisId}" style="width:140px;display:inline-block">
                            <option value="Bekliyor" ${s.odemeDurumu === 'Bekliyor' ? 'selected' : ''}>Bekliyor</option>
                            <option value="Tamamlandi" ${s.odemeDurumu === 'Tamamlandi' ? 'selected' : ''}>Tamamlandı</option>
                            <option value="Basarisiz" ${s.odemeDurumu === 'Basarisiz' ? 'selected' : ''}>Başarısız</option>
                        </select>
                    </td>
                </tr>
                <tr class="bb-siparis-detay-satir d-none" id="bbSiparisDetay${s.siparisId}">
                    <td colspan="5" style="background:var(--bb-grey-light)">
                        <div class="p-2 small text-muted">Yükleniyor...</div>
                    </td>
                </tr>`).join('');

            govde.querySelectorAll('.bb-siparis-detay-ac').forEach(btn => {
                btn.addEventListener('click', () => siparisDetayiniAcKapat(btn));
            });

            govde.querySelectorAll('.bb-siparis-durum').forEach(sel => {
                sel.addEventListener('change', async () => {
                    try {
                        const res = await fetch('api/admin/siparisler?id=' + sel.dataset.id, {
                            method: 'PUT', headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ odemeDurumu: sel.value })
                        });
                        const sonuc = await res.json();
                        if (res.ok && sonuc.basarili) EK.toast('Sipariş durumu güncellendi.', 'success');
                        else EK.toast(sonuc.mesaj || 'Güncellenemedi.', 'error');
                    } catch (err) {
                        EK.toast('Bağlantı hatası: ' + err.message, 'error');
                    }
                });
            });

            if (damga) damga.textContent = saatDamgasi();
        } catch (err) {
            hataGoster('bbSiparisGovde', 'Siparişler yüklenemedi: ' + err.message);
        }
    }

    async function siparisDetayiniAcKapat(btn) {
        const id = btn.dataset.id;
        const satir = document.getElementById('bbSiparisDetay' + id);
        const acikMi = !satir.classList.contains('d-none');

        if (acikMi) {
            satir.classList.add('d-none');
            btn.textContent = '▸';
            return;
        }

        satir.classList.remove('d-none');
        btn.textContent = '▾';

        try {
            const res = await fetch('api/admin/siparisler?id=' + id);
            const siparis = await res.json();
            const detaylar = siparis.detaylar || [];

            satir.querySelector('td').innerHTML = detaylar.length === 0
                ? '<div class="p-2 small text-muted">Kalem bulunamadı.</div>'
                : `<table class="bb-admin-table mb-0"><thead><tr><th>Kitap</th><th>Birim Fiyat</th><th>Adet</th><th>Ara Toplam</th></tr></thead><tbody>` +
                    detaylar.map(d => `
                        <tr>
                            <td>${EK.escapeHtml(d.kitapAdi)}</td>
                            <td>${EK.fiyatFormat(d.birimFiyat)}</td>
                            <td>${d.adet}</td>
                            <td>${EK.fiyatFormat(d.birimFiyat * d.adet)}</td>
                        </tr>`).join('') +
                    `</tbody></table>`;
        } catch (err) {
            satir.querySelector('td').innerHTML = `<div class="p-2 small text-danger">Detay yüklenemedi: ${err.message}</div>`;
        }
    }

    /* ================= KULLANICILAR ================= */
    let kullaniciTumListe = [];
    let kullaniciSayfaDurumu = { sayfa: 1, sayfaBoyutu: 15, q: '', sirala: 'yeni' };

    async function kullanicilariYukle() {
        try {
            const res = await fetch('api/admin/kullanicilar');
            kullaniciTumListe = await res.json();
            kullaniciSayfaDurumu.sayfa = 1;
            kullaniciTablosunuCiz();
        } catch (err) {
            hataGoster('bbKullaniciGovde', 'Kullanıcılar yüklenemedi: ' + err.message);
        }
    }

    function kullaniciTablosunuCiz() {
        const govde = document.getElementById('bbKullaniciGovde');
        const q = kullaniciSayfaDurumu.q.toLocaleLowerCase('tr-TR');

        let liste = kullaniciTumListe.filter(k =>
            !q || k.adSoyad.toLocaleLowerCase('tr-TR').includes(q) || k.email.toLocaleLowerCase('tr-TR').includes(q));

        const siralayicilar = {
            yeni: (a, b) => new Date((b.kayitTarihi || '').replace(' ', 'T')) - new Date((a.kayitTarihi || '').replace(' ', 'T')),
            eski: (a, b) => new Date((a.kayitTarihi || '').replace(' ', 'T')) - new Date((b.kayitTarihi || '').replace(' ', 'T')),
            adAsc: (a, b) => a.adSoyad.localeCompare(b.adSoyad, 'tr'),
        };
        liste = liste.slice().sort(siralayicilar[kullaniciSayfaDurumu.sirala] || siralayicilar.yeni);

        const toplamKayit = liste.length;
        const toplamSayfa = Math.max(1, Math.ceil(toplamKayit / kullaniciSayfaDurumu.sayfaBoyutu));
        if (kullaniciSayfaDurumu.sayfa > toplamSayfa) kullaniciSayfaDurumu.sayfa = toplamSayfa;
        const baslangic = (kullaniciSayfaDurumu.sayfa - 1) * kullaniciSayfaDurumu.sayfaBoyutu;
        const sayfaListesi = liste.slice(baslangic, baslangic + kullaniciSayfaDurumu.sayfaBoyutu);

        if (sayfaListesi.length === 0) {
            govde.innerHTML = '<tr><td colspan="6" class="text-muted text-center py-3">Kullanıcı bulunamadı.</td></tr>';
            document.getElementById('bbKullaniciSayfalama').innerHTML = '';
            return;
        }
        govde.innerHTML = sayfaListesi.map(k => `
                <tr>
                    <td>${EK.escapeHtml(k.adSoyad)}</td>
                    <td>${EK.escapeHtml(k.email)}</td>
                    <td>
                        <select class="form-select form-select-sm bb-kullanici-rol" data-id="${k.kullaniciId}" style="width:110px;display:inline-block">
                            <option value="Musteri" ${k.rol === 'Musteri' ? 'selected' : ''}>Müşteri</option>
                            <option value="Admin" ${k.rol === 'Admin' ? 'selected' : ''}>Admin</option>
                        </select>
                    </td>
                    <td>
                        <span class="bb-status-badge ${k.aktif ? 'bb-status-aktif' : 'bb-status-pasif'} bb-kullanici-durum-etiket" data-id="${k.kullaniciId}" style="cursor:pointer" title="Değiştirmek için tıklayın">
                            ${k.aktif ? 'Aktif' : 'Pasif'}
                        </span>
                    </td>
                    <td class="text-muted small">${tarihFormat(k.kayitTarihi)}</td>
                    <td></td>
                </tr>`).join('');

        govde.querySelectorAll('.bb-kullanici-rol').forEach(sel => {
            sel.addEventListener('change', () => kullaniciGuncelle(sel.dataset.id, { rol: sel.value }));
        });
        govde.querySelectorAll('.bb-kullanici-durum-etiket').forEach(etiket => {
            etiket.addEventListener('click', () => {
                const yeniAktif = etiket.textContent.trim() !== 'Aktif';
                kullaniciGuncelle(etiket.dataset.id, { aktif: yeniAktif });
            });
        });

        sayfalamaCiz('bbKullaniciSayfalama', kullaniciSayfaDurumu.sayfa, toplamSayfa, toplamKayit, (yeniSayfa) => {
            kullaniciSayfaDurumu.sayfa = yeniSayfa;
            kullaniciTablosunuCiz();
        });
    }

    async function kullaniciGuncelle(id, govde) {
        try {
            const res = await fetch('api/admin/kullanicilar?id=' + id, {
                method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(govde)
            });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                EK.toast('Kullanıcı güncellendi.', 'success');
                kullanicilariYukle();
            } else {
                EK.toast(sonuc.mesaj || 'Güncellenemedi.', 'error');
                kullanicilariYukle();
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    /* ================= STOK & INDIRIM ARACLARI (mevcut ozellik) ================= */
    async function yayinevleriniDoldurEskiForm() {
        const select = document.getElementById('ekIndirimYayinevi');
        try {
            const res = await fetch('api/yayinevleri');
            const liste = await res.json();
            select.innerHTML = liste.length
                ? liste.map(y => `<option value="${y.yayineviId}">${y.yayineviAdi}</option>`).join('')
                : '<option value="">Yayınevi bulunamadı</option>';
        } catch (err) {
            select.innerHTML = '<option value="">Yüklenemedi</option>';
        }
    }

    async function stokTablosunuYukle() {
        const yukleniyor = document.getElementById('ekStokYukleniyor');
        const wrapper = document.getElementById('ekStokTabloWrapper');
        const govde = document.getElementById('ekStokGovde');

        yukleniyor.classList.remove('d-none');
        wrapper.classList.add('d-none');

        try {
            const res = await fetch('api/admin/stok');
            const liste = await res.json();
            yukleniyor.classList.add('d-none');

            if (!Array.isArray(liste) || liste.length === 0) {
                govde.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Kitap bulunamadı.</td></tr>';
                wrapper.classList.remove('d-none');
                return;
            }

            govde.innerHTML = liste.map(k => `
                <tr class="${k.stokMiktari < 5 ? 'bb-row-critical' : ''}">
                    <td>${k.kitapAdi}</td>
                    <td>${k.yayineviAdi || '-'}</td>
                    <td>${EK.fiyatFormat(k.fiyat)}</td>
                    <td>${k.indirimliFiyat != null ? EK.fiyatFormat(k.indirimliFiyat) : '<span class="text-muted">-</span>'}</td>
                    <td><span class="${k.stokMiktari < 5 ? 'bb-stock-badge-low' : 'bb-stock-badge-ok'}">${k.stokMiktari}</span></td>
                </tr>`).join('');

            wrapper.classList.remove('d-none');
        } catch (err) {
            yukleniyor.classList.add('d-none');
            govde.innerHTML = `<tr><td colspan="5" class="text-danger">Stok verisi alınamadı: ${err.message}</td></tr>`;
            wrapper.classList.remove('d-none');
        }
    }

    function stokIndirimAraclariHazirla() {
        document.getElementById('ekIndirimForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const yayineviId = parseInt(document.getElementById('ekIndirimYayinevi').value, 10);
            const oran = parseFloat(document.getElementById('ekIndirimOran').value);
            const sonucEl = document.getElementById('ekIndirimSonuc');
            sonucEl.innerHTML = '';

            if (!yayineviId) {
                sonucEl.innerHTML = '<div class="alert alert-warning py-2 mb-0">Lütfen bir yayınevi seçin.</div>';
                return;
            }
            try {
                const res = await fetch('api/admin/indirim', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ yayineviId, indirimOrani: oran })
                });
                const sonuc = await res.json();
                if (res.ok && sonuc.basarili) {
                    sonucEl.innerHTML = `<div class="alert alert-success py-2 mb-0">${sonuc.etkilenenKitapSayisi} kitap güncellendi.</div>`;
                    stokTablosunuYukle();
                } else {
                    sonucEl.innerHTML = `<div class="alert alert-danger py-2 mb-0">${sonuc.mesaj || 'İşlem başarısız.'}</div>`;
                }
            } catch (err) {
                sonucEl.innerHTML = `<div class="alert alert-danger py-2 mb-0">Bağlantı hatası: ${err.message}</div>`;
            }
        });

        document.getElementById('ekKritikForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const esikDeger = parseInt(document.getElementById('ekKritikEsik').value, 10);
            const indirimOrani = parseFloat(document.getElementById('ekKritikOran').value);
            const sonucEl = document.getElementById('ekKritikSonuc');
            sonucEl.innerHTML = '';
            try {
                const res = await fetch('api/admin/stok', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ esikDeger, indirimOrani })
                });
                const sonuc = await res.json();
                if (res.ok && sonuc.basarili) {
                    sonucEl.innerHTML = `<div class="alert alert-success py-2 mb-0">${sonuc.etkilenenKitapSayisi} kitaba otomatik indirim uygulandı.</div>`;
                    stokTablosunuYukle();
                } else {
                    sonucEl.innerHTML = `<div class="alert alert-danger py-2 mb-0">${sonuc.mesaj || 'İşlem başarısız.'}</div>`;
                }
            } catch (err) {
                sonucEl.innerHTML = `<div class="alert alert-danger py-2 mb-0">Bağlantı hatası: ${err.message}</div>`;
            }
        });

        document.getElementById('ekStokYenileBtn').addEventListener('click', stokTablosunuYukle);
    }

    /* ================= BASLAT ================= */
    document.addEventListener('DOMContentLoaded', () => {
        sekmeleriHazirla();
        stokIndirimAraclariHazirla();

        document.getElementById('bbKitapAra').addEventListener('input', () => {
            clearTimeout(kitapAramaZamanlayici);
            kitapAramaZamanlayici = setTimeout(() => { kitapSayfaDurumu.sayfa = 1; kitaplariYukle(); }, 300);
        });
        document.getElementById('bbKitapSiralaSelect').addEventListener('change', (e) => {
            kitapSayfaDurumu.sirala = e.target.value;
            kitapSayfaDurumu.sayfa = 1;
            kitaplariYukle();
        });
        document.getElementById('bbKitapKategoriFiltre').addEventListener('change', (e) => {
            kitapSayfaDurumu.kategoriId = e.target.value;
            kitapSayfaDurumu.sayfa = 1;
            kitaplariYukle();
        });
        kitapKategoriFiltresiniDoldur();
        document.getElementById('bbKitapEkleBtn').addEventListener('click', () => kitapModalAc(null));
        document.getElementById('bbKitapKaydetBtn').addEventListener('click', kitapKaydet);

        document.getElementById('bbYazarEkleBtn').addEventListener('click', () => basitModalAc('yazar', null));
        document.getElementById('bbKategoriEkleBtn').addEventListener('click', () => basitModalAc('kategori', null));
        document.getElementById('bbYayineviEkleBtn').addEventListener('click', () => basitModalAc('yayinevi', null));
        document.getElementById('bbBasitKaydetBtn').addEventListener('click', basitKaydet);

        document.getElementById('bbDuyuruEkleBtn').addEventListener('click', () => duyuruModalAc(null, []));
        document.getElementById('bbDuyuruKaydetBtn').addEventListener('click', duyuruKaydet);
        document.getElementById('bbDuyuruFiltre').addEventListener('click', (e) => {
            const pill = e.target.closest('.bb-pill');
            if (!pill) return;
            document.querySelectorAll('#bbDuyuruFiltre .bb-pill').forEach(p => p.classList.remove('bb-active'));
            pill.classList.add('bb-active');
            duyuruAktifFiltre = pill.dataset.filtre;
            duyuruTablosunuCiz();
        });

        document.getElementById('bbDashYenileBtn').addEventListener('click', dashboardYukle);
        document.getElementById('bbSiparisYenileBtn').addEventListener('click', siparisleriYukle);

        document.getElementById('bbKullaniciAra').addEventListener('input', (e) => {
            clearTimeout(kullaniciAramaZamanlayici);
            kullaniciAramaZamanlayici = setTimeout(() => {
                kullaniciSayfaDurumu.q = e.target.value.trim();
                kullaniciSayfaDurumu.sayfa = 1;
                kullaniciTablosunuCiz();
            }, 250);
        });
        document.getElementById('bbKullaniciSiralaSelect').addEventListener('change', (e) => {
            kullaniciSayfaDurumu.sirala = e.target.value;
            kullaniciSayfaDurumu.sayfa = 1;
            kullaniciTablosunuCiz();
        });

        dosyaSeciminiOnizle('bbKitapKapakInput', 'bbKitapKapakOnizleme');
        dosyaSeciminiOnizle('bbDuyuruResimInput', 'bbDuyuruResimOnizleme');

        // Ilk acilista Dashboard + arac formlari + ortak secim listeleri yuklensin
        dashboardYukle();
        yayinevleriniDoldurEskiForm();
        stokTablosunuYukle();
        ortakSecimleriYukle();
    });
})();
