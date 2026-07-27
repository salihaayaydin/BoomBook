package dao;

import model.KutuphaneKalemi;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * kutuphane tablosu uzerinde islem yapar: kullanicinin satin aldigi
 * e-kitaplarin listesi ve sahiplik kontrolu (indirme / degerlendirme
 * yapabilme yetkisi icin).
 */
public class KutuphaneDAO {

    /** Kullanicinin kutuphanesindeki tum kitaplari (en yeni satin alinan once) getirir. */
    public List<KutuphaneKalemi> getKutuphane(int kullaniciId) throws SQLException {
        String sql = "SELECT kt.kitap_id, k.kitap_adi, y.yazar_adi, k.kapak_resmi_url, " +
                     "       k.dosya_formati, k.dosya_boyutu_mb, kt.indirme_baglantisi, kt.satin_alma_tarihi " +
                     "FROM kutuphane kt " +
                     "JOIN kitap k ON kt.kitap_id = k.kitap_id " +
                     "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
                     "WHERE kt.kullanici_id = ? " +
                     "ORDER BY kt.satin_alma_tarihi DESC";

        List<KutuphaneKalemi> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KutuphaneKalemi kalem = new KutuphaneKalemi();
                    kalem.setKitapId(rs.getInt("kitap_id"));
                    kalem.setKitapAdi(rs.getString("kitap_adi"));
                    kalem.setYazarAdi(rs.getString("yazar_adi"));
                    kalem.setKapakResmiUrl(rs.getString("kapak_resmi_url"));
                    kalem.setDosyaFormati(rs.getString("dosya_formati"));
                    kalem.setDosyaBoyutuMb(rs.getDouble("dosya_boyutu_mb"));
                    kalem.setIndirmeBaglantisi(rs.getString("indirme_baglantisi"));
                    Timestamp ts = rs.getTimestamp("satin_alma_tarihi");
                    kalem.setSatinAlmaTarihi(ts != null ? ts.toString() : null);
                    liste.add(kalem);
                }
            }
        }
        return liste;
    }

    /**
     * Kullanicinin belirtilen kitabi satin alip almadigini (kutuphanesinde
     * olup olmadigini) kontrol eder. Indirme yetkisi ve degerlendirme
     * (yorum) yazma yetkisi bu metoda dayanir.
     */
    public boolean sahipMi(int kullaniciId, int kitapId) throws SQLException {
        String sql = "SELECT 1 FROM kutuphane WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Belirtilen kitabin dosya_yolu / indirme_baglantisi bilgisini kutuphane kaydindan getirir. */
    public String indirmeBaglantisiGetir(int kullaniciId, int kitapId) throws SQLException {
        String sql = "SELECT indirme_baglantisi FROM kutuphane WHERE kullanici_id = ? AND kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            ps.setInt(2, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("indirme_baglantisi") : null;
            }
        }
    }
}
