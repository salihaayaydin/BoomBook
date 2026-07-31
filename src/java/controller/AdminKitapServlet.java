package controller;

import com.google.gson.Gson;
import dao.KitapDAO;
import model.Kitap;
import model.KitapSayfaSonucu;

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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Paneli - Kitap CRUD.
 *   GET    /api/admin/kitaplar?q=...&sayfa=1&sayfaBoyutu=20  -> sayfalanmis kitap listesi (arama destekli)
 *   GET    /api/admin/kitaplar?id=X                          -> tek kitap detayi (duzenleme formu icin)
 *   POST   /api/admin/kitaplar                                -> yeni kitap ekler (govde: Kitap alanlari JSON)
 *   PUT    /api/admin/kitaplar?id=X                           -> kitabi gunceller
 *   DELETE /api/admin/kitaplar?id=X                           -> kitabi siler (gecmiste satildiysa reddedilir)
 */
@WebServlet("/api/admin/kitaplar")
public class AdminKitapServlet extends HttpServlet {

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String idParam = req.getParameter("id");
            if (idParam != null) {
                Kitap k = kitapDAO.getById(Integer.parseInt(idParam));
                if (k == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(hata("Kitap bulunamadi.")));
                } else {
                    out.print(gson.toJson(k));
                }
                return;
            }

            String q = req.getParameter("q");
            int sayfa = req.getParameter("sayfa") != null ? Integer.parseInt(req.getParameter("sayfa")) : 1;
            int sayfaBoyutu = req.getParameter("sayfaBoyutu") != null ? Integer.parseInt(req.getParameter("sayfaBoyutu")) : 20;

            KitapSayfaSonucu sonuc = kitapDAO.araVeFiltrele(q, null, null, null, "enYeni", sayfa, sayfaBoyutu);
            out.print(gson.toJson(sonuc));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Gecersiz parametre.")));
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
            Kitap istek = govdeOku(req);
            String hataMesaji = dogrula(istek);
            if (hataMesaji != null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata(hataMesaji)));
                return;
            }
            Kitap yeni = kitapDAO.ekle(istek);
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

            Kitap istek = govdeOku(req);
            String hataMesaji = dogrula(istek);
            if (hataMesaji != null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata(hataMesaji)));
                return;
            }

            boolean guncellendi = kitapDAO.guncelle(id, istek);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", guncellendi);
            sonuc.put("mesaj", guncellendi ? "Kitap guncellendi." : "Kitap bulunamadi.");
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

            boolean silindi = kitapDAO.sil(id);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", silindi);
            sonuc.put("mesaj", silindi ? "Kitap silindi." : "Kitap bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            // siparis_detay.kitap_id -> ON DELETE RESTRICT: bu kitap gecmiste satildiysa silinemez.
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(gson.toJson(hata("Bu kitap daha once satildigi icin silinemiyor. " +
                    "Onun yerine stogunu 0 yapip yayindan kaldirmayi deneyin.")));
        } finally {
            out.flush();
        }
    }

    private String dogrula(Kitap k) {
        if (k == null || k.getKitapAdi() == null || k.getKitapAdi().isBlank()) {
            return "kitapAdi zorunludur.";
        }
        if (k.getFiyat() < 0) {
            return "fiyat negatif olamaz.";
        }
        if (k.getStokMiktari() < 0) {
            return "stokMiktari negatif olamaz.";
        }
        if (k.getIndirimliFiyat() != null && k.getIndirimliFiyat() >= k.getFiyat()) {
            return "indirimliFiyat, normal fiyattan kucuk olmalidir.";
        }
        return null;
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

    private Kitap govdeOku(HttpServletRequest req) throws IOException {
        String govde;
        try (BufferedReader reader = req.getReader()) {
            govde = reader.lines().collect(Collectors.joining());
        }
        if (govde == null || govde.isBlank()) return null;
        return gson.fromJson(govde, Kitap.class);
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
