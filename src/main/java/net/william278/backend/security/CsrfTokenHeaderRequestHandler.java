/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.util.Assert;

import java.util.function.Supplier;

@Slf4j
public class CsrfTokenHeaderRequestHandler implements CsrfTokenRequestHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> deferredCsrfToken) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        Assert.notNull(deferredCsrfToken, "deferredCsrfToken cannot be null");

        request.setAttribute(HttpServletResponse.class.getName(), response);
        CsrfToken csrfToken = new SupplierCsrfToken(deferredCsrfToken);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        String csrfAttrName = csrfToken.getParameterName();
        request.setAttribute(csrfAttrName, csrfToken);
        log.warn("expected: {}, csrfAttrName: {}", csrfToken.getToken(), csrfAttrName);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(csrfToken, "csrfToken cannot be null");
        final String res = request.getHeader(csrfToken.getHeaderName());
        log.warn("resolveCsrfTokenValue: {}, headerName: {}", res, csrfToken.getHeaderName());
        return res;
    }

    private record SupplierCsrfToken(Supplier<CsrfToken> csrfTokenSupplier) implements CsrfToken {

        @Override
        public String getHeaderName() {
            return getDelegate().getHeaderName();
        }

        @Override
        public String getParameterName() {
            return getDelegate().getParameterName();
        }

        @Override
        public String getToken() {
            return getDelegate().getToken();
        }

        private CsrfToken getDelegate() {
            CsrfToken delegate = this.csrfTokenSupplier.get();
            if (delegate == null) {
                throw new IllegalStateException("csrfTokenSupplier returned null delegate");
            }
            return delegate;
        }

    }

}
