package dao;

import model.SepetKalemi;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * sepet tablosu uzerinde islem yapar. Sepet artik tarayici
 * localStorage'inda degil, kullaniciId'ye bagli olarak bu tabloda
 * tutulur; oturum kapatilip acildiginda korunur.
 */
public class SepetDAO {

    private static final String TEMEL_SELECT =
            "SELECT s.kitap_id, k.kitap_adi, k.kapak_resmi_url, k.fiyat, k.indirimli_fiyat, " +
            "       k.stok_miktari, s.adet " +
            "FROM sepet s JOIN kitap k ON s.kitap_id = k.kitap_id " +
            "WHERE s.kullanici_id = ? ORDER BY s.eklenme_tarihi ASC";

    /** Kullanicinin sepetini (kitap bilgileriyle birlikte) getirir. */
    public List<SepetKalemi> getSepet(int kullaniciId) throws SQLException {
        List<SepetKalemi> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(TEMEL_SELECT)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapToKalem(rs));
                }
            }
        }
        return liste;
    }

    /**
     * Sepete bir kitap ekler; kitap zaten sepette varsa adedini artirir.
     * Eklenen/guncellenen satirin son halini doner (null ise kitap bulunamadi demektir).
     */
    public SepetKalemi ekle(int kullaniciId, int kitapId, int adet) throws SQLException {
        String sql = "INSERT INTO sepet (kullanici_id, kitap_id, adet) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE adet = adet + VALUES(adet)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            ps.setInt(3, adet > 0 ? adet : 1);
            ps.executeUpdate();
        }
        return getKalem(kullaniciId, kitapId);
    }

    /** Sepetteki bir kitabin adedini dogrudan (mutlak deger olarak) gunceller. */
    public SepetKalemi adetGuncelle(int kullaniciId, int kitapId, int yeniAdet) throws SQLException {
        if (yeniAdet <= 0) {
            kaldir(kullaniciId, kitapId);
            return null;
        }
        String sql = "UPDATE sepet SET adet = ? WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, yeniAdet);
            ps.setInt(2, kullaniciId);
            ps.setInt(3, kitapId);
            ps.executeUpdate();
        }
        return getKalem(kullaniciId, kitapId);
    }

    /** Sepetten tek bir kitabi kaldirir. */
    public boolean kaldir(int kullaniciId, int kitapId) throws SQLException {
        String sql = "DELETE FROM sepet WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Kullanicinin tum sepetini bosaltir (kendi baglantisini acar/kapatir). */
    public void temizle(int kullaniciId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            temizle(conn, kullaniciId);
        }
    }

    /** Ayni transaction icinde (ornegin siparis tamamlanirken) sepeti bosaltmak icin. */
    public void temizle(Connection conn, int kullaniciId) throws SQLException {
        String sql = "DELETE FROM sepet WHERE kullanici_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.executeUpdate();
        }
    }

    /** Sepetteki toplam kalem adedini (rozet gosterimi icin) doner. */
    public int toplamAdet(int kullaniciId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(adet), 0) AS toplam FROM sepet WHERE kullanici_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("toplam") : 0;
            }
        }
    }

    private SepetKalemi getKalem(int kullaniciId, int kitapId) throws SQLException {
        String sql = "SELECT s.kitap_id, k.kitap_adi, k.kapak_resmi_url, k.fiyat, k.indirimli_fiyat, " +
                     "       k.stok_miktari, s.adet " +
                     "FROM sepet s JOIN kitap k ON s.kitap_id = k.kitap_id " +
                     "WHERE s.kullanici_id = ? AND s.kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToKalem(rs);
                }
            }
        }
        return null;
    }

    private SepetKalemi mapToKalem(ResultSet rs) throws SQLException {
        SepetKalemi kalem = new SepetKalemi();
        kalem.setKitapId(rs.getInt("kitap_id"));
        kalem.setKitapAdi(rs.getString("kitap_adi"));
        kalem.setKapakResmiUrl(rs.getString("kapak_resmi_url"));
        kalem.setFiyat(rs.getDouble("fiyat"));
        double indirimli = rs.getDouble("indirimli_fiyat");
        kalem.setIndirimliFiyat(rs.wasNull() ? null : indirimli);
        kalem.setStokMiktari(rs.getInt("stok_miktari"));
        kalem.setAdet(rs.getInt("adet"));
        return kalem;
    }
}
