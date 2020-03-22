package com.sproutt.eussyaeussyaapi.api.oauth2.service;

import com.sproutt.eussyaeussyaapi.api.oauth2.dto.GithubOAuth2UserDto;
import com.sproutt.eussyaeussyaapi.api.oauth2.dto.GoogleOAuth2UserDto;
import com.sproutt.eussyaeussyaapi.domain.member.Member;
import com.sproutt.eussyaeussyaapi.domain.member.MemberRepository;
import com.sproutt.eussyaeussyaapi.domain.member.exceptions.DuplicationMemberException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Service
public class GoogleOAuth2Service implements OAuth2Service {

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;

    @Value("${social.google.url}")
    private String requestUrl;

    private GoogleOAuth2UserDto getGithubUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity request = new HttpEntity(headers);
        try {
            ResponseEntity<GoogleOAuth2UserDto> response = restTemplate
                .exchange(requestUrl, HttpMethod.GET, request, GoogleOAuth2UserDto.class);

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException();
    }

    @Override
    public Member getMemberInfo(String accessToken) {
        GoogleOAuth2UserDto googleOAuth2UserDto = getGithubUserInfo(accessToken);

        return memberRepository.findByMemberId(googleOAuth2UserDto.getId()).orElseThrow(RuntimeException::new);
    }

    @Override
    public Member createMember(String accessToken) {
        GoogleOAuth2UserDto googleOAuth2User = getGithubUserInfo(accessToken);

        Optional<Member> member = memberRepository.findByMemberId(googleOAuth2User.getId());

        if (member.isPresent()) {
            throw new DuplicationMemberException();
        }

        return memberRepository.save(googleOAuth2User.toEntity());
    }

    @Override
    public String getProvider() {
        return "google";
    }
}
