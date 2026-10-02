package model;

import java.util.List;

/** Admin istatistik paneli icin toplu sonuc govdesi. */
public class Istatistik {

    private double toplamSatisTutari;
    private int toplamTamamlananSiparis;
    private int toplamKullanici;
    private int toplamKitap;
    private List<EnCokSatan> enCokSatanlar;
    private List<AylikGelir> aylikGelir;

    public double getToplamSatisTutari() {
        return toplamSatisTutari;
    }

    public void setToplamSatisTutari(double toplamSatisTutari) {
        this.toplamSatisTutari = toplamSatisTutari;
    }

    public int getToplamTamamlananSiparis() {
        return toplamTamamlananSiparis;
    }

    public void setToplamTamamlananSiparis(int toplamTamamlananSiparis) {
        this.toplamTamamlananSiparis = toplamTamamlananSiparis;
    }

    public int getToplamKullanici() {
        return toplamKullanici;
    }

    public void setToplamKullanici(int toplamKullanici) {
        this.toplamKullanici = toplamKullanici;
    }

    public int getToplamKitap() {
        return toplamKitap;
    }

    public void setToplamKitap(int toplamKitap) {
        this.toplamKitap = toplamKitap;
    }

    public List<EnCokSatan> getEnCokSatanlar() {
        return enCokSatanlar;
    }

    public void setEnCokSatanlar(List<EnCokSatan> enCokSatanlar) {
        this.enCokSatanlar = enCokSatanlar;
    }

    public List<AylikGelir> getAylikGelir() {
        return aylikGelir;
    }

    public void setAylikGelir(List<AylikGelir> aylikGelir) {
        this.aylikGelir = aylikGelir;
    }

    public static class EnCokSatan {
        private int kitapId;
        private String kitapAdi;
        private int satisAdedi;

        public EnCokSatan(int kitapId, String kitapAdi, int satisAdedi) {
            this.kitapId = kitapId;
            this.kitapAdi = kitapAdi;
            this.satisAdedi = satisAdedi;
        }

        public int getKitapId() { return kitapId; }
        public String getKitapAdi() { return kitapAdi; }
        public int getSatisAdedi() { return satisAdedi; }
    }

    public static class AylikGelir {
        private String ay;   // "2026-01" formatinda
        private double tutar;

        public AylikGelir(String ay, double tutar) {
            this.ay = ay;
            this.tutar = tutar;
        }

        public String getAy() { return ay; }
        public double getTutar() { return tutar; }
    }
}
