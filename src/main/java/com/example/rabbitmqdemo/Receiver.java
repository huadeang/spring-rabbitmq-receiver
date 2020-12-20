package com.example.rabbitmqdemo;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingExchangeFilterFunction;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.print.attribute.standard.Media;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

@Component
public class Receiver {

    @Autowired
    private Tracer tracer;

    @Value("${receiver.baseurl}")
    private String baseUrl;

    private CountDownLatch latch = new CountDownLatch(1);

    public void receiveMessage(String message) {
        System.out.println("Received <" + message + ">");
//        WebClient.UriSpec<WebClient.RequestBodySpec> request1 = createWebClientWithServerURLAndDefaultValues().post();
        WebClient.RequestBodySpec reqUri1 = createWebClientWithServerURLAndDefaultValues().post().uri("/receive");
        BodyInserter<String, ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue(message);

        WebClient.ResponseSpec res = reqUri1.body(inserter)
                .header(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .acceptCharset(Charset.forName("UTF-8"))
                .ifNoneMatch("*")
                .ifModifiedSince(ZonedDateTime.now())
                .retrieve();

        String resMsg = res.bodyToMono(String.class).block();
        System.out.println("Sending message to API success " + resMsg);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    private WebClient createWebClientWithServerURLAndDefaultValues() {
//        String url = "http://localhost:8080";

        return WebClient.builder()
                .filter(new TracingExchangeFilterFunction(tracer, Collections.singletonList(new WebClientSpanDecorator.StandardTags())))
                .baseUrl(baseUrl)
                .defaultCookie("cookieKey", "cookieValue")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultUriVariables(Collections.singletonMap("url", baseUrl))
                .build();
    }
}
