package controller;

import com.google.gson.Gson;
import dao.DuyuruDAO;
import model.Duyuru;

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
import java.util.stream.Collectors;

/**
 * Admin Paneli - Duyuru (Hero Slider) CRUD.
 *   GET    /api/admin/duyurular        -> tum duyurulari (aktif/pasif) doner
 *   POST   /api/admin/duyurular        -> yeni duyuru ekler
 *   PUT    /api/admin/duyurular?id=X   -> gunceller
 *   DELETE /api/admin/duyurular?id=X   -> siler
 */
@WebServlet("/api/admin/duyurular")
public class AdminDuyuruServlet extends HttpServlet {

    private final DuyuruDAO duyuruDAO = new DuyuruDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Duyuru> liste = duyuruDAO.listeTumu();
            out.print(gson.toJson(liste));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Duyuru istek = govdeOku(req);
            if (istek == null || istek.getBaslik() == null || istek.getBaslik().isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("baslik zorunludur.")));
                return;
            }
            Duyuru yeni = duyuruDAO.ekle(istek);
            out.print(gson.toJson(yeni));
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
            int id = idParamOku(req, resp, out);
            if (id <= 0) return;

            Duyuru istek = govdeOku(req);
            if (istek == null || istek.getBaslik() == null || istek.getBaslik().isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("baslik zorunludur.")));
                return;
            }
            boolean guncellendi = duyuruDAO.guncelle(id, istek);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", guncellendi);
            sonuc.put("mesaj", guncellendi ? "Duyuru guncellendi." : "Duyuru bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            int id = idParamOku(req, resp, out);
            if (id <= 0) return;
            boolean silindi = duyuruDAO.sil(id);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", silindi);
            sonuc.put("mesaj", silindi ? "Duyuru silindi." : "Duyuru bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private int idParamOku(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("id parametresi gereklidir.")));
            return -1;
        }
        try {
            return Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("id sayisal olmalidir.")));
            return -1;
        }
    }

    private Duyuru govdeOku(HttpServletRequest req) throws IOException {
        String govde;
        try (BufferedReader reader = req.getReader()) {
            govde = reader.lines().collect(Collectors.joining());
        }
        if (govde == null || govde.isBlank()) return null;
        return gson.fromJson(govde, Duyuru.class);
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
