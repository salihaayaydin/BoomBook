package model;

/**
 * sepet tablosundaki bir satiri, on yuzde gosterim icin gerekli
 * kitap bilgileriyle (join edilmis) birlikte temsil eder.
 */
public class SepetKalemi {

    private int kitapId;
    private String kitapAdi;
    private String kapakResmiUrl;
    private double fiyat;
    private Double indirimliFiyat;
    private int stokMiktari;
    private int adet;

    public SepetKalemi() {
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

    public int getAdet() {
        return adet;
    }

    public void setAdet(int adet) {
        this.adet = adet;
    }

    /** Gecerli birim fiyat: indirimli fiyat varsa o, yoksa normal fiyat. */
    public double getBirimFiyat() {
        return indirimliFiyat != null ? indirimliFiyat : fiyat;
    }

    /** Bu kalemin satir toplami (birimFiyat * adet). */
    public double getSatirToplami() {
        return getBirimFiyat() * adet;
    }
}
