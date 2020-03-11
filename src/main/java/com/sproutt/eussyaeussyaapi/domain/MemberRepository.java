package com.sproutt.eussyaeussyaapi.domain;

import com.sproutt.eussyaeussyaapi.infrastructure.MemberCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {

    Optional<Member> findByMemberId(String memberId);
}
