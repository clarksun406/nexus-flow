package com.nexusflow.listener;

import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import com.nexusflow.domain.wallet.AddressPoolStatus;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Processes scanned blockchain transactions:
 * - Matches incoming transfers to managed wallets
 * - Triggers payment detection / confirmation flow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProcessor {

    private final WalletRepository walletRepository;
    private final AddressPoolRepository addressPoolRepository;
    private final PaymentApplicationService paymentService;

    public void process(ScannedTransaction tx, Chain chain) {
        Optional<AddressPoolEntry> poolAddress = addressPoolRepository.findByAddress(tx.getToAddress());
        Optional<Wallet> targetWallet = walletRepository.findByAddress(tx.getToAddress());
        if (poolAddress.isEmpty() && targetWallet.isEmpty()) {
            log.trace("Ignoring tx {} - not to our wallet", tx.getTxHash());
            return;
        }

        if (poolAddress.isPresent() && poolAddress.get().getStatus() == AddressPoolStatus.DISABLED) {
            log.trace("Ignoring tx {} - address {} is disabled", tx.getTxHash(), tx.getToAddress());
            return;
        }
        if (targetWallet.isPresent()) {
            Wallet wallet = targetWallet.get();
            if (!wallet.isActive()) {
                log.trace("Ignoring tx {} - wallet {} is inactive", tx.getTxHash(), wallet.getId());
                return;
            }
        }

        log.info("Detected incoming transaction: txHash={}, to={}, amount={}",
                tx.getTxHash(), tx.getToAddress(), tx.getAmount());

        // Notify application layer about the detected transaction
        // The application layer will handle matching to specific payments
        paymentService.onPaymentDetected(
                tx.getTxHash(),
                tx.getToAddress(),
                tx.getAmount(),
                resolveCurrency(chain, tx.getContractAddress()),
                tx.getBlockNumber()
        );
    }

    private String resolveCurrency(Chain chain, String contractAddress) {
        // Map chain + contract to currency string
        // For MVP, assume TRC20 USDT
        return switch (chain) {
            case TRON -> "USDT_TRC20";
            case ETH -> "USDT_ERC20";
            default -> chain.name();
        };
    }
}
