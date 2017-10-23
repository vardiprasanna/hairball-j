package com.oath.gemini.merchant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@NoArgsConstructor
@Getter @Setter
public class HttpStatus {
    private int status = 200;
    private String brief;
    private String message;

    public boolean isOk() {
        return (status >= 200 && status < 300);
    }
}
