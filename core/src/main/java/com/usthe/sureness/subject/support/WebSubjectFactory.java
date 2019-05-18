package com.usthe.sureness.subject.support;

import com.usthe.sureness.processor.exception.UnsupportedTokenException;
import com.usthe.sureness.subject.SubjectAuToken;
import com.usthe.sureness.util.JsonWebTokenUtil;
import com.usthe.sureness.util.SurenessCommonUtil;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author tomsun28
 * @date 23:35 2019-05-12
 */
public class WebSubjectFactory extends BaseSubjectFactory {

    private static final String BEARER = "Bearer";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic";
    private static final int COUNT_2 = 2;

    @Override
    public SubjectAuToken createSubjectAuToken(Object request) throws UnsupportedTokenException {
        if (request instanceof ServletRequest) {
            String authorization = ((HttpServletRequest)request).getHeader(AUTHORIZATION);
            // 根据head里面的参数内容，判断其请求认证鉴权的方式，新建对应的token
            // 现在支持 json web token, Basic auth, ...
            // ("Authorization", "Bearer eyJhbGciOiJIUzUxMi...")  --- jwt auth
            // ("Authorization", "Basic YWRtaW46YWRtaW4=")        --- basic auth
            if (authorization.startsWith(BEARER)) {
                // jwt token
                String jwtValue = authorization.replace(BEARER, "").trim();
                if (JsonWebTokenUtil.isNotJsonWebToken(jwtValue)) {
                    throw new UnsupportedTokenException("Bearer token now support jwt");
                }
                String remoteHost = ((HttpServletRequest) request).getRemoteHost();
                String requestUri = ((HttpServletRequest) request).getRequestURI();
                String requestType = ((HttpServletRequest) request).getMethod();
                String targetUri = requestUri.concat("===").concat(requestType.toUpperCase());
                String userAgent = SurenessCommonUtil.findUserAgent((HttpServletRequest) request);
                return JwtSubjectToken.builder(jwtValue)
                        .setRemoteHost(remoteHost)
                        .setTargetResource(targetUri)
                        .setUserAgent(userAgent)
                        .build();
            } else if (authorization.startsWith(BASIC)) {
                //basic auth
                String basicAuth = authorization.replace(BASIC, "").trim();
                basicAuth = new String(Base64.getDecoder().decode(basicAuth), StandardCharsets.UTF_8);
                String[] auth = basicAuth.split(":");
                if (auth.length != COUNT_2) {
                    throw new UnsupportedTokenException("can not create token due the request message");
                }
                String username = auth[0];
                String password = auth[1];
                String remoteHost = ((HttpServletRequest) request).getRemoteHost();
                String requestUri = ((HttpServletRequest) request).getRequestURI();
                String requestType = ((HttpServletRequest) request).getMethod();
                String targetUri = requestUri.concat("===").concat(requestType.toUpperCase());
                return PasswordSubjectToken.builder(username, password)
                        .setRemoteHost(remoteHost)
                        .setTargetResource(targetUri)
                        .build();
            } else {
                throw new UnsupportedTokenException("can not create token due the request message");
            }
        } else {
            throw new UnsupportedTokenException("can not create token due the request message");
        }
    }
}
