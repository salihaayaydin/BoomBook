/* ============================================================
   BOOMBOOK — Siparislerim Sayfasi
   Her siparis, kapak resmi + yazar + adet + fiyat gosteren
   satirlarla bir kart olarak render edilir. "Tamamlandi" durumundaki
   siparislerdeki her kitap icin "Degerlendir" butonu, yildiz + yorum +
   fotograf iceren bir modal acar (api/degerlendirme, multipart/form-data).
   ============================================================ */
(() => {
    const DURUM_ETIKET = { 'Bekliyor': 'Hazırlanıyor', 'Tamamlandi': 'Tamamlandı', 'Basarisiz': 'İptal Edildi' };
    const DURUM_SINIF = { 'Bekliyor': 'bb-status-bekliyor', 'Tamamlandi': 'bb-status-tamamlandi', 'Basarisiz': 'bb-status-basarisiz' };

    function tarihFormat(iso) {
        if (!iso) return '-';
        const d = new Date(iso.replace(' ', 'T'));
        if (isNaN(d)) return iso;
        return d.toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' }) +
               ' · ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
    }

    function siparisKartiOlustur(s) {
        const kalemler = (s.detaylar || []).map(d => `
            <div class="bb-siparis-kalem">
                <img class="bb-siparis-kalem-kapak" src="${d.kapakResmiUrl || 'assets/img/logo.png'}" alt="${EK.escapeHtml(d.kitapAdi)}">
                <div class="bb-siparis-kalem-bilgi">
                    <div class="bb-siparis-kalem-baslik">${EK.escapeHtml(d.kitapAdi)}</div>
                    ${d.yazarAdi ? `<div class="bb-siparis-kalem-yazar">${EK.escapeHtml(d.yazarAdi)}</div>` : ''}
                    <div class="bb-siparis-kalem-adet">${d.adet} adet</div>
                </div>
                <div class="d-flex flex-column align-items-end gap-2">
                    <div class="bb-siparis-kalem-fiyat">${EK.fiyatFormat(d.birimFiyat * d.adet)}</div>
                    ${s.odemeDurumu === 'Tamamlandi'
                        ? `<button class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink);padding:5px 14px;font-size:0.78rem" data-degerlendir data-kitap-id="${d.kitapId}" data-kitap-adi="${EK.escapeHtml(d.kitapAdi)}">⭐ Değerlendir</button>`
                        : ''}
                </div>
            </div>`).join('');

        const kargoSatiri = s.kargoUcreti > 0
            ? `<small>Kargo: ${EK.fiyatFormat(s.kargoUcreti)} dahil</small>` : '<small>Ücretsiz kargo</small>';

        return `
        <div class="bb-siparis-karti">
            <div class="bb-siparis-karti-ust">
                <div class="bb-siparis-karti-ust-sol">
                    <span class="bb-siparis-no">Sipariş #${s.siparisId}</span>
                    <span class="bb-siparis-tarih">${tarihFormat(s.siparisTarihi)}</span>
                </div>
                <span class="bb-status-badge ${DURUM_SINIF[s.odemeDurumu] || ''}">${DURUM_ETIKET[s.odemeDurumu] || s.odemeDurumu}</span>
            </div>
            ${kalemler || '<div class="p-3 text-muted small">Kalem bulunamadı.</div>'}
            <div class="bb-siparis-karti-alt">
                ${kargoSatiri}
                <div class="bb-siparis-toplam">
                    <small>Toplam</small>
                    ${EK.fiyatFormat(s.toplamTutar)}
                </div>
            </div>
        </div>`;
    }

    /* ---------------- DEGERLENDIRME MODALI ---------------- */
    let secilenPuan = 0;

    function yildizlariCiz() {
        const wrap = document.getElementById('ekDegerlendirYildizlar');
        wrap.innerHTML = '';
        for (let i = 1; i <= 5; i++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn p-0 border-0 bg-transparent';
            btn.style.color = i <= secilenPuan ? 'var(--bb-accent-dark)' : 'var(--bb-border)';
            btn.textContent = '★';
            btn.addEventListener('click', () => { secilenPuan = i; yildizlariCiz(); });
            wrap.appendChild(btn);
        }
    }

    function degerlendirModaliniAc(kitapId, kitapAdi) {
        document.getElementById('ekDegerlendirKitapId').value = kitapId;
        document.getElementById('ekDegerlendirBaslik').textContent = 'Değerlendir: ' + kitapAdi;
        document.getElementById('ekDegerlendirYorum').value = '';
        document.getElementById('ekDegerlendirFoto').value = '';
        const onizleme = document.getElementById('ekDegerlendirFotoOnizleme');
        onizleme.src = '';
        onizleme.style.display = 'none';
        secilenPuan = 0;
        yildizlariCiz();
        new bootstrap.Modal(document.getElementById('ekDegerlendirModal')).show();
    }

    async function degerlendirmeGonder() {
        if (secilenPuan < 1) { EK.toast('Lütfen bir puan seçin.', 'error'); return; }
        const kitapId = document.getElementById('ekDegerlendirKitapId').value;
        const yorum = document.getElementById('ekDegerlendirYorum').value.trim();
        const dosya = document.getElementById('ekDegerlendirFoto').files[0];

        const formData = new FormData();
        formData.append('kitapId', kitapId);
        formData.append('puan', secilenPuan);
        formData.append('yorum', yorum);
        if (dosya) formData.append('resim', dosya);

        try {
            const res = await fetch('api/degerlendirme', { method: 'POST', body: formData });
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                EK.toast('Değerlendirmeniz kaydedildi, teşekkürler!', 'success');
                bootstrap.Modal.getInstance(document.getElementById('ekDegerlendirModal'))?.hide();
            } else {
                EK.toast(sonuc.mesaj || 'Kaydedilemedi.', 'error');
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
        }
    }

    /* ---------------- SIPARISLERI YUKLE ---------------- */
    async function yukle() {
        const yukleniyor = document.getElementById('ekYukleniyor');
        const bosMesaj = document.getElementById('ekBosMesaj');
        const hataMesaj = document.getElementById('ekHataMesaj');
        const liste = document.getElementById('ekSiparisListe');

        yukleniyor.classList.remove('d-none');
        bosMesaj.classList.add('d-none');
        hataMesaj.classList.add('d-none');
        liste.classList.add('d-none');

        try {
            const res = await fetch('api/siparislerim');

            if (res.status === 401) {
                window.location.href = 'login.jsp?sonraki=siparislerim.html';
                return;
            }

            const sonuc = await res.json();
            yukleniyor.classList.add('d-none');

            if (!res.ok) {
                hataMesaj.textContent = sonuc.mesaj || 'Siparişler alınamadı.';
                hataMesaj.classList.remove('d-none');
                return;
            }
            if (!Array.isArray(sonuc) || sonuc.length === 0) {
                bosMesaj.classList.remove('d-none');
                return;
            }

            liste.innerHTML = sonuc.map(siparisKartiOlustur).join('');
            liste.classList.remove('d-none');

            liste.querySelectorAll('[data-degerlendir]').forEach(btn => {
                btn.addEventListener('click', () => degerlendirModaliniAc(btn.dataset.kitapId, btn.dataset.kitapAdi));
            });
        } catch (err) {
            yukleniyor.classList.add('d-none');
            hataMesaj.textContent = 'Bağlantı hatası: ' + err.message;
            hataMesaj.classList.remove('d-none');
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        yukle();
        document.getElementById('ekDegerlendirGonderBtn').addEventListener('click', degerlendirmeGonder);
        document.getElementById('ekDegerlendirFoto').addEventListener('change', () => {
            const dosya = document.getElementById('ekDegerlendirFoto').files[0];
            if (!dosya) return;
            const onizleme = document.getElementById('ekDegerlendirFotoOnizleme');
            onizleme.src = URL.createObjectURL(dosya);
            onizleme.style.display = 'block';
        });
    });
})();
