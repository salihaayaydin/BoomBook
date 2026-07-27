package util;

import jakarta.servlet.ServletContext;

import java.io.File;

/**
 * Kapak resmi ve e-kitap dosyalarinin fiziksel olarak saklandigi
 * dizini cozumleyen ortak yardimci. Adim 5'teki admin dosya yukleme
 * ozelligiyle de paylasilacaktir.
 *
 * Varsayilan olarak web uygulamasinin gercek yolu altindaki
 * "uploads" klasoru kullanilir (ornegin Tomcat webapps/web_ekitap/uploads).
 * Ortam degiskeni / sistem property'si EKITAP_DOSYA_DIZINI tanimliysa
 * onun yerine o mutlak yol kullanilir (deploy sonrasi dosyalarin
 * yeniden deploy'da silinmemesi icin sunucu disinda bir dizin
 * gostermek onerilir, orn: -DEKITAP_DOSYA_DIZINI=/var/ekitap/uploads).
 */
public final class DosyaYardimcisi {

    private DosyaYardimcisi() {
    }

    /** uploads/ ana dizinini (yoksa olusturarak) doner. */
    public static File anaDizin(ServletContext ctx) {
        String ozelYol = oku("EKITAP_DOSYA_DIZINI");
        File dizin;
        if (ozelYol != null && !ozelYol.isBlank()) {
            dizin = new File(ozelYol);
        } else {
            dizin = new File(ctx.getRealPath("/"), "uploads");
        }
        if (!dizin.exists()) {
            dizin.mkdirs();
        }
        return dizin;
    }

    /** uploads/kapaklar alt dizinini (yoksa olusturarak) doner. */
    public static File kapaklarDizini(ServletContext ctx) {
        File dizin = new File(anaDizin(ctx), "kapaklar");
        if (!dizin.exists()) {
            dizin.mkdirs();
        }
        return dizin;
    }

    /** uploads/kitaplar alt dizinini (e-kitap dosyalari, yoksa olusturarak) doner. */
    public static File kitaplarDizini(ServletContext ctx) {
        File dizin = new File(anaDizin(ctx), "kitaplar");
        if (!dizin.exists()) {
            dizin.mkdirs();
        }
        return dizin;
    }

    /**
     * Veritabaninda saklanan goreli bir dosya_yolu / indirme_baglantisi
     * degerini (ornegin "kitaplar/kitap-12.pdf" ya da "/uploads/kitaplar/kitap-12.pdf")
     * diskteki gercek dosyaya cozumler. Mutlak yol / URL verilmisse oldugu gibi
     * kullanilmaya calisilir; sadece dosya adi verilmisse kitaplar dizininde aranir.
     */
    public static File dosyayaCozumle(ServletContext ctx, String kayitliYol) {
        if (kayitliYol == null || kayitliYol.isBlank()) {
            return null;
        }
        String temiz = kayitliYol.trim();
        if (temiz.startsWith("/uploads/")) {
            temiz = temiz.substring("/uploads/".length());
        } else if (temiz.startsWith("uploads/")) {
            temiz = temiz.substring("uploads/".length());
        }
        File aday = new File(anaDizin(ctx), temiz);
        if (aday.exists()) {
            return aday;
        }
        // Sadece dosya adi verilmis olabilir; kitaplar alt dizininde de dene.
        File altDizinAdayi = new File(kitaplarDizini(ctx), new File(temiz).getName());
        return altDizinAdayi.exists() ? altDizinAdayi : aday;
    }

    private static String oku(String anahtar) {
        String deger = System.getProperty(anahtar);
        if (deger == null || deger.isBlank()) {
            deger = System.getenv(anahtar);
        }
        return deger;
    }
}
