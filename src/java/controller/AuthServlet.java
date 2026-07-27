package controller;

import com.google.gson.Gson;
import dao.KullaniciDAO;
import model.Kullanici;
import util.SessionYardimcisi;
import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Kimlik dogrulama uc noktalari:
 *   POST /api/auth/kayit  { "adSoyad": "...", "email": "...", "sifre": "...", "sifreTekrar": "..." }
 *   POST /api/auth/giris  { "email": "...", "sifre": "..." }
 *   POST /api/auth/cikis  (govde gerekmez)
 *   GET  /api/auth/ben    -> oturumdaki kullaniciyi doner (giris yoksa 401)
 *
 * Basarili giris/kayit sonrasi HttpSession icine "kullanici" anahtariyla
 * (sifre_hash ICERMEYEN) bir Kullanici nesnesi konur. Diger servlet'ler
 * oturumdaki kullaniciId'yi SessionYardimcisi.oturumdakiKullanici(req) ile okur.
 */
@WebServlet({"/api/auth/kayit", "/api/auth/giris", "/api/auth/cikis", "/api/auth/ben"})
public class AuthServlet extends HttpServlet {

    private static final Pattern EMAIL_DESENI =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final KullaniciDAO kullaniciDAO = new KullaniciDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String yol = req.getServletPath();

        try {
            switch (yol) {
                case "/api/auth/kayit":
                    kayitOl(req, resp, out);
                    break;
                case "/api/auth/giris":
                    girisYap(req, resp, out);
                    break;
                case "/api/auth/cikis":
                    cikisYap(req, out);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(hata("Bilinmeyen uc nokta.")));
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        if (!"/api/auth/ben".equals(req.getServletPath())) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(gson.toJson(hata("Bilinmeyen uc nokta.")));
            out.flush();
            return;
        }

        HttpSession oturum = req.getSession(false);
        Kullanici k = (oturum != null) ? (Kullanici) oturum.getAttribute(SessionYardimcisi.OTURUM_ANAHTARI) : null;

        if (k == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(hata("Oturum acik degil.")));
        } else {
            out.print(gson.toJson(k));
        }
        out.flush();
    }

    private void kayitOl(HttpServletRequest req, HttpServletResponse resp, PrintWriter out)
            throws IOException, SQLException {
        KayitIstek istek = govdeOku(req, KayitIstek.class);

        if (istek == null || bos(istek.adSoyad) || bos(istek.email) || bos(istek.sifre)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Ad soyad, email ve sifre zorunludur.")));
            return;
        }
        String email = istek.email.trim().toLowerCase();
        if (!EMAIL_DESENI.matcher(email).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Gecerli bir email adresi girin.")));
            return;
        }
        if (istek.sifre.length() < 6) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Sifre en az 6 karakter olmalidir.")));
            return;
        }
        if (istek.sifreTekrar != null && !istek.sifre.equals(istek.sifreTekrar)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Sifreler eslesmiyor.")));
            return;
        }
        if (kullaniciDAO.emailKullanimda(email)) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(gson.toJson(hata("Bu email adresi zaten kayitli.")));
            return;
        }

        String hash = BCrypt.hashpw(istek.sifre, BCrypt.gensalt(10));
        Kullanici yeni = kullaniciDAO.kayitOl(istek.adSoyad.trim(), email, hash);

        HttpSession oturum = req.getSession(true);
        oturum.setAttribute(SessionYardimcisi.OTURUM_ANAHTARI, yeni.genelGorunum());

        Map<String, Object> sonuc = new HashMap<>();
        sonuc.put("basarili", true);
        sonuc.put("mesaj", "Kayit basarili, hos geldiniz " + yeni.getAdSoyad() + "!");
        sonuc.put("kullanici", yeni.genelGorunum());
        out.print(gson.toJson(sonuc));
    }

    private void girisYap(HttpServletRequest req, HttpServletResponse resp, PrintWriter out)
            throws IOException, SQLException {
        GirisIstek istek = govdeOku(req, GirisIstek.class);

        if (istek == null || bos(istek.email) || bos(istek.sifre)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Email ve sifre zorunludur.")));
            return;
        }

        Kullanici k = kullaniciDAO.getByEmail(istek.email.trim().toLowerCase());
        if (k == null || !k.isAktif() || !BCrypt.checkpw(istek.sifre, k.getSifreHash())) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(hata("Email veya sifre hatali.")));
            return;
        }

        HttpSession oturum = req.getSession(true);
        oturum.setAttribute(SessionYardimcisi.OTURUM_ANAHTARI, k.genelGorunum());

        Map<String, Object> sonuc = new HashMap<>();
        sonuc.put("basarili", true);
        sonuc.put("mesaj", "Hos geldiniz " + k.getAdSoyad() + "!");
        sonuc.put("kullanici", k.genelGorunum());
        out.print(gson.toJson(sonuc));
    }

    private void cikisYap(HttpServletRequest req, PrintWriter out) {
        HttpSession oturum = req.getSession(false);
        if (oturum != null) {
            oturum.invalidate();
        }
        Map<String, Object> sonuc = new HashMap<>();
        sonuc.put("basarili", true);
        sonuc.put("mesaj", "Cikis yapildi.");
        out.print(gson.toJson(sonuc));
    }

    private <T> T govdeOku(HttpServletRequest req, Class<T> tip) throws IOException {
        String govde;
        try (BufferedReader reader = req.getReader()) {
            govde = reader.lines().collect(Collectors.joining());
        }
        if (govde == null || govde.isBlank()) {
            return null;
        }
        return gson.fromJson(govde, tip);
    }

    private boolean bos(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }

    private static class KayitIstek {
        String adSoyad;
        String email;
        String sifre;
        String sifreTekrar;
    }

    private static class GirisIstek {
        String email;
        String sifre;
    }
}
