package me.jinmin.querydsl.member;

import me.jinmin.querydsl.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    public void init(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void searchTest() throws Exception {

        MemberCond condition = MemberCond.builder()
                .teamName("teamB")
                .ageGoe(35)
                .ageLoe(40)
                .build();

        List<MemberTeamDto> result = memberRepository.searchByWhereParam(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchPageSimple() throws Exception {

        MemberCond condition = MemberCond.builder()
                .teamName("teamB")
                .ageGoe(35)
                .ageLoe(40)
                .build();
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }
}