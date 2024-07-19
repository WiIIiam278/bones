/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
