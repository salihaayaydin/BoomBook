/* ============================================================
   BOOMBOOK — Kitap Detay Sayfasi JS
   ?id=X parametresiyle tek bir kitabi getirir, detaylarini render
   eder ve ayni kategorideki "Benzer Kitaplar" carousel'ini doldurur.
   ============================================================ */

(() => {
    const PLACEHOLDER_ICON = '📖';

    function kitapIdAl() {
        const params = new URLSearchParams(window.location.search);
        const id = parseInt(params.get('id'), 10);
        return Number.isFinite(id) ? id : null;
    }

    function kapakHtml(k) {
        return k.kapakResmiUrl
            ? `<img src="${k.kapakResmiUrl}" alt="${k.kitapAdi}"
                   onerror="this.style.display='none'; this.parentElement.innerHTML+='<div style=\\'display:flex;align-items:center;justify-content:center;height:100%;font-size:3rem;color:rgba(255,255,255,.6)\\'>${PLACEHOLDER_ICON}</div>'">`
            : `<div style="display:flex;align-items:center;justify-content:center;height:100%;font-size:3rem;color:rgba(255,255,255,.6)">${PLACEHOLDER_ICON}</div>`;
    }

    async function detayiRenderla(k) {
        document.getElementById('bbSayfaBaslik').textContent = k.kitapAdi + ' | BOOMBOOK';
        document.getElementById('bbBreadcrumbBaslik').textContent = k.kitapAdi;

        const kapakWrap = document.getElementById('bbDetayKapak');
        const indirimYuzde = window.BB.indirimYuzdesi(k);
        kapakWrap.innerHTML = kapakHtml(k) + (indirimYuzde ? `<span class="bb-discount-badge">%${indirimYuzde} İndirim</span>` : '');

        document.getElementById('bbDetayBaslik').textContent = k.kitapAdi;
        const yazarAdiGuvenli = EK.escapeHtml(k.yazarAdi);
        const yayineviAdiGuvenli = EK.escapeHtml(k.yayineviAdi);
        const kategoriAdiGuvenli = EK.escapeHtml(k.kategoriAdi);
        document.getElementById('bbDetayMeta').innerHTML =
            `${k.yazarAdi ? `<a href="index.html?yazarId=${k.yazarId}">${yazarAdiGuvenli}</a>` : 'Bilinmeyen Yazar'}` +
            (k.yayineviAdi ? ` &middot; <a href="index.html?yayineviId=${k.yayineviId}">${yayineviAdiGuvenli}</a>` : '') +
            (k.kategoriAdi ? ` &middot; <a href="index.html?kategoriId=${k.kategoriId}">${kategoriAdiGuvenli}</a>` : '') +
            (k.sayfaSayisi ? ` &middot; ${k.sayfaSayisi} sayfa` : '') +
            (k.dosyaFormati ? ` &middot; ${k.dosyaFormati}` : '');

        document.getElementById('bbDetayPuan').innerHTML = window.BB.yildizHtml(k.ortalamaPuan, k.degerlendirmeSayisi);

        const fiyatEl = document.getElementById('bbDetayFiyat');
        fiyatEl.innerHTML = k.indirimliFiyat
            ? `<span class="bb-detay-price-old">${EK.fiyatFormat(k.fiyat)}</span><span class="bb-detay-price-current">${EK.fiyatFormat(k.indirimliFiyat)}</span>`
            : `<span class="bb-detay-price-current">${EK.fiyatFormat(k.fiyat)}</span>`;

        const stokEl = document.getElementById('bbDetayStok');
        const stokYok = k.stokMiktari <= 0;
        stokEl.textContent = stokYok ? '✕ Stokta Yok' : `✓ Stokta (${k.stokMiktari} adet)`;
        stokEl.className = 'bb-detay-stok ' + (stokYok ? 'bb-stok-yok' : 'bb-stok-var');

        document.getElementById('bbDetayAciklama').textContent = k.aciklama || 'Bu kitap için henüz bir açıklama girilmemiş.';

        const sepetBtn = document.getElementById('bbDetaySepeteEkle');
        sepetBtn.disabled = stokYok;
        if (stokYok) sepetBtn.textContent = 'Stokta Yok';
        sepetBtn.addEventListener('click', async () => {
            sepetBtn.disabled = true;
            const ok = await EK.sepeteEkle(k);
            if (ok) {
                EK.toast(`"${k.kitapAdi}" sepete eklendi.`, 'success');
                window.BB.sepetPopoverGuncelle();
            }
            sepetBtn.disabled = stokYok;
        });

        const favoriBtn = document.getElementById('bbDetayFavori');
        await window.BB.favorilerHazir;
        const favoriGuncelle = () => {
            const favoride = window.BB.favoriSet.has(k.kitapId);
            favoriBtn.innerHTML = favoride ? '♥ Favorilerde' : '♡ Favorile';
            favoriBtn.style.color = favoride ? '#fff' : 'var(--bb-ink)';
            favoriBtn.style.background = favoride ? 'var(--bb-discount)' : 'var(--bb-grey-light)';
        };
        favoriGuncelle();
        favoriBtn.addEventListener('click', async () => {
            const favorideMi = window.BB.favoriSet.has(k.kitapId);
            if (favorideMi) {
                const ok = await EK.favoridenCikar(k.kitapId);
                if (ok) { window.BB.favoriSet.delete(k.kitapId); favoriGuncelle(); }
            } else {
                const ok = await EK.favoriyeEkle(k.kitapId);
                if (ok) { window.BB.favoriSet.add(k.kitapId); favoriGuncelle(); EK.toast('Favorilere eklendi.', 'success'); }
            }
        });

        document.getElementById('bbDetayIskelet').classList.add('d-none');
        document.getElementById('bbDetayIcerik').classList.remove('d-none');
    }

    /* ---------------- DEGERLENDIRME / YORUM BOLUMU ---------------- */

    function isimGizle(adSoyad) {
        // Gizlilik icin: "Ayşe Yılmaz" -> "Ayşe Y."
        if (!adSoyad) return 'Kullanıcı';
        const parcalar = adSoyad.trim().split(/\s+/);
        if (parcalar.length === 1) return parcalar[0];
        return `${parcalar[0]} ${parcalar[parcalar.length - 1].charAt(0)}.`;
    }

    function tarihFormat(iso) {
        if (!iso) return '';
        const d = new Date(iso.replace(' ', 'T'));
        return isNaN(d) ? '' : d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' });
    }

    function yildizSeciciHtml(secili) {
        let html = '';
        for (let i = 1; i <= 5; i++) {
            html += `<button type="button" class="bb-yildiz-sec" data-puan="${i}" style="background:none;border:none;font-size:1.5rem;cursor:pointer;color:${i <= secili ? 'var(--bb-accent-dark)' : 'var(--bb-border)'}">★</button>`;
        }
        return html;
    }

    async function degerlendirmeleriYukle(kitapId) {
        const govde = document.getElementById('bbYorumGovde');
        const ozetEl = document.getElementById('bbYorumOzet');
        const formAlani = document.getElementById('bbYorumFormAlani');

        try {
            const res = await fetch('api/degerlendirme?kitapId=' + kitapId);
            const veri = await res.json();

            ozetEl.innerHTML = veri.sayim > 0
                ? `${window.BB.yildizHtml(veri.ortalama, veri.sayim)} <span class="text-muted">— ${veri.sayim} değerlendirme</span>`
                : '<span class="text-muted">Henüz değerlendirme yok</span>';

            if (!veri.yorumlar || veri.yorumlar.length === 0) {
                govde.innerHTML = '<div class="bb-yorum-bos">💬 Bu kitap için henüz yorum bulunmuyor.</div>';
            } else {
                govde.innerHTML = veri.yorumlar.map(y => `
                    <div class="border-bottom py-3">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <strong>${EK.escapeHtml(isimGizle(y.kullaniciAdSoyad))}</strong>
                            <span style="color:var(--bb-accent-dark)">${'★'.repeat(y.puan)}${'<span style="color:var(--bb-border)">★</span>'.repeat(5 - y.puan)}</span>
                        </div>
                        <div class="text-muted small mb-1">${tarihFormat(y.tarih)}</div>
                        ${y.yorum ? `<div>${EK.escapeHtml(y.yorum)}</div>` : ''}
                        ${y.resimUrl ? `<img src="${y.resimUrl}" class="mt-2" style="width:90px;height:90px;object-fit:cover;border-radius:8px;cursor:pointer" onclick="window.open('${y.resimUrl}','_blank')">` : ''}
                    </div>`).join('');
            }

            formAlaniniOlustur(kitapId, veri.satinAlmisMi, veri.kullaniciDegerlendirmesi);
        } catch (err) {
            govde.innerHTML = `<div class="bb-yorum-bos text-danger">Değerlendirmeler yüklenemedi: ${err.message}</div>`;
        }
    }

    function formAlaniniOlustur(kitapId, satinAlmisMi, mevcutDegerlendirme) {
        const formAlani = document.getElementById('bbYorumFormAlani');

        if (!satinAlmisMi) {
            formAlani.innerHTML = `
                <div class="alert alert-light border small mb-0">
                    ℹ️ Bu kitaba değerlendirme yapabilmek için önce satın almanız gerekiyor.
                </div>`;
            return;
        }

        let secilenPuan = mevcutDegerlendirme ? mevcutDegerlendirme.puan : 0;
        formAlani.innerHTML = `
            <div class="bb-admin-card" style="margin-bottom:0">
                <div class="bb-admin-card-header">${mevcutDegerlendirme ? 'Değerlendirmenizi Düzenleyin' : 'Bu Kitabı Değerlendirin'}</div>
                <div class="bb-admin-card-body">
                    <div id="bbYildizSecici" class="mb-2">${yildizSeciciHtml(secilenPuan)}</div>
                    <textarea class="form-control mb-2" id="bbYorumMetni" rows="3" placeholder="Yorumunuz (opsiyonel)">${mevcutDegerlendirme ? EK.escapeHtml(mevcutDegerlendirme.yorum || '') : ''}</textarea>
                    <div class="mb-2">
                        <label class="form-label small text-muted mb-1">Fotoğraf ekle (opsiyonel)</label>
                        <div class="d-flex align-items-center gap-2">
                            ${mevcutDegerlendirme && mevcutDegerlendirme.resimUrl
                                ? `<img src="${mevcutDegerlendirme.resimUrl}" id="bbYorumFotoOnizleme" style="width:56px;height:56px;object-fit:cover;border-radius:8px">`
                                : `<img src="" id="bbYorumFotoOnizleme" style="width:56px;height:56px;object-fit:cover;border-radius:8px;display:none">`}
                            <input type="file" class="form-control form-control-sm" id="bbYorumFoto" accept=".jpg,.jpeg,.png,.webp">
                        </div>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="button" class="bb-btn bb-btn-navy" id="bbYorumGonder">${mevcutDegerlendirme ? 'Güncelle' : 'Gönder'}</button>
                        ${mevcutDegerlendirme ? '<button type="button" class="bb-btn" style="background:var(--bb-grey-light);color:var(--bb-ink)" id="bbYorumSil">Sil</button>' : ''}
                    </div>
                </div>
            </div>`;

        const yildizSecici = document.getElementById('bbYildizSecici');
        yildizSecici.querySelectorAll('.bb-yildiz-sec').forEach(btn => {
            btn.addEventListener('click', () => {
                secilenPuan = parseInt(btn.dataset.puan, 10);
                yildizSecici.innerHTML = yildizSeciciHtml(secilenPuan);
                // Yeniden olusturulunca listener'lari tekrar bagla
                yildizSecici.querySelectorAll('.bb-yildiz-sec').forEach(b2 =>
                    b2.addEventListener('click', () => { secilenPuan = parseInt(b2.dataset.puan, 10); yildizSecici.innerHTML = yildizSeciciHtml(secilenPuan); }));
            });
        });

        const fotoInput = document.getElementById('bbYorumFoto');
        fotoInput.addEventListener('change', () => {
            const dosya = fotoInput.files[0];
            if (!dosya) return;
            const onizleme = document.getElementById('bbYorumFotoOnizleme');
            onizleme.src = URL.createObjectURL(dosya);
            onizleme.style.display = 'block';
        });

        document.getElementById('bbYorumGonder').addEventListener('click', async () => {
            if (secilenPuan < 1) { EK.toast('Lütfen bir puan seçin.', 'error'); return; }
            const yorum = document.getElementById('bbYorumMetni').value.trim();
            const dosya = fotoInput.files[0];

            const formData = new FormData();
            formData.append('kitapId', kitapId);
            formData.append('puan', secilenPuan);
            formData.append('yorum', yorum);
            if (dosya) formData.append('resim', dosya);

            try {
                const res = await fetch('api/degerlendirme', { method: 'POST', body: formData });
                const sonuc = await res.json();
                if (res.ok && sonuc.basarili) {
                    EK.toast('Değerlendirmeniz kaydedildi.', 'success');
                    degerlendirmeleriYukle(kitapId);
                } else {
                    EK.toast(sonuc.mesaj || 'Kaydedilemedi.', 'error');
                }
            } catch (err) {
                EK.toast('Bağlantı hatası: ' + err.message, 'error');
            }
        });

        const silBtn = document.getElementById('bbYorumSil');
        if (silBtn) {
            silBtn.addEventListener('click', async () => {
                if (!confirm('Değerlendirmenizi silmek istediğinize emin misiniz?')) return;
                try {
                    const res = await fetch('api/degerlendirme?kitapId=' + kitapId, { method: 'DELETE' });
                    const sonuc = await res.json();
                    if (res.ok && sonuc.basarili) {
                        EK.toast('Değerlendirmeniz silindi.', 'success');
                        degerlendirmeleriYukle(kitapId);
                    } else {
                        EK.toast(sonuc.mesaj || 'Silinemedi.', 'error');
                    }
                } catch (err) {
                    EK.toast('Bağlantı hatası: ' + err.message, 'error');
                }
            });
        }
    }

    async function benzerKitaplariYukle(k) {
        const track = document.getElementById('bbBenzerTrack');
        const bos = document.getElementById('bbBenzerBos');
        try {
            const url = k.kategoriId
                ? 'api/kitaplar?kategoriId=' + k.kategoriId
                : 'api/kitaplar';
            const res = await fetch(url);
            const liste = (await res.json()).filter(item => item.kitapId !== k.kitapId);

            if (liste.length === 0) {
                bos.classList.remove('d-none');
                return;
            }
            liste.slice(0, 12).forEach(item => track.appendChild(window.BB.kitapKartOlustur(item)));
        } catch (err) {
            bos.classList.remove('d-none');
            bos.textContent = 'Benzer kitaplar yüklenemedi.';
        }
    }

    async function baslat() {
        const id = kitapIdAl();
        if (!id) {
            hataGoster('Geçersiz kitap. Lütfen ana sayfadan bir kitap seçin.');
            return;
        }

        try {
            const res = await fetch('api/kitaplar?id=' + id);
            if (res.status === 404) {
                hataGoster('Bu kitap bulunamadı. Kaldırılmış ya da yayından kaldırılmış olabilir.');
                return;
            }
            if (!res.ok) throw new Error('Sunucu hatası: ' + res.status);

            const kitap = await res.json();
            await detayiRenderla(kitap);
            window.BB.carouselOkHazirla('bbBenzerTrack', 'bbBenzerPrev', 'bbBenzerNext');
            benzerKitaplariYukle(kitap);
            degerlendirmeleriYukle(id);
        } catch (err) {
            hataGoster('Kitap yüklenirken bir hata oluştu: ' + err.message);
        }
    }

    function hataGoster(mesaj) {
        document.getElementById('bbDetayIskelet').classList.add('d-none');
        const hataEl = document.getElementById('bbDetayHata');
        hataEl.textContent = mesaj;
        hataEl.classList.remove('d-none');
    }

    document.addEventListener('DOMContentLoaded', baslat);
})();
