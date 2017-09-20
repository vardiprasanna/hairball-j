package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oath.gemini.merchant.HttpStatus;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EWSResponseData<T> extends HttpStatus {
    private String errors;
    private String timestamp;

    @Override
    public void setMessage(String msg) {
        super.setMessage(msg);
        if (StringUtils.isBlank(errors)) {
            errors = msg;
        }
    }

    @JsonIgnore
    private T[] objects; // will be loaded separately

    public T get(int index) {
        return objects[index];
    }

    public int size() {
        return objects == null ? 0 : objects.length;
    }

    @SuppressWarnings("unchecked")
    public Stream<T> stream() {
        if (objects != null) {
            return Arrays.stream(objects);
        }
        return Arrays.stream((T[]) (new Object[0]));
    }

    public static <E> boolean isEmpty(EWSResponseData<E> data) {
        return data == null || data.size() == 0;
    }

    public static <E> boolean isNotEmpty(EWSResponseData<E> data) {
        return data != null && data.size() > 0;
    }
}
