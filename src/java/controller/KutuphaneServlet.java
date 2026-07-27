package controller;

import com.google.gson.Gson;
import dao.KutuphaneDAO;
import model.KutuphaneKalemi;
import model.Kullanici;
import util.DosyaYardimcisi;
import util.SessionYardimcisi;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * "Kutuphanem" API'si:
 *   GET /api/kutuphanem              -> giris yapmis kullanicinin satin aldigi kitaplarin listesi
 *   GET /api/kutuphane/indir?kitapId=X -> kullanici bu kitaba sahipse dosyayi indirir (401/403/404 kontrolleriyle)
 */
@WebServlet({"/api/kutuphanem", "/api/kutuphane/indir"})
public class KutuphaneServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(KutuphaneServlet.class.getName());

    private final KutuphaneDAO kutuphaneDAO = new KutuphaneDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String yol = req.getServletPath();

        Kullanici kullanici = SessionYardimcisi.oturumdakiKullanici(req);
        if (kullanici == null) {
            if ("/api/kutuphane/indir".equals(yol)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bu islem icin giris yapmalisiniz.");
            } else {
                resp.setContentType("application/json;charset=UTF-8");
                PrintWriter out = resp.getWriter();
                try {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print(gson.toJson(hata("Kutuphanenizi gormek icin giris yapmalisiniz.")));
                } finally {
                    out.flush();
                }
            }
            return;
        }

        if ("/api/kutuphane/indir".equals(yol)) {
            dosyaIndir(req, resp, kullanici);
        } else {
            kutuphaneyiListele(resp, kullanici);
        }
    }

    private void kutuphaneyiListele(HttpServletResponse resp, Kullanici kullanici) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            List<KutuphaneKalemi> liste = kutuphaneDAO.getKutuphane(kullanici.getKullaniciId());
            out.print(gson.toJson(liste));
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(hata("Veritabani hatasi: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private void dosyaIndir(HttpServletRequest req, HttpServletResponse resp, Kullanici kullanici) throws IOException {
        String kitapIdParam = req.getParameter("kitapId");
        int kitapId;
        try {
            kitapId = Integer.parseInt(kitapIdParam);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Gecerli bir kitapId gereklidir.");
            return;
        }

        try {
            boolean sahip = kutuphaneDAO.sahipMi(kullanici.getKullaniciId(), kitapId);
            if (!sahip) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bu kitaba erisim yetkiniz yok. Once satin almaniz gerekiyor.");
                return;
            }

            String kayitliYol = kutuphaneDAO.indirmeBaglantisiGetir(kullanici.getKullaniciId(), kitapId);
            File dosya = DosyaYardimcisi.dosyayaCozumle(getServletContext(), kayitliYol);

            if (dosya == null || !dosya.exists() || !dosya.isFile()) {
                LOG.log(Level.WARNING, "Kutuphane dosyasi diskte bulunamadi: kitapId={0}, kayitliYol={1}",
                        new Object[]{kitapId, kayitliYol});
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Dosya sunucuda bulunamadi. Lutfen yonetici ile iletisime gecin.");
                return;
            }

            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + dosya.getName() + "\"");
            resp.setContentLengthLong(dosya.length());

            try (FileInputStream giris = new FileInputStream(dosya);
                 OutputStream cikis = resp.getOutputStream()) {
                byte[] tampon = new byte[8192];
                int okunan;
                while ((okunan = giris.read(tampon)) != -1) {
                    cikis.write(tampon, 0, okunan);
                }
                cikis.flush();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Kutuphane indirme sirasinda veritabani hatasi", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Veritabani hatasi: " + e.getMessage());
        }
    }

    private Map<String, Object> hata(String mesaj) {
        Map<String, Object> map = new HashMap<>();
        map.put("basarili", false);
        map.put("mesaj", mesaj);
        return map;
    }
}
