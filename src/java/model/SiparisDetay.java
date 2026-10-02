package model;

/**
 * siparis_detay tablosunu ve JSON istek/yanit govdesindeki
 * satir bilgisini temsil eder.
 */
public class SiparisDetay {

    private int siparisId;
    private int kitapId;
    private String kitapAdi; // yanitlarda kolaylik icin
    private String yazarAdi; // yanitlarda kolaylik icin (siparislerim karti icin)
    private String kapakResmiUrl; // yanitlarda kolaylik icin (siparislerim karti icin)
    private double birimFiyat;
    private int adet;

    public SiparisDetay() {
    }

    public SiparisDetay(int kitapId, double birimFiyat, int adet) {
        this.kitapId = kitapId;
        this.birimFiyat = birimFiyat;
        this.adet = adet;
    }

    public int getSiparisId() {
        return siparisId;
    }

    public void setSiparisId(int siparisId) {
        this.siparisId = siparisId;
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

    public double getBirimFiyat() {
        return birimFiyat;
    }

    public void setBirimFiyat(double birimFiyat) {
        this.birimFiyat = birimFiyat;
    }

    public int getAdet() {
        return adet;
    }

    public void setAdet(int adet) {
        this.adet = adet;
    }
}
