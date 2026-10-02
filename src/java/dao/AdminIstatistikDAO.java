package dao;

import model.Istatistik;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Admin panelindeki istatistik paneli icin sorgular (toplam satis, en cok satanlar, aylik gelir). */
public class AdminIstatistikDAO {

    public Istatistik getIstatistik() throws SQLException {
        Istatistik ist = new Istatistik();
        try (Connection conn = DBUtil.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(toplam_tutar), 0) AS toplam, COUNT(*) AS adet " +
                    "FROM siparis WHERE odeme_durumu = 'Tamamlandi'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ist.setToplamSatisTutari(rs.getDouble("toplam"));
                    ist.setToplamTamamlananSiparis(rs.getInt("adet"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS adet FROM kullanici");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ist.setToplamKullanici(rs.getInt("adet"));
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS adet FROM kitap");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ist.setToplamKitap(rs.getInt("adet"));
            }

            List<Istatistik.EnCokSatan> enCokSatanlar = new ArrayList<>();
            String enCokSql =
                    "SELECT sd.kitap_id, k.kitap_adi, SUM(sd.adet) AS satis_adedi " +
                    "FROM siparis_detay sd " +
                    "JOIN siparis s ON sd.siparis_id = s.siparis_id " +
                    "JOIN kitap k ON sd.kitap_id = k.kitap_id " +
                    "WHERE s.odeme_durumu = 'Tamamlandi' " +
                    "GROUP BY sd.kitap_id, k.kitap_adi " +
                    "ORDER BY satis_adedi DESC LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(enCokSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    enCokSatanlar.add(new Istatistik.EnCokSatan(
                            rs.getInt("kitap_id"), rs.getString("kitap_adi"), rs.getInt("satis_adedi")));
                }
            }
            ist.setEnCokSatanlar(enCokSatanlar);

            List<Istatistik.AylikGelir> aylikGelir = new ArrayList<>();
            String aylikSql =
                    "SELECT DATE_FORMAT(siparis_tarihi, '%Y-%m') AS ay, SUM(toplam_tutar) AS tutar " +
                    "FROM siparis WHERE odeme_durumu = 'Tamamlandi' " +
                    "AND siparis_tarihi >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                    "GROUP BY ay ORDER BY ay ASC";
            try (PreparedStatement ps = conn.prepareStatement(aylikSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    aylikGelir.add(new Istatistik.AylikGelir(rs.getString("ay"), rs.getDouble("tutar")));
                }
            }
            ist.setAylikGelir(aylikGelir);
        }
        return ist;
    }
}
