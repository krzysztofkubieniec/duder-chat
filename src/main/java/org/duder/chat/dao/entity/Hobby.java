package org.duder.chat.dao.entity;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Hobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "hobbies")
    private Set<User> users = new HashSet<>();

    @ManyToMany(mappedBy = "hobbies")
    private Set<Event> hobbies = new HashSet<>();
}
