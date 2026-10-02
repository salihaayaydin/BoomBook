package dao;

import model.FavoriKalemi;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * favori tablosu uzerinde islem yapar (kalp ikonu). Sepet ile ayni
 * mantik: kullaniciId'ye bagli, sunucu tarafinda kalici.
 */
public class FavoriDAO {

    /** Kullanicinin favori listesini (kitap bilgileriyle birlikte) getirir. */
    public List<FavoriKalemi> getFavoriler(int kullaniciId) throws SQLException {
        String sql = "SELECT f.kitap_id, k.kitap_adi, y.yazar_adi, k.kapak_resmi_url, " +
                     "       k.fiyat, k.indirimli_fiyat, k.stok_miktari " +
                     "FROM favori f " +
                     "JOIN kitap k ON f.kitap_id = k.kitap_id " +
                     "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
                     "WHERE f.kullanici_id = ? ORDER BY f.eklenme_tarihi DESC";

        List<FavoriKalemi> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FavoriKalemi kalem = new FavoriKalemi();
                    kalem.setKitapId(rs.getInt("kitap_id"));
                    kalem.setKitapAdi(rs.getString("kitap_adi"));
                    kalem.setYazarAdi(rs.getString("yazar_adi"));
                    kalem.setKapakResmiUrl(rs.getString("kapak_resmi_url"));
                    kalem.setFiyat(rs.getDouble("fiyat"));
                    double indirimli = rs.getDouble("indirimli_fiyat");
                    kalem.setIndirimliFiyat(rs.wasNull() ? null : indirimli);
                    kalem.setStokMiktari(rs.getInt("stok_miktari"));
                    liste.add(kalem);
                }
            }
        }
        return liste;
    }

    /** Sadece kitap_id'leri (hafif, ana sayfada kalp ikonlarini doldurmak icin) getirir. */
    public Set<Integer> getFavoriKitapIdleri(int kullaniciId) throws SQLException {
        String sql = "SELECT kitap_id FROM favori WHERE kullanici_id = ?";
        Set<Integer> idler = new HashSet<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idler.add(rs.getInt("kitap_id"));
                }
            }
        }
        return idler;
    }

    /** Favoriye ekler; zaten favorideyse bir sey yapmaz (INSERT IGNORE). */
    public void ekle(int kullaniciId, int kitapId) throws SQLException {
        String sql = "INSERT IGNORE INTO favori (kullanici_id, kitap_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            ps.executeUpdate();
        }
    }

    /** Favoriden kaldirir. */
    public boolean kaldir(int kullaniciId, int kitapId) throws SQLException {
        String sql = "DELETE FROM favori WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean favoriMi(int kullaniciId, int kitapId) throws SQLException {
        String sql = "SELECT 1 FROM favori WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int toplamAdet(int kullaniciId) throws SQLException {
        String sql = "SELECT COUNT(*) AS toplam FROM favori WHERE kullanici_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("toplam") : 0;
            }
        }
    }
}
