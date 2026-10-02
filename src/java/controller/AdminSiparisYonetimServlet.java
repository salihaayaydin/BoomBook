package controller;

import com.google.gson.Gson;
import dao.SiparisDAO;
import model.Siparis;

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
 * Admin Paneli - Siparis Yonetimi.
 *   GET /api/admin/siparisler         -> TUM siparisleri (kullanici bilgisiyle) doner
 *   GET /api/admin/siparisler?id=X    -> TEK bir siparisin detayini (kalemleriyle) doner
 *   PUT /api/admin/siparisler?id=X    -> { "odemeDurumu": "Tamamlandi" } durumunu gunceller
 */
@WebServlet("/api/admin/siparisler")
public class AdminSiparisYonetimServlet extends HttpServlet {

    private static final Set<String> GECERLI_DURUMLAR = Set.of("Bekliyor", "Tamamlandi", "Basarisiz");

    private final SiparisDAO siparisDAO = new SiparisDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String idParam = req.getParameter("id");
            if (idParam != null) {
                Siparis siparis = siparisDAO.siparisDetayiGetir(Integer.parseInt(idParam));
                if (siparis == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(hata("Siparis bulunamadi.")));
                } else {
                    out.print(gson.toJson(siparis));
                }
                return;
            }
            List<Siparis> liste = siparisDAO.tumSiparisleriGetir();
            out.print(gson.toJson(liste));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("id sayisal olmalidir.")));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String idParam = req.getParameter("id");
            if (idParam == null || idParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("id parametresi gereklidir.")));
                return;
            }
            int siparisId = Integer.parseInt(idParam);

            String govde;
            try (BufferedReader reader = req.getReader()) {
                govde = reader.lines().collect(Collectors.joining());
            }
            Istek istek = (govde == null || govde.isBlank()) ? null : gson.fromJson(govde, Istek.class);

            if (istek == null || istek.odemeDurumu == null || !GECERLI_DURUMLAR.contains(istek.odemeDurumu)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("odemeDurumu 'Bekliyor', 'Tamamlandi' veya 'Basarisiz' olmalidir.")));
                return;
            }

            boolean guncellendi = siparisDAO.durumGuncelle(siparisId, istek.odemeDurumu);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", guncellendi);
            sonuc.put("mesaj", guncellendi ? "Siparis durumu guncellendi." : "Siparis bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("id sayisal olmalidir.")));
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

    private static class Istek {
        String odemeDurumu;
    }
}
