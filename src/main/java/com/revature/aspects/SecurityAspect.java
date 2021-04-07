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
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Aspect
public class SecurityAspect {
    private Logger logger = Logger.getLogger(SecurityAspect.class);

    public static String getEnv(String key) {
        return System.getenv(key);
    }

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Around("controllerMethodsPointCut()")
    public Object verifyJwt(ProceedingJoinPoint pjp) throws Throwable {
        // PRODUCTION CODE
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        String auth = request.getHeader("Authorization");

        //WebClient webClient = WebClient.create("http://auth-service");

        try{
            logger.info("TEST TEST");
            UserDTO userDTO = webClientBuilder.build()
                    // This uses the auth service from consul
                    .post().uri("http://auth-service/verify")
                    .body(Mono.just(auth), String.class)
                    .retrieve()
                    .onStatus(httpStatus -> HttpStatus.UNAUTHORIZED.equals(httpStatus),
                            clientResponse -> {
                                Mono.empty();
                                return null;
                            })
                    .bodyToMono(UserDTO.class).block();
            logger.info(userDTO);
            if (userDTO.getId() != 0){
                logger.info("JWT verified: " + userDTO);
                Object[] args = pjp.getArgs();
                args[0] = userDTO;
                Object obj = pjp.proceed();
                return obj;
            }
        }catch(Exception e){
            logger.error("Unable to verify JWT: " + e.getMessage());
            response.sendError(401, "Unable to verify JWT");
        }
        return null;

        // THIS IS FOR TESTING (UNABLE TO FIGURE OUT HOW TO MOCK OUT ASPECT)
//        UserDTO userDTO = new UserDTO(1,"email@revature.com","trainer");
//        logger.info("JWT verified: " + userDTO);
//        Object[] args = pjp.getArgs();
//        args[0] = userDTO;
//        Object obj = pjp.proceed();
//        return obj;
    }

    @Pointcut("@annotation(com.revature.aspects.Verify)")
    private void controllerMethodsPointCut(){}
}
