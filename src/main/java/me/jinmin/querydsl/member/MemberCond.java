package me.jinmin.querydsl.member;

import lombok.Builder;
import lombok.Data;

@Data
public class MemberCond {
    //회원명, 팀명, 나이(goe, loe)

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

    @Builder
    public MemberCond(String teamName, Integer ageGoe, Integer ageLoe) {
        this.teamName = teamName;
        this.ageGoe = ageGoe;
        this.ageLoe = ageLoe;
    }
}
