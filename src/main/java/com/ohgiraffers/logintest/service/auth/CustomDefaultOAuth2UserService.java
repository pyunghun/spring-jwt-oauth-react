package com.ohgiraffers.logintest.service.auth;


import com.ohgiraffers.logintest.advice.assertThat.DefaultAssert;
import com.ohgiraffers.logintest.config.security.auth.OAuth2UserInfo;
import com.ohgiraffers.logintest.config.security.auth.OAuth2UserInfoFactory;
import com.ohgiraffers.logintest.config.security.token.UserPrincipal;
import com.ohgiraffers.logintest.domain.entity.user.Provider;
import com.ohgiraffers.logintest.domain.entity.user.Role;
import com.ohgiraffers.logintest.domain.entity.user.User;
import com.ohgiraffers.logintest.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomDefaultOAuth2UserService extends DefaultOAuth2UserService{
    
    private final UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        System.out.println("oAuth2User = " + oAuth2User);
        System.out.println("oAuth2User.getName() = " + oAuth2User.getName());
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception e) {
            DefaultAssert.isAuthentication(e.getMessage());
        }
        return null;
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        DefaultAssert.isAuthentication(!oAuth2UserInfo.getEmail().isEmpty());
        
        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            DefaultAssert.isAuthentication(user.getProvider().equals(Provider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId())));
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = User.builder()
                    .provider(Provider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))
                    .providerId(oAuth2UserInfo.getId())
                    .name(oAuth2UserInfo.getName())
                    .email(oAuth2UserInfo.getEmail())
                    .imageUrl(oAuth2UserInfo.getImageUrl())
                    .role(Role.USER)
                    .build();
        
        return userRepository.save(user);
    }

    private User updateExistingUser(User user, OAuth2UserInfo oAuth2UserInfo) {

        user.updateName(oAuth2UserInfo.getName());
        user.updateImageUrl(oAuth2UserInfo.getImageUrl());

        return userRepository.save(user);
    }
}
