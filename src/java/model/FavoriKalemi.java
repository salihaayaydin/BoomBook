package model;

/**
 * favori tablosundaki bir satiri, on yuzde gosterim icin gerekli
 * kitap bilgileriyle (join edilmis) birlikte temsil eder.
 */
public class FavoriKalemi {

    private int kitapId;
    private String kitapAdi;
    private String yazarAdi;
    private String kapakResmiUrl;
    private double fiyat;
    private Double indirimliFiyat;
    private int stokMiktari;

    public FavoriKalemi() {
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

    public double getFiyat() {
        return fiyat;
    }

    public void setFiyat(double fiyat) {
        this.fiyat = fiyat;
    }

    public Double getIndirimliFiyat() {
        return indirimliFiyat;
    }

    public void setIndirimliFiyat(Double indirimliFiyat) {
        this.indirimliFiyat = indirimliFiyat;
    }

    public int getStokMiktari() {
        return stokMiktari;
    }

    public void setStokMiktari(int stokMiktari) {
        this.stokMiktari = stokMiktari;
    }
}
