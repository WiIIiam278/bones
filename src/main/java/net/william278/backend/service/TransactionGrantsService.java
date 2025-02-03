package net.william278.backend.service;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.model.Transaction;
import net.william278.backend.database.model.User;
import net.william278.backend.database.repository.TransactionRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionGrantsService {

    private final TransactionRepository transactions;

    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = Exception.class)
    public User applyTransactionGrants(@NotNull User user) {
        final String e = user.getEmail();
        if (e == null || !user.isEmailVerified()) {
            return user;
        }

        final Iterable<Transaction> search = transactions
                .findAllByEmailAndGrantedToIsNullAndPassedValidationIsTrueAndRefundedIsFalseAndProjectGrantIsNotNull(e);
        final Set<Project> projectGrants = Sets.newHashSet();
        final Set<Transaction> toSave = Sets.newHashSet();
        search.forEach(completed -> {
            completed.setGrantedTo(user);
            if (completed.getProjectGrant() != null) {
                projectGrants.add(completed.getProjectGrant());
            }
            toSave.add(completed);
        });
        transactions.saveAll(toSave);
        user.addPurchases(projectGrants);
        log.info("Applied project grants for user from transactions {}: {}", user.getName(),
                projectGrants.stream().map(Project::getSlug).toList());
        return user;
    }

}
