package dao;

import model.Siparis;
import model.SiparisDetay;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * siparis ve siparis_detay tablolari uzerinde islem yapar.
 * Satin alma islemi tek bir transaction icinde yurutulur:
 *   1) siparis kaydi olusturulur
 *   2) her kitap icin siparis_detay satiri eklenir
 *   3) kitap.stok_miktari dusurulur (yetersizse tum islem geri alinir)
 *   4) kullaniciya kutuphane erisimi taniniir
 */
public class SiparisDAO {

    private final KitapDAO kitapDAO = new KitapDAO();

    /**
     * Yeni siparis olusturur. Basarili olursa olusan siparis_id'yi doner.
     * Herhangi bir kitabin stogu yetersizse SQLException firlatir ve
     * hicbir degisiklik veritabanina yansimaz (rollback).
     */
    public int siparisOlustur(int kullaniciId, List<SiparisDetay> kalemler) throws SQLException {
        if (kalemler == null || kalemler.isEmpty()) {
            throw new SQLException("Siparis en az bir kitap icermelidir.");
        }

        String siparisInsert = "INSERT INTO siparis (kullanici_id, toplam_tutar, odeme_durumu) VALUES (?, ?, 'Bekliyor')";
        String detayInsert = "INSERT INTO siparis_detay (siparis_id, kitap_id, birim_fiyat, adet) VALUES (?, ?, ?, ?)";
        String kutuphaneInsert = "INSERT INTO kutuphane (kullanici_id, kitap_id, indirme_baglantisi) " +
                                  "SELECT ?, ?, dosya_yolu FROM kitap WHERE kitap_id = ?";
        String siparisTamamla = "UPDATE siparis SET toplam_tutar = ?, odeme_durumu = 'Tamamlandi' WHERE siparis_id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int siparisId;
                try (PreparedStatement ps = conn.prepareStatement(siparisInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, kullaniciId);
                    ps.setDouble(2, 0.0);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Siparis id alinamadi.");
                        }
                        siparisId = keys.getInt(1);
                    }
                }

                double toplamTutar = 0.0;

                for (SiparisDetay kalem : kalemler) {
                    int adet = kalem.getAdet() > 0 ? kalem.getAdet() : 1;

                    // Stok dusur - yetersizse false doner ve rollback tetiklenir
                    boolean basarili = kitapDAO.stokDustur(conn, kalem.getKitapId(), adet);
                    if (!basarili) {
                        throw new SQLException("Kitap id=" + kalem.getKitapId() + " icin stok yetersiz.");
                    }

                    try (PreparedStatement ps = conn.prepareStatement(detayInsert)) {
                        ps.setInt(1, siparisId);
                        ps.setInt(2, kalem.getKitapId());
                        ps.setDouble(3, kalem.getBirimFiyat());
                        ps.setInt(4, adet);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = conn.prepareStatement(kutuphaneInsert)) {
                        ps.setInt(1, kullaniciId);
                        ps.setInt(2, kalem.getKitapId());
                        ps.setInt(3, kalem.getKitapId());
                        ps.executeUpdate();
                    }

                    toplamTutar += kalem.getBirimFiyat() * adet;
                }

                try (PreparedStatement ps = conn.prepareStatement(siparisTamamla)) {
                    ps.setDouble(1, toplamTutar);
                    ps.setInt(2, siparisId);
                    ps.executeUpdate();
                }

                conn.commit();
                return siparisId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Kullanicinin SUNUCUDAKI sepet tablosundan yeni bir siparis olusturur.
     * Fiyatlar istemciden degil, siparis anindaki guncel kitap.fiyat /
     * kitap.indirimli_fiyat degerlerinden okunur (fiyat manipulasyonuna
     * karsi guvenli). Basarili olursa sepet bosaltilir ve kutuphaneye
     * erisim taniniir; herhangi bir kitabin stogu yetersizse tum islem
     * geri alinir (sepet de bosaltilmaz).
     *
     * @return olusan siparis_id
     * @throws SQLException sepet bossa, stok yetersizse veya baska bir veritabani hatasi olusursa
     */
    public int siparisOlusturSepetten(int kullaniciId) throws SQLException {
        String sepetSelect =
                "SELECT s.kitap_id, s.adet, k.kitap_adi, k.fiyat, k.indirimli_fiyat " +
                "FROM sepet s JOIN kitap k ON s.kitap_id = k.kitap_id " +
                "WHERE s.kullanici_id = ? FOR UPDATE";
        String siparisInsert = "INSERT INTO siparis (kullanici_id, toplam_tutar, odeme_durumu) VALUES (?, ?, 'Bekliyor')";
        String detayInsert = "INSERT INTO siparis_detay (siparis_id, kitap_id, birim_fiyat, adet) VALUES (?, ?, ?, ?)";
        String kutuphaneInsert = "INSERT IGNORE INTO kutuphane (kullanici_id, kitap_id, indirme_baglantisi) " +
                                  "SELECT ?, ?, dosya_yolu FROM kitap WHERE kitap_id = ?";
        String siparisTamamla = "UPDATE siparis SET toplam_tutar = ?, odeme_durumu = 'Tamamlandi' WHERE siparis_id = ?";
        String sepetTemizle = "DELETE FROM sepet WHERE kullanici_id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<SiparisDetay> kalemler = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(sepetSelect)) {
                    ps.setInt(1, kullaniciId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            double fiyat = rs.getDouble("fiyat");
                            double indirimli = rs.getDouble("indirimli_fiyat");
                            boolean indirimliVar = !rs.wasNull();
                            double birimFiyat = indirimliVar ? indirimli : fiyat;
                            SiparisDetay d = new SiparisDetay(rs.getInt("kitap_id"), birimFiyat, rs.getInt("adet"));
                            d.setKitapAdi(rs.getString("kitap_adi"));
                            kalemler.add(d);
                        }
                    }
                }

                if (kalemler.isEmpty()) {
                    throw new SQLException("Sepetiniz bos, siparis olusturulamadi.");
                }

                int siparisId;
                try (PreparedStatement ps = conn.prepareStatement(siparisInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, kullaniciId);
                    ps.setDouble(2, 0.0);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Siparis id alinamadi.");
                        }
                        siparisId = keys.getInt(1);
                    }
                }

                double toplamTutar = 0.0;

                for (SiparisDetay kalem : kalemler) {
                    int adet = kalem.getAdet() > 0 ? kalem.getAdet() : 1;

                    boolean stokYeterli = kitapDAO.stokDustur(conn, kalem.getKitapId(), adet);
                    if (!stokYeterli) {
                        throw new SQLException("\"" + kalem.getKitapAdi() + "\" icin stok yetersiz (kitap id=" +
                                kalem.getKitapId() + "). Lutfen adedi azaltin.");
                    }

                    try (PreparedStatement ps = conn.prepareStatement(detayInsert)) {
                        ps.setInt(1, siparisId);
                        ps.setInt(2, kalem.getKitapId());
                        ps.setDouble(3, kalem.getBirimFiyat());
                        ps.setInt(4, adet);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = conn.prepareStatement(kutuphaneInsert)) {
                        ps.setInt(1, kullaniciId);
                        ps.setInt(2, kalem.getKitapId());
                        ps.setInt(3, kalem.getKitapId());
                        ps.executeUpdate();
                    }

                    toplamTutar += kalem.getBirimFiyat() * adet;
                }

                try (PreparedStatement ps = conn.prepareStatement(siparisTamamla)) {
                    ps.setDouble(1, toplamTutar);
                    ps.setInt(2, siparisId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(sepetTemizle)) {
                    ps.setInt(1, kullaniciId);
                    ps.executeUpdate();
                }

                conn.commit();
                return siparisId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Kullanicinin gecmis siparislerini (detaylariyla birlikte) getirir. */
    public List<Siparis> getSiparislerByKullanici(int kullaniciId) throws SQLException {
        String siparisSql = "SELECT siparis_id, kullanici_id, toplam_tutar, odeme_durumu, siparis_tarihi " +
                             "FROM siparis WHERE kullanici_id = ? ORDER BY siparis_tarihi DESC";
        String detaySql = "SELECT sd.siparis_id, sd.kitap_id, k.kitap_adi, sd.birim_fiyat, sd.adet " +
                           "FROM siparis_detay sd JOIN kitap k ON sd.kitap_id = k.kitap_id " +
                           "WHERE sd.siparis_id = ?";

        List<Siparis> siparisler = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(siparisSql)) {
                ps.setInt(1, kullaniciId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Siparis s = new Siparis();
                        s.setSiparisId(rs.getInt("siparis_id"));
                        s.setKullaniciId(rs.getInt("kullanici_id"));
                        s.setToplamTutar(rs.getDouble("toplam_tutar"));
                        s.setOdemeDurumu(rs.getString("odeme_durumu"));
                        Timestamp ts = rs.getTimestamp("siparis_tarihi");
                        s.setSiparisTarihi(ts != null ? ts.toString() : null);
                        siparisler.add(s);
                    }
                }
            }

            for (Siparis s : siparisler) {
                List<SiparisDetay> detaylar = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(detaySql)) {
                    ps.setInt(1, s.getSiparisId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            SiparisDetay d = new SiparisDetay();
                            d.setSiparisId(rs.getInt("siparis_id"));
                            d.setKitapId(rs.getInt("kitap_id"));
                            d.setKitapAdi(rs.getString("kitap_adi"));
                            d.setBirimFiyat(rs.getDouble("birim_fiyat"));
                            d.setAdet(rs.getInt("adet"));
                            detaylar.add(d);
                        }
                    }
                }
                s.setDetaylar(detaylar);
            }
        }
        return siparisler;
    }
}
