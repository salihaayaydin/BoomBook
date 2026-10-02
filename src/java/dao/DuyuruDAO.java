package dao;

import model.Duyuru;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** duyuru tablosu uzerinde CRUD islemleri (ana sayfa hero slider'i). */
public class DuyuruDAO {

    private static final String SELECT_ALANLAR =
            "duyuru_id, baslik, aciklama, buton_metni, buton_link, renk1, renk2, resim_url, tur, sira, aktif, olusturma_tarihi";

    /** Sadece aktif duyurulari, siraya gore, herkese acik ana sayfa icin getirir.
     *  tur = "slider" (buyuk hero banner) veya "kart" (kucuk kampanya kartlari); null verilirse tumu doner. */
    public List<Duyuru> listeAktif(String tur) throws SQLException {
        String sql = "SELECT " + SELECT_ALANLAR + " FROM duyuru WHERE aktif = 1"
                + (tur != null ? " AND tur = ?" : "")
                + " ORDER BY sira ASC, duyuru_id ASC";
        List<Duyuru> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (tur != null) ps.setString(1, tur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(map(rs));
                }
            }
        }
        return liste;
    }

    /** Admin paneli icin TUM duyurulari (aktif/pasif fark etmeksizin) getirir. */
    public List<Duyuru> listeTumu() throws SQLException {
        String sql = "SELECT " + SELECT_ALANLAR + " FROM duyuru ORDER BY sira ASC, duyuru_id ASC";
        return sorguCalistir(sql, null);
    }

    private List<Duyuru> sorguCalistir(String sql, Integer id) throws SQLException {
        List<Duyuru> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (id != null) ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(map(rs));
                }
            }
        }
        return liste;
    }

    public Duyuru ekle(Duyuru d) throws SQLException {
        String sql = "INSERT INTO duyuru (baslik, aciklama, buton_metni, buton_link, renk1, renk2, resim_url, tur, sira, aktif) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            doldur(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Duyuru id alinamadi.");
                return getById(keys.getInt(1));
            }
        }
    }

    public boolean guncelle(int id, Duyuru d) throws SQLException {
        String sql = "UPDATE duyuru SET baslik=?, aciklama=?, buton_metni=?, buton_link=?, renk1=?, renk2=?, " +
                     "resim_url=COALESCE(?, resim_url), tur=?, sira=?, aktif=? WHERE duyuru_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int son = doldur(ps, d);
            ps.setInt(son + 1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Sadece kampanya/banner resmi URL'sini gunceller (admin dosya yukleme sonrasi). */
    public boolean resimGuncelle(int id, String resimUrl) throws SQLException {
        String sql = "UPDATE duyuru SET resim_url = ? WHERE duyuru_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resimUrl);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean sil(int id) throws SQLException {
        String sql = "DELETE FROM duyuru WHERE duyuru_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Duyuru getById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_ALANLAR + " FROM duyuru WHERE duyuru_id = ?";
        List<Duyuru> liste = sorguCalistir(sql, id);
        return liste.isEmpty() ? null : liste.get(0);
    }

    private int doldur(PreparedStatement ps, Duyuru d) throws SQLException {
        int i = 1;
        ps.setString(i++, d.getBaslik());
        ps.setString(i++, d.getAciklama());
        ps.setString(i++, d.getButonMetni() != null ? d.getButonMetni() : "Keşfet");
        ps.setString(i++, d.getButonLink() != null ? d.getButonLink() : "katalog.html");
        ps.setString(i++, d.getRenk1() != null ? d.getRenk1() : "#2b2a6b");
        ps.setString(i++, d.getRenk2() != null ? d.getRenk2() : "#3a1f35");
        ps.setString(i++, d.getResimUrl());
        ps.setString(i++, d.getTur() != null ? d.getTur() : "slider");
        ps.setInt(i++, d.getSira());
        ps.setBoolean(i++, d.isAktif());
        return i - 1;
    }

    private Duyuru map(ResultSet rs) throws SQLException {
        Duyuru d = new Duyuru();
        d.setDuyuruId(rs.getInt("duyuru_id"));
        d.setBaslik(rs.getString("baslik"));
        d.setAciklama(rs.getString("aciklama"));
        d.setButonMetni(rs.getString("buton_metni"));
        d.setButonLink(rs.getString("buton_link"));
        d.setRenk1(rs.getString("renk1"));
        d.setRenk2(rs.getString("renk2"));
        d.setResimUrl(rs.getString("resim_url"));
        d.setTur(rs.getString("tur"));
        d.setSira(rs.getInt("sira"));
        d.setAktif(rs.getBoolean("aktif"));
        Timestamp ts = rs.getTimestamp("olusturma_tarihi");
        d.setOlusturmaTarihi(ts != null ? ts.toString() : null);
        return d;
    }
}
