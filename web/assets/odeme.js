/* ============================================================
   BOOMBOOK — Odeme (Checkout) Sayfasi JS
   sepet.html'deki "Odemeye Gec" butonundan buraya gelinir. Burada
   teslimat adresi + odeme yontemi toplanir, ardindan siparis
   api/siparis uc noktasina POST edilerek olusturulur.

   NOT (onemli, dogruluk icin belirtiyoruz): bu projede backend Java
   kaynak kodu elimde degil (sadece frontend/JSP dosyalari var), bu
   yuzden asagida "adres" ve "odemeYontemi" alanlari istek govdesine
   (body) EKLENIYOR ama api/siparis uc noktasinin bu alanlari su an
   OKUYUP KAYDETTIGINDEN EMIN DEGILIZ. Once oldugu gibi parametresiz
   calisan bu uc nokta bozulmasin diye govdeye ekstra alan eklemek
   guvenlidir (JSON parser'lar bilinmeyen alanlari yok sayar), ama
   adresin siparise gercekten islenmesi icin backend tarafinin da
   guncellenmesi gerekir.
   ============================================================ */

(() => {
    async function siparisSepetiGetir() {
        try {
            const res = await fetch('api/sepet');
            if (res.status === 401) return { misafir: true, liste: [] };
            if (!res.ok) throw new Error('Sunucu hatası: ' + res.status);
            return { misafir: false, liste: await res.json() };
        } catch (err) {
            return { misafir: true, liste: [] };
        }
    }

    function kalemMiniHtml(k) {
        const birimFiyat = k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat;
        return `
            <div class="bb-odeme-ozet-kalem">
                <span>${EK.escapeHtml(k.kitapAdi)} × ${k.adet}</span>
                <span>${EK.fiyatFormat(birimFiyat * k.adet)}</span>
            </div>`;
    }

    function odemeYontemiToggleHazirla() {
        const kartRadio = document.getElementById('odemeYontemKart');
        const kartAlanlari = document.getElementById('bbKartAlanlari');
        document.querySelectorAll('input[name="odemeYontemi"]').forEach(radio => {
            radio.addEventListener('change', () => {
                kartAlanlari.classList.toggle('bb-open', kartRadio.checked);
                kartAlanlari.querySelectorAll('input').forEach(inp => { inp.required = kartRadio.checked; });
            });
        });
        kartAlanlari.querySelectorAll('input').forEach(inp => { inp.required = kartRadio.checked; });
    }

    async function sayfayiRenderla() {
        const iskelet = document.getElementById('bbOdemeIskelet');
        const bosDurum = document.getElementById('bbOdemeBosDurum');
        const form = document.getElementById('bbOdemeForm');

        const { misafir, liste } = await siparisSepetiGetir();
        iskelet.classList.add('d-none');

        if (misafir) {
            // Guvenlik agi: sepet.html'den zaten giris kontrolu yapilarak gelinir,
            // ama biri odeme.html'e direkt linkle gelirse yine de yonlendir.
            window.location.href = 'login.jsp?sonraki=' + encodeURIComponent('odeme.html');
            return;
        }

        if (liste.length === 0) {
            bosDurum.classList.remove('d-none');
            return;
        }

        form.classList.remove('d-none');
        document.getElementById('bbOdemeKalemler').innerHTML = liste.map(kalemMiniHtml).join('');
        const araToplam = liste.reduce((t, k) => t + (k.indirimliFiyat != null ? k.indirimliFiyat : k.fiyat) * k.adet, 0);
        document.getElementById('bbOdemeAraToplam').textContent = EK.fiyatFormat(araToplam);
        document.getElementById('bbOdemeGenelToplam').textContent = EK.fiyatFormat(araToplam);
    }

    async function siparisiOnayla(e) {
        e.preventDefault();
        const form = document.getElementById('bbOdemeForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const btn = document.getElementById('bbOdemeOnaylaBtn');
        btn.disabled = true;
        btn.textContent = 'İşleniyor...';

        const govde = {
            adres: {
                adSoyad: document.getElementById('odemeAdSoyad').value.trim(),
                telefon: document.getElementById('odemeTelefon').value.trim(),
                acikAdres: document.getElementById('odemeAdres').value.trim(),
                il: document.getElementById('odemeIl').value.trim(),
                ilce: document.getElementById('odemeIlce').value.trim()
            },
            odemeYontemi: document.querySelector('input[name="odemeYontemi"]:checked').value
        };

        try {
            const res = await fetch('api/siparis', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(govde)
            });
            if (res.status === 401) {
                window.location.href = 'login.jsp?sonraki=' + encodeURIComponent('odeme.html');
                return;
            }
            const sonuc = await res.json();
            if (res.ok && sonuc.basarili) {
                EK.toast('Siparişiniz oluşturuldu! Sipariş No: ' + sonuc.siparisId, 'success');
                if (window.BB && window.BB.sepetPopoverGuncelle) window.BB.sepetPopoverGuncelle();
                setTimeout(() => { window.location.href = 'siparislerim.html'; }, 1200);
            } else {
                EK.toast(sonuc.mesaj || 'Sipariş oluşturulamadı.', 'error');
                btn.disabled = false;
                btn.textContent = 'Siparişi Onayla';
            }
        } catch (err) {
            EK.toast('Bağlantı hatası: ' + err.message, 'error');
            btn.disabled = false;
            btn.textContent = 'Siparişi Onayla';
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        odemeYontemiToggleHazirla();
        document.getElementById('bbOdemeForm').addEventListener('submit', siparisiOnayla);
        sayfayiRenderla();
    });
})();
