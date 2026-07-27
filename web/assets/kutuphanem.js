(() => {

    const iskelet = document.getElementById('ekIskelet');
    const grid = document.getElementById('ekKutuphaneGrid');
    const bosMesaj = document.getElementById('ekBosMesaj');
    const hataMesaj = document.getElementById('ekHataMesaj');
    const girisUyari = document.getElementById('ekGirisUyari');

    const PLACEHOLDER = 'https://placehold.co/300x450/ece3cd/16241f?text=Kapak+Yok';

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
            <div class="card ek-book-card h-100">
                <img src="${kapak}" class="ek-book-cover" alt="${k.kitapAdi}"
                     onerror="this.onerror=null;this.src='${PLACEHOLDER}'">
                <div class="card-body d-flex flex-column">
                    <h3 class="ek-book-title">${k.kitapAdi}</h3>
                    <div class="ek-book-meta mb-1">${k.yazarAdi || 'Bilinmeyen Yazar'}</div>
                    <div class="ek-book-meta mb-2">
                        ${k.dosyaFormati || 'E-Kitap'}${k.dosyaBoyutuMb ? ' &middot; ' + k.dosyaBoyutuMb + ' MB' : ''}
                    </div>
                    <div class="small text-muted mb-3 flex-grow-1">
                        Satın alma: ${tarihFormat(k.satinAlmaTarihi)}
                    </div>
                    <a class="btn btn-ek-gold w-100" href="api/kutuphane/indir?kitapId=${k.kitapId}">
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
