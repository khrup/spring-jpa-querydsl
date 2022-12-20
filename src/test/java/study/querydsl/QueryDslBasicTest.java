package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach //테스트 시작전 실행됨
    public void before() {

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
    public void startJPQL() {
        //member1을 찾아라

        String query = "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(query, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertEquals("member1", findMember.getUsername());
    }

    @Test
    public void startQueryDsl() {

        QMember member1 = new QMember("member~~~"); //해당 방법은 같은 테이블을 조인할때 사용한다.

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertEquals("member1", findMember.getUsername());
    }

    @Test
    public void resultFetch() {

        Member findMember = queryFactory.selectFrom(member).fetchOne();

        List<Member> findMemberList = queryFactory.selectFrom(member).fetch();

//        Member findMemberOne = queryFactory.selectFrom(member).fetchFirst();
        int size = findMemberList.size();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 2에서 회원이름이 없으면 마지막에 출력 (nulls last : null 은 마지막에 출력)
     */
    @Test
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member("member7", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertEquals("member5", result.get(0).getUsername());
        assertEquals("member6", result.get(1).getUsername());
        assertEquals("member7", result.get(2).getUsername());
        assertNull(result.get(3).getUsername());
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(0은 1페이지 , 1은 2페이지다.)
                .limit(2) //몇개 가져올 것인지
                .fetch();

        assertEquals(2, result.size());
    }

    @Test
    public void paging2() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작
                .limit(2) //몇개 가져올 것인지
                .fetch();

        //카운트쿼리는 별도로 작성하는것이 성능상 더 좋음
        int totalCount = queryFactory
                .selectFrom(member)
                .fetch().size();

        assertEquals(2, result.size());
        assertEquals(4, totalCount);
    }


    @Test
    public void search() {
        Member findMember = queryFactory.selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30)
                )
                .fetchOne();

        assertEquals("member1", findMember.getUsername());

    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.min()
                ).from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertEquals(4, tuple.get(member.count()));
        assertEquals(80, tuple.get(member.age.sum()));
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     *
     * @throws Exception
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertEquals("teamA", teamA.get(team.name));
        assertEquals("teamB", teamB.get(team.name));
    }

    /**
     * 팀A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        System.out.println("result = " + result);
    }

    /**
     * 세타조인 : entity 끼리 매핑이 없을경우 사용
     * 회원의 이름이 팀 이름과 같은 회원 조회 , 단점 외부 조인이 안됨
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .selectFrom(member)
                .from(member, team) //from 에 테이블 나열
                .where(member.username.eq(team.name))
                .fetch();

        System.out.println("result = " + result);

    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member, team)
                .where(member.team.id.eq(team.id), team.name.eq("teamA")).fetch();

        for (Tuple member :
                result) {
            System.out.println("member = " + member);
        }

        for (Tuple member :
                result2) {
            System.out.println("member2 = " + member);
        }

    }

    /**
     * 연관관계 없는 엔티티 외부조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.team.id.eq(team.id))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertFalse(loaded);
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertTrue(loaded);
    }


    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

    }

    /**
     * 나이가 10살 이상인 사람들의 나이에 해당하는 회원
     */
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

    }

    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.
                select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(20, 30)).then("20~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //프로젝션 : select 대상 지정
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    //바깥으로 데이터를 보낼때는 dto로 보낸다.
    @Test
    public void tupleProjection() {

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findBySetter() {
        List<MemberDto> result = queryFactory
                //Projections.bean : getter setter 를 통해 값을 넣고 뺀다.
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findByField() {
        List<MemberDto> result = queryFactory
                //Projections.fields : 필드를 통해 값을 넣고 뺀다.
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findByConstruct() {
        List<MemberDto> result = queryFactory
                //Projections.fields : 필드를 통해 값을 넣고 뺀다.
                //런타임시점에 오류를 발견하여 별로 안좋음, 컴파일시점에 오류나는게 좋음
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findByUserDto() {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                //Projections.fields : 필드를 통해 값을 넣고 뺀다.
                .select(Projections.constructor(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                //컴파일시점에 오류를 알수 있음. 좋은 설계방식(@QueryProjection 사용)
                //@QueryProjection 에 의존하고 사용 용다는 단점이 있음.
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertEquals(1, result.size());
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertEquals(1, result.size());
    }

    //메소드를 통해 다른쿼리에서도 재활용이 가능하다.
    //쿼리 자체의 가독성이 높아진다.
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                //where 에 null 이 들어가면 무시된다
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        } else {
            return member.username.eq(usernameCond);
        }
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //대용량 삭제 및 수정은 영속성 컨텍스트를 거치지 않고 바로 DB에 접근하여 수정 및 삭제쿼리를 날린다.
    //당연히 Transaction 이 끝나야 commit 이 날라가는건 같다.
    @Test
    public void bulkUpdate() {

        //쿼리 실행 되기 전
        //member1 = 10 -> member1
        //member2 = 20 -> member2
        //member3 = 30 -> member3
        //member4 = 40 -> member4
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //쿼리 실행 되기 후
        //영속성 컨텍스트 : member1 ,  DB : 비회원
        //영속성 컨텍스트 : member2 ,  DB : 비회원
        //영속성 컨텍스트 : member3 ,  DB : member3
        //영속성 컨텍스트 : member4 ,  DB : member4
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result1) {
            System.out.println("member1 = " + member1);
        }

        //해당 이슈를 해결하기 위한 방법 :
        em.flush(); //영속성 컨텍스트에 있는 데이터를 DB 로 보낸다.
        em.clear(); //영속성 컨텍스트 내용을 클리어한다.

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member2 : result2) {
            System.out.println("member2 = " + member2);
        }
    }

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))//위에 라인과 같은 기능
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}