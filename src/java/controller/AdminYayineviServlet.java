package controller;

import com.google.gson.Gson;
import dao.YayineviDAO;
import model.Yayinevi;

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
 * Admin Paneli - Yayinevi CRUD.
 *   GET    /api/admin/yayinevleri
 *   POST   /api/admin/yayinevleri        -> { "yayineviAdi": "..." }
 *   PUT    /api/admin/yayinevleri?id=X   -> { "yayineviAdi": "..." }
 *   DELETE /api/admin/yayinevleri?id=X
 */
@WebServlet("/api/admin/yayinevleri")
public class AdminYayineviServlet extends HttpServlet {

    private final YayineviDAO yayineviDAO = new YayineviDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Yayinevi> liste = yayineviDAO.liste();
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
            Istek istek = govdeOku(req);
            if (istek == null || istek.yayineviAdi == null || istek.yayineviAdi.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("yayineviAdi zorunludur.")));
                return;
            }
            Yayinevi yeni = yayineviDAO.ekle(istek.yayineviAdi.trim());
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

            Istek istek = govdeOku(req);
            if (istek == null || istek.yayineviAdi == null || istek.yayineviAdi.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("yayineviAdi zorunludur.")));
                return;
            }
            boolean guncellendi = yayineviDAO.guncelle(id, istek.yayineviAdi.trim());
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", guncellendi);
            sonuc.put("mesaj", guncellendi ? "Yayinevi guncellendi." : "Yayinevi bulunamadi.");
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

            int bagliKitap = yayineviDAO.bagliKitapSayisi(id);
            boolean silindi = yayineviDAO.sil(id);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", silindi);
            sonuc.put("mesaj", silindi
                    ? (bagliKitap > 0 ? bagliKitap + " kitabin yayinevi bilgisi bosaltildi ve yayinevi silindi." : "Yayinevi silindi.")
                    : "Yayinevi bulunamadi.");
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

    private Istek govdeOku(HttpServletRequest req) throws IOException {
        String govde;
        try (BufferedReader reader = req.getReader()) {
            govde = reader.lines().collect(Collectors.joining());
        }
        if (govde == null || govde.isBlank()) return null;
        return gson.fromJson(govde, Istek.class);
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }

    private static class Istek {
        String yayineviAdi;
    }
}
