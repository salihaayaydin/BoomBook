package dao;

import model.Yayinevi;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** yayinevi tablosu uzerinde tam CRUD islemleri (Admin Paneli). */
public class YayineviDAO {

    public List<Yayinevi> liste() throws SQLException {
        String sql = "SELECT yayinevi_id, yayinevi_adi, son_indirim_orani FROM yayinevi ORDER BY yayinevi_adi";
        List<Yayinevi> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(map(rs));
            }
        }
        return liste;
    }

    public Yayinevi getById(int id) throws SQLException {
        String sql = "SELECT yayinevi_id, yayinevi_adi, son_indirim_orani FROM yayinevi WHERE yayinevi_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Yayinevi ekle(String yayineviAdi) throws SQLException {
        String sql = "INSERT INTO yayinevi (yayinevi_adi) VALUES (?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, yayineviAdi);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Yayinevi id alinamadi.");
                return getById(keys.getInt(1));
            }
        }
    }

    public boolean guncelle(int id, String yayineviAdi) throws SQLException {
        String sql = "UPDATE yayinevi SET yayinevi_adi = ? WHERE yayinevi_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yayineviAdi);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int bagliKitapSayisi(int id) throws SQLException {
        String sql = "SELECT COUNT(*) AS toplam FROM kitap WHERE yayinevi_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("toplam") : 0;
            }
        }
    }

    public boolean sil(int id) throws SQLException {
        String sql = "DELETE FROM yayinevi WHERE yayinevi_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Yayinevi map(ResultSet rs) throws SQLException {
        Yayinevi y = new Yayinevi();
        y.setYayineviId(rs.getInt("yayinevi_id"));
        y.setYayineviAdi(rs.getString("yayinevi_adi"));
        double oran = rs.getDouble("son_indirim_orani");
        y.setSonIndirimOrani(rs.wasNull() ? null : oran);
        return y;
    }
}
