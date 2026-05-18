package com.chushi.template.controller;

import com.chushi.template.commons.vo.Response;

public class BaseController {
    protected <T> Response<T> wrap(T payload) {
        return Response.<T>builder()
                .code(0)
                .message("")
                .data(payload)
                .build();
    }

    protected Response<Void> wrap() {
        return wrap(null);
    }
}
