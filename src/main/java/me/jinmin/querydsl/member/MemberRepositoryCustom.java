package me.jinmin.querydsl.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> searchByWhereParam(MemberCond cond);

    //전체 카운트를 한 번에 조회하는 단순한 방법
    Page<MemberTeamDto> searchPageSimple(MemberCond condition, Pageable pageable);

    //데이터 내용과 전체 카운트를 별도로 조회하는 방법
    Page<MemberTeamDto> searchPageComplex(MemberCond condition, Pageable pageable);
}
