package vn.edu.hcmute.jwt.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {
    private final String secret;
    private final long expirationMs;
    private byte[] signingSecret;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void initialize() {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and contain at least 32 UTF-8 bytes"
            );
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS must be positive");
        }
        signingSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("bt10-nimbus")
                .subject(userDetails.getUsername())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(expirationMs)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(signingSecret));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new JwtValidationException("Unable to sign JWT", exception);
        }
    }

    public String extractUsername(String token) {
        return parseAndVerify(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        JWTClaimsSet claims = parseAndVerify(token);
        return userDetails.getUsername().equalsIgnoreCase(claims.getSubject());
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                throw new JwtValidationException("Unexpected JWT algorithm");
            }
            if (!jwt.verify(new MACVerifier(signingSecret))) {
                throw new JwtValidationException("JWT signature is invalid");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (!"bt10-nimbus".equals(claims.getIssuer())
                    || !StringUtils.hasText(claims.getSubject())
                    || expiration == null
                    || !expiration.after(new Date())) {
                throw new JwtValidationException("JWT claims are invalid or expired");
            }
            return claims;
        } catch (ParseException | JOSEException exception) {
            throw new JwtValidationException("JWT is malformed or cannot be verified", exception);
        }
    }

    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }

        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
