package dao;

import model.Kategori;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** kategori tablosu uzerinde tam CRUD islemleri (Admin Paneli). */
public class KategoriDAO {

    public List<Kategori> liste() throws SQLException {
        String sql = "SELECT kategori_id, kategori_adi FROM kategori ORDER BY kategori_adi";
        List<Kategori> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new Kategori(rs.getInt("kategori_id"), rs.getString("kategori_adi")));
            }
        }
        return liste;
    }

    public Kategori getById(int id) throws SQLException {
        String sql = "SELECT kategori_id, kategori_adi FROM kategori WHERE kategori_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Kategori(rs.getInt("kategori_id"), rs.getString("kategori_adi")) : null;
            }
        }
    }

    public Kategori ekle(String kategoriAdi) throws SQLException {
        String sql = "INSERT INTO kategori (kategori_adi) VALUES (?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kategoriAdi);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Kategori id alinamadi.");
                return getById(keys.getInt(1));
            }
        }
    }

    public boolean guncelle(int id, String kategoriAdi) throws SQLException {
        String sql = "UPDATE kategori SET kategori_adi = ? WHERE kategori_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kategoriAdi);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Bu kategoriye bagli kac kitap oldugunu doner (silme oncesi uyari icin). */
    public int bagliKitapSayisi(int id) throws SQLException {
        String sql = "SELECT COUNT(*) AS toplam FROM kitap WHERE kategori_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("toplam") : 0;
            }
        }
    }

    /** Sil: semada kitap.kategori_id -> ON DELETE SET NULL oldugundan kitaplar silinmez, kategorisi bosalir. */
    public boolean sil(int id) throws SQLException {
        String sql = "DELETE FROM kategori WHERE kategori_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
