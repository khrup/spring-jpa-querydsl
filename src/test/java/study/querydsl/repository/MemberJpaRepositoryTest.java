package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {

        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Member findMember = memberJpaRepository.findById(member1.getId()).get();
        assertEquals(findMember, member1);

        List<Member> result1 = memberJpaRepository.findAll();
        assertEquals(1, result1.size());

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertEquals(1, result2.size());

    }

    @Test
    public void basicTestQuerydsl() {

        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Member findMember = memberJpaRepository.findById(member1.getId()).get();
        assertEquals(findMember, member1);

        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
        assertEquals(1, result1.size());

        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");
        assertEquals(1, result2.size());

    }

    @Test
    public void searchTest() {

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

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setTeamName("teamB");
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertNotEquals(0, result.size());

    }
}