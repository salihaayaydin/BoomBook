(() => {
    const UCRETSIZ_KARGO_LIMIT = 150; // 

    let sonMisafirDurumu = false; 

    async function sepetiGetir() {
        try {
            const res = await fetch('api/sepet');
            if (res.status === 401) return { misafir: true, liste: EK.misafirSepetiOku() };
            if (!res.ok) throw new Error('Sunucu hatası: ' + res.status);
            return { misafir: false, liste: await res.json() };
        } catch (err) {
            if (err instanceof TypeError) return { misafir: true, liste: EK.misafirSepetiOku() };
            throw err;
        }
    }
    function kargoBolumunuGuncelle(araToplam) {
        const mesajEl = document.getElementById('bbKargoMesaj');
        const doluluk = document.getElementById('bbKargoDoluluk');
        const kargoUcretiEl = document.getElementById('bbKargoUcreti');

        if (araToplam >= UCRETSIZ_KARGO_LIMIT) {
            mesajEl.innerHTML = '🎉 <strong>Ücretsiz kargo</strong> kazandınız!';
            doluluk.style.width = '100%';
            kargoUcretiEl.textContent = 'Ücretsiz';
        } else {
            const kalan = UCRETSIZ_KARGO_LIMIT - araToplam;
            mesajEl.innerHTML = `Ücretsiz kargoya <strong>${EK.fiyatFormat(kalan)}</strong> kaldı!`;
            doluluk.style.width = Math.min(100, (araToplam / UCRETSIZ_KARGO_LIMIT) * 100) + '%';
            kargoUcretiEl.textContent = 'Ücretsiz (150 TL üzeri)';
        }
    }

    function kalemHtml(k) {
        const birimFiyat = k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat;
        const satirToplam = birimFiyat * k.adet;
        const stokLimitineUlasti = Number.isFinite(k.stokMiktari) && k.adet >= k.stokMiktari;
        return `
            <div class="bb-sepet-item" data-kitap-id="${k.kitapId}">
                ${k.kapakResmiUrl
                    ? `<img src="${k.kapakResmiUrl}" class="bb-sepet-item-cover" alt="${k.kitapAdi}">`
                    : `<div class="bb-sepet-item-cover"></div>`}
                <div>
                    <div class="bb-sepet-item-title"><a href="kitap-detay.html?id=${k.kitapId}">${k.kitapAdi}</a></div>
                    <div class="bb-sepet-item-author">${EK.fiyatFormat(birimFiyat)} / adet</div>
                    <div class="bb-sepet-qty">
                        <button type="button" class="bb-azalt" data-kitap-id="${k.kitapId}" data-adet="${k.adet}">−</button>
                        <span>${k.adet}</span>
                        <button type="button" class="bb-artir" data-kitap-id="${k.kitapId}" data-adet="${k.adet}" ${stokLimitineUlasti ? 'disabled title="Stokta bu kadar var"' : ''}>+</button>
                    </div>
                    ${stokLimitineUlasti ? `<div class="text-muted small mt-1">Stokta ${k.stokMiktari} adet kaldı</div>` : ''}
                </div>
                <div class="bb-sepet-item-satirtoplam">${EK.fiyatFormat(satirToplam)}</div>
                <button type="button" class="bb-sepet-item-remove" data-kitap-id="${k.kitapId}" title="Kaldır">&times;</button>
            </div>`;
    }

    async function sayfayiRenderla() {
        const iskelet = document.getElementById('bbSepetIskelet');
        const misafirBanner = document.getElementById('bbSepetMisafirBanner');
        const bosDurum = document.getElementById('bbSepetBosDurum');
        const doluLayout = document.getElementById('bbSepetDoluLayout');

        iskelet.classList.remove('d-none');
        misafirBanner.classList.add('d-none');
        bosDurum.classList.add('d-none');
        doluLayout.classList.add('d-none');

        try {
            const { misafir, liste } = await sepetiGetir();
            sonMisafirDurumu = misafir;
            iskelet.classList.add('d-none');

            if (misafir && liste.length > 0) {
                misafirBanner.classList.remove('d-none');
            }
            if (liste.length === 0) {
                bosDurum.classList.remove('d-none');
                return;
            }

            doluLayout.classList.remove('d-none');
            document.getElementById('bbSepetKalemler').innerHTML = liste.map(kalemHtml).join('');

            const araToplam = liste.reduce((t, k) => t + (k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat) * k.adet, 0);
            document.getElementById('bbAraToplam').textContent = EK.fiyatFormat(araToplam);
            document.getElementById('bbGenelToplam').textContent = EK.fiyatFormat(araToplam);
            kargoBolumunuGuncelle(araToplam);

            document.querySelectorAll('.bb-azalt').forEach(btn => btn.addEventListener('click', async () => {
                const adet = parseInt(btn.dataset.adet, 10);
                await EK.adetGuncelle(parseInt(btn.dataset.kitapId, 10), Math.max(1, adet - 1));
                sayfayiRenderla();
            }));
            document.querySelectorAll('.bb-artir').forEach(btn => btn.addEventListener('click', async () => {
                const adet = parseInt(btn.dataset.adet, 10);
                await EK.adetGuncelle(parseInt(btn.dataset.kitapId, 10), adet + 1);
                sayfayiRenderla();
            }));
            document.querySelectorAll('.bb-sepet-item-remove').forEach(btn => btn.addEventListener('click', async () => {
                await EK.sepettenCikar(parseInt(btn.dataset.kitapId, 10));
                sayfayiRenderla();
            }));
        } catch (err) {
            iskelet.classList.add('d-none');
            EK.toast('Sepet yüklenemedi: ' + err.message, 'error');
        }
    }

    function odemeyeGec() {
        if (sonMisafirDurumu) {
            window.location.href = 'login.jsp?sonraki=' + encodeURIComponent('odeme.html');
        } else {
            window.location.href = 'odeme.html';
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('bbSiparisTamamlaBtn').addEventListener('click', odemeyeGec);
        sayfayiRenderla();
    });
})();
