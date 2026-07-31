package controller;

import com.google.gson.Gson;
import dao.KitapDAO;
import model.KitapFiltre;
import model.KitapSayfaSonucu;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sol filtreli Kategori/Yazar/Yayinevi katalog sayfasi icin API.
 *
 *   GET /api/katalog                       -> asagidaki parametrelerle sayfalanmis+filtrelenmis kitap listesi
 *   GET /api/katalog/fiyat-araligi         -> { "min": 0, "max": 999.9 } (fiyat slider'inin sinirlarini kurmak icin)
 *
 * /api/katalog parametreleri (hepsi opsiyonel):
 *   q               -> arama metni
 *   kategoriId      -> COKLU gonderilebilir: kategoriId=1&kategoriId=2 (OR/IN mantigiyla)
 *   yazarId         -> coklu
 *   yayineviId      -> coklu
 *   format          -> coklu: format=PDF&format=EPUB
 *   fiyatMin, fiyatMax -> ondalik
 *   sadeceStokta    -> "true" ise stok_miktari > 0 olanlar
 *   sirala          -> fiyatArtan | fiyatAzalan | cokSatan | enYeni
 *   sayfa, sayfaBoyutu
 *
 * Yanit her zaman KitapSayfaSonucu govdesiyle doner (bkz. model.KitapSayfaSonucu).
 */
@WebServlet({"/api/katalog", "/api/katalog/fiyat-araligi"})
public class KatalogServlet extends HttpServlet {

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            if ("/api/katalog/fiyat-araligi".equals(req.getServletPath())) {
                double[] aralik = kitapDAO.fiyatAraligi();
                Map<String, Object> sonuc = new HashMap<>();
                sonuc.put("min", aralik[0]);
                sonuc.put("max", aralik[1]);
                out.print(gson.toJson(sonuc));
                return;
            }

            KitapFiltre filtre = new KitapFiltre();
            filtre.setAramaMetni(req.getParameter("q"));
            filtre.setKategoriIdler(intListeOku(req, "kategoriId"));
            filtre.setYazarIdler(intListeOku(req, "yazarId"));
            filtre.setYayineviIdler(intListeOku(req, "yayineviId"));

            String[] formatlar = req.getParameterValues("format");
            if (formatlar != null && formatlar.length > 0) {
                filtre.setFormatlar(List.of(formatlar));
            }

            String fiyatMin = req.getParameter("fiyatMin");
            String fiyatMax = req.getParameter("fiyatMax");
            if (fiyatMin != null && !fiyatMin.isBlank()) filtre.setFiyatMin(Double.parseDouble(fiyatMin));
            if (fiyatMax != null && !fiyatMax.isBlank()) filtre.setFiyatMax(Double.parseDouble(fiyatMax));

            filtre.setSadeceStokta("true".equalsIgnoreCase(req.getParameter("sadeceStokta")));
            filtre.setSirala(req.getParameter("sirala"));

            String sayfaParam = req.getParameter("sayfa");
            String sayfaBoyutuParam = req.getParameter("sayfaBoyutu");
            if (sayfaParam != null && !sayfaParam.isBlank()) filtre.setSayfa(Integer.parseInt(sayfaParam));
            if (sayfaBoyutuParam != null && !sayfaBoyutuParam.isBlank()) filtre.setSayfaBoyutu(Integer.parseInt(sayfaBoyutuParam));

            KitapSayfaSonucu sonuc = kitapDAO.araVeFiltrele(filtre);
            out.print(gson.toJson(sonuc));

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("Gecersiz parametre: " + e.getMessage())));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private List<Integer> intListeOku(HttpServletRequest req, String parametreAdi) {
        String[] degerler = req.getParameterValues(parametreAdi);
        if (degerler == null || degerler.length == 0) return null;
        List<Integer> liste = new ArrayList<>();
        for (String d : degerler) {
            if (d != null && !d.isBlank()) liste.add(Integer.parseInt(d));
        }
        return liste.isEmpty() ? null : liste;
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
