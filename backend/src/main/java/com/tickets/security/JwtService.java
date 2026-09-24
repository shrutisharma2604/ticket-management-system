package com.tickets.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ISSUER = "ticket-management-system";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecurityProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    @Autowired
    public JwtService(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    private JwtService(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;

        byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }

        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public String issueToken(String username) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.tokenTtl()))
                .subject(username)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String validateAndGetSubject(String token) {
        return decoder.decode(token).getSubject();
    }
}
