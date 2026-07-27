package dao;

import model.Kullanici;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * kullanici tablosu uzerinde CRUD ve kimlik dogrulama sorgulari.
 * Sifreler her zaman hash'lenmis halde (sifre_hash) saklanir/okunur;
 * duz metin sifre bu sinifa asla ulasmaz (BCrypt islemi AuthServlet'te yapilir).
 */
public class KullaniciDAO {

    private static final String SELECT_ALANLAR =
            "kullanici_id, ad_soyad, email, sifre_hash, rol, aktif, kayit_tarihi";

    public Kullanici getByEmail(String email) throws SQLException {
        String sql = "SELECT " + SELECT_ALANLAR + " FROM kullanici WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Kullanici getById(int kullaniciId) throws SQLException {
        String sql = "SELECT " + SELECT_ALANLAR + " FROM kullanici WHERE kullanici_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kullaniciId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Yeni kullanici kaydeder ve olusan kullanici_id'yi dolu Kullanici olarak doner. */
    public Kullanici kayitOl(String adSoyad, String email, String sifreHash) throws SQLException {
        String sql = "INSERT INTO kullanici (ad_soyad, email, sifre_hash, rol, aktif) " +
                     "VALUES (?, ?, ?, 'Musteri', 1)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, adSoyad);
            ps.setString(2, email);
            ps.setString(3, sifreHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Kullanici id alinamadi.");
                }
                return getById(keys.getInt(1));
            }
        }
    }

    /** email zaten kayitli mi? Kayit formunda benzersizlik kontrolu icin kullanilir. */
    public boolean emailKullanimda(String email) throws SQLException {
        String sql = "SELECT 1 FROM kullanici WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Kullanici map(ResultSet rs) throws SQLException {
        Kullanici k = new Kullanici();
        k.setKullaniciId(rs.getInt("kullanici_id"));
        k.setAdSoyad(rs.getString("ad_soyad"));
        k.setEmail(rs.getString("email"));
        k.setSifreHash(rs.getString("sifre_hash"));
        k.setRol(rs.getString("rol"));
        k.setAktif(rs.getBoolean("aktif"));
        Timestamp ts = rs.getTimestamp("kayit_tarihi");
        k.setKayitTarihi(ts != null ? ts.toString() : null);
        return k;
    }
}
