package controller;

import com.google.gson.Gson;
import dao.FavoriDAO;
import model.FavoriKalemi;
import model.Kullanici;
import util.SessionYardimcisi;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Favoriler API'si (favori tablosu, kullaniciId'ye bagli, sepet ile ayni mantik).
 *
 *   GET    /api/favori                 -> oturumdaki kullanicinin favori listesini (kitap bilgileriyle) doner
 *   GET    /api/favori?sadeceId=true   -> hafif: sadece kitapId dizisi doner (kart kalp ikonlarini doldurmak icin)
 *   POST   /api/favori                 -> { "kitapId": 5 } favoriye ekler
 *   DELETE /api/favori?kitapId=5       -> favoriden kaldirir
 *
 * Tum uc noktalar giris yapilmasini gerektirir; giris yoksa 401 doner
 * (ana sayfa bunu sessizce bos/kapali kalp olarak yorumlar).
 */
@WebServlet("/api/favori")
public class FavoriServlet extends HttpServlet {

    private final FavoriDAO favoriDAO = new FavoriDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            if (kullanici == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Favorilerinizi gormek icin giris yapmalisiniz.")));
                return;
            }

            if ("true".equalsIgnoreCase(req.getParameter("sadeceId"))) {
                Set<Integer> idler = favoriDAO.getFavoriKitapIdleri(kullanici.getKullaniciId());
                out.print(gson.toJson(idler));
                return;
            }

            List<FavoriKalemi> liste = favoriDAO.getFavoriler(kullanici.getKullaniciId());
            out.print(gson.toJson(liste));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            if (kullanici == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Favorilere eklemek icin giris yapmalisiniz.")));
                return;
            }

            String govde;
            try (BufferedReader reader = req.getReader()) {
                govde = reader.lines().collect(Collectors.joining());
            }
            FavoriIstek istek = (govde == null || govde.isBlank()) ? null : gson.fromJson(govde, FavoriIstek.class);

            if (istek == null || istek.kitapId <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Gecerli bir kitapId gereklidir.")));
                return;
            }

            favoriDAO.ekle(kullanici.getKullaniciId(), istek.kitapId);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            sonuc.put("mesaj", "Kitap favorilere eklendi.");
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            if (kullanici == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Bu islem icin giris yapmalisiniz.")));
                return;
            }

            String kitapIdParam = req.getParameter("kitapId");
            if (kitapIdParam == null || kitapIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId parametresi gereklidir.")));
                return;
            }

            int kitapId = Integer.parseInt(kitapIdParam);
            boolean kaldirildi = favoriDAO.kaldir(kullanici.getKullaniciId(), kitapId);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", kaldirildi);
            sonuc.put("mesaj", kaldirildi ? "Kitap favorilerden kaldirildi." : "Kitap favorilerde bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("kitapId sayisal olmalidir.")));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }

    private static class FavoriIstek {
        int kitapId;
    }
}
