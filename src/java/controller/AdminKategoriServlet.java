package controller;

import com.google.gson.Gson;
import dao.KategoriDAO;
import model.Kategori;

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
 * Admin Paneli - Kategori CRUD.
 *   GET    /api/admin/kategoriler        -> tum kategorileri doner
 *   POST   /api/admin/kategoriler        -> { "kategoriAdi": "..." } yeni kategori ekler
 *   PUT    /api/admin/kategoriler?id=X   -> { "kategoriAdi": "..." } gunceller
 *   DELETE /api/admin/kategoriler?id=X   -> siler (baglı kitap varsa uyari doner, kitaplar SILINMEZ, kategorisi bosalir)
 *
 * Bu uc nokta AuthFilter tarafindan Admin rolu ile korunur.
 */
@WebServlet("/api/admin/kategoriler")
public class AdminKategoriServlet extends HttpServlet {

    private final KategoriDAO kategoriDAO = new KategoriDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Kategori> liste = kategoriDAO.liste();
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
            if (istek == null || istek.kategoriAdi == null || istek.kategoriAdi.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kategoriAdi zorunludur.")));
                return;
            }
            Kategori yeni = kategoriDAO.ekle(istek.kategoriAdi.trim());
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
            if (istek == null || istek.kategoriAdi == null || istek.kategoriAdi.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kategoriAdi zorunludur.")));
                return;
            }
            boolean guncellendi = kategoriDAO.guncelle(id, istek.kategoriAdi.trim());
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", guncellendi);
            sonuc.put("mesaj", guncellendi ? "Kategori guncellendi." : "Kategori bulunamadi.");
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

            int bagliKitap = kategoriDAO.bagliKitapSayisi(id);
            boolean silindi = kategoriDAO.sil(id);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", silindi);
            sonuc.put("mesaj", silindi
                    ? (bagliKitap > 0
                        ? bagliKitap + " kitabin kategorisi bosaltildi ve kategori silindi."
                        : "Kategori silindi.")
                    : "Kategori bulunamadi.");
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
        String kategoriAdi;
    }
}
