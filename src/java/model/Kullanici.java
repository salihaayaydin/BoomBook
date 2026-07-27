package model;

/**
 * kullanici tablosunu temsil eden model sinifi.
 * DIKKAT: sifreHash alani hicbir zaman JSON yanitlarinda disariya
 * verilmemelidir (bkz. KullaniciDAO / AuthServlet - genel kullanicilar
 * icin daima "PublicKullanici" gibi hash icermeyen bir gorunum uretilir).
 */
public class Kullanici {

    private int kullaniciId;
    private String adSoyad;
    private String email;
    private String sifreHash;
    private String rol;       // "Musteri" | "Admin"
    private boolean aktif;
    private String kayitTarihi;

    public Kullanici() {
    }

    public int getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(int kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public void setAdSoyad(String adSoyad) {
        this.adSoyad = adSoyad;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSifreHash() {
        return sifreHash;
    }

    public void setSifreHash(String sifreHash) {
        this.sifreHash = sifreHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public String getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(String kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(rol);
    }

    /**
     * Oturuma ve istemciye gonderilecek, sifre_hash ICERMEYEN gorunum.
     * HttpSession'a ve JSON yanitlarina daima bu tur bir kopya konulmali.
     */
    public Kullanici genelGorunum() {
        Kullanici k = new Kullanici();
        k.setKullaniciId(this.kullaniciId);
        k.setAdSoyad(this.adSoyad);
        k.setEmail(this.email);
        k.setRol(this.rol);
        k.setAktif(this.aktif);
        k.setKayitTarihi(this.kayitTarihi);
        // sifreHash bilerek atanmiyor
        return k;
    }
}
