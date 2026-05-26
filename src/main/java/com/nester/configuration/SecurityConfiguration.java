// SecurityConfiguration.java
package com.nester.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.security.jwt.JwtConfigurer;
import com.nester.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final JwtTokenProvider jwtTokenProvider;
    private ObjectMapper objectMapper;

    public SecurityConfiguration(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // Preflight OPTIONS — всегда разрешён
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Публичные эндпоинты
                .antMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .antMatchers(HttpMethod.POST, "/auth/logout").authenticated()

                // Администратор
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/event-logs/**").hasRole("ADMIN")

                // Менеджер
                .antMatchers("/manager/**").hasRole("MANAGER")

                // Материалы
                .antMatchers(HttpMethod.GET, "/materials/**").authenticated()
                .antMatchers(HttpMethod.POST, "/materials/**").hasAnyRole("ADMIN", "WORKER")
                .antMatchers(HttpMethod.PUT, "/materials/**").hasAnyRole("ADMIN", "WORKER")
                .antMatchers(HttpMethod.DELETE, "/materials/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/materials/*/batch").hasAnyRole("ADMIN", "WORKER")

                // Заявки
                .antMatchers(HttpMethod.GET, "/requests/incoming").hasAnyRole("WORKER", "MANAGER", "ADMIN")
                .antMatchers(HttpMethod.GET, "/requests/all").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/requests").authenticated()
                .antMatchers(HttpMethod.PUT, "/requests/**").authenticated()
                .antMatchers(HttpMethod.POST, "/requests/*/status").authenticated()
                .antMatchers(HttpMethod.POST, "/requests/*/confirm").hasRole("EMPLOYEE")
                .antMatchers(HttpMethod.POST, "/requests/*/archive").authenticated()

                // Заказы (справочник для RECEIPT)
                .antMatchers(HttpMethod.GET, "/orders/**").authenticated()
                .antMatchers(HttpMethod.POST, "/orders/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/orders/**").hasAnyRole("ADMIN", "MANAGER")
                .antMatchers(HttpMethod.DELETE, "/orders/**").hasRole("ADMIN")

                // Пользователь
                .antMatchers(HttpMethod.GET, "/user/me").authenticated()

                // Остатки на производственном участке
                .antMatchers(HttpMethod.GET, "/inventory/production-line").hasAnyRole("EMPLOYEE", "ADMIN")

                // Дашборд
                .antMatchers(HttpMethod.GET, "/dashboard/**").authenticated()

                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Не авторизован", null));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
                })
                .and()
                .apply(new JwtConfigurer(jwtTokenProvider));
    }
}