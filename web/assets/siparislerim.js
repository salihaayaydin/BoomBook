(() => {
    const durumRenkleri = {
        'Bekliyor': 'text-bg-warning',
        'Tamamlandi': 'text-bg-success',
        'Basarisiz': 'text-bg-danger'
    };

    function tarihFormat(iso) {
        if (!iso) return '-';
        const d = new Date(iso.replace(' ', 'T'));
        if (isNaN(d)) return iso;
        return d.toLocaleString('tr-TR');
    }

    function detaySatiri(siparis) {
        const kalemler = (siparis.detaylar || []).map(d =>
            `<li>${d.kitapAdi} &times; ${d.adet} &mdash; ${EK.fiyatFormat(d.birimFiyat * d.adet)}</li>`
        ).join('');
        return `
            <tr class="d-none" id="detay-${siparis.siparisId}">
                <td colspan="5">
                    <ul class="mb-0 small">${kalemler || '<li class="text-muted">Kalem bulunamadı.</li>'}</ul>
                </td>
            </tr>`;
    }

    async function yukle() {
        const yukleniyor = document.getElementById('ekYukleniyor');
        const bosMesaj = document.getElementById('ekBosMesaj');
        const hataMesaj = document.getElementById('ekHataMesaj');
        const tabloWrapper = document.getElementById('ekTabloWrapper');
        const govde = document.getElementById('ekSiparisGovde');

        yukleniyor.classList.remove('d-none');
        bosMesaj.classList.add('d-none');
        hataMesaj.classList.add('d-none');
        tabloWrapper.classList.add('d-none');

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

            govde.innerHTML = sonuc.map(s => `
                <tr>
                    <td>#${s.siparisId}</td>
                    <td>${tarihFormat(s.siparisTarihi)}</td>
                    <td>${EK.fiyatFormat(s.toplamTutar)}</td>
                    <td><span class="badge ${durumRenkleri[s.odemeDurumu] || 'text-bg-secondary'}">${s.odemeDurumu}</span></td>
                    <td><button class="btn btn-sm btn-outline-secondary ek-detay-btn" data-id="${s.siparisId}">Detay</button></td>
                </tr>
                ${detaySatiri(s)}
            `).join('');

            govde.querySelectorAll('.ek-detay-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    document.getElementById('detay-' + btn.dataset.id).classList.toggle('d-none');
                });
            });

            tabloWrapper.classList.remove('d-none');
        } catch (err) {
            yukleniyor.classList.add('d-none');
            hataMesaj.textContent = 'Bağlantı hatası: ' + err.message;
            hataMesaj.classList.remove('d-none');
        }
    }

    document.addEventListener('DOMContentLoaded', yukle);
})();
