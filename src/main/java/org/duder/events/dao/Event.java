package org.duder.events.dao;

import lombok.*;
import org.duder.user.dao.Hobby;
import org.duder.user.dao.UserEvent;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Name should not be null but is not necessarily unique - not a natural id
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Timestamp timestamp;

    @ManyToMany
    @JoinTable(name = "event_hobby"
            , joinColumns = {@JoinColumn(name = "id_event")}
            , inverseJoinColumns = {@JoinColumn(name = "id_hobby")})
    private Set<Hobby> hobbies = new HashSet<>();

    // Users participating/interested in the event
    @OneToMany(mappedBy = "primaryKey.event", cascade = CascadeType.ALL)
    private List<UserEvent> eventUsers = new ArrayList<>();
}