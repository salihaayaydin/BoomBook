package controller;

import com.google.gson.Gson;
import dao.KitapDAO;
import dao.SepetDAO;
import model.Kitap;
import model.Kullanici;
import model.SepetKalemi;
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

@WebServlet("/api/sepet")
public class SepetServlet extends HttpServlet {

    private final SepetDAO sepetDAO = new SepetDAO();
    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = girisKontrol(req, resp, out);
            if (kullanici == null) {
                return;
            }
            List<SepetKalemi> sepet = sepetDAO.getSepet(kullanici.getKullaniciId());
            out.print(gson.toJson(sepet));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = girisKontrol(req, resp, out);
            if (kullanici == null) {
                return;
            }

            SepetIstek istek = govdeOku(req);
            if (istek == null || istek.kitapId <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Gecerli bir kitapId gereklidir.")));
                return;
            }

            SepetKalemi kalem = sepetDAO.ekle(kullanici.getKullaniciId(), istek.kitapId,
                    istek.adet > 0 ? istek.adet : 1);

            if (kalem == null) {
                Kitap kitap = kitapDAO.getById(istek.kitapId);
                if (kitap == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(hata("Kitap bulunamadi.")));
                } else {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(hata("Üzgünüz, \"" + kitap.getKitapAdi() + "\" şu anda stokta yok.")));
                }
                return;
            }

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            boolean stoklaSinirli = kalem.getStokMiktari() > 0 && kalem.getAdet() >= kalem.getStokMiktari();
            sonuc.put("mesaj", stoklaSinirli
                    ? "Kitap sepete eklendi. Stok sınırı nedeniyle bu üründen en fazla " + kalem.getStokMiktari() + " adet sepete konabilir."
                    : "Kitap sepete eklendi.");
            sonuc.put("stoklaSinirli", stoklaSinirli);
            sonuc.put("kalem", kalem);
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = girisKontrol(req, resp, out);
            if (kullanici == null) {
                return;
            }

            SepetIstek istek = govdeOku(req);
            if (istek == null || istek.kitapId <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Gecerli bir kitapId gereklidir.")));
                return;
            }

            SepetKalemi kalem = sepetDAO.adetGuncelle(kullanici.getKullaniciId(), istek.kitapId, istek.adet);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            boolean stoklaSinirli = kalem != null && kalem.getStokMiktari() > 0
                    && kalem.getAdet() >= kalem.getStokMiktari() && kalem.getAdet() < istek.adet;
            sonuc.put("mesaj", kalem == null
                    ? "Kitap sepetten kaldırıldı."
                    : stoklaSinirli
                        ? "Stok sınırı nedeniyle adet " + kalem.getAdet() + " ile sınırlandırıldı."
                        : "Adet güncellendi.");
            sonuc.put("stoklaSinirli", stoklaSinirli);
            sonuc.put("kalem", kalem);
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = girisKontrol(req, resp, out);
            if (kullanici == null) {
                return;
            }

            boolean hepsi = "true".equalsIgnoreCase(req.getParameter("hepsi"));
            Map<String, Object> sonuc = new HashMap<>();

            if (hepsi) {
                sepetDAO.temizle(kullanici.getKullaniciId());
                sonuc.put("basarili", true);
                sonuc.put("mesaj", "Sepet bosaltildi.");
                out.print(gson.toJson(sonuc));
                return;
            }

            String kitapIdParam = req.getParameter("kitapId");
            if (kitapIdParam == null || kitapIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId parametresi gereklidir.")));
                return;
            }

            int kitapId = Integer.parseInt(kitapIdParam);
            boolean kaldirildi = sepetDAO.kaldir(kullanici.getKullaniciId(), kitapId);

            sonuc.put("basarili", kaldirildi);
            sonuc.put("mesaj", kaldirildi ? "Kitap sepetten kaldirildi." : "Kitap sepette bulunamadi.");
            out.print(gson.toJson(sonuc));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("kitapId sayisal olmalidir.")));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    /** Giris kontrolu yapar; giris yoksa 401 yazip null doner (cagiran taraf hemen return etmeli). */
    private Kullanici girisKontrol(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
        if (kullanici == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(hata("Sepetinizi kullanmak icin giris yapmalisiniz.")));
        }
        return kullanici;
    }

    private SepetIstek govdeOku(HttpServletRequest req) throws IOException {
        String govde;
        try (BufferedReader reader = req.getReader()) {
            govde = reader.lines().collect(Collectors.joining());
        }
        if (govde == null || govde.isBlank()) {
            return null;
        }
        return gson.fromJson(govde, SepetIstek.class);
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }

    private static class SepetIstek {
        int kitapId;
        int adet;
    }
}
