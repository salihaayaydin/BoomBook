/* ============================================================
   BOOMBOOK — Katalog (Sol Filtreli Listeleme) Sayfasi JS
   /api/katalog uc noktasini kullanir (coklu kategori/yazar/yayinevi
   secimi, fiyat araligi, format, sadece stokta, siralama, sayfalama).
   ============================================================ */

(() => {
    const SAYFA_BOYUTU = 12;

    let kategoriler = [];
    let yazarlar = [];
    let yayinevleri = [];
    let fiyatSinirlari = { min: 0, max: 1000 };

    /* ---------------- DURUM (URL parametrelerinden okunur) ---------------- */
    function durumOku() {
        const p = new URLSearchParams(window.location.search);
        return {
            q: p.get('q') || '',
            kategoriId: p.getAll('kategoriId').map(Number),
            yazarId: p.getAll('yazarId').map(Number),
            yayineviId: p.getAll('yayineviId').map(Number),
            format: p.getAll('format'),
            fiyatMin: p.get('fiyatMin') ? Number(p.get('fiyatMin')) : null,
            fiyatMax: p.get('fiyatMax') ? Number(p.get('fiyatMax')) : null,
            minPuan: p.get('minPuan') ? Number(p.get('minPuan')) : null,
            sadeceStokta: p.get('sadeceStokta') === 'true',
            sirala: p.get('sirala') || 'enYeni',
            sayfa: parseInt(p.get('sayfa'), 10) || 1
        };
    }

    function durumdanUrlOlustur(durum) {
        const p = new URLSearchParams();
        if (durum.q) p.set('q', durum.q);
        durum.kategoriId.forEach(id => p.append('kategoriId', id));
        durum.yazarId.forEach(id => p.append('yazarId', id));
        durum.yayineviId.forEach(id => p.append('yayineviId', id));
        durum.format.forEach(f => p.append('format', f));
        if (durum.fiyatMin != null) p.set('fiyatMin', durum.fiyatMin);
        if (durum.fiyatMax != null) p.set('fiyatMax', durum.fiyatMax);
        if (durum.minPuan != null) p.set('minPuan', durum.minPuan);
        if (durum.sadeceStokta) p.set('sadeceStokta', 'true');
        if (durum.sirala && durum.sirala !== 'enYeni') p.set('sirala', durum.sirala);
        if (durum.sayfa && durum.sayfa !== 1) p.set('sayfa', durum.sayfa);
        return p;
    }

    function urlGuncelle(durum) {
        const p = durumdanUrlOlustur(durum);
        window.history.pushState({}, '', 'katalog.html' + (p.toString() ? '?' + p.toString() : ''));
    }

    /* ---------------- FILTRE PANELINI DOLDUR ---------------- */
    async function filtrePaneliniDoldur() {
        try {
            const [katRes, yazRes, yayRes, fiyatRes] = await Promise.all([
                fetch('api/kategoriler'), fetch('api/yazarlar'), fetch('api/yayinevleri'), fetch('api/katalog/fiyat-araligi')
            ]);
            kategoriler = await katRes.json();
            yazarlar = await yazRes.json();
            yayinevleri = await yayRes.json();
            fiyatSinirlari = await fiyatRes.json();

            document.getElementById('bbRangeMin').min = Math.floor(fiyatSinirlari.min);
            document.getElementById('bbRangeMin').max = Math.ceil(fiyatSinirlari.max);
            document.getElementById('bbRangeMax').min = Math.floor(fiyatSinirlari.min);
            document.getElementById('bbRangeMax').max = Math.ceil(fiyatSinirlari.max);

            const durum = durumOku();
            document.getElementById('bbRangeMin').value = durum.fiyatMin ?? fiyatSinirlari.min;
            document.getElementById('bbRangeMax').value = durum.fiyatMax ?? fiyatSinirlari.max;
            document.getElementById('bbFiyatMinInput').value = durum.fiyatMin ?? Math.floor(fiyatSinirlari.min);
            document.getElementById('bbFiyatMaxInput').value = durum.fiyatMax ?? Math.ceil(fiyatSinirlari.max);
            rangeGorseliniGuncelle();

            checkboxListesiOlustur('bbKategoriFiltreListe', kategoriler, 'kategoriId', 'kategoriAdi', durum.kategoriId);
            checkboxListesiOlustur('bbYazarFiltreListe', yazarlar, 'yazarId', 'yazarAdi', durum.yazarId);
            checkboxListesiOlustur('bbYayineviFiltreListe', yayinevleri, 'yayineviId', 'yayineviAdi', durum.yayineviId);

            document.querySelectorAll('.bb-format-check').forEach(cb => {
                cb.checked = durum.format.includes(cb.value);
            });
            document.getElementById('bbSadeceStokta').checked = durum.sadeceStokta;
            document.getElementById('bbSiralaSelect').value = durum.sirala;

            document.querySelectorAll('.bb-puan-radio').forEach(radio => {
                radio.checked = (durum.minPuan == null && radio.value === '') ||
                                 (durum.minPuan != null && Number(radio.value) === durum.minPuan);
            });

            // Ust nav'daki mega menuler de ayni verilerle (hafif bicimde) doldurulsun
            megaMenuDoldur('kategoriMenuNav', kategoriler, 'kategoriId', 'kategoriAdi');
            megaMenuDoldur('yazarMenuNav', yazarlar, 'yazarId', 'yazarAdi');
            megaMenuDoldur('yayineviMenuNav', yayinevleri, 'yayineviId', 'yayineviAdi');
        } catch (err) {
            EK.toast('Filtreler yüklenemedi: ' + err.message, 'error');
        }
    }

    function megaMenuDoldur(elId, liste, idAlan, adAlan) {
        const el = document.getElementById(elId);
        if (!el) return;
        if (!liste || liste.length === 0) {
            el.innerHTML = '<div class="bb-mega-menu-empty">Kayıt bulunamadı</div>';
            return;
        }
        el.innerHTML = liste.map(item => `<a href="katalog.html?${idAlan}=${item[idAlan]}">${item[adAlan]}</a>`).join('');
    }

    function checkboxListesiOlustur(elId, liste, idAlan, adAlan, seciliIdler) {
        const el = document.getElementById(elId);
        if (!liste || liste.length === 0) {
            el.innerHTML = '<div class="bb-filtre-bos-mesaj">Kayıt bulunamadı.</div>';
            return;
        }
        el.innerHTML = liste.map(item => `
            <label class="bb-filtre-check">
                <input type="checkbox" class="bb-genel-check" data-grup="${idAlan}" value="${item[idAlan]}"
                       ${seciliIdler.includes(item[idAlan]) ? 'checked' : ''}>
                ${item[adAlan]}
            </label>`).join('');
    }

    function aramaKutusuFiltreleme(inputId, listeId) {
        const input = document.getElementById(inputId);
        input.addEventListener('input', () => {
            const q = input.value.toLocaleLowerCase('tr-TR');
            document.querySelectorAll(`#${listeId} .bb-filtre-check`).forEach(label => {
                label.style.display = label.textContent.toLocaleLowerCase('tr-TR').includes(q) ? '' : 'none';
            });
        });
    }

    /* ---------------- FIYAT SLIDER ---------------- */
    function rangeGorseliniGuncelle() {
        const minEl = document.getElementById('bbRangeMin');
        const maxEl = document.getElementById('bbRangeMax');
        const aktif = document.getElementById('bbRangeAktif');
        const sinirMin = Number(minEl.min), sinirMax = Number(minEl.max);
        const genislik = sinirMax - sinirMin || 1;

        let minV = Number(minEl.value), maxV = Number(maxEl.value);
        if (minV > maxV) { [minV, maxV] = [maxV, minV]; }

        const solYuzde = ((minV - sinirMin) / genislik) * 100;
        const sagYuzde = ((maxV - sinirMin) / genislik) * 100;
        aktif.style.left = solYuzde + '%';
        aktif.style.width = (sagYuzde - solYuzde) + '%';

        document.getElementById('bbFiyatMinInput').value = Math.round(minV);
        document.getElementById('bbFiyatMaxInput').value = Math.round(maxV);
    }

    function fiyatSliderHazirla() {
        const minEl = document.getElementById('bbRangeMin');
        const maxEl = document.getElementById('bbRangeMax');
        minEl.addEventListener('input', rangeGorseliniGuncelle);
        maxEl.addEventListener('input', rangeGorseliniGuncelle);

        document.getElementById('bbFiyatMinInput').addEventListener('change', (ev) => {
            minEl.value = ev.target.value; rangeGorseliniGuncelle();
        });
        document.getElementById('bbFiyatMaxInput').addEventListener('change', (ev) => {
            maxEl.value = ev.target.value; rangeGorseliniGuncelle();
        });
    }

    /* ---------------- AKTIF FILTRE CIPLERI ---------------- */
    function aktifFiltreleriRenderla(durum) {
        const kutu = document.getElementById('bbAktifFiltreler');
        const cipler = [];

        durum.kategoriId.forEach(id => {
            const k = kategoriler.find(x => x.kategoriId === id);
            if (k) cipler.push({ etiket: k.kategoriAdi, grup: 'kategoriId', deger: id });
        });
        durum.yazarId.forEach(id => {
            const y = yazarlar.find(x => x.yazarId === id);
            if (y) cipler.push({ etiket: y.yazarAdi, grup: 'yazarId', deger: id });
        });
        durum.yayineviId.forEach(id => {
            const y = yayinevleri.find(x => x.yayineviId === id);
            if (y) cipler.push({ etiket: y.yayineviAdi, grup: 'yayineviId', deger: id });
        });
        durum.format.forEach(f => cipler.push({ etiket: f, grup: 'format', deger: f }));
        if (durum.fiyatMin != null || durum.fiyatMax != null) {
            cipler.push({ etiket: `${durum.fiyatMin ?? fiyatSinirlari.min} - ${durum.fiyatMax ?? fiyatSinirlari.max} TL`, grup: 'fiyat', deger: null });
        }
        if (durum.sadeceStokta) cipler.push({ etiket: 'Sadece Stoktakiler', grup: 'stok', deger: null });
        if (durum.minPuan != null) cipler.push({ etiket: `${durum.minPuan}★ ve üzeri`, grup: 'minPuan', deger: null });

        if (cipler.length === 0) {
            kutu.innerHTML = '<span class="bb-filtre-bos-mesaj">Henüz filtre seçilmedi.</span>';
            return;
        }

        kutu.innerHTML = cipler.map(c => `
            <span class="bb-filtre-chip" data-grup="${c.grup}" data-deger="${c.deger ?? ''}">
                ${c.etiket} <button type="button">&times;</button>
            </span>`).join('');

        kutu.querySelectorAll('.bb-filtre-chip button').forEach(btn => {
            btn.addEventListener('click', (ev) => {
                const chip = ev.target.closest('.bb-filtre-chip');
                cipKaldir(chip.dataset.grup, chip.dataset.deger);
            });
        });
    }

    function cipKaldir(grup, deger) {
        const durum = durumOku();
        if (grup === 'fiyat') { durum.fiyatMin = null; durum.fiyatMax = null; }
        else if (grup === 'stok') { durum.sadeceStokta = false; }
        else if (grup === 'minPuan') { durum.minPuan = null; }
        else if (grup === 'format') { durum.format = durum.format.filter(f => f !== deger); }
        else { durum[grup] = durum[grup].filter(id => String(id) !== String(deger)); }
        durum.sayfa = 1;
        urlGuncelle(durum);
        sayfayiTazele();
    }

    /* ---------------- FORMDAN DURUM OKUMA + UYGULAMA ---------------- */
    function formdanDurumOlustur() {
        const durum = durumOku();
        durum.kategoriId = grupSecimleriniOku('kategoriId');
        durum.yazarId = grupSecimleriniOku('yazarId');
        durum.yayineviId = grupSecimleriniOku('yayineviId');
        durum.format = [...document.querySelectorAll('.bb-format-check:checked')].map(cb => cb.value);
        durum.sadeceStokta = document.getElementById('bbSadeceStokta').checked;
        const secilenPuan = document.querySelector('.bb-puan-radio:checked');
        durum.minPuan = (secilenPuan && secilenPuan.value) ? Number(secilenPuan.value) : null;
        durum.sirala = document.getElementById('bbSiralaSelect').value;
        durum.sayfa = 1;
        return durum;
    }

    function grupSecimleriniOku(grup) {
        return [...document.querySelectorAll(`.bb-genel-check[data-grup="${grup}"]:checked`)].map(cb => Number(cb.value));
    }

    function filtreDegisikliginiDinle() {
        document.querySelectorAll('.bb-genel-check, .bb-format-check, #bbSadeceStokta, .bb-puan-radio').forEach(el => {
            el.addEventListener('change', () => {
                urlGuncelle(formdanDurumOlustur());
                sayfayiTazele();
            });
        });

        document.getElementById('bbSiralaSelect').addEventListener('change', () => {
            urlGuncelle(formdanDurumOlustur());
            sayfayiTazele();
        });

        document.getElementById('bbFiyatUygula').addEventListener('click', () => {
            const durum = formdanDurumOlustur();
            durum.fiyatMin = Number(document.getElementById('bbFiyatMinInput').value);
            durum.fiyatMax = Number(document.getElementById('bbFiyatMaxInput').value);
            urlGuncelle(durum);
            sayfayiTazele();
        });

        document.getElementById('bbTumunuTemizle').addEventListener('click', filtreleriSifirla);
        document.getElementById('bbFiltreSifirla').addEventListener('click', filtreleriSifirla);

        document.getElementById('bbGridGorunum').addEventListener('click', () => gorunumDegistir('grid'));
        document.getElementById('bbListeGorunum').addEventListener('click', () => gorunumDegistir('liste'));

        filtrePaneliniHazirla();
    }

    /* ---------------- FILTRE PANELI AC/KAPA (Adım 3) ----------------
       Tum sol filtre sidebar'ini tek tikla gizler/gosterir; kapaliyken
       kitap grid'i tum genisligi kullanir. Son tercih localStorage'da
       tutulur, boylece sayfa yenilenince/baska sayfaya gecince kalir. */
    function filtrePaneliniHazirla() {
        const layout = document.getElementById('bbKatalogLayout');
        const btn = document.getElementById('bbFiltrePanelToggle');
        const ikon = document.getElementById('bbFiltrePanelToggleIkon');
        if (!layout || !btn) return;

        function uygula(kapali) {
            layout.classList.toggle('bb-filtre-kapali', kapali);
            ikon.textContent = kapali ? '▶' : '◀';
            btn.setAttribute('aria-expanded', String(!kapali));
        }

        const kayitliDurum = localStorage.getItem('bbFiltrePaneliKapali') === '1';
        uygula(kayitliDurum);

        btn.addEventListener('click', () => {
            const yeniDurum = !layout.classList.contains('bb-filtre-kapali');
            uygula(yeniDurum);
            localStorage.setItem('bbFiltrePaneliKapali', yeniDurum ? '1' : '0');
        });
    }

    function filtreleriSifirla() {
        window.history.pushState({}, '', 'katalog.html');
        window.location.reload();
    }

    function gorunumDegistir(tur) {
        const grid = document.getElementById('bbKatalogGrid');
        grid.classList.toggle('bb-liste-gorunum', tur === 'liste');
        document.getElementById('bbGridGorunum').classList.toggle('bb-active', tur === 'grid');
        document.getElementById('bbListeGorunum').classList.toggle('bb-active', tur === 'liste');
        localStorage.setItem('bb-gorunum', tur);
    }

    /* ---------------- SAYFALAMA ---------------- */
    function sayfalamaRenderla(sonuc) {
        const kutu = document.getElementById('bbSayfalama');
        kutu.innerHTML = '';
        if (sonuc.toplamSayfa <= 1) return;

        const ekle = (etiket, sayfa, aktif, devreDisi) => {
            const btn = document.createElement('button');
            btn.className = 'bb-page-btn' + (aktif ? ' bb-active' : '');
            btn.textContent = etiket;
            btn.disabled = !!devreDisi;
            btn.addEventListener('click', () => {
                const durum = durumOku();
                durum.sayfa = sayfa;
                urlGuncelle(durum);
                window.scrollTo({ top: 0, behavior: 'smooth' });
                sayfayiTazele();
            });
            kutu.appendChild(btn);
        };

        ekle('‹', sonuc.sayfa - 1, false, sonuc.sayfa <= 1);
        const baslangic = Math.max(1, sonuc.sayfa - 2);
        const bitis = Math.min(sonuc.toplamSayfa, baslangic + 4);
        for (let s = baslangic; s <= bitis; s++) ekle(String(s), s, s === sonuc.sayfa, false);
        ekle('›', sonuc.sayfa + 1, false, sonuc.sayfa >= sonuc.toplamSayfa);
    }

    /* ---------------- ANA YUKLEME ---------------- */
    async function sayfayiTazele() {
        const durum = durumOku();
        const iskelet = document.getElementById('bbKatalogIskelet');
        const grid = document.getElementById('bbKatalogGrid');
        const bos = document.getElementById('bbKatalogBos');

        iskelet.classList.remove('d-none');
        grid.innerHTML = '';
        bos.classList.add('d-none');

        aktifFiltreleriRenderla(durum);

        let baslik = 'Kitaplar';
        if (durum.kategoriId.length === 1) baslik = (kategoriler.find(k => k.kategoriId === durum.kategoriId[0]) || {}).kategoriAdi || baslik;
        else if (durum.yazarId.length === 1) baslik = (yazarlar.find(y => y.yazarId === durum.yazarId[0]) || {}).yazarAdi || baslik;
        else if (durum.yayineviId.length === 1) baslik = (yayinevleri.find(y => y.yayineviId === durum.yayineviId[0]) || {}).yayineviAdi || baslik;
        else if (durum.q) baslik = `"${durum.q}" için sonuçlar`;
        document.getElementById('bbKatalogBaslik').textContent = baslik;
        document.getElementById('bbKatalogBaslikBreadcrumb').textContent = baslik;

        try {
            const p = durumdanUrlOlustur(durum);
            p.set('sayfaBoyutu', SAYFA_BOYUTU);
            const res = await fetch('api/katalog?' + p.toString());
            const sonuc = await res.json();

            iskelet.classList.add('d-none');
            document.getElementById('bbKatalogSonucSayisi').textContent = `${sonuc.toplamKayit} kitap bulundu`;

            if (!sonuc.kitaplar || sonuc.kitaplar.length === 0) {
                bos.classList.remove('d-none');
                document.getElementById('bbSayfalama').innerHTML = '';
                return;
            }

            await window.BB.favorilerHazir;
            sonuc.kitaplar.forEach(k => grid.appendChild(window.BB.kitapKartOlustur(k)));
            sayfalamaRenderla(sonuc);
        } catch (err) {
            iskelet.classList.add('d-none');
            bos.classList.remove('d-none');
            bos.textContent = 'Kitaplar yüklenemedi: ' + err.message;
        }
    }

    function filtreAkordeonlariniHazirla() {
        document.querySelectorAll('.bb-filtre-akordeon-baslik').forEach(btn => {
            btn.addEventListener('click', () => {
                const blok = btn.closest('.bb-filtre-akordeon');
                const kapaliMi = blok.classList.toggle('bb-kapali');
                btn.setAttribute('aria-expanded', kapaliMi ? 'false' : 'true');
            });
        });
    }

    /* ---------------- BASLAT ---------------- */
    document.addEventListener('DOMContentLoaded', async () => {
        fiyatSliderHazirla();
        aramaKutusuFiltreleme('bbYazarAra', 'bbYazarFiltreListe');
        aramaKutusuFiltreleme('bbYayineviAra', 'bbYayineviFiltreListe');
        filtreAkordeonlariniHazirla();

        const kayitliGorunum = localStorage.getItem('bb-gorunum') || 'grid';
        gorunumDegistir(kayitliGorunum);

        await filtrePaneliniDoldur();
        filtreDegisikliginiDinle();
        await sayfayiTazele();
    });
})();
