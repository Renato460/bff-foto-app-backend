package com.boda.bfffotoappbackend.service.impl;

import com.boda.bfffotoappbackend.auth.dto.LoginRequest;
import com.boda.bfffotoappbackend.auth.dto.SupabaseAuthResponse;
import com.boda.bfffotoappbackend.dto.Profile;
import com.boda.bfffotoappbackend.security.JwtService;
import com.boda.bfffotoappbackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final WebClient supabaseWebClient;
    private final JwtService jwtService;
    private final String supabaseServiceKey; // Añadimos la Service Key

    @Autowired
    public AuthServiceImpl(WebClient supabaseWebClient, JwtService jwtService,
                           @Value("${supabase.service.key}") String supabaseServiceKey) {
        this.supabaseWebClient = supabaseWebClient;
        this.jwtService = jwtService;
        this.supabaseServiceKey = supabaseServiceKey;

    }

    @Override
    public String login(LoginRequest request) {
        // Cuerpo de la petición para autenticar en Supabase
        Map<String, String> requestBody = Map.of(
                "email", request.getEmail(),
                "password", request.getPassword()
        );

        // Llamamos al endpoint de autenticación de Supabase
        // Si las credenciales son incorrectas, Supabase devuelve un 4xx y WebClient lanzará una excepción
        try {
            SupabaseAuthResponse supabaseAuthResponse = supabaseWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/v1/token")
                            .queryParam("grant_type", "password")
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new BadCredentialsException("Email o contraseña inválidos.")))
                    .bodyToMono(SupabaseAuthResponse.class)
                    .block(); // Hacemos la llamada bloqueante para este caso de uso simple

            if (supabaseAuthResponse == null || supabaseAuthResponse.getUser() == null || supabaseAuthResponse.getUser().getId() == null) {
                throw new BadCredentialsException("No se pudo obtener el ID de usuario de Supabase.");
            }
            // --- NUEVO: OBTENER EL ROL DEL USUARIO ---
            // Usamos la Service Key para leer la tabla 'profiles' de forma segura

            String userId = supabaseAuthResponse.getUser().getId();

            List<Profile> profiles = supabaseWebClient.get()
                    .uri("/rest/v1/profiles?select=role&id=eq." + userId)
                    .header("Authorization", "Bearer " + this.supabaseServiceKey)
                    .retrieve()
                    .bodyToFlux(Profile.class)
                    .collectList()
                    .block();

            // Extraemos el rol, si no existe le asignamos 'guest' por defecto
            String role = Optional.ofNullable(profiles)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0).getRole())
                    .orElse("guest");

            // Si la llamada fue exitosa (código 2xx), generamos nuestro propio JWT
            return jwtService.generateToken(request.getEmail(), role, userId);

        } catch (Exception e) {
            // Capturamos cualquier error de la llamada y lo relanzamos como un error de credenciales
            throw new BadCredentialsException("Error de autenticación: Email o contraseña inválidos.");
        }
    }
}
