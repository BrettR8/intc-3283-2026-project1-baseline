package edu.northwestu.project1p2.repositories;

import edu.northwestu.project1p2.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    /** Derived query: Spring Data escapes LIKE wildcards in the arguments. */
    List<User> findTop50ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByUsernameAsc(
            String username, String email);

    List<User> findTop10ByOrderByCreatedAtDesc();
}
