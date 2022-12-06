package study.querydsl.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor //JPA에서는 protected 까지 열려있음, private 쓰면 에러남
public class Team {

    @ToString.Include
    @Id
    @GeneratedValue
    @Column(name = "team_id", nullable = false)
    private Long id;
    @ToString.Include
    private String name;

    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>(); //없으면 null pointer Exception 에러가 난다.

    public Team(String name) {
        this.name = name;
    }

}
