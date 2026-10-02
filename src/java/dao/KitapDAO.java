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

    // Ortak SELECT govdesi: kitap + yazar + kategori + yayinevi + degerlendirme ortalamasi join'i
    private static final String TEMEL_SELECT =
            "SELECT k.kitap_id, k.kitap_adi, k.yazar_id, y.yazar_adi, " +
            "       k.kategori_id, kat.kategori_adi, k.yayinevi_id, ye.yayinevi_adi, " +
            "       k.sayfa_sayisi, k.fiyat, k.indirimli_fiyat, k.dosya_formati, " +
            "       k.dosya_boyutu_mb, k.dosya_yolu, k.kapak_resmi_url, k.aciklama, " +
            "       k.yayin_tarihi, k.stok_miktari, dg.ortalama_puan, COALESCE(dg.degerlendirme_sayisi, 0) AS degerlendirme_sayisi " +
            "FROM kitap k " +
            "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
            "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id " +
            "LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id " +
            "LEFT JOIN (SELECT kitap_id, AVG(puan) AS ortalama_puan, COUNT(*) AS degerlendirme_sayisi " +
            "           FROM degerlendirme GROUP BY kitap_id) dg ON dg.kitap_id = k.kitap_id ";

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

    // Arama/siralama/sayfalama icin satis adedini ve degerlendirme ortalamasini da iceren SELECT govdesi
    private static final String ARAMA_SELECT =
            "SELECT k.kitap_id, k.kitap_adi, k.yazar_id, y.yazar_adi, " +
            "       k.kategori_id, kat.kategori_adi, k.yayinevi_id, ye.yayinevi_adi, " +
            "       k.sayfa_sayisi, k.fiyat, k.indirimli_fiyat, k.dosya_formati, " +
            "       k.dosya_boyutu_mb, k.dosya_yolu, k.kapak_resmi_url, k.aciklama, " +
            "       k.yayin_tarihi, k.stok_miktari, COALESCE(sd.satis_adedi, 0) AS satis_adedi, " +
            "       dg.ortalama_puan, COALESCE(dg.degerlendirme_sayisi, 0) AS degerlendirme_sayisi " +
            "FROM kitap k " +
            "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
            "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id " +
            "LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id " +
            "LEFT JOIN (SELECT kitap_id, SUM(adet) AS satis_adedi FROM siparis_detay GROUP BY kitap_id) sd " +
            "       ON sd.kitap_id = k.kitap_id " +
            "LEFT JOIN (SELECT kitap_id, AVG(puan) AS ortalama_puan, COUNT(*) AS degerlendirme_sayisi " +
            "           FROM degerlendirme GROUP BY kitap_id) dg ON dg.kitap_id = k.kitap_id ";

    /** WHERE kosullarini (kategoriId/yazarId/yayineviId/arama) olusturup parametreleri doldurur. */
    private String whereVeParametreler(Integer kategoriId, Integer yazarId, Integer yayineviId,
                                        String aramaMetni, List<Object> parametreler) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        if (kategoriId != null) {
            where.append("AND k.kategori_id = ? ");
            parametreler.add(kategoriId);
        }
        if (yazarId != null) {
            where.append("AND k.yazar_id = ? ");
            parametreler.add(yazarId);
        }
        if (yayineviId != null) {
            where.append("AND k.yayinevi_id = ? ");
            parametreler.add(yayineviId);
        }
        if (aramaMetni != null && !aramaMetni.isBlank()) {
            where.append("AND (k.kitap_adi LIKE ? OR y.yazar_adi LIKE ? OR ye.yayinevi_adi LIKE ?) ");
            String desen = "%" + aramaMetni.trim() + "%";
            parametreler.add(desen);
            parametreler.add(desen);
            parametreler.add(desen);
        }
        return where.toString();
    }

    private String icinYerTutucular(int adet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < adet; i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.toString();
    }

    /** model.KitapFiltre'ye gore WHERE kosullarini olusturup parametreleri doldurur (sol filtreli katalog sayfasi). */
    private String whereVeParametreler(model.KitapFiltre f, List<Object> parametreler) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");

        if (f.getKategoriIdler() != null && !f.getKategoriIdler().isEmpty()) {
            where.append("AND k.kategori_id IN (").append(icinYerTutucular(f.getKategoriIdler().size())).append(") ");
            parametreler.addAll(f.getKategoriIdler());
        }
        if (f.getYazarIdler() != null && !f.getYazarIdler().isEmpty()) {
            where.append("AND k.yazar_id IN (").append(icinYerTutucular(f.getYazarIdler().size())).append(") ");
            parametreler.addAll(f.getYazarIdler());
        }
        if (f.getYayineviIdler() != null && !f.getYayineviIdler().isEmpty()) {
            where.append("AND k.yayinevi_id IN (").append(icinYerTutucular(f.getYayineviIdler().size())).append(") ");
            parametreler.addAll(f.getYayineviIdler());
        }
        if (f.getFormatlar() != null && !f.getFormatlar().isEmpty()) {
            where.append("AND k.dosya_formati IN (").append(icinYerTutucular(f.getFormatlar().size())).append(") ");
            parametreler.addAll(f.getFormatlar());
        }
        if (f.getFiyatMin() != null) {
            where.append("AND COALESCE(k.indirimli_fiyat, k.fiyat) >= ? ");
            parametreler.add(f.getFiyatMin());
        }
        if (f.getFiyatMax() != null) {
            where.append("AND COALESCE(k.indirimli_fiyat, k.fiyat) <= ? ");
            parametreler.add(f.getFiyatMax());
        }
        if (f.isSadeceStokta()) {
            where.append("AND k.stok_miktari > 0 ");
        }
        if (f.getMinPuan() != null) {
            where.append("AND dg.ortalama_puan >= ? ");
            parametreler.add(f.getMinPuan());
        }
        if (f.getAramaMetni() != null && !f.getAramaMetni().isBlank()) {
            where.append("AND (k.kitap_adi LIKE ? OR y.yazar_adi LIKE ? OR ye.yayinevi_adi LIKE ?) ");
            String desen = "%" + f.getAramaMetni().trim() + "%";
            parametreler.add(desen);
            parametreler.add(desen);
            parametreler.add(desen);
        }
        return where.toString();
    }

    private String siralamaIfadesi(String sirala) {
        if (sirala == null) return "ORDER BY k.kitap_id DESC";
        switch (sirala) {
            case "fiyatArtan": return "ORDER BY COALESCE(k.indirimli_fiyat, k.fiyat) ASC";
            case "fiyatAzalan": return "ORDER BY COALESCE(k.indirimli_fiyat, k.fiyat) DESC";
            case "cokSatan": return "ORDER BY satis_adedi DESC, k.kitap_id DESC";
            case "puanAzalan": return "ORDER BY dg.ortalama_puan IS NULL, dg.ortalama_puan DESC, k.kitap_id DESC";
            case "stokAzalan": return "ORDER BY k.stok_miktari ASC, k.kitap_id DESC";
            case "enYeni":
            default: return "ORDER BY k.kitap_id DESC";
        }
    }

    /**
     * Arama metni + kategori/yazar/yayinevi filtreleri + siralama + sayfalama
     * destekleyen genel amacli sorgu. kitap-detay.html'deki "benzer kitaplar"
     * ve ileride gelecek sol filtreli katalog sayfasi tarafindan kullanilir.
     *
     * @param sirala "fiyatArtan" | "fiyatAzalan" | "cokSatan" | "enYeni" (null ise enYeni)
     * @param sayfa 1 tabanli sayfa numarasi
     * @param sayfaBoyutu sayfa basina kayit sayisi
     */
    public model.KitapSayfaSonucu araVeFiltrele(String aramaMetni, Integer kategoriId, Integer yazarId,
                                                 Integer yayineviId, String sirala, int sayfa, int sayfaBoyutu)
            throws SQLException {

        if (sayfa < 1) sayfa = 1;
        if (sayfaBoyutu < 1) sayfaBoyutu = 12;

        List<Object> parametreler = new ArrayList<>();
        String where = whereVeParametreler(kategoriId, yazarId, yayineviId, aramaMetni, parametreler);

        int toplamKayit;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS toplam FROM kitap k " +
                     "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
                     "LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id " + where)) {
            for (int i = 0; i < parametreler.size(); i++) {
                ps.setObject(i + 1, parametreler.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                toplamKayit = rs.next() ? rs.getInt("toplam") : 0;
            }
        }

        String sql = ARAMA_SELECT + where + siralamaIfadesi(sirala) + " LIMIT ? OFFSET ?";
        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object p : parametreler) {
                ps.setObject(i++, p);
            }
            ps.setInt(i++, sayfaBoyutu);
            ps.setInt(i, (sayfa - 1) * sayfaBoyutu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapToKitap(rs));
                }
            }
        }

        return new model.KitapSayfaSonucu(liste, toplamKayit, sayfa, sayfaBoyutu);
    }

    /**
     * Sol filtreli katalog sayfasi icin: coklu kategori/yazar/yayinevi secimi,
     * fiyat araligi, format ve stok filtreleriyle birlikte sorgu yapar.
     */
    public model.KitapSayfaSonucu araVeFiltrele(model.KitapFiltre f) throws SQLException {
        int sayfa = f.getSayfa() < 1 ? 1 : f.getSayfa();
        int sayfaBoyutu = f.getSayfaBoyutu() < 1 ? 12 : f.getSayfaBoyutu();

        List<Object> parametreler = new ArrayList<>();
        String where = whereVeParametreler(f, parametreler);

        int toplamKayit;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS toplam FROM kitap k " +
                     "LEFT JOIN yazar y ON k.yazar_id = y.yazar_id " +
                     "LEFT JOIN yayinevi ye ON k.yayinevi_id = ye.yayinevi_id " +
                     "LEFT JOIN (SELECT kitap_id, AVG(puan) AS ortalama_puan FROM degerlendirme GROUP BY kitap_id) dg " +
                     "       ON dg.kitap_id = k.kitap_id " + where)) {
            for (int i = 0; i < parametreler.size(); i++) {
                ps.setObject(i + 1, parametreler.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                toplamKayit = rs.next() ? rs.getInt("toplam") : 0;
            }
        }

        String sql = ARAMA_SELECT + where + siralamaIfadesi(f.getSirala()) + " LIMIT ? OFFSET ?";
        List<Kitap> liste = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object p : parametreler) {
                ps.setObject(i++, p);
            }
            ps.setInt(i++, sayfaBoyutu);
            ps.setInt(i, (sayfa - 1) * sayfaBoyutu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapToKitap(rs));
                }
            }
        }

        return new model.KitapSayfaSonucu(liste, toplamKayit, sayfa, sayfaBoyutu);
    }

    /** Katalogdaki en dusuk ve en yuksek (indirimli varsa indirimli) fiyati doner: {min, max}. */
    public double[] fiyatAraligi() throws SQLException {
        String sql = "SELECT MIN(COALESCE(indirimli_fiyat, fiyat)) AS min_fiyat, " +
                     "       MAX(COALESCE(indirimli_fiyat, fiyat)) AS max_fiyat FROM kitap";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new double[]{ rs.getDouble("min_fiyat"), rs.getDouble("max_fiyat") };
            }
        }
        return new double[]{ 0, 0 };
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

        double ortalamaPuan = rs.getDouble("ortalama_puan");
        k.setOrtalamaPuan(rs.wasNull() ? null : Math.round(ortalamaPuan * 10) / 10.0);
        try {
            k.setDegerlendirmeSayisi(rs.getInt("degerlendirme_sayisi"));
        } catch (SQLException e) {
            // getStokDurumu() gibi bu kolonu icermeyen sorgularda sessizce atla
            k.setDegerlendirmeSayisi(0);
        }
        return k;
    }

    /* ================= ADMIN CRUD ================= */

    /** Yeni kitap ekler ve olusan kitap_id'yi dolu Kitap olarak doner. */
    public Kitap ekle(Kitap k) throws SQLException {
        String sql = "INSERT INTO kitap (kitap_adi, yazar_id, kategori_id, yayinevi_id, sayfa_sayisi, fiyat, " +
                     "stok_miktari, indirimli_fiyat, dosya_formati, dosya_boyutu_mb, dosya_yolu, kapak_resmi_url, " +
                     "aciklama, yayin_tarihi) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            doldurParametreler(ps, k);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Kitap id alinamadi.");
                return getById(keys.getInt(1));
            }
        }
    }

    /** Mevcut bir kitabin tum alanlarini gunceller. */
    public boolean guncelle(int kitapId, Kitap k) throws SQLException {
        String sql = "UPDATE kitap SET kitap_adi=?, yazar_id=?, kategori_id=?, yayinevi_id=?, sayfa_sayisi=?, " +
                     "fiyat=?, stok_miktari=?, indirimli_fiyat=?, dosya_formati=?, dosya_boyutu_mb=?, " +
                     "dosya_yolu=COALESCE(?, dosya_yolu), kapak_resmi_url=COALESCE(?, kapak_resmi_url), " +
                     "aciklama=?, yayin_tarihi=? WHERE kitap_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int son = doldurParametreler(ps, k);
            ps.setInt(son + 1, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Sadece kapak resmi URL'sini gunceller (admin dosya yukleme sonrasi). */
    public boolean kapakGuncelle(int kitapId, String kapakResmiUrl) throws SQLException {
        String sql = "UPDATE kitap SET kapak_resmi_url = ? WHERE kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kapakResmiUrl);
            ps.setInt(2, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Sadece e-kitap dosya yolunu (ve varsa formati/boyutu) gunceller. */
    public boolean dosyaGuncelle(int kitapId, String dosyaYolu, String dosyaFormati, Double dosyaBoyutuMb) throws SQLException {
        String sql = "UPDATE kitap SET dosya_yolu = ?, dosya_formati = COALESCE(?, dosya_formati), " +
                     "dosya_boyutu_mb = COALESCE(?, dosya_boyutu_mb) WHERE kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dosyaYolu);
            ps.setString(2, dosyaFormati);
            if (dosyaBoyutuMb != null) ps.setDouble(3, dosyaBoyutuMb); else ps.setNull(3, Types.DOUBLE);
            ps.setInt(4, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Kitabi siler. siparis_detay.kitap_id -> ON DELETE RESTRICT oldugundan,
     * bu kitap gecmiste satildiysa silme islemi veritabani tarafindan
     * reddedilir (SQLException firlatilir) -- cagiran taraf bunu yakalayip
     * anlasilir bir mesaj gostermelidir (bkz. AdminKitapServlet).
     */
    public boolean sil(int kitapId) throws SQLException {
        String sql = "DELETE FROM kitap WHERE kitap_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kitapId);
            return ps.executeUpdate() > 0;
        }
    }

    private int doldurParametreler(PreparedStatement ps, Kitap k) throws SQLException {
        int i = 1;
        ps.setString(i++, k.getKitapAdi());
        if (k.getYazarId() != null) ps.setInt(i++, k.getYazarId()); else ps.setNull(i++, Types.INTEGER);
        if (k.getKategoriId() != null) ps.setInt(i++, k.getKategoriId()); else ps.setNull(i++, Types.INTEGER);
        if (k.getYayineviId() != null) ps.setInt(i++, k.getYayineviId()); else ps.setNull(i++, Types.INTEGER);
        ps.setInt(i++, k.getSayfaSayisi());
        ps.setDouble(i++, k.getFiyat());
        ps.setInt(i++, k.getStokMiktari());
        if (k.getIndirimliFiyat() != null) ps.setDouble(i++, k.getIndirimliFiyat()); else ps.setNull(i++, Types.DECIMAL);
        ps.setString(i++, k.getDosyaFormati() != null ? k.getDosyaFormati() : "PDF");
        ps.setDouble(i++, k.getDosyaBoyutuMb());
        ps.setString(i++, k.getDosyaYolu());
        ps.setString(i++, k.getKapakResmiUrl());
        ps.setString(i++, k.getAciklama());
        if (k.getYayinTarihi() != null && !k.getYayinTarihi().isBlank()) {
            ps.setDate(i++, Date.valueOf(k.getYayinTarihi()));
        } else {
            ps.setNull(i++, Types.DATE);
        }
        return i - 1;
    }
}
