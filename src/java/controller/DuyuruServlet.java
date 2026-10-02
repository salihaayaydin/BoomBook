package controller;

import com.google.gson.Gson;
import dao.DuyuruDAO;
import model.Duyuru;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GET /api/duyurular[?tur=slider|kart] -> aktif duyurulari, siraya gore doner.
 *  tur belirtilmezse hepsi (slider+kart) doner; ana sayfa hero slider'i icin
 *  ?tur=slider, kucuk kampanya kartlari icin ?tur=kart kullanilir. */
@WebServlet("/api/duyurular")
public class DuyuruServlet extends HttpServlet {

    private final DuyuruDAO duyuruDAO = new DuyuruDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String tur = req.getParameter("tur");
            List<Duyuru> liste = duyuruDAO.listeAktif(tur);
            out.print(gson.toJson(liste));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> hata = new HashMap<>();
            hata.put("basarili", false);
            hata.put("mesaj", "Veritabani hatasi: " + e.getMessage());
            out.print(gson.toJson(hata));
        } finally {
            out.flush();
        }
    }
}
