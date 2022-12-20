package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {

        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);

        Member findMember = memberRepository.findById(member1.getId()).get();
        assertEquals(findMember, member1);

        List<Member> result1 = memberRepository.findAll();
        assertEquals(1, result1.size());

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertEquals(1, result2.size());

    }

}