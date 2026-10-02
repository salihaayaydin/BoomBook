(() => {

    const iskelet = document.getElementById('ekIskelet');
    const grid = document.getElementById('ekKutuphaneGrid');
    const bosMesaj = document.getElementById('ekBosMesaj');
    const hataMesaj = document.getElementById('ekHataMesaj');
    const girisUyari = document.getElementById('ekGirisUyari');

    const PLACEHOLDER = 'https://placehold.co/300x450/3a1f35/ffffff?text=Kapak+Yok';

    function tarihFormat(isoBenzeriTarih) {
        if (!isoBenzeriTarih) return '';
        // "yyyy-MM-dd HH:mm:ss.S" (Timestamp.toString()) -> tr-TR tarih
        const d = new Date(isoBenzeriTarih.replace(' ', 'T'));
        if (isNaN(d.getTime())) return isoBenzeriTarih;
        return d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' });
    }

    function kitapKarti(k) {
        const kapak = k.kapakResmiUrl && k.kapakResmiUrl.trim() !== '' ? k.kapakResmiUrl : PLACEHOLDER;
        const col = document.createElement('div');
        col.className = 'col-6 col-md-4 col-lg-3';
        col.innerHTML = `
            <div class="bb-lib-card">
                <img src="${kapak}" class="bb-lib-cover" alt="${EK.escapeHtml(k.kitapAdi)}"
                     onerror="this.onerror=null;this.src='${PLACEHOLDER}'">
                <div class="bb-lib-body">
                    <h3 class="bb-lib-title">${EK.escapeHtml(k.kitapAdi)}</h3>
                    <div class="bb-lib-meta">${EK.escapeHtml(k.yazarAdi || 'Bilinmeyen Yazar')}</div>
                    <div class="bb-lib-meta mb-2">
                        ${k.dosyaFormati || 'E-Kitap'}${k.dosyaBoyutuMb ? ' &middot; ' + k.dosyaBoyutuMb + ' MB' : ''}
                    </div>
                    <div class="small text-muted mb-3 flex-grow-1">
                        Satın alma: ${tarihFormat(k.satinAlmaTarihi)}
                    </div>
                    <a class="bb-btn bb-btn-navy w-100" href="api/kutuphane/indir?kitapId=${k.kitapId}">
                        ⬇ İndir
                    </a>
                </div>
            </div>`;
        return col;
    }

    async function kutuphaneyiYukle() {
        iskelet.classList.remove('d-none');
        grid.classList.add('d-none');
        bosMesaj.classList.add('d-none');
        hataMesaj.classList.add('d-none');
        girisUyari.classList.add('d-none');
        grid.innerHTML = '';

        try {
            const res = await fetch('api/kutuphanem');

            if (res.status === 401) {
                iskelet.classList.add('d-none');
                girisUyari.classList.remove('d-none');
                return;
            }
            if (!res.ok) {
                throw new Error('Sunucu hatası: ' + res.status);
            }

            const liste = await res.json();

            iskelet.classList.add('d-none');
            grid.classList.remove('d-none');

            if (!Array.isArray(liste) || liste.length === 0) {
                bosMesaj.classList.remove('d-none');
                return;
            }

            liste.forEach(k => grid.appendChild(kitapKarti(k)));
        } catch (err) {
            iskelet.classList.add('d-none');
            hataMesaj.classList.remove('d-none');
            hataMesaj.textContent = 'Kütüphane yüklenemedi: ' + err.message;
        }
    }

    document.addEventListener('DOMContentLoaded', kutuphaneyiYukle);
})();
