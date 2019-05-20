package com.cyanoth.secretwarden.config.UI;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.soy.renderer.SoyException;

// [1] https://developer.atlassian.com/server/framework/atlassian-sdk/creating-an-admin-configuration-form/

@Scanned
public class GlobalConfigServlet extends HttpServlet
{
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private SoyTemplateRenderer soyTemplateRenderer;

  @Inject
  public GlobalConfigServlet(@ComponentImport UserManager userManager,
                             @ComponentImport LoginUriProvider loginUriProvider,
                             @ComponentImport SoyTemplateRenderer soyTemplateRenderer)
  {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.soyTemplateRenderer = soyTemplateRenderer;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    UserKey userKey = userManager.getRemoteUserKey(request);
    if (userKey == null || !userManager.isAdmin(userKey))
    {
      redirectToLogin(request, response);
      return;
    }

    response.setContentType("text/html;charset=UTF-8");
		soyTemplateRenderer.render(response.getWriter(), "com.cyanoth.secretwarden:secretwarden-globalconfig-ui-res",
				"com.cyanoth.secretwarden.configPage", null);
  }

  private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
  {
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
  }

  private URI getUri(HttpServletRequest request)
  {
    StringBuffer builder = request.getRequestURL();
    if (request.getQueryString() != null)
    {
      builder.append("?");
      builder.append(request.getQueryString());
    }
    return URI.create(builder.toString());
  }

}