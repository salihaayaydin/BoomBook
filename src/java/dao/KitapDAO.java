package dao;

import model.Kitap;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * kitap tablosu uzerinde CRUD, filtreleme, stok ve indirim islemlerini yurutur.
 */
public class KitapDAO {

    // Ortak SELECT govdesi: kitap + yazar + kategori + yayinevi join'i
    private static final String TEMEL_SELECT =
            "SELECT k.kitap_id, k.kitap_adi, k.yazar_id, y.yazar_adi, " +
            "       k.kategori_id, kat.kategori_adi, k.yayinevi_id, ye.yayinevi_adi, " +
            "       k.sayfa_sayisi, k.fiyat, k.indirimli_fiyat, k.dosya_formati, " +
            "       k.dosya_boyutu_mb, k.dosya_yolu, k.kapak_resmi_url, k.aciklama, " +
            "       k.yayin_tarihi, k.stok_miktari " +
            "FROM kitap k " +
            "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
            "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id " +
            "LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id ";

    /** Tum kitaplari getirir. */
    public List<Kitap> getAll() throws SQLException {
        String sql = TEMEL_SELECT + "ORDER BY k.kitap_id DESC";
        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(mapToKitap(rs));
            }
        }
        return liste;
    }

    /** Belirli bir kategoriye ait kitaplari getirir. kategoriId null ise tumu doner. */
    public List<Kitap> getByKategori(Integer kategoriId) throws SQLException {
        if (kategoriId == null) {
            return getAll();
        }
        String sql = TEMEL_SELECT + "WHERE k.kategori_id = ? ORDER BY k.kitap_id DESC";
        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kategoriId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapToKitap(rs));
                }
            }
        }
        return liste;
    }

    /**
     * Kategori, yazar ve/veya yayinevi id'lerine gore kitaplari filtreler.
     * Parametrelerden herhangi biri null ise o alana gore filtre uygulanmaz.
     * Hepsi null ise tum kitaplar doner.
     */
    public List<Kitap> getFiltered(Integer kategoriId, Integer yazarId, Integer yayineviId) throws SQLException {
        StringBuilder sql = new StringBuilder(TEMEL_SELECT).append("WHERE 1=1 ");
        List<Integer> parametreler = new ArrayList<>();

        if (kategoriId != null) {
            sql.append("AND k.kategori_id = ? ");
            parametreler.add(kategoriId);
        }
        if (yazarId != null) {
            sql.append("AND k.yazar_id = ? ");
            parametreler.add(yazarId);
        }
        if (yayineviId != null) {
            sql.append("AND k.yayinevi_id = ? ");
            parametreler.add(yayineviId);
        }
        sql.append("ORDER BY k.kitap_id DESC");

        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametreler.size(); i++) {
                ps.setInt(i + 1, parametreler.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapToKitap(rs));
                }
            }
        }
        return liste;
    }

    /** Tek bir kitabi id ile getirir, bulunamazsa null doner. */
    public Kitap getById(int kitapId) throws SQLException {
        String sql = TEMEL_SELECT + "WHERE k.kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kitapId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToKitap(rs);
                }
            }
        }
        return null;
    }

    /**
     * Belirtilen kitabin stogunu verilen miktar kadar azaltir.
     * Ayni transaction icinde SiparisDAO tarafindan cagrilmak uzere Connection parametresi alir.
     * Stok yetersizse false doner, hicbir guncelleme yapilmaz.
     */
    public boolean stokDustur(Connection conn, int kitapId, int miktar) throws SQLException {
        String sql = "UPDATE kitap SET stok_miktari = stok_miktari - ? " +
                     "WHERE kitap_id = ? AND stok_miktari >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, miktar);
            ps.setInt(2, kitapId);
            ps.setInt(3, miktar);
            int etkilenen = ps.executeUpdate();
            return etkilenen > 0;
        }
    }

    /** Admin panelinden bir kitabin stogunu dogrudan gunceller (yeni deger olarak). */
    public boolean stokGuncelle(int kitapId, int yeniStok) throws SQLException {
        String sql = "UPDATE kitap SET stok_miktari = ? WHERE kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, yeniStok);
            ps.setInt(2, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Belirtilen yayinevine ait tum kitaplara toplu indirim uygular.
     * indirim_orani yuzde olarak verilir (ornegin 20 -> %20 indirim).
     * indirimli_fiyat = fiyat * (1 - oran/100) olacak sekilde hesaplanir.
     * Etkilenen satir sayisini doner.
     */
    public int yayineviIndirimUygula(int yayineviId, double indirimOrani) throws SQLException {
        String sql = "UPDATE kitap SET indirimli_fiyat = ROUND(fiyat * (1 - ?/100), 2) " +
                     "WHERE yayinevi_id = ?";
        String yayineviGuncelle = "UPDATE yayinevi SET son_indirim_orani = ? WHERE yayinevi_id = ?";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, indirimOrani);
                ps.setInt(2, yayineviId);
                int etkilenen = ps.executeUpdate();

                try (PreparedStatement ps2 = conn.prepareStatement(yayineviGuncelle)) {
                    ps2.setDouble(1, indirimOrani);
                    ps2.setInt(2, yayineviId);
                    ps2.executeUpdate();
                }

                conn.commit();
                return etkilenen;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Kritik stok kurali: stogu esikDeger'in (orn. 5) altina dusen ve henuz
     * indirimli_fiyat atanmamis kitaplara otomatik olarak indirimOrani uygular.
     * Etkilenen satir sayisini doner. Zamanlanmis bir gorev (scheduler) veya
     * her stok dususunden sonra cagrilabilir.
     */
    public int kritikStokIndirimiUygula(int esikDeger, double indirimOrani) throws SQLException {
        String sql = "UPDATE kitap SET indirimli_fiyat = ROUND(fiyat * (1 - ?/100), 2) " +
                     "WHERE stok_miktari < ? AND stok_miktari >= 0 AND indirimli_fiyat IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, indirimOrani);
            ps.setInt(2, esikDeger);
            return ps.executeUpdate();
        }
    }

    /** Admin stok takip tablosu icin sadece stokla ilgili alanlari donen sorgu. */
    public List<Kitap> getStokDurumu() throws SQLException {
        String sql = "SELECT k.kitap_id, k.kitap_adi, k.stok_miktari, k.fiyat, k.indirimli_fiyat, " +
                     "       ye.yayinevi_adi " +
                     "FROM kitap k LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id " +
                     "ORDER BY k.stok_miktari ASC";
        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Kitap k = new Kitap();
                k.setKitapId(rs.getInt("kitap_id"));
                k.setKitapAdi(rs.getString("kitap_adi"));
                k.setStokMiktari(rs.getInt("stok_miktari"));
                k.setFiyat(rs.getDouble("fiyat"));
                double indirimli = rs.getDouble("indirimli_fiyat");
                k.setIndirimliFiyat(rs.wasNull() ? null : indirimli);
                k.setYayineviAdi(rs.getString("yayinevi_adi"));
                liste.add(k);
            }
        }
        return liste;
    }

    /** ResultSet satirini Kitap nesnesine cevirir. */
    private Kitap mapToKitap(ResultSet rs) throws SQLException {
        Kitap k = new Kitap();
        k.setKitapId(rs.getInt("kitap_id"));
        k.setKitapAdi(rs.getString("kitap_adi"));

        int yazarId = rs.getInt("yazar_id");
        k.setYazarId(rs.wasNull() ? null : yazarId);
        k.setYazarAdi(rs.getString("yazar_adi"));

        int kategoriId = rs.getInt("kategori_id");
        k.setKategoriId(rs.wasNull() ? null : kategoriId);
        k.setKategoriAdi(rs.getString("kategori_adi"));

        int yayineviId = rs.getInt("yayinevi_id");
        k.setYayineviId(rs.wasNull() ? null : yayineviId);
        k.setYayineviAdi(rs.getString("yayinevi_adi"));

        k.setSayfaSayisi(rs.getInt("sayfa_sayisi"));
        k.setFiyat(rs.getDouble("fiyat"));

        double indirimli = rs.getDouble("indirimli_fiyat");
        k.setIndirimliFiyat(rs.wasNull() ? null : indirimli);

        k.setDosyaFormati(rs.getString("dosya_formati"));
        k.setDosyaBoyutuMb(rs.getDouble("dosya_boyutu_mb"));
        k.setDosyaYolu(rs.getString("dosya_yolu"));
        k.setKapakResmiUrl(rs.getString("kapak_resmi_url"));
        k.setAciklama(rs.getString("aciklama"));

        Date yayinTarihi = rs.getDate("yayin_tarihi");
        k.setYayinTarihi(yayinTarihi != null ? yayinTarihi.toString() : null);

        k.setStokMiktari(rs.getInt("stok_miktari"));
        return k;
    }
}
