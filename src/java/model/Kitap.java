package model;

/**
 * kitap tablosunu ve JSON API yanitlarinda kullanilacak
 * (join edilmis) alanlari temsil eden model sinifi.
 */
public class Kitap {

    private int kitapId;
    private String kitapAdi;
    private Integer yazarId;
    private String yazarAdi;
    private Integer kategoriId;
    private String kategoriAdi;
    private Integer yayineviId;
    private String yayineviAdi;
    private int sayfaSayisi;
    private double fiyat;
    private Double indirimliFiyat;
    private String dosyaFormati;
    private double dosyaBoyutuMb;
    private String dosyaYolu;
    private String kapakResmiUrl;
    private String aciklama;
    private String yayinTarihi;
    private int stokMiktari;
    private Double ortalamaPuan;       // null = henuz hic degerlendirme yok
    private int degerlendirmeSayisi;

    public Kitap() {
    }

    // ---- Getter / Setter ----

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

    public Integer getYazarId() {
        return yazarId;
    }

    public void setYazarId(Integer yazarId) {
        this.yazarId = yazarId;
    }

    public String getYazarAdi() {
        return yazarAdi;
    }

    public void setYazarAdi(String yazarAdi) {
        this.yazarAdi = yazarAdi;
    }

    public Integer getKategoriId() {
        return kategoriId;
    }

    public void setKategoriId(Integer kategoriId) {
        this.kategoriId = kategoriId;
    }

    public String getKategoriAdi() {
        return kategoriAdi;
    }

    public void setKategoriAdi(String kategoriAdi) {
        this.kategoriAdi = kategoriAdi;
    }

    public Integer getYayineviId() {
        return yayineviId;
    }

    public void setYayineviId(Integer yayineviId) {
        this.yayineviId = yayineviId;
    }

    public String getYayineviAdi() {
        return yayineviAdi;
    }

    public void setYayineviAdi(String yayineviAdi) {
        this.yayineviAdi = yayineviAdi;
    }

    public int getSayfaSayisi() {
        return sayfaSayisi;
    }

    public void setSayfaSayisi(int sayfaSayisi) {
        this.sayfaSayisi = sayfaSayisi;
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

    public String getDosyaYolu() {
        return dosyaYolu;
    }

    public void setDosyaYolu(String dosyaYolu) {
        this.dosyaYolu = dosyaYolu;
    }

    public String getKapakResmiUrl() {
        return kapakResmiUrl;
    }

    public void setKapakResmiUrl(String kapakResmiUrl) {
        this.kapakResmiUrl = kapakResmiUrl;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getYayinTarihi() {
        return yayinTarihi;
    }

    public void setYayinTarihi(String yayinTarihi) {
        this.yayinTarihi = yayinTarihi;
    }

    public int getStokMiktari() {
        return stokMiktari;
    }

    public void setStokMiktari(int stokMiktari) {
        this.stokMiktari = stokMiktari;
    }

    public Double getOrtalamaPuan() {
        return ortalamaPuan;
    }

    public void setOrtalamaPuan(Double ortalamaPuan) {
        this.ortalamaPuan = ortalamaPuan;
    }

    public int getDegerlendirmeSayisi() {
        return degerlendirmeSayisi;
    }

    public void setDegerlendirmeSayisi(int degerlendirmeSayisi) {
        this.degerlendirmeSayisi = degerlendirmeSayisi;
    }
}
