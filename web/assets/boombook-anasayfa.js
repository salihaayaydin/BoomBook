
(() => {
    let tumKitaplar = [];

    function cokSatanlariRenderla(liste, baslik) {
        const track = document.getElementById('bbCokSatanTrack');
        const bosMesaj = document.getElementById('bbCokSatanBos');
        const baslikEl = document.getElementById('bbCokSatanBaslik');
        if (baslikEl && baslik) baslikEl.textContent = baslik;

        track.innerHTML = '';
        if (!liste || liste.length === 0) {
            bosMesaj.classList.remove('d-none');
            return;
        }
        bosMesaj.classList.add('d-none');
        liste.slice(0, 16).forEach(k => track.appendChild(window.BB.kitapKartOlustur(k)));
    }

    async function yayinevleriYukle() {
        const track = document.getElementById('bbYayineviTrack');
        try {
            const res = await fetch('api/yayinevleri');
            const liste = await res.json();
            track.innerHTML = '';
            liste.forEach(y => {
                const kart = document.createElement('div');
                kart.className = 'bb-yayinevi-card';
                const harf = (y.yayineviAdi || '?').trim().charAt(0).toUpperCase();
                kart.innerHTML = `
                    <div class="bb-yayinevi-avatar">${harf}</div>
                    <div class="bb-yayinevi-name">${y.yayineviAdi}</div>`;
                kart.addEventListener('click', () => {
                    window.location.href = 'katalog.html?yayineviId=' + y.yayineviId;
                });
                track.appendChild(kart);
            });
        } catch (err) {
            track.innerHTML = '<div class="text-muted small px-2">Yayınevleri yüklenemedi.</div>';
        }
    }

    async function kampanyaKartlariHazirla() {
        const grid = document.getElementById('bbKampanyaGrid');
        if (!grid) return;
        try {
            const res = await fetch('api/duyurular?tur=kart');
            const kartlar = await res.json();
            if (!Array.isArray(kartlar) || kartlar.length === 0) {
                grid.closest('section')?.classList.add('d-none');
                return;
            }
            grid.innerHTML = kartlar.map(d => {
                const arkaPlan = d.resimUrl
                    ? `linear-gradient(0deg, rgba(0,0,0,0.38), rgba(0,0,0,0.38)), url('${d.resimUrl}') center/cover no-repeat`
                    : `linear-gradient(120deg,#2b2a6b,#3a1f35)`;
                return `
                <a class="bb-kampanya-card" href="${d.butonLink || 'katalog.html'}" style="background:${arkaPlan}">
                    <span>${EK.escapeHtml(d.baslik).replace(/ /g, '<br>')}</span>
                </a>`;
            }).join('');
        } catch (err) {
            console.error('Kampanya kartları yüklenemedi:', err);
            grid.closest('section')?.classList.add('d-none');
        }
    }

    async function heroCarouselHazirla() {
        const track = document.getElementById('bbHeroTrack');
        const dotsWrap = document.getElementById('bbHeroDots');
        if (!track) return;

        try {
            const res = await fetch('api/duyurular?tur=slider');
            const duyurular = await res.json();
            if (Array.isArray(duyurular) && duyurular.length > 0) {
                track.innerHTML = duyurular.map(d => {
                    const arkaPlan = d.resimUrl
                        ? `linear-gradient(0deg, rgba(0,0,0,0.32), rgba(0,0,0,0.32)), url('${d.resimUrl}') center/cover no-repeat`
                        : `linear-gradient(120deg,#2b2a6b,#3a1f35)`;
                    return `
                    <div class="bb-hero-slide" style="background:${arkaPlan}">
                        <div>
                            <h2>${EK.escapeHtml(d.baslik)}</h2>
                            ${d.aciklama ? `<p>${EK.escapeHtml(d.aciklama)}</p>` : ''}
                            <a href="${d.butonLink || 'katalog.html'}" class="bb-btn bb-btn-outline">${EK.escapeHtml(d.butonMetni || 'Keşfet')}</a>
                        </div>
                    </div>`;
                }).join('');
            }
        } catch (err) {
            console.error('Duyurular yuklenemedi, varsayilan slayt gosteriliyor:', err);
        }

        const slaytlar = track.querySelectorAll('.bb-hero-slide');
        let index = 0;
        let zamanlayici = null;

        dotsWrap.innerHTML = '';
        slaytlar.forEach((_, i) => {
            const dot = document.createElement('button');
            dot.className = 'bb-hero-dot' + (i === 0 ? ' bb-active' : '');
            dot.addEventListener('click', () => git(i));
            dotsWrap.appendChild(dot);
        });

        function git(i) {
            index = (i + slaytlar.length) % slaytlar.length;
            track.style.transform = `translateX(-${index * 100}%)`;
            dotsWrap.querySelectorAll('.bb-hero-dot').forEach((d, di) => d.classList.toggle('bb-active', di === index));
        }

        function otomatikBaslat() {
            if (slaytlar.length <= 1) return;
            clearInterval(zamanlayici);
            zamanlayici = setInterval(() => git(index + 1), 5000);
        }

        document.getElementById('bbHeroPrev').addEventListener('click', () => { git(index - 1); otomatikBaslat(); });
        document.getElementById('bbHeroNext').addEventListener('click', () => { git(index + 1); otomatikBaslat(); });

        otomatikBaslat();
    }

    async function kitaplariYukle() {
        const iskelet = document.getElementById('bbCokSatanIskelet');
        try {
            const res = await fetch('api/kitaplar');
            tumKitaplar = await res.json();
            iskelet.classList.add('d-none');

            const params = new URLSearchParams(window.location.search);
            let baslangicListesi = tumKitaplar;
            let baslik = 'Çok Satanlar';

            if (params.has('kategoriId')) {
                const id = parseInt(params.get('kategoriId'), 10);
                baslangicListesi = tumKitaplar.filter(k => k.kategoriId === id);
                baslik = (baslangicListesi[0]?.kategoriAdi || 'Kategori') + ' Kitapları';
            } else if (params.has('yazarId')) {
                const id = parseInt(params.get('yazarId'), 10);
                baslangicListesi = tumKitaplar.filter(k => k.yazarId === id);
                baslik = (baslangicListesi[0]?.yazarAdi || 'Yazar') + ' Kitapları';
            } else if (params.has('yayineviId')) {
                const id = parseInt(params.get('yayineviId'), 10);
                baslangicListesi = tumKitaplar.filter(k => k.yayineviId === id);
                baslik = (baslangicListesi[0]?.yayineviAdi || 'Yayınevi') + ' Kitapları';
            }

            cokSatanlariRenderla(baslangicListesi, baslik);
        } catch (err) {
            iskelet.classList.add('d-none');
            document.getElementById('bbCokSatanBos').classList.remove('d-none');
            document.getElementById('bbCokSatanBos').textContent = 'Kitaplar yüklenemedi: ' + err.message;
        }
    }

    document.addEventListener('DOMContentLoaded', async () => {
        [heroCarouselHazirla,
         () => window.BB.carouselOkHazirla('bbCokSatanTrack', 'bbCokSatanPrev', 'bbCokSatanNext'),
         () => window.BB.carouselOkHazirla('bbYayineviTrack', 'bbYayineviPrev', 'bbYayineviNext'),
         yayinevleriYukle,
         kampanyaKartlariHazirla
        ].forEach(fn => {
            try { fn(); } catch (err) { console.error('BOOMBOOK ana sayfa baslatma hatasi:', err); }
        });

        try {
            await window.BB.favorilerHazir;
        } catch (err) {
            console.error('Favoriler yuklenemedi:', err);
        }
        await kitaplariYukle();
    });
})();
