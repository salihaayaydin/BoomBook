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
     * ONEMLI: nihai adet, kitabin GUNCEL stok_miktari ile sinirlandirilir
     * (musteri stoktan fazlasini sepete koyamaz). Kitap satiri FOR UPDATE
     * ile kilitlenir, boylece ayni anda gelen istekler yarisamaz.
     * Eklenen/guncellenen satirin son halini doner (null ise kitap bulunamadi demektir).
     */
    public SepetKalemi ekle(int kullaniciId, int kitapId, int eklenecekAdet) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer stok = stokOku(conn, kitapId);
                if (stok == null) {
                    conn.rollback();
                    return null; // kitap bulunamadi
                }

                int mevcutAdet = mevcutAdetOku(conn, kullaniciId, kitapId);
                int istenen = mevcutAdet + (eklenecekAdet > 0 ? eklenecekAdet : 1);
                int uygulanan = Math.min(istenen, Math.max(stok, 0));

                if (uygulanan > 0) {
                    String sql = "INSERT INTO sepet (kullanici_id, kitap_id, adet) VALUES (?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE adet = VALUES(adet)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, kullaniciId);
                        ps.setInt(2, kitapId);
                        ps.setInt(3, uygulanan);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return getKalem(kullaniciId, kitapId);
    }

    /** kitap.stok_miktari degerini FOR UPDATE ile kilitleyerek okur (transaction icinde kullanilmali). */
    private Integer stokOku(Connection conn, int kitapId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stok_miktari FROM kitap WHERE kitap_id = ? FOR UPDATE")) {
            ps.setInt(1, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("stok_miktari") : null;
            }
        }
    }

    private int mevcutAdetOku(Connection conn, int kullaniciId, int kitapId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT adet FROM sepet WHERE kullanici_id = ? AND kitap_id = ?")) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("adet") : 0;
            }
        }
    }

    /**
     * Sepetteki bir kitabin adedini dogrudan (mutlak deger olarak) gunceller.
     * ONEMLI: istenen adet, kitabin GUNCEL stok_miktari ile sinirlandirilir.
     */
    public SepetKalemi adetGuncelle(int kullaniciId, int kitapId, int yeniAdet) throws SQLException {
        if (yeniAdet <= 0) {
            kaldir(kullaniciId, kitapId);
            return null;
        }
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer stok = stokOku(conn, kitapId);
                int uygulanan = Math.min(yeniAdet, Math.max(stok != null ? stok : 0, 0));

                if (uygulanan <= 0) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sepet WHERE kullanici_id = ? AND kitap_id = ?")) {
                        ps.setInt(1, kullaniciId);
                        ps.setInt(2, kitapId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    return null;
                }

                String sql = "UPDATE sepet SET adet = ? WHERE kullanici_id = ? AND kitap_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, uygulanan);
                    ps.setInt(2, kullaniciId);
                    ps.setInt(3, kitapId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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
