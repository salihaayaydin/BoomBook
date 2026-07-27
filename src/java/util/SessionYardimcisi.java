package util;

import model.Kullanici;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Oturumdaki (giris yapmis) kullaniciyi okumak icin ortak yardimci.
 * AuthServlet basarili giris/kayit sonrasi HttpSession'a bu anahtarla
 * (sifre_hash icermeyen) bir Kullanici nesnesi koyar.
 */
public final class SessionYardimcisi {

    public static final String OTURUM_ANAHTARI = "kullanici";

    private SessionYardimcisi() {
    }

    /** Oturum yoksa ya da giris yapilmamissa null doner. */
    public static Kullanici oturumdakiKullanici(HttpServletRequest req) {
        HttpSession oturum = req.getSession(false);
        if (oturum == null) {
            return null;
        }
        return (Kullanici) oturum.getAttribute(OTURUM_ANAHTARI);
    }

    public static boolean girisYapilmis(HttpServletRequest req) {
        return oturumdakiKullanici(req) != null;
    }

    public static boolean adminMi(HttpServletRequest req) {
        Kullanici k = oturumdakiKullanici(req);
        return k != null && k.isAdmin();
    }
}
