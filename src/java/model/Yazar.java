package model;

/**
 * yazar tablosunu temsil eden model sinifi.
 * Navbar'daki "Yazarlar" filtresi ve yazar bazli kitap listeleme icin kullanilir.
 */
public class Yazar {

    private int yazarId;
    private String yazarAdi;
    private String biyografi;

    public Yazar() {
    }

    public Yazar(int yazarId, String yazarAdi) {
        this.yazarId = yazarId;
        this.yazarAdi = yazarAdi;
    }

    public int getYazarId() {
        return yazarId;
    }

    public void setYazarId(int yazarId) {
        this.yazarId = yazarId;
    }

    public String getYazarAdi() {
        return yazarAdi;
    }

    public void setYazarAdi(String yazarAdi) {
        this.yazarAdi = yazarAdi;
    }

    public String getBiyografi() {
        return biyografi;
    }

    public void setBiyografi(String biyografi) {
        this.biyografi = biyografi;
    }
}
