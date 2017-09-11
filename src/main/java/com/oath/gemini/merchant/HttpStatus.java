package com.oath.gemini.merchant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpStatus {
    private int status = 200;
    private String message;

    public boolean isOk() {
        return (status >= 200 && status < 300);
    }
}
