package model;

/**
 * kutuphane tablosundaki bir satiri, "Kutuphanem" sayfasinda
 * gosterim icin gerekli kitap bilgileriyle birlikte temsil eder.
 */
public class KutuphaneKalemi {

    private int kitapId;
    private String kitapAdi;
    private String yazarAdi;
    private String kapakResmiUrl;
    private String dosyaFormati;
    private double dosyaBoyutuMb;
    private String indirmeBaglantisi;
    private String satinAlmaTarihi;

    public KutuphaneKalemi() {
    }

    public int getKitapId() {
        return kitapId;
    }

    public void setKitapId(int kitapId) {
        this.kitapId = kitapId;
    }

    public String getKitapAdi() {
        return kitapAdi;
    }

    public void setKitapAdi(String kitapAdi) {
        this.kitapAdi = kitapAdi;
    }

    public String getYazarAdi() {
        return yazarAdi;
    }

    public void setYazarAdi(String yazarAdi) {
        this.yazarAdi = yazarAdi;
    }

    public String getKapakResmiUrl() {
        return kapakResmiUrl;
    }

    public void setKapakResmiUrl(String kapakResmiUrl) {
        this.kapakResmiUrl = kapakResmiUrl;
    }

    public String getDosyaFormati() {
        return dosyaFormati;
    }

    public void setDosyaFormati(String dosyaFormati) {
        this.dosyaFormati = dosyaFormati;
    }

    public double getDosyaBoyutuMb() {
        return dosyaBoyutuMb;
    }

    public void setDosyaBoyutuMb(double dosyaBoyutuMb) {
        this.dosyaBoyutuMb = dosyaBoyutuMb;
    }

    public String getIndirmeBaglantisi() {
        return indirmeBaglantisi;
    }

    public void setIndirmeBaglantisi(String indirmeBaglantisi) {
        this.indirmeBaglantisi = indirmeBaglantisi;
    }

    public String getSatinAlmaTarihi() {
        return satinAlmaTarihi;
    }

    public void setSatinAlmaTarihi(String satinAlmaTarihi) {
        this.satinAlmaTarihi = satinAlmaTarihi;
    }
}
