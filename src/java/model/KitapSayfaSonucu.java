package model;

import java.util.List;

/**
 * /api/kitaplar?sayfa=X cagrilarinin doneceği sayfalanmis sonuc govdesi.
 * (sayfa parametresi verilmediginde eski davranis korunur: duz dizi doner,
 * bu sinif kullanilmaz -- bkz. KitapServlet.)
 */
public class KitapSayfaSonucu {

    private List<Kitap> kitaplar;
    private int toplamKayit;
    private int sayfa;
    private int sayfaBoyutu;
    private int toplamSayfa;

    public KitapSayfaSonucu() {
    }

    public KitapSayfaSonucu(List<Kitap> kitaplar, int toplamKayit, int sayfa, int sayfaBoyutu) {
        this.kitaplar = kitaplar;
        this.toplamKayit = toplamKayit;
        this.sayfa = sayfa;
        this.sayfaBoyutu = sayfaBoyutu;
        this.toplamSayfa = sayfaBoyutu > 0 ? (int) Math.ceil((double) toplamKayit / sayfaBoyutu) : 0;
    }

    public List<Kitap> getKitaplar() {
        return kitaplar;
    }

    public void setKitaplar(List<Kitap> kitaplar) {
        this.kitaplar = kitaplar;
    }

    public int getToplamKayit() {
        return toplamKayit;
    }

    public void setToplamKayit(int toplamKayit) {
        this.toplamKayit = toplamKayit;
    }

    public int getSayfa() {
        return sayfa;
    }

    public void setSayfa(int sayfa) {
        this.sayfa = sayfa;
    }

    public int getSayfaBoyutu() {
        return sayfaBoyutu;
    }

    public void setSayfaBoyutu(int sayfaBoyutu) {
        this.sayfaBoyutu = sayfaBoyutu;
    }

    public int getToplamSayfa() {
        return toplamSayfa;
    }

    public void setToplamSayfa(int toplamSayfa) {
        this.toplamSayfa = toplamSayfa;
    }
}
