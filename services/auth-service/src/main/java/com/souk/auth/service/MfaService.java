package com.souk.auth.service;

import com.souk.auth.api.dto.MfaSetupResponse;
import com.souk.common.adapters.jpa.repository.UserRepository;
import com.souk.common.domain.User;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final String ISSUER = "SoukConect";

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public MfaService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public MfaSetupResponse generateSetup(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        String secret = secretGenerator.generate();

        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUrl;
        try {
            byte[] imageData = qrGenerator.generate(qrData);
            qrCodeUrl = getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code: {}", e.getMessage());
            qrCodeUrl = null;
        }

        // Format secret for manual entry (add spaces every 4 characters)
        String manualEntryKey = formatSecretForDisplay(secret);

        return new MfaSetupResponse(secret, qrCodeUrl, manualEntryKey);
    }

    @Transactional
    public boolean enableMfa(Long userId, String secret, String code) {
        // Verify the code before enabling
        if (!verifyCode(secret, code)) {
            log.debug("Invalid MFA code during setup for user: {}", userId);
            return false;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        // Send confirmation email
        emailService.sendMfaEnabledEmail(user.getEmail());

        log.info("MFA enabled for user: {}", userId);
        return true;
    }

    @Transactional
    public boolean disableMfa(Long userId, String code) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (!user.getMfaEnabled()) {
            return true; // Already disabled
        }

        // Verify current code before disabling
        if (!verifyCode(user.getMfaSecret(), code)) {
            log.debug("Invalid MFA code during disable for user: {}", userId);
            return false;
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        log.info("MFA disabled for user: {}", userId);
        return true;
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code);
    }

    public boolean verifyCodeForUser(Long userId, String code) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!user.getMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }

        return verifyCode(user.getMfaSecret(), code);
    }

    private String formatSecretForDisplay(String secret) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(secret.charAt(i));
        }
        return formatted.toString();
    }
}
