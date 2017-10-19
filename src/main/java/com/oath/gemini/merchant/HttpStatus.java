package com.oath.gemini.merchant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author tong on 10/1/2017
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class HttpStatus {
    private int status = 200;
    private String message;

    public boolean isOk() {
        return (status >= 200 && status < 300);
    }
}
