package com.toeflsimulator.identity;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.*;
import com.toeflsimulator.common.ApiException;
@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy strategy;
    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts, CsrfTokenRepository csrf) {
        this.manager=manager; this.contexts=contexts;
        this.strategy=new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }
    public record Login(@NotBlank @Size(max=254) String email, @NotBlank @Size(max=128) String password) {}
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
        try {
            var authentication=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login.email().toLowerCase(Locale.ROOT),login.password()));
            strategy.onAuthentication(authentication,request,response);
            var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context); contexts.saveContext(context,request,response);
            var student=(StudentPrincipal)authentication.getPrincipal();
            return Map.of("user", Map.of("id",student.id(),"email",student.email()));
        } catch (AuthenticationException error) { throw new ApiException(401,"INVALID_CREDENTIALS","Invalid credentials."); }
    }
    @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal StudentPrincipal user) { return Map.of("id",user.id(),"email",user.email()); }
}
