package com.hutech.demo;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(com.hutech.demo.service.UserService userService) {
        return userService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        var auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http,
                                                   DaoAuthenticationProvider authenticationProvider,
                                                   UserDetailsService userDetailsService) throws Exception {
        return http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/webfonts/**", "/product-images/**",
                                "/", "/oauth/**", "/register", "/login", "/error", "/403",
                                "/products", "/products/{id}", "/tabs/**", "/cart", "/cart/**",
                                "/order/checkout", "/order/submit", "/order/confirmation",
                                "/api/**", "/home/**")
                        .permitAll()
                        .requestMatchers(
                                "/products/add", "/products/edit/**", "/products/update/**")
                        .hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers(
                                "/products/delete/**", "/products/fix-images", "/categories/**", "/orders/**", "/admin/**")
                        .hasAnyAuthority("ADMIN")
                        .requestMatchers("/loyalty/**")
                        .hasAnyAuthority("USER")
                        .anyRequest().permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("hutech")
                        .rememberMeCookieName("hutech")
                        .tokenValiditySeconds(24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedPage("/403")
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .maximumSessions(1)
                        .expiredUrl("/login")
                )
                .httpBasic(httpBasic -> httpBasic
                        .realmName("hutech")
                )
                .build();
    }
}
