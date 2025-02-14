package sv.gov.cnr.factelectrcnrservice.models.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CNRPOS_USERS")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "USERS_SEQ", allocationSize = 1)
    @Column(name = "ID_USER")
    private Long idUser;
    private String firstname;
    private String lastname;
    private String email;
    private Long docType;
    private String docNumber;
    private String password;
    private String phone;
    private Boolean isActive;
    private Boolean testMode;
    private Long idCompany;
    private Long idBranch;
    private Long idPosition;

    @Column(name = "CREATED_AT")
    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "UPDATED_AT")
    @UpdateTimestamp
    private Timestamp updatedAt;

    @Column(name = "DELETED_AT")
    private Timestamp deletedAt;


    @Transient
    private List<Long> rolIds;

//    @Enumerated(EnumType.STRING)
//    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "CNRPOS_USERS_ROLES", joinColumns = @JoinColumn(name = "ID_USER"), inverseJoinColumns = @JoinColumn(name = "ID_ROLE"))
    private Set<Rol> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return List.of(new SimpleGrantedAuthority(role.name()));
        System.out.println("GrantedAuthority - Obteniendo roles: " + getRoless());
        return null;
    }

    //    @Override
    public List<Long> getRoless() {
//        return List.of(getRoles().)
        return roles.stream().map(Rol::getIdRole).collect(Collectors.toList());


    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
