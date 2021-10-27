/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class AngularServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
			@Override
			public void sendError(int sc) throws IOException {
				if (sc == HttpServletResponse.SC_NOT_FOUND) {
					try {
						// Write index.html when resource not found
						includeWebJarAsset(request, response, true);
					} catch (ServletException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					super.sendError(sc);
				}
			}
		};
		includeWebJarAsset(request, responseWrapper, false);
		
	}

	private void includeWebJarAsset(HttpServletRequest request, HttpServletResponse response, boolean forceIndexHtml)
			throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if ("/".equals(pathInfo) || forceIndexHtml) {
			pathInfo = "/index.html";
		}
		if ("/index.html".equals(pathInfo)) {
			response = new HttpServletResponseWrapper(response) {
				@Override
				public ServletOutputStream getOutputStream() throws IOException {
					String base = request.getServletPath();
					if (base.equals("")) {
						base = "/";
					} else if (!base.endsWith("/")) {
						base = base + "/";
					}
					BaseRewritingServletOutputStream baseRewritingServletOutputStream =
							new BaseRewritingServletOutputStream(super.getOutputStream(), base);
					return baseRewritingServletOutputStream;
				}
			};
		}
		final String webJarsServletPath = "/webjars/";
		final String webJarsRequestURI = webJarsServletPath + "wearefrank__ladybug" + pathInfo;
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
			@Override
			public String getServletPath() {
				return webJarsServletPath;
			}
			@Override
			public String getRequestURI() {
				return webJarsRequestURI;
			}
		};
		RequestDispatcher requestDispatcher = request.getRequestDispatcher(webJarsRequestURI);
		requestDispatcher.include(wrapper, response);
	}
}

class BaseRewritingServletOutputStream extends ServletOutputStream {
	ServletOutputStream servletOutputStream;
	String newBase;
	int i = -1;
	int phase = 1;
	StringBuffer stringBuffer = new StringBuffer();
	
	BaseRewritingServletOutputStream(ServletOutputStream servletOutputStream, String newBase) {
		this.servletOutputStream = servletOutputStream;
		this.newBase = newBase;
		if (newBase != null) {
			i = 0;
		}
	}

	@Override
	public boolean isReady() {
		return servletOutputStream.isReady();
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		servletOutputStream.setWriteListener(writeListener);
		
	}

	@Override
	public void write(int b) throws IOException {
		if (i != -1) {
			if (phase == 2 && (char)b != '"') {
				// Discard char from old base
			} else {
				servletOutputStream.write(b);
				stringBuffer.append((char)b);
				if (phase == 2 && (char)b == '"') {
					i = -1;
				} else {
					if (stringBuffer.length() > 11
							&& stringBuffer.charAt(stringBuffer.length() - 12) == '<'
							&& stringBuffer.charAt(stringBuffer.length() - 11) == 'b'
							&& stringBuffer.charAt(stringBuffer.length() - 10) == 'a'
							&& stringBuffer.charAt(stringBuffer.length() - 9) == 's'
							&& stringBuffer.charAt(stringBuffer.length() - 8) == 'e'
							&& stringBuffer.charAt(stringBuffer.length() - 7) == ' '
							&& stringBuffer.charAt(stringBuffer.length() - 6) == 'h'
							&& stringBuffer.charAt(stringBuffer.length() - 5) == 'r'
							&& stringBuffer.charAt(stringBuffer.length() - 4) == 'e'
							&& stringBuffer.charAt(stringBuffer.length() - 3) == 'f'
							&& stringBuffer.charAt(stringBuffer.length() - 2) == '='
							&& stringBuffer.charAt(stringBuffer.length() - 1) == '"'
							) {
						for (int i = 0; i < newBase.length(); i++) {
							servletOutputStream.write(newBase.charAt(i));
						}
						phase = 2;
					}
				}
			}
		} else {
			servletOutputStream.write(b);
		}
	}

}
