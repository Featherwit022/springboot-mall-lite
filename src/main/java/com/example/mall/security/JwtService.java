package com.example.mall.security;

import com.example.mall.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 令牌签发与校验。
 * 只依赖配置和密钥，不碰数据库——这正是「无状态」的含义：
 * 服务端重启、多实例部署都不影响已有 token 的校验。
 */
@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final AuthProperties properties;
    private final SecretKey key;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        // 早失败优于运行期报 WeakKeyException：配错了应在启动时就暴露
        if (bytes.length < 32) {
            throw new IllegalStateException("mall.auth.secret 至少需要 32 字节（HS256 要求 256 bit）");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** 签发：sub 放 userId，claim 放 username 与 role */
    public String issue(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .setIssuer(properties.getIssuer())
                // jti 唯一标识本次令牌，未来做「单点下线 / 黑名单」时靠它定位
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(properties.getTtl())))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验并还原身份。
     * 任何非法情况（签名不符、过期、issuer 不对、结构损坏）都会抛 JwtException 子类，
     * 由调用方（拦截器）统一翻译成 4010，这里不吞异常。
     */
    public LoginUser parse(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(properties.getIssuer())
                .setAllowedClockSkewSeconds(30)   // 容忍集群间几十秒时钟偏差
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new LoginUser(userId, username, role);
    }

    public long getTtlSeconds() {
        return properties.getTtl().getSeconds();
    }
}
