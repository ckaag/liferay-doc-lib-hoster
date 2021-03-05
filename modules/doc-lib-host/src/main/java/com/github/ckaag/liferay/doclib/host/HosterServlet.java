package com.github.ckaag.liferay.doclib.host;

import com.github.ckaag.liferay.doclib.host.config.HostDirectoriesSource;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.util.PortalUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component(
        immediate = true,
        property = {
                "osgi.http.whiteboard.context.path=/",
                "osgi.http.whiteboard.servlet.pattern=" + HosterServlet.SERVLET_PATTERN
        },
        service = Servlet.class
)
public class HosterServlet extends HttpServlet {

    public static final String SERVLET_PATTERN = "/dlhost/*";
    private final Log log = LogFactoryUtil.getLog(HosterServlet.class);

    @Reference
    private HostDirectoriesSource hostDirectoriesSource;

    @Override
    public void init() throws ServletException {
        log.info(this.getClass().getName() + " initialized, listening under servlet path: " + SERVLET_PATTERN);

        super.init();
    }

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response) {
        processHostRequest(request, response);
    }

    private DLFileEntry findFileToRequest(long companyId, HttpServletRequest request) throws PortalException {
        List<String> path = getFilePath(request);
        if (path.size() < 2) {
            throw new IllegalArgumentException("illegal path: " + request.getPathInfo());
        }
        String filename = path.get(path.size() - 1);
        String folderName = path.get(0);
        List<String> subfolder = path.subList(1, path.size() - 1);
        return hostDirectoriesSource.getFileOrNull(companyId, folderName, filename, subfolder);
    }
    
    private List<String> getFilePath(HttpServletRequest request) {
        return Arrays.stream(request.getPathInfo().split("/", -1)).skip(1).collect(Collectors.toList());
    }

    private String getFolderName(HttpServletRequest request) {
        return request.getPathInfo().split("/", -1)[1];
    }

    private void processHostRequest(HttpServletRequest httpServletRequest, HttpServletResponse response) {
        try {
            Company company = PortalUtil.getCompany(httpServletRequest);
            User user = PortalUtil.getUser(httpServletRequest);
            if (user == null) {
                String currentPath = "/o/dlhost" + httpServletRequest.getPathInfo();
                response.sendRedirect("/c/portal/login?redirect="+ URLEncoder.encode(currentPath, UTF_8));
                return;
            }
            PrincipalThreadLocal.setName(user.getUserId());
            PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(user);
            PermissionThreadLocal.setPermissionChecker(permissionChecker);
            DLFileEntry file = findFileToRequest(company.getCompanyId(), httpServletRequest);
            if (file == null || file.getSize() > 1_000_000_000) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }


            response.setCharacterEncoding(UTF_8.displayName());
            response.setContentType(file.getMimeType());
            response.setStatus(HttpServletResponse.SC_OK);

            response.setContentLength(Math.toIntExact(file.getSize()));
            byte[] buffer = new byte[10240];
            try (
                    InputStream input = file.getContentStream();
                    OutputStream output = response.getOutputStream()
            ) {
                for (int length; (length = input.read(buffer)) > 0; ) {
                    output.write(buffer, 0, length);
                }
            } catch (PortalException e) {
                log.warn("Exception occured during writing of output", e);
                throw new ServletException(e);
            }
        } catch (Exception e) {
            log.warn("Exception occured during processing", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
