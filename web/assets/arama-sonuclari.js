/* ============================================================
   BOOMBOOK — Arama Sonuclari Sayfasi JS
   ?q=metin (opsiyonel), &sirala=..., &sayfa=N parametreleriyle
   /api/kitaplar?sayfa=... sayfalanmis uc noktasini kullanir.
   ============================================================ */

(() => {
    const SAYFA_BOYUTU = 12;

    function urlParametreleri() {
        const params = new URLSearchParams(window.location.search);
        return {
            q: params.get('q') || '',
            sirala: params.get('sirala') || 'enYeni',
            sayfa: parseInt(params.get('sayfa'), 10) || 1
        };
    }

    function urlGuncelle(yeniParcalar) {
        const mevcut = urlParametreleri();
        const guncel = { ...mevcut, ...yeniParcalar };
        const params = new URLSearchParams();
        if (guncel.q) params.set('q', guncel.q);
        if (guncel.sirala && guncel.sirala !== 'enYeni') params.set('sirala', guncel.sirala);
        if (guncel.sayfa && guncel.sayfa !== 1) params.set('sayfa', guncel.sayfa);
        window.history.pushState({}, '', 'arama-sonuclari.html' + (params.toString() ? '?' + params.toString() : ''));
        return guncel;
    }

    function sayfalamaRenderla(sonuc, onSayfaDegis) {
        const kutu = document.getElementById('bbSayfalama');
        kutu.innerHTML = '';
        if (sonuc.toplamSayfa <= 1) return;

        const ekle = (etiket, sayfa, aktif, devreDisi) => {
            const btn = document.createElement('button');
            btn.className = 'bb-page-btn' + (aktif ? ' bb-active' : '');
            btn.textContent = etiket;
            btn.disabled = !!devreDisi;
            btn.addEventListener('click', () => onSayfaDegis(sayfa));
            kutu.appendChild(btn);
        };

        ekle('‹', sonuc.sayfa - 1, false, sonuc.sayfa <= 1);
        const baslangic = Math.max(1, sonuc.sayfa - 2);
        const bitis = Math.min(sonuc.toplamSayfa, baslangic + 4);
        for (let s = baslangic; s <= bitis; s++) {
            ekle(String(s), s, s === sonuc.sayfa, false);
        }
        ekle('›', sonuc.sayfa + 1, false, sonuc.sayfa >= sonuc.toplamSayfa);
    }

    async function sonuclariYukle() {
        const { q, sirala, sayfa } = urlParametreleri();

        document.getElementById('bbAramaBaslik').textContent = q ? `"${q}" için sonuçlar` : 'Tüm Kitaplar';
        document.getElementById('bbSiralaSelect').value = sirala;

        const iskelet = document.getElementById('bbAramaIskelet');
        const grid = document.getElementById('bbAramaGrid');
        const bos = document.getElementById('bbAramaBos');
        const sonucSayisiEl = document.getElementById('bbAramaSonucSayisi');

        iskelet.classList.remove('d-none');
        grid.innerHTML = '';
        bos.classList.add('d-none');

        try {
            const urlParams = new URLSearchParams();
            if (q) urlParams.set('q', q);
            urlParams.set('sirala', sirala);
            urlParams.set('sayfa', sayfa);
            urlParams.set('sayfaBoyutu', SAYFA_BOYUTU);

            const res = await fetch('api/kitaplar?' + urlParams.toString());
            const sonuc = await res.json();

            iskelet.classList.add('d-none');
            sonucSayisiEl.textContent = `${sonuc.toplamKayit} kitap bulundu`;

            if (!sonuc.kitaplar || sonuc.kitaplar.length === 0) {
                bos.classList.remove('d-none');
                document.getElementById('bbSayfalama').innerHTML = '';
                return;
            }

            await window.BB.favorilerHazir;
            sonuc.kitaplar.forEach(k => grid.appendChild(window.BB.kitapKartOlustur(k)));

            sayfalamaRenderla(sonuc, (yeniSayfa) => {
                urlGuncelle({ sayfa: yeniSayfa });
                window.scrollTo({ top: 0, behavior: 'smooth' });
                sonuclariYukle();
            });
        } catch (err) {
            iskelet.classList.add('d-none');
            bos.classList.remove('d-none');
            bos.querySelector('div + *')?.remove();
            bos.textContent = 'Sonuçlar yüklenemedi: ' + err.message;
        }
    }

    document.getElementById('bbSiralaSelect').addEventListener('change', (ev) => {
        urlGuncelle({ sirala: ev.target.value, sayfa: 1 });
        sonuclariYukle();
    });

    document.addEventListener('DOMContentLoaded', sonuclariYukle);
})();
