package controller;

import com.google.gson.Gson;
import dao.KullaniciDAO;
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
import java.util.stream.Collectors;

/**
 * Admin Paneli - Kullanici Yonetimi.
 *   GET /api/admin/kullanicilar        -> tum kullanicilari doner (sifre_hash ASLA gelmez, alan transient)
 *   PUT /api/admin/kullanicilar?id=X   -> { "rol": "Admin" } ve/veya { "aktif": false } gunceller
 *
 * Guvenlik: oturumdaki admin kendi hesabinin rolunu Musteri'ye dusuremez /
 * kendi hesabini pasife alamaz (kendini kilitlemesini onlemek icin).
 */
@WebServlet("/api/admin/kullanicilar")
public class AdminKullaniciServlet extends HttpServlet {

    private final KullaniciDAO kullaniciDAO = new KullaniciDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<Kullanici> liste = kullaniciDAO.tumKullanicilariGetir();
            out.print(gson.toJson(liste));
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
            int hedefId = Integer.parseInt(idParam);

            Kullanici oturumdaki = SessionYardimcisi.oturumdakiKullanici(req);
            if (oturumdaki != null && oturumdaki.getKullaniciId() == hedefId) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print(gson.toJson(hata("Kendi hesabinizin rolunu veya aktiflik durumunu buradan degistiremezsiniz.")));
                return;
            }

            String govde;
            try (BufferedReader reader = req.getReader()) {
                govde = reader.lines().collect(Collectors.joining());
            }
            Istek istek = (govde == null || govde.isBlank()) ? null : gson.fromJson(govde, Istek.class);

            if (istek == null || (istek.rol == null && istek.aktif == null)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Guncellenecek en az bir alan (rol veya aktif) gereklidir.")));
                return;
            }

            boolean basarili = true;
            if (istek.rol != null) {
                if (!istek.rol.equals("Musteri") && !istek.rol.equals("Admin")) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(hata("rol 'Musteri' veya 'Admin' olmalidir.")));
                    return;
                }
                basarili = kullaniciDAO.rolGuncelle(hedefId, istek.rol) && basarili;
            }
            if (istek.aktif != null) {
                basarili = kullaniciDAO.aktifGuncelle(hedefId, istek.aktif) && basarili;
            }

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", basarili);
            sonuc.put("mesaj", basarili ? "Kullanici guncellendi." : "Kullanici bulunamadi.");
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
        String rol;
        Boolean aktif;
    }
}
