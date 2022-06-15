package com.grs.api.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Service
public class ReadFromProperty {
    String result = "";
    InputStream inputStream;

    public String getPropValues(String name, String propFileName) throws IOException {

        try {
            Properties prop = new Properties();
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
            result = prop.getProperty(name);

        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            inputStream.close();
        }
        return result;
    }
}
