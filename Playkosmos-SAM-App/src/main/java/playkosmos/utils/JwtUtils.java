package playkosmos.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtils {

    private final static String SECRET_KEY_STRING = System.getenv("JWT_SECRET_KEY");

    private static final SecretKey SECRET_KEY;

    static {
        SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    }

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static SecretKey getSecretKey() {
        return SECRET_KEY;
    }

}