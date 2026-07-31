package controller;

import com.google.gson.Gson;
import dao.KitapDAO;
import util.DosyaYardimcisi;

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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin Paneli - Kapak Resmi / E-Kitap Dosyasi Yukleme.
 *
 * POST /api/admin/yukle (multipart/form-data)
 *   Form alanlari:
 *     kitapId   -> zorunlu, hangi kitaba ait oldugunu belirtir
 *     tur       -> "kapak" | "dosya"
 *     dosya     -> yuklenen fiziksel dosya (Part)
 *
 *   "kapak" turunde: uploads/kapaklar/ altina kaydedilir, kitap.kapak_resmi_url guncellenir.
 *   "dosya" turunde: uploads/kitaplar/ altina kaydedilir, kitap.dosya_yolu (+ formati) guncellenir.
 *
 * Basarili yanit: { "basarili": true, "url": "/uploads/kapaklar/kitap-5.jpg" }
 */
@WebServlet("/api/admin/yukle")
@MultipartConfig(
        maxFileSize = 50L * 1024 * 1024,       // 50 MB (e-kitap dosyalari icin)
        maxRequestSize = 55L * 1024 * 1024,
        fileSizeThreshold = 1024 * 1024
)
public class AdminYuklemeServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AdminYuklemeServlet.class.getName());

    private static final java.util.Set<String> IZINLI_KAPAK_UZANTI = java.util.Set.of("jpg", "jpeg", "png", "webp");
    private static final java.util.Set<String> IZINLI_DOSYA_UZANTI = java.util.Set.of("pdf", "epub", "mobi");

    private final KitapDAO kitapDAO = new KitapDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String kitapIdParam = req.getParameter("kitapId");
            String tur = req.getParameter("tur");
            Part dosyaPart = req.getPart("dosya");

            if (kitapIdParam == null || kitapIdParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("kitapId zorunludur.")));
                return;
            }
            if (tur == null || (!tur.equals("kapak") && !tur.equals("dosya"))) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("tur 'kapak' veya 'dosya' olmalidir.")));
                return;
            }
            if (dosyaPart == null || dosyaPart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(hata("Yuklenecek dosya bulunamadi.")));
                return;
            }

            int kitapId = Integer.parseInt(kitapIdParam);
            String orijinalAd = dosyaPart.getSubmittedFileName();
            String uzanti = uzantiAl(orijinalAd);

            if ("kapak".equals(tur)) {
                if (!IZINLI_KAPAK_UZANTI.contains(uzanti)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(hata("Kapak resmi sadece jpg/jpeg/png/webp olabilir.")));
                    return;
                }
                File hedefDizin = DosyaYardimcisi.kapaklarDizini(getServletContext());
                String dosyaAdi = "kitap-" + kitapId + "-" + System.currentTimeMillis() + "." + uzanti;
                File hedefDosya = new File(hedefDizin, dosyaAdi);
                kaydet(dosyaPart, hedefDosya);

                String url = "uploads/kapaklar/" + dosyaAdi;
                kitapDAO.kapakGuncelle(kitapId, url);

                Map<String, Object> sonuc = new HashMap<>();
                sonuc.put("basarili", true);
                sonuc.put("url", url);
                sonuc.put("mesaj", "Kapak resmi yuklendi.");
                out.print(gson.toJson(sonuc));

            } else {
                if (!IZINLI_DOSYA_UZANTI.contains(uzanti)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(hata("E-kitap dosyasi sadece pdf/epub/mobi olabilir.")));
                    return;
                }
                File hedefDizin = DosyaYardimcisi.kitaplarDizini(getServletContext());
                String dosyaAdi = "kitap-" + kitapId + "-" + System.currentTimeMillis() + "." + uzanti;
                File hedefDosya = new File(hedefDizin, dosyaAdi);
                kaydet(dosyaPart, hedefDosya);

                String url = "uploads/kitaplar/" + dosyaAdi;
                double boyutMb = Math.round((dosyaPart.getSize() / (1024.0 * 1024.0)) * 100.0) / 100.0;
                kitapDAO.dosyaGuncelle(kitapId, url, uzanti.toUpperCase(), boyutMb);

                Map<String, Object> sonuc = new HashMap<>();
                sonuc.put("basarili", true);
                sonuc.put("url", url);
                sonuc.put("mesaj", "E-kitap dosyasi yuklendi.");
                out.print(gson.toJson(sonuc));
            }

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(hata("kitapId sayisal olmalidir.")));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } catch (IllegalStateException e) {
            // dosya boyutu maxFileSize/maxRequestSize sinirini astiginda Tomcat bunu firlatir
            resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            out.print(gson.toJson(hata("Dosya cok buyuk (maksimum 50 MB).")));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Dosya yukleme hatasi", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Dosya yuklenemedi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private void kaydet(Part part, File hedef) throws IOException {
        try (InputStream giris = part.getInputStream();
             OutputStream cikis = Files.newOutputStream(hedef.toPath())) {
            byte[] tampon = new byte[8192];
            int okunan;
            while ((okunan = giris.read(tampon)) != -1) {
                cikis.write(tampon, 0, okunan);
            }
        }
    }

    private String uzantiAl(String dosyaAdi) {
        if (dosyaAdi == null) return "";
        int nokta = dosyaAdi.lastIndexOf('.');
        return nokta >= 0 ? dosyaAdi.substring(nokta + 1).toLowerCase() : "";
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
