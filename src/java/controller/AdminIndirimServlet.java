package controller;

import dao.KitapDAO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/admin/indirim
 *      -> Admin panelindeki "Yayinevine Gore Toplu Indirim Formu" tarafindan cagrilir.
 *      Istek govdesi: { "yayineviId": 3, "indirimOrani": 20 }
 *      Secilen yayinevine ait tum kitaplarin indirimli_fiyat kolonunu
 *      fiyat * (1 - oran/100) olacak sekilde gunceller.
 */
@WebServlet("/api/admin/indirim")
public class AdminIndirimServlet extends HttpServlet {

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        IndirimIstek istek = gson.fromJson(req.getReader(), IndirimIstek.class);

        if (istek == null || istek.yayineviId <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(gson.toJson(hata("Gecerli bir yayineviId gonderilmelidir.")));
            return;
        }
        if (istek.indirimOrani < 0 || istek.indirimOrani > 100) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(gson.toJson(hata("indirimOrani 0 ile 100 arasinda olmalidir.")));
            return;
        }

        try {
            int etkilenen = kitapDAO.yayineviIndirimUygula(istek.yayineviId, istek.indirimOrani);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            sonuc.put("etkilenenKitapSayisi", etkilenen);
            sonuc.put("mesaj", "Indirim basariyla uygulandi.");
            resp.getWriter().print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        }
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }

    private static class IndirimIstek {
        int yayineviId;
        double indirimOrani;
    }
}
