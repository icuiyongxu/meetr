package com.meetr.exception;

import com.meetr.domain.vo.RuleViolation;
import lombok.Getter;

import java.util.List;

@Getter
public class RuleViolationException extends BusinessException {

    private final List<RuleViolation> violations;

    public RuleViolationException(String message, List<RuleViolation> violations) {
        super(40022, message);
        this.violations = violations;
    }
}
