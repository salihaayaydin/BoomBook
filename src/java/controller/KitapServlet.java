package controller;

import dao.KitapDAO;
import model.Kitap;
import model.KitapSayfaSonucu;
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

/**
 * GET /api/kitaplar                       -> tum kitaplari doner (DUZ DIZI, eski davranis)
 * GET /api/kitaplar?kategoriId=X           -> sadece o kategoriye ait kitaplari doner
 * GET /api/kitaplar?yazarId=X              -> sadece o yazara ait kitaplari doner
 * GET /api/kitaplar?yayineviId=X           -> sadece o yayinevine ait kitaplari doner
 * (kategoriId, yazarId, yayineviId birlikte de gonderilebilir, AND ile birlesir)
 * GET /api/kitaplar?id=X                   -> tek bir kitabin detayini doner
 * GET /api/kitaplar?q=metin                -> kitap adi/yazar/yayinevine gore arama (DUZ DIZI)
 * GET /api/kitaplar?sirala=...             -> fiyatArtan | fiyatAzalan | cokSatan | enYeni
 * GET /api/kitaplar?sayfa=1&sayfaBoyutu=12 -> SAYFALANMIS govde doner:
 *      { "kitaplar":[...], "toplamKayit":N, "sayfa":1, "sayfaBoyutu":12, "toplamSayfa":M }
 *
 * ONEMLI (geriye donuk uyumluluk): "sayfa" parametresi GONDERILMEDIGI surece
 * yanit HER ZAMAN duz bir dizi olur (mevcut ana sayfa / mega menu kodu bunu
 * bekliyor); "sayfa" gonderildiginde yanit yukaridaki sayfalanmis nesneye doner.
 */
@WebServlet("/api/kitaplar")
public class KitapServlet extends HttpServlet {

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String idParam = req.getParameter("id");
        String kategoriParam = req.getParameter("kategoriId");
        String yazarParam = req.getParameter("yazarId");
        String yayineviParam = req.getParameter("yayineviId");
        String q = req.getParameter("q");
        String sirala = req.getParameter("sirala");
        String sayfaParam = req.getParameter("sayfa");
        String sayfaBoyutuParam = req.getParameter("sayfaBoyutu");

        try {
            if (idParam != null) {
                Kitap kitap = kitapDAO.getById(Integer.parseInt(idParam));
                if (kitap == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(hata("Kitap bulunamadi.")));
                } else {
                    out.print(gson.toJson(kitap));
                }
                return;
            }

            Integer kategoriId = (kategoriParam != null) ? Integer.parseInt(kategoriParam) : null;
            Integer yazarId = (yazarParam != null) ? Integer.parseInt(yazarParam) : null;
            Integer yayineviId = (yayineviParam != null) ? Integer.parseInt(yayineviParam) : null;

            if (sayfaParam != null) {
                int sayfa = Integer.parseInt(sayfaParam);
                int sayfaBoyutu = (sayfaBoyutuParam != null) ? Integer.parseInt(sayfaBoyutuParam) : 12;
                KitapSayfaSonucu sonuc = kitapDAO.araVeFiltrele(q, kategoriId, yazarId, yayineviId, sirala, sayfa, sayfaBoyutu);
                out.print(gson.toJson(sonuc));
                return;
            }

            if (q != null || sirala != null) {
                // Sayfalama istenmedi ama arama/siralama istendi: duz dizi olarak dondur (genis bir tavan ile).
                KitapSayfaSonucu sonuc = kitapDAO.araVeFiltrele(q, kategoriId, yazarId, yayineviId, sirala, 1, 1000);
                out.print(gson.toJson(sonuc.getKitaplar()));
                return;
            }

            List<Kitap> kitaplar = kitapDAO.getFiltered(kategoriId, yazarId, yayineviId);
            out.print(gson.toJson(kitaplar));

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

    /** Standart hata govdesi: { "basarili": false, "mesaj": "..." } */
    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
