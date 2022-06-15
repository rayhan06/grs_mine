package com.grs.api.exception_handler;

import java.util.HashMap;

public class ApiErrorMessages {
    public static final HashMap<Integer, String> ApiErrorMessagesMap = new HashMap<Integer, String>() {{
        put(ApiErrorEnum.NUMBER_FORMAT_EXCEPTION.getValue(), "Illegal format of number for: ");
        put(ApiErrorEnum.MISSING_PARAMETER_EXCEPTION.getValue(), "Missing required parameter: ");
        put(ApiErrorEnum.MISSING_OBJECT_EXCEPTION.getValue(), "Missing required object: ");
        put(ApiErrorEnum.BLACKLIST_EXCEPTION.getValue(), "Sorry, this complainant cannot complain to this office!");
    }};
}
