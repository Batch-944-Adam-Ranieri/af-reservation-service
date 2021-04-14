package com.revature.aspects;

import com.revature.dtos.UserDTO;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;

/**
 * Before any controller method is called, the verifyJWT is called to make sure the jwt is valid. If valid, the user info
 * is placed into the first arg of the controller. If not valid, throws error.
 */
@Component
@Aspect
public class SecurityAspect {

    private final Logger logger = Logger.getLogger(SecurityAspect.class);

    /**
     * WebClient injection for WebClientBuilder
     */
    @Autowired
    private WebClient.Builder webClientBuilder;

    /**
     * Called when pointcut method is implemented. Used for verifying incoming JWT at join point.
     *
     * @param pjp       Join Point
     * @return          If JWT is valid, sets the userDTO for the method at that join point.
     *                  If JWT is not valid, throws exception and returns not authorized request.
     * @throws Throwable when JWT cannot be verified.
     */
    @Around("controllerMethodsPointCut()")
    public Object verifyJwt(ProceedingJoinPoint pjp) throws Throwable {
        // PRODUCTION CODE
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String auth = request.getHeader("Authorization");

        try {
            UserDTO userDTO = webClientBuilder.build()
                    // This uses the auth service from consul
                    .post().uri(System.getenv("AUTH_SERVER"))
                    .body(Mono.just(auth), String.class)
                    .retrieve()
                    .onStatus(HttpStatus.UNAUTHORIZED::equals,
                            clientResponse -> null)
                    .bodyToMono(UserDTO.class).block();
            logger.info(userDTO);
            if (userDTO != null && userDTO.getId() != 0) {
                logger.info("JWT verified: " + userDTO);
                Object[] args = pjp.getArgs();
                args[0] = userDTO;
                return pjp.proceed(args);
            }
        } catch (Exception e) {
            logger.error("Unable to verify JWT: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify JWT");
        }
        return null;
    }

    /**
     * Point cut method to run when @Verify is used at join point
     */
    @Pointcut("@annotation(com.revature.aspects.Verify)")
    private void controllerMethodsPointCut() {
    }
}
