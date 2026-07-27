package model;

public class Yayinevi {

    private int yayineviId;
    private String yayineviAdi;
    private Double sonIndirimOrani;

    public Yayinevi() {
    }

    public Yayinevi(int yayineviId, String yayineviAdi) {
        this.yayineviId = yayineviId;
        this.yayineviAdi = yayineviAdi;
    }

    public int getYayineviId() {
        return yayineviId;
    }

    public void setYayineviId(int yayineviId) {
        this.yayineviId = yayineviId;
    }

    public String getYayineviAdi() {
        return yayineviAdi;
    }

    public void setYayineviAdi(String yayineviAdi) {
        this.yayineviAdi = yayineviAdi;
    }

    public Double getSonIndirimOrani() {
        return sonIndirimOrani;
    }

    public void setSonIndirimOrani(Double sonIndirimOrani) {
        this.sonIndirimOrani = sonIndirimOrani;
    }
}
