package model;

/** duyuru tablosunu (ana sayfa hero slider'i) temsil eder. */
public class Duyuru {

    private int duyuruId;
    private String baslik;
    private String aciklama;
    private String butonMetni;
    private String butonLink;
    private String renk1;
    private String renk2;
    private String resimUrl;
    private String tur;
    private int sira;
    private boolean aktif;
    private String olusturmaTarihi;

    public int getDuyuruId() {
        return duyuruId;
    }

    public void setDuyuruId(int duyuruId) {
        this.duyuruId = duyuruId;
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getButonMetni() {
        return butonMetni;
    }

    public void setButonMetni(String butonMetni) {
        this.butonMetni = butonMetni;
    }

    public String getButonLink() {
        return butonLink;
    }

    public void setButonLink(String butonLink) {
        this.butonLink = butonLink;
    }

    public String getRenk1() {
        return renk1;
    }

    public void setRenk1(String renk1) {
        this.renk1 = renk1;
    }

    public String getRenk2() {
        return renk2;
    }

    public void setRenk2(String renk2) {
        this.renk2 = renk2;
    }

    public String getResimUrl() {
        return resimUrl;
    }

    public void setResimUrl(String resimUrl) {
        this.resimUrl = resimUrl;
    }

    public String getTur() {
        return tur;
    }

    public void setTur(String tur) {
        this.tur = tur;
    }

    public int getSira() {
        return sira;
    }

    public void setSira(int sira) {
        this.sira = sira;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public String getOlusturmaTarihi() {
        return olusturmaTarihi;
    }

    public void setOlusturmaTarihi(String olusturmaTarihi) {
        this.olusturmaTarihi = olusturmaTarihi;
    }
}
