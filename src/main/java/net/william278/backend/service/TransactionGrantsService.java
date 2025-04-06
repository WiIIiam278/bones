/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
