package net.william278.backend.service;

import net.william278.backend.database.model.Transaction;
import org.jetbrains.annotations.NotNull;

public interface TransactionHandlerService {

    @NotNull
    EmailService getEmailService();

    default void sendTransactionEmail(@NotNull Transaction transaction) {
        if (transaction.isRefunded() || transaction.getProjectGrant() == null) {
            return;
        }
        getEmailService().sendTransactionEmail(transaction);
    }

}
