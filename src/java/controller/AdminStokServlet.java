package controller;

import dao.KitapDAO;
import model.Kitap;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET  /api/admin/stok
 *      -> Tum kitaplarin anlik stok durumunu doner (admin.html Stok Takip Tablosu icin).
 *
 * POST /api/admin/stok
 *      -> Kritik Stok Kurali'ni tetikler: stogu esikDeger altina dusen ve
 *         henuz indirimli fiyati olmayan kitaplara otomatik indirim uygular.
 *      Istek govdesi: { "esikDeger": 5, "indirimOrani": 15 }
 */
@WebServlet("/api/admin/stok")
public class AdminStokServlet extends HttpServlet {

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        try {
            List<Kitap> stokListesi = kitapDAO.getStokDurumu();
            resp.getWriter().print(gson.toJson(stokListesi));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        KritikStokIstek istek = gson.fromJson(req.getReader(), KritikStokIstek.class);

        if (istek == null || istek.esikDeger <= 0 || istek.indirimOrani <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(gson.toJson(hata("esikDeger ve indirimOrani pozitif olmalidir.")));
            return;
        }

        try {
            int etkilenen = kitapDAO.kritikStokIndirimiUygula(istek.esikDeger, istek.indirimOrani);
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            sonuc.put("etkilenenKitapSayisi", etkilenen);
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

    private static class KritikStokIstek {
        int esikDeger;
        double indirimOrani;
    }
}
