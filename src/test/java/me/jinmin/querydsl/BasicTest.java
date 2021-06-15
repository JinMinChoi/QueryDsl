package me.jinmin.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.jinmin.querydsl.member.Member;
import me.jinmin.querydsl.member.MemberDto;
import me.jinmin.querydsl.member.QMember;
import me.jinmin.querydsl.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static me.jinmin.querydsl.member.QMember.member;
import static me.jinmin.querydsl.team.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class BasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() throws Exception {
        queryFactory = new JPAQueryFactory(em);

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
    public void jpql_시작() throws Exception {

        String qlString = "select m from Member m where m.username = :username";

        List<Member> results = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getResultList();

        assertThat(results.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    public void querydsl_시작() throws Exception {

        QMember m = member;

        List<Member> results = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetch();

        assertThat(results.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    public void 조회() throws Exception {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetch();

        for (Member member1 : findMember) {
            System.out.println("member1 = " + member1);
        }

        assertThat(findMember.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    public void groupBy() throws Exception {
        List<Tuple> results = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    public void 조인() throws Exception {
        List<Member> userOfTeamA = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(userOfTeamA)
                .extracting("username")
                .containsExactly("member1", "member2");

        for (Member result : userOfTeamA) {
            System.out.println("result = " + result);
        }

    }

    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> res = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                //.where(team.isNotNull()) // left outer join : team이 null인 경우는 결과 X
                .fetch();

        for (Tuple re : res) {
            System.out.println("re = " + re);
        }
    }

    @Test
    public void basicCase() throws Exception {

        List<String> res = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("스무살 이상")
                )
                .from(member)
                .fetch();

        for (String re : res) {
            System.out.println("re = " + re);
        }
    }

    @Test
    public void complexCase() throws Exception {
        StringExpression explain = new CaseBuilder()
                .when(member.age.between(0, 20)).then("어린애")
                .when(member.age.between(21, 30)).then("중간 어른")
                .otherwise("어른");

        List<Tuple> res = queryFactory
                .select(member.username, member.age, explain)
                .from(member)
                .orderBy(explain.desc())
                .fetch();

        for (Tuple re : res) {
            System.out.println("re = " + re);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        List<MemberDto> res = queryFactory
                .select(Projections.constructor(
                        MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto re : res) {
            System.out.println("re = " + re);
        }
    }

    @Test
    public void findUserDtoUsedAlias() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<MemberDto> res = queryFactory
                .select(Projections.constructor(
                        MemberDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                select(memberSub.age.max())
                                        .from(memberSub),
                                "age")
                        )
                )
                .from(member)
                .fetch();

        for (MemberDto re : res) {
            System.out.println("re = " + re);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> res = searchMember(usernameParam, ageParam);

        assertThat(res.size()).isEqualTo(1);
    }

    private List<Member> searchMember(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }

        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> res = searchMemberWhere(usernameParam, ageParam);

        assertThat(res.size()).isEqualTo(1);

    }

    private List<Member> searchMemberWhere(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam), ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam == null ? null : member.username.eq(usernameParam);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam == null ? null : member.age.eq(ageParam);
    }

    private Predicate usernameAndAgeEq(String usernameParam, Integer ageParam) {
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

}
