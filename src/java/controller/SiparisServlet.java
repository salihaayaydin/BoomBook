package controller;

import dao.SiparisDAO;
import model.Kullanici;
import model.Siparis;
import util.SessionYardimcisi;
import com.google.gson.Gson;
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

@WebServlet({"/api/siparis", "/api/siparislerim"})
public class SiparisServlet extends HttpServlet {

    private final SiparisDAO siparisDAO = new SiparisDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            Kullanici oturumKullanicisi = SessionYardimcisi.oturumdakiKullanici(req);
            if (oturumKullanicisi == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Siparis vermek icin giris yapmalisiniz.")));
                return;
            }
            int siparisId = siparisDAO.siparisOlusturSepetten(oturumKullanicisi.getKullaniciId());
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            sonuc.put("siparisId", siparisId);
            sonuc.put("mesaj", "Siparis basariyla olusturuldu.");
            out.print(gson.toJson(sonuc));

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(gson.toJson(hata(e.getMessage())));
        } finally {
            out.flush();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            Kullanici oturumKullanicisi = SessionYardimcisi.oturumdakiKullanici(req);
            if (oturumKullanicisi == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Siparislerinizi gormek icin giris yapmalisiniz.")));
                return;
            }

            List<Siparis> siparisler = siparisDAO.getSiparislerByKullanici(oturumKullanicisi.getKullaniciId());
            out.print(gson.toJson(siparisler));
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
}

