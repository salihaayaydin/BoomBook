package controller;

import com.google.gson.Gson;
import model.Kullanici;
import util.SessionYardimcisi;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Yetkilendirme filtresi:
 *   - /admin.jsp             -> yalnizca giris yapmis VE rolu "Admin" olanlar erisebilir,
 *                                aksi halde login.jsp'ye yonlendirilir.
 *   - /api/admin/*           -> yalnizca rolu "Admin" olanlar erisebilir; aksi halde
 *                                JSON hata govdesiyle 401/403 doner (sayfa yonlendirmesi YAPILMAZ,
 *                                cunku bu uc noktalar fetch/AJAX ile cagirilir).
 *
 * Diger tum istekler (mağaza, kitap API'leri, statik dosyalar) bu filtreden etkilenmez.
 */
@WebFilter({"/admin.jsp", "/api/admin/*"})
public class AuthFilter implements Filter {

    private final Gson gson = new Gson();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        Kullanici k = SessionYardimcisi.oturumdakiKullanici(req);
        boolean apiIstegi = req.getRequestURI().contains("/api/admin/");

        if (k == null) {
            if (apiIstegi) {
                jsonHataGonder(resp, HttpServletResponse.SC_UNAUTHORIZED, "Bu islem icin giris yapmalisiniz.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?sonraki=admin.jsp");
            }
            return;
        }

        if (!k.isAdmin()) {
            if (apiIstegi) {
                jsonHataGonder(resp, HttpServletResponse.SC_FORBIDDEN, "Bu islem icin Admin yetkisi gereklidir.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/index.html?hata=admin_yetkisi_yok");
            }
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private void jsonHataGonder(HttpServletResponse resp, int statusKodu, String mesaj) throws IOException {
        resp.setStatus(statusKodu);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Map<String, Object> hata = new HashMap<>();
            hata.put("basarili", false);
            hata.put("mesaj", mesaj);
            out.print(gson.toJson(hata));
        } finally {
            out.flush();
        }
    }
}
