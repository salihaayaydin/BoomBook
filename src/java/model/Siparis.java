package model;

import java.util.List;

public class Siparis {

    private int siparisId;
    private int kullaniciId;
    private double toplamTutar;
    private double kargoUcreti;
    private String odemeDurumu;
    private String siparisTarihi;
    private List<SiparisDetay> detaylar;
    private String kullaniciAdSoyad; // sadece admin siparis listesinde doldurulur
    private String kullaniciEmail;   // sadece admin siparis listesinde doldurulur

    public Siparis() {
    }

    public int getSiparisId() {
        return siparisId;
    }

    public void setSiparisId(int siparisId) {
        this.siparisId = siparisId;
    }

    public int getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(int kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public double getToplamTutar() {
        return toplamTutar;
    }

    public void setToplamTutar(double toplamTutar) {
        this.toplamTutar = toplamTutar;
    }

    public double getKargoUcreti() {
        return kargoUcreti;
    }

    public void setKargoUcreti(double kargoUcreti) {
        this.kargoUcreti = kargoUcreti;
    }

    public String getOdemeDurumu() {
        return odemeDurumu;
    }

    public void setOdemeDurumu(String odemeDurumu) {
        this.odemeDurumu = odemeDurumu;
    }

    public String getSiparisTarihi() {
        return siparisTarihi;
    }

    public void setSiparisTarihi(String siparisTarihi) {
        this.siparisTarihi = siparisTarihi;
    }

    public List<SiparisDetay> getDetaylar() {
        return detaylar;
    }

    public void setDetaylar(List<SiparisDetay> detaylar) {
        this.detaylar = detaylar;
    }

    public String getKullaniciAdSoyad() {
        return kullaniciAdSoyad;
    }

    public void setKullaniciAdSoyad(String kullaniciAdSoyad) {
        this.kullaniciAdSoyad = kullaniciAdSoyad;
    }

    public String getKullaniciEmail() {
        return kullaniciEmail;
    }

    public void setKullaniciEmail(String kullaniciEmail) {
        this.kullaniciEmail = kullaniciEmail;
    }
}
