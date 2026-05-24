package com.nester.security.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nester.model.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class JwtUser implements UserDetails {
    @JsonIgnore
    private String id;
    private String login;
    private String password;
    private List<GrantedAuthority> authorities = new ArrayList<>();

    public JwtUser(User user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.password = user.getPassword();

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
        this.authorities.add(authority);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getId() {
        return id;
    }
}
