package dao;

import model.Degerlendirme;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * degerlendirme tablosu uzerinde islem yapar. Bir kullanici bir kitaba
 * SADECE BIR degerlendirme birakabilir (uq_degerlendirme_kullanici_kitap
 * benzersiz anahtari + ON DUPLICATE KEY UPDATE ile garanti edilir).
 */
public class DegerlendirmeDAO {

    /** Bir kitabin tum degerlendirmelerini (kullanici adiyla birlikte, en yeni once) getirir. */
    public List<Degerlendirme> getByKitap(int kitapId) throws SQLException {
        String sql = "SELECT d.degerlendirme_id, d.kullanici_id, k.ad_soyad, d.kitap_id, d.puan, d.yorum, d.resim_url, d.tarih " +
                     "FROM degerlendirme d JOIN kullanici k ON d.kullanici_id = k.kullanici_id " +
                     "WHERE d.kitap_id = ? ORDER BY d.tarih DESC";
        List<Degerlendirme> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(map(rs));
                }
            }
        }
        return liste;
    }

    /** Bir kullanicinin belirli bir kitaba yazdigi degerlendirmeyi (varsa) getirir. */
    public Degerlendirme kullaniciDegerlendirmesi(int kullaniciId, int kitapId) throws SQLException {
        String sql = "SELECT d.degerlendirme_id, d.kullanici_id, k.ad_soyad, d.kitap_id, d.puan, d.yorum, d.resim_url, d.tarih " +
                     "FROM degerlendirme d JOIN kullanici k ON d.kullanici_id = k.kullanici_id " +
                     "WHERE d.kullanici_id = ? AND d.kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Ortalama puani ve toplam degerlendirme sayisini doner: {ortalama, sayim}. */
    public double[] ortalamaVeSayim(int kitapId) throws SQLException {
        String sql = "SELECT AVG(puan) AS ortalama, COUNT(*) AS sayim FROM degerlendirme WHERE kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double ortalama = rs.getDouble("ortalama");
                    return new double[]{ rs.wasNull() ? 0 : ortalama, rs.getInt("sayim") };
                }
            }
        }
        return new double[]{ 0, 0 };
    }

    /**
     * Degerlendirme ekler; kullanici bu kitaba daha once yazdiysa GUNCELLER
     * (ayni kullanici+kitap icin ikinci bir satir olusmaz, uq kisitlamasi sayesinde).
     * resimUrl null verilirse ve bu bir GUNCELLEME ise, mevcut fotograf KORUNUR
     * (kullanici sadece yildiz/yorumunu degistirip fotografi silmek istemiyor olabilir).
     */
    public void ekleVeyaGuncelle(int kullaniciId, int kitapId, int puan, String yorum, String resimUrl) throws SQLException {
        String sql = "INSERT INTO degerlendirme (kullanici_id, kitap_id, puan, yorum, resim_url) VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE puan = VALUES(puan), yorum = VALUES(yorum), " +
                     "resim_url = COALESCE(VALUES(resim_url), resim_url), tarih = CURRENT_TIMESTAMP";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            ps.setInt(3, puan);
            ps.setString(4, yorum);
            ps.setString(5, resimUrl);
            ps.executeUpdate();
        }
    }

    /** Kullanicinin kendi degerlendirmesini siler. */
    public boolean sil(int kullaniciId, int kitapId) throws SQLException {
        String sql = "DELETE FROM degerlendirme WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    private Degerlendirme map(ResultSet rs) throws SQLException {
        Degerlendirme d = new Degerlendirme();
        d.setDegerlendirmeId(rs.getInt("degerlendirme_id"));
        d.setKullaniciId(rs.getInt("kullanici_id"));
        d.setKullaniciAdSoyad(rs.getString("ad_soyad"));
        d.setKitapId(rs.getInt("kitap_id"));
        d.setPuan(rs.getInt("puan"));
        d.setYorum(rs.getString("yorum"));
        d.setResimUrl(rs.getString("resim_url"));
        Timestamp ts = rs.getTimestamp("tarih");
        d.setTarih(ts != null ? ts.toString() : null);
        return d;
    }
}
