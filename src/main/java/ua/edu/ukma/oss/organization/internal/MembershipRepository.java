package ua.edu.ukma.oss.organization.internal;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("""
            select m from Membership m
            where m.orgId = :orgId and m.termStart <= :day and (m.termEnd is null or m.termEnd >= :day)
            order by m.position, m.id
            """)
    List<Membership> findActive(@Param("orgId") Long orgId, @Param("day") LocalDate day);

    @Query("""
            select count(m) > 0 from Membership m
            where m.userId = :userId and m.termStart <= :day and (m.termEnd is null or m.termEnd >= :day)
            """)
    boolean existsActive(@Param("userId") Long userId, @Param("day") LocalDate day);

    @Query("""
            select count(m) > 0 from Membership m
            where m.userId = :userId and m.orgId = :orgId and m.termStart <= :day
              and (m.termEnd is null or m.termEnd >= :day)
            """)
    boolean existsActiveInOrg(@Param("userId") Long userId, @Param("orgId") Long orgId, @Param("day") LocalDate day);

    /**
     * Finds terms of the same person in the same body that intersect the interval {@code [start, end]}.
     */
    @Query("""
            select count(m) > 0 from Membership m
            where m.userId = :userId and m.orgId = :orgId
              and m.termStart <= :end and (m.termEnd is null or m.termEnd >= :start)
            """)
    boolean existsOverlapping(@Param("userId") Long userId, @Param("orgId") Long orgId,
                              @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Membership> findByOrgIdOrderByTermStartDescIdDesc(Long orgId);
}
