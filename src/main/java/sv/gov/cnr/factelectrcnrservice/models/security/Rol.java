package sv.gov.cnr.factelectrcnrservice.models.security;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "CNRPOS_ROLES")
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_seq")
    @SequenceGenerator(name = "roles_seq", sequenceName = "ROLES_SEQ", allocationSize = 1)
    @Column(name = "ID_ROLE", nullable = false)
    private Long idRole;

    @Column(name = "NAME_ROLE", length = 150)
    private String nameRole;

    @Column(name = "DESCRIPTION_ROLE", length = 250)
    private String descriptionRole;

    @Column(name = "CREATED_AT")
    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "UPDATED_AT")
    @UpdateTimestamp
    private Timestamp updatedAt;

    @Column(name = "DELETED_AT")
    private Timestamp deletedAt;

    @Transient
    private List<Long> permissionIds;

    @ManyToMany
    private Set<User> user;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "CNRPOS_ROLES_PERMISOS", joinColumns = @JoinColumn(name = "ID_ROLE", referencedColumnName = "ID_ROLE"), inverseJoinColumns = @JoinColumn(name = "ID_PERMISSIONS", referencedColumnName = "ID_PERMISSIONS"))
    private Set<Permission> permission;

    // Constructors, getters, and setters
}
