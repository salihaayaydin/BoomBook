package controller;

import com.google.gson.Gson;
import dao.AdminIstatistikDAO;
import model.Istatistik;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/admin/istatistik -> toplam satis, en cok satan 5 kitap, aylik gelir (son 12 ay).
 */
@WebServlet("/api/admin/istatistik")
public class AdminIstatistikServlet extends HttpServlet {

    private final AdminIstatistikDAO istatistikDAO = new AdminIstatistikDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Istatistik ist = istatistikDAO.getIstatistik();
            out.print(gson.toJson(ist));
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
