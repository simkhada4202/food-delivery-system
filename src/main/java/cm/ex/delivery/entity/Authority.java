package cm.ex.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "authority")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authority;

    private String requestAuthority;

    private int level;

    private String authorityAccept;

    public Authority(String authority, int level, String authorityAccept) {
        this.authority = authority;
        this.level = level;
        this.authorityAccept = authorityAccept;
    }
}
