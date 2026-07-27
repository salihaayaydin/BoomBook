package controller;

import model.Kategori;
import util.DBUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /api/kategoriler -> Navbar filtreleme icin tum kategorileri doner.
 */
@WebServlet("/api/kategoriler")
public class KategoriServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        String sql = "SELECT kategori_id, kategori_adi FROM kategori ORDER BY kategori_adi";
        List<Kategori> liste = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new Kategori(rs.getInt("kategori_id"), rs.getString("kategori_adi")));
            }
            resp.getWriter().print(gson.toJson(liste));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print(gson.toJson("Veritabani hatasi: " + e.getMessage()));
        }
    }
}
