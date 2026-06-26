package com.stagelog.Stagelog.user.dto;

import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String publicId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Provider provider;
    private Boolean isSocial;
    private Boolean emailNotificationEnabled;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getPublicId().toString(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getProvider(),
                user.getProvider() != Provider.LOCAL,
                user.getEmailNotificationEnabled()
        );
    }
}
