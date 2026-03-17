package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.commons.vo.Response;

public class BaseController {
    protected <T> Response<T> wrap(T payload) {
        Response<T> response = new Response<>();
        response.setCode(0);
        response.setMessage("");
        response.setData(payload);
        return response;
    }

    protected Response<Void> wrap() {return wrap(null); }
}
