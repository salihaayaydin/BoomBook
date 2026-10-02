package controller;

import com.google.gson.Gson;
import dao.DegerlendirmeDAO;
import dao.KutuphaneDAO;
import model.Degerlendirme;
import model.Kullanici;
import util.DosyaYardimcisi;
import util.SessionYardimcisi;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Degerlendirme / Yorum API'si.
 *
 *   GET /api/degerlendirme?kitapId=X
 *       -> { "ortalama": 4.3, "sayim": 12, "yorumlar": [...],
 *            "satinAlmisMi": true|false (giris yoksa false),
 *            "kullaniciDegerlendirmesi": {...} | null (giris yoksa null) }
 *
 *   POST /api/degerlendirme  (multipart/form-data)
 *       Form alanlari: kitapId, puan (1-5), yorum (opsiyonel), resim (opsiyonel, Part)
 *       Giris + bu kitabi SATIN ALMIS OLMA sarti (kutuphane tablosunda kayit) aranir.
 *       Kullanici bu kitaba daha once yazdiysa GUNCELLENIR (tek yorum kurali); resim
 *       gonderilmezse mevcut fotograf (varsa) korunur.
 *
 *   DELETE /api/degerlendirme?kitapId=X
 *       -> Kullanicinin kendi degerlendirmesini siler.
 */
@WebServlet("/api/degerlendirme")
@MultipartConfig(
        maxFileSize = 8L * 1024 * 1024,        // 8 MB
        maxRequestSize = 10L * 1024 * 1024,
        fileSizeThreshold = 1024 * 1024
)
public class DegerlendirmeServlet extends HttpServlet {

    private static final Set<String> IZINLI_RESIM_UZANTI = Set.of("jpg", "jpeg", "png", "webp");

    private final DegerlendirmeDAO degerlendirmeDAO = new DegerlendirmeDAO();
    private final KutuphaneDAO kutuphaneDAO = new KutuphaneDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String kitapIdParam = req.getParameter("kitapId");
            if (kitapIdParam == null || kitapIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId parametresi gereklidir.")));
                return;
            }
            int kitapId = Integer.parseInt(kitapIdParam);

            double[] ortalamaVeSayim = degerlendirmeDAO.ortalamaVeSayim(kitapId);
            List<Degerlendirme> yorumlar = degerlendirmeDAO.getByKitap(kitapId);

            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            boolean satinAlmisMi = false;
            Degerlendirme kullaniciDegerlendirmesi = null;
            if (kullanici != null) {
                satinAlmisMi = kutuphaneDAO.sahipMi(kullanici.getKullaniciId(), kitapId);
                kullaniciDegerlendirmesi = degerlendirmeDAO.kullaniciDegerlendirmesi(kullanici.getKullaniciId(), kitapId);
            }

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("ortalama", Math.round(ortalamaVeSayim[0] * 10) / 10.0);
            sonuc.put("sayim", (int) ortalamaVeSayim[1]);
            sonuc.put("yorumlar", yorumlar);
            sonuc.put("satinAlmisMi", satinAlmisMi);
            sonuc.put("kullaniciDegerlendirmesi", kullaniciDegerlendirmesi);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            if (kullanici == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Değerlendirme yapmak için giriş yapmalısınız.")));
                return;
            }

            String kitapIdParam = req.getParameter("kitapId");
            String puanParam = req.getParameter("puan");
            String yorum = req.getParameter("yorum");

            int kitapId, puan;
            try {
                kitapId = Integer.parseInt(kitapIdParam);
                puan = Integer.parseInt(puanParam);
            } catch (NumberFormatException | NullPointerException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId ve 1-5 arasi bir puan gereklidir.")));
                return;
            }
            if (kitapId <= 0 || puan < 1 || puan > 5) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId ve 1-5 arasi bir puan gereklidir.")));
                return;
            }
            if (yorum != null && yorum.length() > 2000) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Yorum en fazla 2000 karakter olabilir.")));
                return;
            }

            boolean satinAlmisMi = kutuphaneDAO.sahipMi(kullanici.getKullaniciId(), kitapId);
            if (!satinAlmisMi) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print(gson.toJson(hata("Sadece satın aldığınız kitaplara değerlendirme yapabilirsiniz.")));
                return;
            }

            // Fotograf opsiyonel: gonderilmediyse null kalir, DAO mevcut fotografi korur.
            String resimUrl = null;
            Part resimPart = req.getPart("resim");
            if (resimPart != null && resimPart.getSize() > 0) {
                String uzanti = uzantiAl(resimPart.getSubmittedFileName());
                if (!IZINLI_RESIM_UZANTI.contains(uzanti)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(hata("Fotoğraf sadece jpg/jpeg/png/webp olabilir.")));
                    return;
                }
                File hedefDizin = DosyaYardimcisi.degerlendirmelerDizini(getServletContext());
                String dosyaAdi = "yorum-" + kullanici.getKullaniciId() + "-" + kitapId + "-" + System.currentTimeMillis() + "." + uzanti;
                File hedefDosya = new File(hedefDizin, dosyaAdi);
                kaydet(resimPart, hedefDosya);
                resimUrl = "uploads/degerlendirmeler/" + dosyaAdi;
            }

            degerlendirmeDAO.ekleVeyaGuncelle(kullanici.getKullaniciId(), kitapId, puan,
                    yorum != null ? yorum.trim() : null, resimUrl);

            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", true);
            sonuc.put("mesaj", "Değerlendirmeniz kaydedildi.");
            out.print(gson.toJson(sonuc));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private void kaydet(Part part, File hedef) throws IOException {
        try (InputStream in = part.getInputStream();
             OutputStream outStream = Files.newOutputStream(hedef.toPath())) {
            in.transferTo(outStream);
        }
    }

    private String uzantiAl(String dosyaAdi) {
        if (dosyaAdi == null) return "";
        int nokta = dosyaAdi.lastIndexOf('.');
        return nokta >= 0 ? dosyaAdi.substring(nokta + 1).toLowerCase() : "";
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
            if (kullanici == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print(gson.toJson(hata("Bu işlem için giriş yapmalısınız.")));
                return;
            }
            String kitapIdParam = req.getParameter("kitapId");
            if (kitapIdParam == null || kitapIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId parametresi gereklidir.")));
                return;
            }
            boolean silindi = degerlendirmeDAO.sil(kullanici.getKullaniciId(), Integer.parseInt(kitapIdParam));
            Map<String, Object> sonuc = new HashMap<>();
            sonuc.put("basarili", silindi);
            sonuc.put("mesaj", silindi ? "Değerlendirmeniz silindi." : "Değerlendirme bulunamadı.");
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

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
