package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.repositories.UserRepository;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Backs the dashboard's "find users" panel. */
@Service
public class UserDirectoryService {

    /** Longest search term accepted; anything longer is truncated. */
    static final int MAX_QUERY_LENGTH = 100;

    private final UserRepository userRepository;

    public UserDirectoryService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Users whose username or email contains {@code query} (case-insensitive); recent users when blank. */
    public List<UserSummary> search(final String query) {
        final String term = normalizeQuery(query);
        if (term.isEmpty()) {
            return this.recent();
        }
        return this.userRepository
                .findTop50ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByUsernameAsc(term, term)
                .stream()
                .map(UserSummary::from)
                .toList();
    }

    /** The ten most recently registered users. */
    public List<UserSummary> recent() {
        return this.userRepository.findTop10ByOrderByCreatedAtDesc().stream().map(UserSummary::from).toList();
    }

    public long count() {
        return this.userRepository.count();
    }

    static String normalizeQuery(final @Nullable String query) {
        if (query == null) {
            return "";
        }
        final String trimmed = query.trim();
        return trimmed.length() > MAX_QUERY_LENGTH ? trimmed.substring(0, MAX_QUERY_LENGTH) : trimmed;
    }
}
