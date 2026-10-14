package com.roqhrcl.sealed;

import com.roqhrcl.common.error.BusinessException;
import com.roqhrcl.common.error.ErrorCode;
import com.roqhrcl.sealed.error.SealedErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorTestController {
    @GetMapping("/test/sealeds/{id}")
    String notFound(@PathVariable Long id) {
        throw new BusinessException(SealedErrorCode.SEALED_NOT_FOUND, "SEALED " + id + "를 찾을 수 없습니다.");
    }

    @GetMapping("/test/boom")
    String boom() {
        throw new IllegalStateException("secret-db-password");
    }
}
