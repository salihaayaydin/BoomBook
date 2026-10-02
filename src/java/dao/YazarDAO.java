package dao;

import model.Yazar;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** yazar tablosu uzerinde tam CRUD islemleri (Admin Paneli). */
public class YazarDAO {

    public List<Yazar> liste() throws SQLException {
        String sql = "SELECT yazar_id, yazar_adi, biyografi FROM yazar ORDER BY yazar_adi";
        List<Yazar> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(map(rs));
            }
        }
        return liste;
    }

    public Yazar getById(int id) throws SQLException {
        String sql = "SELECT yazar_id, yazar_adi, biyografi FROM yazar WHERE yazar_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Yazar ekle(String yazarAdi, String biyografi) throws SQLException {
        String sql = "INSERT INTO yazar (yazar_adi, biyografi) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, yazarAdi);
            ps.setString(2, biyografi);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Yazar id alinamadi.");
                return getById(keys.getInt(1));
            }
        }
    }

    public boolean guncelle(int id, String yazarAdi, String biyografi) throws SQLException {
        String sql = "UPDATE yazar SET yazar_adi = ?, biyografi = ? WHERE yazar_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yazarAdi);
            ps.setString(2, biyografi);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int bagliKitapSayisi(int id) throws SQLException {
        String sql = "SELECT COUNT(*) AS toplam FROM kitap WHERE yazar_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("toplam") : 0;
            }
        }
    }

    public boolean sil(int id) throws SQLException {
        String sql = "DELETE FROM yazar WHERE yazar_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Yazar map(ResultSet rs) throws SQLException {
        Yazar y = new Yazar();
        y.setYazarId(rs.getInt("yazar_id"));
        y.setYazarAdi(rs.getString("yazar_adi"));
        y.setBiyografi(rs.getString("biyografi"));
        return y;
    }
}
