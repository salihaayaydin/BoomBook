package model;

import java.util.List;

public class Siparis {

    private int siparisId;
    private int kullaniciId;
    private double toplamTutar;
    private String odemeDurumu;
    private String siparisTarihi;
    private List<SiparisDetay> detaylar;

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
}
