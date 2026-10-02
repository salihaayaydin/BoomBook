package model;

import java.util.List;

/**
 * Sol filtreli katalog sayfasi (ve arama sonuclari sayfasi) tarafindan
 * kullanilan filtre kriterleri. KitapDAO.araVeFiltrele(KitapFiltre) bu
 * nesneye gore dinamik SQL uretir.
 */
public class KitapFiltre {

    private String aramaMetni;
    private List<Integer> kategoriIdler;
    private List<Integer> yazarIdler;
    private List<Integer> yayineviIdler;
    private List<String> formatlar;      // "PDF" | "EPUB" | "MOBI"
    private Double fiyatMin;
    private Double fiyatMax;
    private Double minPuan;              // 4 -> "4 yildiz ve uzeri" gibi
    private boolean sadeceStokta;
    private String sirala;               // fiyatArtan | fiyatAzalan | cokSatan | enYeni
    private int sayfa = 1;
    private int sayfaBoyutu = 12;

    public String getAramaMetni() {
        return aramaMetni;
    }

    public void setAramaMetni(String aramaMetni) {
        this.aramaMetni = aramaMetni;
    }

    public List<Integer> getKategoriIdler() {
        return kategoriIdler;
    }

    public void setKategoriIdler(List<Integer> kategoriIdler) {
        this.kategoriIdler = kategoriIdler;
    }

    public List<Integer> getYazarIdler() {
        return yazarIdler;
    }

    public void setYazarIdler(List<Integer> yazarIdler) {
        this.yazarIdler = yazarIdler;
    }

    public List<Integer> getYayineviIdler() {
        return yayineviIdler;
    }

    public void setYayineviIdler(List<Integer> yayineviIdler) {
        this.yayineviIdler = yayineviIdler;
    }

    public List<String> getFormatlar() {
        return formatlar;
    }

    public void setFormatlar(List<String> formatlar) {
        this.formatlar = formatlar;
    }

    public Double getFiyatMin() {
        return fiyatMin;
    }

    public void setFiyatMin(Double fiyatMin) {
        this.fiyatMin = fiyatMin;
    }

    public Double getFiyatMax() {
        return fiyatMax;
    }

    public void setFiyatMax(Double fiyatMax) {
        this.fiyatMax = fiyatMax;
    }

    public Double getMinPuan() {
        return minPuan;
    }

    public void setMinPuan(Double minPuan) {
        this.minPuan = minPuan;
    }

    public boolean isSadeceStokta() {
        return sadeceStokta;
    }

    public void setSadeceStokta(boolean sadeceStokta) {
        this.sadeceStokta = sadeceStokta;
    }

    public String getSirala() {
        return sirala;
    }

    public void setSirala(String sirala) {
        this.sirala = sirala;
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
}
