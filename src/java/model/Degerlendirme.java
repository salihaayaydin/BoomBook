package model;

/** degerlendirme tablosunu (yorum + puan) temsil eder. */
public class Degerlendirme {

    private int degerlendirmeId;
    private int kullaniciId;
    private String kullaniciAdSoyad; // sadece listelemede (join) doldurulur
    private int kitapId;
    private int puan;                // 1-5
    private String yorum;
    private String resimUrl;         // kullanicinin ekledigi opsiyonel fotograf
    private String tarih;

    public int getDegerlendirmeId() {
        return degerlendirmeId;
    }

    public void setDegerlendirmeId(int degerlendirmeId) {
        this.degerlendirmeId = degerlendirmeId;
    }

    public int getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(int kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public String getKullaniciAdSoyad() {
        return kullaniciAdSoyad;
    }

    public void setKullaniciAdSoyad(String kullaniciAdSoyad) {
        this.kullaniciAdSoyad = kullaniciAdSoyad;
    }

    public int getKitapId() {
        return kitapId;
    }

    public void setKitapId(int kitapId) {
        this.kitapId = kitapId;
    }

    public int getPuan() {
        return puan;
    }

    public void setPuan(int puan) {
        this.puan = puan;
    }

    public String getYorum() {
        return yorum;
    }

    public void setYorum(String yorum) {
        this.yorum = yorum;
    }

    public String getResimUrl() {
        return resimUrl;
    }

    public void setResimUrl(String resimUrl) {
        this.resimUrl = resimUrl;
    }

    public String getTarih() {
        return tarih;
    }

    public void setTarih(String tarih) {
        this.tarih = tarih;
    }
}
