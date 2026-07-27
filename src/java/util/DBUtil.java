package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL veritabani baglantisini yoneten yardimci sinif.
 * JDBC URL, kullanici adi ve sifreyi kendi ortaminiza gore asagidaki
 * sabitlerden (ya da DB_URL / DB_USER / DB_PASS sistem/ortam degiskenlerinden) guncelleyin.
 */
public class DBUtil {

    private static final String VARSAYILAN_URL =
            "jdbc:mysql://localhost:3306/ekitap_db";
    private static final String VARSAYILAN_KULLANICI = "root";
    private static final String VARSAYILAN_SIFRE = "12345678";

    private static final String URL = oku("DB_URL", VARSAYILAN_URL);
    private static final String KULLANICI = oku("DB_USER", VARSAYILAN_KULLANICI);
    private static final String SIFRE = oku("DB_PASS", VARSAYILAN_SIFRE);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver bulunamadi. mysql-connector-j'yi classpath'e ekleyin.", e);
        }
    }

    /** Once sistem property'sine, sonra ortam degiskenine, o da yoksa varsayilana bakar. */
    private static String oku(String anahtar, String varsayilan) {
        String deger = System.getProperty(anahtar);
        if (deger == null || deger.isBlank()) {
            deger = System.getenv(anahtar);
        }
        return (deger == null || deger.isBlank()) ? varsayilan : deger;
    }

    private DBUtil() {
        // utility class, instantiate edilemez
    }

    /**
     * Yeni bir veritabani baglantisi doner. Cagiran taraf try-with-resources
     * ile baglantiyi kapatmakla yukumludur.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, KULLANICI, SIFRE);
    }
}
