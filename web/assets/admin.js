(() => {

    async function yayinevleriniDoldur() {
        const select = document.getElementById('ekIndirimYayinevi');
        try {
            const res = await fetch('api/yayinevleri');
            const liste = await res.json();
            if (!Array.isArray(liste) || liste.length === 0) {
                select.innerHTML = '<option value="">Yayınevi bulunamadı</option>';
                return;
            }
            select.innerHTML = liste.map(y => `<option value="${y.yayineviId}">${y.yayineviAdi}</option>`).join('');
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
                <tr class="${k.stokMiktari < 5 ? 'ek-row-critical' : ''}">
                    <td>${k.kitapAdi}</td>
                    <td>${k.yayineviAdi || '-'}</td>
                    <td>${EK.fiyatFormat(k.fiyat)}</td>
                    <td>${k.indirimliFiyat != null ? EK.fiyatFormat(k.indirimliFiyat) : '<span class="text-muted">-</span>'}</td>
                    <td><span class="badge ${k.stokMiktari < 5 ? 'ek-stock-badge-low' : 'ek-stock-badge-ok'}">${k.stokMiktari}</span></td>
                </tr>`).join('');

            wrapper.classList.remove('d-none');
        } catch (err) {
            yukleniyor.classList.add('d-none');
            govde.innerHTML = `<tr><td colspan="5" class="text-danger">Stok verisi alınamadı: ${err.message}</td></tr>`;
            wrapper.classList.remove('d-none');
        }
    }

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
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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

    document.addEventListener('DOMContentLoaded', () => {
        yayinevleriniDoldur();
        stokTablosunuYukle();
    });
})();
